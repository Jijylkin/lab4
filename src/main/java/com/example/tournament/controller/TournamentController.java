package com.example.tournament.controller;

import com.example.tournament.dto.Standing;
import com.example.tournament.dto.TournamentRequest;
import com.example.tournament.model.Match;
import com.example.tournament.model.Tournament;
import com.example.tournament.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tournament tournament : tournamentService.getAll()) {
            result.add(toJson(tournament));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(tournamentService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody TournamentRequest request) {
        return toJson(tournamentService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody TournamentRequest request) {
        return toJson(tournamentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tournamentService.delete(id);
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> start(@PathVariable Long id) {
        Map<String, Object> map = toJson(tournamentService.start(id));
        List<Map<String, Object>> matches = new ArrayList<>();
        for (Match m : tournamentService.getMatches(id)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("team1Id", m.getTeam1().getId());
            row.put("team2Id", m.getTeam2().getId());
            matches.add(row);
        }
        map.put("matches", matches);
        return map;
    }

    @PostMapping("/{id}/finish")
    public Map<String, Object> finish(@PathVariable Long id) {
        return toJson(tournamentService.finish(id));
    }

    @GetMapping("/{id}/standings")
    public List<Map<String, Object>> standings(@PathVariable Long id) {
        List<Map<String, Object>> result = new ArrayList<>();
        int place = 1;
        for (Standing s : tournamentService.getStandings(id)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("place", place++);
            row.put("teamId", s.team.getId());
            row.put("team", s.team.getName());
            row.put("withdrawn", s.withdrawn);
            row.put("played", s.played);
            row.put("won", s.won);
            row.put("drawn", s.drawn);
            row.put("lost", s.lost);
            row.put("goals", s.goalsFor + ":" + s.goalsAgainst);
            row.put("points", s.points);
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> toJson(Tournament tournament) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tournament.getId());
        map.put("name", tournament.getName());
        map.put("teamLimit", tournament.getTeamLimit());
        map.put("status", tournament.getStatus());
        map.put("winnerTeamId", tournament.getWinner() == null ? null : tournament.getWinner().getId());
        map.put("finishedAt", tournament.getFinishedAt());
        return map;
    }
}
