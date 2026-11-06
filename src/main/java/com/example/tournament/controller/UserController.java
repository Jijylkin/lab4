package com.example.tournament.controller;

import com.example.tournament.dto.UserRequest;
import com.example.tournament.model.User;
import com.example.tournament.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : userService.getAll()) {
            result.add(toJson(user));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody UserRequest request) {
        return toJson(userService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody UserRequest request) {
        return toJson(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    private Map<String, Object> toJson(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("enabled", user.getEnabled());
        return map;
    }
}
