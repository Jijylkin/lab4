package com.example.tournament.service;

import com.example.tournament.dto.TeamApplicationRequest;
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
import com.example.tournament.security.AccessService;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@PreAuthorize("hasRole('ORGANIZER')")
public class TeamApplicationService {

    @Autowired
    private AccessService access;

    @Autowired
    private TeamApplicationRepository applicationRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private MatchRepository matchRepository;

    @PreAuthorize("isAuthenticated()")
    public List<TeamApplication> getAll() {
        return access.isOrganizer() ? applicationRepository.findAll()
                : applicationRepository.findByTeamCaptainEmailIgnoreCase(access.email());
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isApplicationCaptain(#id)")
    public TeamApplication getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isCaptain(#request.teamId())")
    public TeamApplication create(TeamApplicationRequest request) {
        if (request.teamId() == null || request.tournamentId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать teamId и tournamentId");
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда не найдена"));
        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Турнир не найден"));
        if (tournament.getStatus() != TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Регистрация на турнир закрыта");
        if (applicationRepository.existsByTeamIdAndTournamentId(team.getId(), tournament.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Команда уже подала заявку на этот турнир");

        TeamApplication application = new TeamApplication();
        application.setTeam(team);
        application.setTournament(tournament);
        application.setStatus(ApplicationStatus.PENDING);
        application.setAppliedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public TeamApplication update(Long id, TeamApplicationRequest request) {
        TeamApplication application = getById(id);
        if ((request.teamId() != null && !request.teamId().equals(application.getTeam().getId()))
                || (request.tournamentId() != null && !request.tournamentId().equals(application.getTournament().getId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команду и турнир в заявке менять нельзя");
        if (request.status() == null || request.status() == application.getStatus()) return application;
        if (request.status() != ApplicationStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Допуск и снятие команды делаются операциями approve и withdraw");
        if (application.getStatus() != ApplicationStatus.PENDING
                || application.getTournament().getStatus() != TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Отклонить можно только заявку на рассмотрении");

        application.setStatus(ApplicationStatus.REJECTED);
        return application;
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isApplicationCaptain(#id)")
    public void delete(Long id) {
        TeamApplication application = getById(id);
        if (application.getTournament().getStatus() != TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Турнир уже начался, заявку удалить нельзя");
        applicationRepository.delete(application);
    }

    public TeamApplication approve(Long id) {
        TeamApplication application = getById(id);
        Tournament tournament = application.getTournament();
        if (application.getStatus() != ApplicationStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Допустить можно только заявку на рассмотрении");
        if (tournament.getStatus() != TournamentStatus.REGISTRATION)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Регистрация на турнир закрыта");
        if (countApproved(tournament.getId()) >= tournament.getTeamLimit())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лимит команд в турнире уже набран");
        if (application.getTeam().getMembers().size() < TeamService.MIN_PLAYERS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "В команде должно быть минимум " + TeamService.MIN_PLAYERS + " игрока");

        application.setStatus(ApplicationStatus.APPROVED);
        return application;
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isApplicationCaptain(#id)")
    public TeamApplication withdraw(Long id) {
        TeamApplication application = getById(id);
        Tournament tournament = application.getTournament();
        if (application.getStatus() != ApplicationStatus.APPROVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Снять можно только допущенную команду");
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Снять команду можно только во время турнира, до старта заявку можно удалить");

        Long teamId = application.getTeam().getId();
        for (Match m : matchRepository.findByTournamentId(tournament.getId())) {
            if (m.getStatus() != MatchStatus.SCHEDULED) continue;
            if (m.getTeam1().getId().equals(teamId)) {
                m.setScore1(0);
                m.setScore2(3);
            } else if (m.getTeam2().getId().equals(teamId)) {
                m.setScore1(3);
                m.setScore2(0);
            } else {
                continue;
            }
            m.setStatus(MatchStatus.FINISHED);
            m.setPlayedAt(LocalDateTime.now());
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        return application;
    }

    private int countApproved(Long tournamentId) {
        int count = 0;
        for (TeamApplication a : applicationRepository.findByTournamentId(tournamentId)) {
            if (a.getStatus() == ApplicationStatus.APPROVED) count++;
        }
        return count;
    }
}
