package com.example.tournament.service;

import com.example.tournament.dto.UserRequest;
import com.example.tournament.model.Team;
import com.example.tournament.model.User;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.UserRepository;
import com.example.tournament.security.AccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamService teamService;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private AccessService access;

    @PreAuthorize("hasRole('ORGANIZER')")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isSelf(#id)")
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    public User register(UserRequest request) {
        return saveNew(request, "PLAYER", true);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    public User create(UserRequest request) {
        return saveNew(request, request.role() == null ? "PLAYER" : request.role(),
                request.enabled() == null || request.enabled());
    }

    private User saveNew(UserRequest request, String role, boolean enabled) {
        checkName(request.name());
        String email = checkEmail(request.email(), null);
        checkPassword(request.password());
        checkRole(role);
        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isSelf(#id)")
    public User update(Long id, UserRequest request) {
        User user = getById(id);
        if (!access.isOrganizer() && (request.role() != null || request.enabled() != null))
            throw new AccessDeniedException("Роль и блокировку меняет организатор");
        if (request.name() != null) { checkName(request.name()); user.setName(request.name()); }
        if (request.email() != null) user.setEmail(checkEmail(request.email(), id));
        if (request.password() != null) {
            checkPassword(request.password());
            user.setPasswordHash(encoder.encode(request.password()));
        }
        if (request.role() != null) { checkRole(request.role()); user.setRole(request.role()); }
        if (request.enabled() != null) user.setEnabled(request.enabled());
        return user;
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    public void delete(Long id) {
        User user = getById(id);
        for (Team team : teamRepository.findByCaptainId(id)) teamService.delete(team.getId());
        for (Team team : teamRepository.findAll()) team.getMembers().remove(user);
        userRepository.delete(user);
    }

    private void checkName(String name) {
        if (name == null || name.isBlank() || name.length() > 100)
            bad("Имя должно содержать от 1 до 100 символов");
    }

    private String checkEmail(String email, Long id) {
        if (email == null || email.length() > 100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            bad("Нужно указать корректный email");
        String value = email.toLowerCase(Locale.ROOT);
        User other = userRepository.findByEmailIgnoreCase(value).orElse(null);
        if (other != null && !other.getId().equals(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот email уже занят");
        return value;
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < 10
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*") || password.chars().anyMatch(Character::isWhitespace))
            bad("Пароль: от 10 символов, заглавная и строчная буквы, цифра, без пробелов; максимум 72 байта");
    }

    private void checkRole(String role) {
        if (!role.equals("PLAYER") && !role.equals("ORGANIZER")) bad("Неизвестная роль");
    }

    private void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
