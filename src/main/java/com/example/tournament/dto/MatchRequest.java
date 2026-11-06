package com.example.tournament.dto;

import com.example.tournament.model.MatchStatus;

public record MatchRequest(Long tournamentId, Long team1Id, Long team2Id, Integer score1, Integer score2,
                           MatchStatus status) {
}
