package com.example.tournament.controller;

import com.example.tournament.dto.TeamApplicationRequest;
import com.example.tournament.model.TeamApplication;
import com.example.tournament.service.TeamApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team-applications")
public class TeamApplicationController {

    @Autowired
    private TeamApplicationService applicationService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamApplication application : applicationService.getAll()) {
            result.add(toJson(application));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(applicationService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody TeamApplicationRequest request) {
        return toJson(applicationService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody TeamApplicationRequest request) {
        return toJson(applicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationService.delete(id);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id) {
        return toJson(applicationService.approve(id));
    }

    @PostMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable Long id) {
        return toJson(applicationService.withdraw(id));
    }

    private Map<String, Object> toJson(TeamApplication application) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", application.getId());
        map.put("teamId", application.getTeam().getId());
        map.put("tournamentId", application.getTournament().getId());
        map.put("status", application.getStatus());
        map.put("appliedAt", application.getAppliedAt());
        return map;
    }
}
