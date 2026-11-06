package com.example.tournament.dto;

import com.example.tournament.model.TournamentStatus;

public record TournamentRequest(String name, Integer teamLimit, TournamentStatus status) {
}
