package com.example.tournament.repository;

import com.example.tournament.model.TeamApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

    List<TeamApplication> findByTournamentId(Long tournamentId);

    List<TeamApplication> findByTeamId(Long teamId);

    List<TeamApplication> findByTeamCaptainEmailIgnoreCase(String email);

    boolean existsByTeamIdAndTournamentId(Long teamId, Long tournamentId);
}
