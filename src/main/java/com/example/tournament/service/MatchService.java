package com.example.tournament.service;

import com.example.tournament.dto.MatchRequest;
import com.example.tournament.model.ApplicationStatus;
import com.example.tournament.model.Match;
import com.example.tournament.model.MatchStatus;
import com.example.tournament.model.Team;
import com.example.tournament.model.TeamApplication;
import com.example.tournament.model.Tournament;
import com.example.tournament.model.TournamentStatus;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TeamApplicationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@PreAuthorize("hasRole('ORGANIZER')")
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamApplicationRepository applicationRepository;

    @PreAuthorize("isAuthenticated()")
    public List<Match> getAll() {
        return matchRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public Match getById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч не найден"));
    }

    public Match create(MatchRequest request) {
        if (request.tournamentId() == null || request.team1Id() == null || request.team2Id() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать tournamentId, team1Id и team2Id");
        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Турнир не найден"));
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Матчи можно создавать только когда турнир идёт");
        if (request.team1Id().equals(request.team2Id()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда не может играть сама с собой");
        Team team1 = findTeam(request.team1Id());
        Team team2 = findTeam(request.team2Id());
        if (!isApproved(team1, tournament) || !isApproved(team2, tournament))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Обе команды должны быть допущены к турниру");

        Match match = new Match();
        match.setTournament(tournament);
        match.setTeam1(team1);
        match.setTeam2(team2);
        match.setScore1(0);
        match.setScore2(0);
        match.setStatus(MatchStatus.SCHEDULED);
        return matchRepository.save(match);
    }

    public Match update(Long id, MatchRequest request) {
        Match match = getById(id);
        checkCanChange(match);
        if ((request.tournamentId() != null && !request.tournamentId().equals(match.getTournament().getId()))
                || (request.team1Id() != null && !request.team1Id().equals(match.getTeam1().getId()))
                || (request.team2Id() != null && !request.team2Id().equals(match.getTeam2().getId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Турнир и команды матча менять нельзя");
        if (request.status() != null && request.status() != match.getStatus())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Результат записывается операцией result");
        checkScores(request.score1(), request.score2());

        if (request.score1() != null) match.setScore1(request.score1());
        if (request.score2() != null) match.setScore2(request.score2());
        return match;
    }

    public void delete(Long id) {
        Match match = getById(id);
        if (match.getTournament().getStatus() == TournamentStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Турнир завершён, матчи менять нельзя");
        matchRepository.delete(match);
    }

    public Match result(Long id, MatchRequest request) {
        Match match = getById(id);
        if (match.getTournament().getStatus() != TournamentStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Турнир не идёт");
        if (match.getStatus() == MatchStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Матч уже сыгран");
        if (request.score1() == null || request.score2() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать score1 и score2");
        checkScores(request.score1(), request.score2());

        match.setScore1(request.score1());
        match.setScore2(request.score2());
        match.setStatus(MatchStatus.FINISHED);
        match.setPlayedAt(LocalDateTime.now());
        return match;
    }

    private void checkCanChange(Match match) {
        if (match.getTournament().getStatus() == TournamentStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Турнир завершён, матчи менять нельзя");
        if (match.getStatus() == MatchStatus.FINISHED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Матч уже сыгран");
    }

    private void checkScores(Integer score1, Integer score2) {
        if ((score1 != null && score1 < 0) || (score2 != null && score2 < 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Счёт не может быть отрицательным");
    }

    private boolean isApproved(Team team, Tournament tournament) {
        for (TeamApplication a : applicationRepository.findByTournamentId(tournament.getId())) {
            if (a.getTeam().getId().equals(team.getId()) && a.getStatus() == ApplicationStatus.APPROVED) return true;
        }
        return false;
    }

    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда не найдена"));
    }
}
