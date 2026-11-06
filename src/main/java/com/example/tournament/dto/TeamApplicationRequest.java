package com.example.tournament.dto;

import com.example.tournament.model.ApplicationStatus;

public record TeamApplicationRequest(Long teamId, Long tournamentId, ApplicationStatus status) {
}
