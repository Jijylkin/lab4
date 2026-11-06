package com.example.tournament.repository;

import com.example.tournament.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentId(Long tournamentId);
}
