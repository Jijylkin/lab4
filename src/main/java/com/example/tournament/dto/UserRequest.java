package com.example.tournament.dto;

public record UserRequest(String name, String email, String password, String role, Boolean enabled) {
}
