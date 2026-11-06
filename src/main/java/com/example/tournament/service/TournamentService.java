package com.example.tournament.service;

import com.example.tournament.dto.Standing;
import com.example.tournament.dto.TournamentRequest;
import com.example.tournament.model.ApplicationStatus;
import com.example.tournament.model.Match;
import com.example.tournament.model.MatchStatus;
import com.example.tournament.model.TeamApplication;
import com.example.tournament.model.Tournament;
import com.example.tournament.model.TournamentStatus;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TeamApplicationRepository;
import com.example.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@PreAuthorize("hasRole('ORGANIZER')")
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TeamApplicationRepository applicationRepository;
    @Autowired
    private MatchRepository matchRepository;

    @PreAuthorize("isAuthenticated()")
    public List<Tournament> getAll() {
        return tournamentRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public Tournament getById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Турнир не найден"));
    }

    public Tournament create(TournamentRequest request) {
        if (request.name() == null || request.name().isBlank() || request.teamLimit() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать name и teamLimit");
        if (request.teamLimit() < 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Лимит команд должен быть не меньше 2");

        Tournament tournament = new Tournament();
        tournament.setName(request.name());
        tournament.setTeamLimit(request.teamLimit());
        tournament.setStatus(TournamentStatus.REGISTRATION);
        return tournamentRepository.save(tournament);
    }

    public Tournament update(Long id, TournamentRequest request) {
        Tournament tournament = getById(id);
        if (request.status() != null && request.status() != tournament.getStatus())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Статус турнира меняется операциями start и finish");
        if (request.name() != null && request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название не может быть пустым");
        if (request.teamLimit() != null) {
            if (tournament.getStatus() != TournamentStatus.REGISTRATION)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лимит можно менять только во время регистрации");
            if (request.teamLimit() < 2)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Лимит команд должен быть не меньше 2");
            if (request.teamLimit() < getApproved(id).size())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Допущено больше команд, чем новый лимит");
        }

        if (request.name() != null) tournament.setName(request.name());
        if (request.teamLimit() != null) tournament.setTeamLimit(request.teamLimit());
        return tournament;
    }

    public void delete(Long id) {
        tournamentRepository.delete(getById(id));
    }

    public Tournament start(Long id) {
        Tournament tournament = getById(id);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Турнир уже начат или завершён");
        List<TeamApplication> approved = getApproved(id);
        if (approved.size() < 2)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Для старта нужно минимум 2 допущенные команды");

        for (TeamApplication a : applicationRepository.findByTournamentId(id)) {
            if (a.getStatus() == ApplicationStatus.PENDING) a.setStatus(ApplicationStatus.REJECTED);
        }
        for (int i = 0; i < approved.size(); i++) {
            for (int j = i + 1; j < approved.size(); j++) {
                Match match = new Match();
                match.setTournament(tournament);
                match.setTeam1(approved.get(i).getTeam());
                match.setTeam2(approved.get(j).getTeam());
                match.setScore1(0);
                match.setScore2(0);
                match.setStatus(MatchStatus.SCHEDULED);
                matchRepository.save(match);
            }
        }
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        return tournament;
    }

    public Tournament finish(Long id) {
        Tournament tournament = getById(id);
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Завершить можно только идущий турнир");
        for (Match m : matchRepository.findByTournamentId(id)) {
            if (m.getStatus() != MatchStatus.FINISHED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ещё не все матчи сыграны");
        }

        for (Standing s : getStandings(id)) {
            if (!s.withdrawn) {
                tournament.setWinner(s.team);
                break;
            }
        }
        tournament.setStatus(TournamentStatus.FINISHED);
        tournament.setFinishedAt(LocalDateTime.now());
        return tournament;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Standing> getStandings(Long id) {
        Tournament tournament = getById(id);
        if (tournament.getStatus() == TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Таблица появится после старта турнира");

        Map<Long, Standing> table = new LinkedHashMap<>();
        List<TeamApplication> applications = new ArrayList<>(applicationRepository.findByTournamentId(id));
        applications.sort(Comparator.comparing(TeamApplication::getId));
        for (TeamApplication a : applications) {
            if (a.getStatus() == ApplicationStatus.APPROVED || a.getStatus() == ApplicationStatus.WITHDRAWN)
                table.put(a.getTeam().getId(), new Standing(a.getTeam(), a.getStatus() == ApplicationStatus.WITHDRAWN));
        }
        for (Match m : matchRepository.findByTournamentId(id)) {
            if (m.getStatus() != MatchStatus.FINISHED) continue;
            Standing s1 = table.get(m.getTeam1().getId());
            Standing s2 = table.get(m.getTeam2().getId());
            if (s1 != null) s1.addGame(m.getScore1(), m.getScore2());
            if (s2 != null) s2.addGame(m.getScore2(), m.getScore1());
        }

        List<Standing> result = new ArrayList<>(table.values());
        result.sort((a, b) -> {
            if (a.points != b.points) return b.points - a.points;
            int diffA = a.goalsFor - a.goalsAgainst;
            int diffB = b.goalsFor - b.goalsAgainst;
            if (diffA != diffB) return diffB - diffA;
            return b.goalsFor - a.goalsFor;
        });
        return result;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Match> getMatches(Long id) {
        List<Match> matches = new ArrayList<>(matchRepository.findByTournamentId(id));
        matches.sort(Comparator.comparing(Match::getId));
        return matches;
    }

    private List<TeamApplication> getApproved(Long tournamentId) {
        List<TeamApplication> approved = new ArrayList<>();
        for (TeamApplication a : applicationRepository.findByTournamentId(tournamentId)) {
            if (a.getStatus() == ApplicationStatus.APPROVED) approved.add(a);
        }
        approved.sort(Comparator.comparing(TeamApplication::getId));
        return approved;
    }
}
