package com.example.tournament.controller;

import com.example.tournament.dto.MatchRequest;
import com.example.tournament.model.Match;
import com.example.tournament.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Match match : matchService.getAll()) {
            result.add(toJson(match));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(matchService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody MatchRequest request) {
        return toJson(matchService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody MatchRequest request) {
        return toJson(matchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        matchService.delete(id);
    }

    @PostMapping("/{id}/result")
    public Map<String, Object> result(@PathVariable Long id, @RequestBody MatchRequest request) {
        return toJson(matchService.result(id, request));
    }

    private Map<String, Object> toJson(Match match) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", match.getId());
        map.put("tournamentId", match.getTournament().getId());
        map.put("team1Id", match.getTeam1().getId());
        map.put("team2Id", match.getTeam2().getId());
        map.put("score1", match.getScore1());
        map.put("score2", match.getScore2());
        map.put("status", match.getStatus());
        map.put("playedAt", match.getPlayedAt());
        return map;
    }
}
