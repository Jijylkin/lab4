package com.example.tournament.dto;

import java.util.List;

public record TeamRequest(String name, Long captainId, List<Long> memberIds) {
}
