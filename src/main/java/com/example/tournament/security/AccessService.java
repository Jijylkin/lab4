package com.example.tournament.security;

import com.example.tournament.model.User;
import com.example.tournament.repository.TeamApplicationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("access")
public class AccessService {

    @Autowired
    private UserRepository users;
    @Autowired
    private TeamRepository teams;
    @Autowired
    private TeamApplicationRepository applications;

    public String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User currentUser() {
        return users.findByEmailIgnoreCase(email()).orElseThrow();
    }

    public boolean isOrganizer() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER"));
    }

    public boolean isSelf(Long userId) {
        return currentUser().getId().equals(userId);
    }

    public boolean isCaptain(Long teamId) {
        return teamId != null && teams.findById(teamId)
                .map(team -> isSelf(team.getCaptain().getId())).orElse(false);
    }

    public boolean isApplicationCaptain(Long applicationId) {
        return applicationId != null && applications.findById(applicationId)
                .map(application -> isSelf(application.getTeam().getCaptain().getId())).orElse(false);
    }
}
