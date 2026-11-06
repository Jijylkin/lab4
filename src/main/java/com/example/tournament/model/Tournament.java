package com.example.tournament.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "team_limit", nullable = false)
    private Integer teamLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TournamentStatus status;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Team winner;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.REMOVE)
    private List<TeamApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.REMOVE)
    private List<Match> matches = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTeamLimit() {
        return teamLimit;
    }

    public void setTeamLimit(Integer teamLimit) {
        this.teamLimit = teamLimit;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public Team getWinner() {
        return winner;
    }

    public void setWinner(Team winner) {
        this.winner = winner;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<TeamApplication> getApplications() {
        return applications;
    }

    public List<Match> getMatches() {
        return matches;
    }
}
