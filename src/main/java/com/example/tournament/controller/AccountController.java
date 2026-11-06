package com.example.tournament.controller;

import com.example.tournament.dto.UserRequest;
import com.example.tournament.model.User;
import com.example.tournament.security.AccessService;
import com.example.tournament.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private UserService users;
    @Autowired
    private AccessService access;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody UserRequest request) {
        return profile(users.register(request));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return profile(access.currentUser());
    }

    private Map<String, Object> profile(User user) {
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
                "role", user.getRole(), "enabled", user.getEnabled());
    }
}
