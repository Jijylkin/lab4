package com.example.tournament.service;

import com.example.tournament.dto.TeamRequest;
import com.example.tournament.model.ApplicationStatus;
import com.example.tournament.model.Team;
import com.example.tournament.model.TeamApplication;
import com.example.tournament.model.Tournament;
import com.example.tournament.model.TournamentStatus;
import com.example.tournament.model.User;
import com.example.tournament.repository.TeamApplicationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.example.tournament.security.AccessService;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@PreAuthorize("hasRole('ORGANIZER')")
public class TeamService {

    public static final int MIN_PLAYERS = 2;

    @Autowired
    private AccessService access;

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TeamApplicationRepository applicationRepository;

    @PreAuthorize("isAuthenticated()")
    public List<Team> getAll() {
        return teamRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public Team getById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена"));
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isSelf(#request.captainId())")
    public Team create(TeamRequest request) {
        if (request.name() == null || request.name().isBlank() || request.captainId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать name и captainId");
        User captain = findUser(request.captainId(), "Капитан не найден");
        List<User> members = makeMembers(captain, request.memberIds());

        Team team = new Team();
        team.setName(request.name());
        team.setCaptain(captain);
        team.getMembers().addAll(members);
        return teamRepository.save(team);
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isCaptain(#id)")
    public Team update(Long id, TeamRequest request) {
        Team team = getById(id);
        if (!access.isOrganizer() && request.captainId() != null
                && !request.captainId().equals(team.getCaptain().getId()))
            throw new AccessDeniedException("Сменить капитана может организатор");
        if (request.name() != null && request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название не может быть пустым");
        User captain = request.captainId() != null ? findUser(request.captainId(), "Капитан не найден") : team.getCaptain();
        List<Long> ids = new ArrayList<>();
        if (request.memberIds() != null) {
            ids.addAll(request.memberIds());
        } else {
            for (User u : team.getMembers()) ids.add(u.getId());
        }
        List<User> members = makeMembers(captain, ids);
        if (members.size() < MIN_PLAYERS && playsInTournament(team))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Команда участвует в турнире, в ней должно быть минимум " + MIN_PLAYERS + " игрока");

        if (request.name() != null) team.setName(request.name());
        team.setCaptain(captain);
        team.getMembers().clear();
        team.getMembers().addAll(members);
        return team;
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isCaptain(#id)")
    public void delete(Long id) {
        Team team = getById(id);
        if (!access.isOrganizer()) {
            for (TeamApplication application : applicationRepository.findByTeamId(id)) {
                if (application.getTournament().getStatus() != TournamentStatus.REGISTRATION)
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Команду из начатого турнира нельзя удалить; используйте withdraw");
            }
        }
        for (Tournament t : tournamentRepository.findByWinnerId(id)) {
            t.setWinner(null);
        }
        teamRepository.delete(team);
    }

    private boolean playsInTournament(Team team) {
        for (TeamApplication a : applicationRepository.findByTeamId(team.getId())) {
            if (a.getStatus() == ApplicationStatus.APPROVED && a.getTournament().getStatus() != TournamentStatus.FINISHED)
                return true;
        }
        return false;
    }

    private List<User> makeMembers(User captain, List<Long> memberIds) {
        List<User> members = new ArrayList<>();
        members.add(captain);
        if (memberIds == null) return members;
        for (Long userId : memberIds) {
            if (userId == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустой id игрока");
            User user = findUser(userId, "Игрок " + userId + " не найден");
            if (!members.contains(user)) members.add(user);
        }
        return members;
    }

    private User findUser(Long id, String message) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }
}
