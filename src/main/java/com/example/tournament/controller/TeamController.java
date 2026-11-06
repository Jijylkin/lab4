package com.example.tournament.controller;

import com.example.tournament.dto.TeamRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.tournament.model.Team;
import com.example.tournament.model.User;
import com.example.tournament.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Team team : teamService.getAll()) {
            result.add(toJson(team));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(teamService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody TeamRequest request) {
        return toJson(teamService.create(request));
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isCaptain(#id)")
    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody TeamRequest request) {
        return toJson(teamService.update(id, request));
    }

    @PreAuthorize("hasRole('ORGANIZER') or @access.isCaptain(#id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teamService.delete(id);
    }

    private Map<String, Object> toJson(Team team) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Long> memberIds = new ArrayList<>();
        for (User u : team.getMembers()) {
            memberIds.add(u.getId());
        }
        map.put("id", team.getId());
        map.put("name", team.getName());
        map.put("captainId", team.getCaptain().getId());
        map.put("memberIds", memberIds);
        return map;
    }
}
