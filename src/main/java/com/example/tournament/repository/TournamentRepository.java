package com.example.tournament.repository;

import com.example.tournament.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByWinnerId(Long winnerId);
}
