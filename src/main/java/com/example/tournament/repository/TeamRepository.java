package com.example.tournament.repository;

import com.example.tournament.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByCaptainId(Long captainId);
}
