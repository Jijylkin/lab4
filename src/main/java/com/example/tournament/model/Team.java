package com.example.tournament.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "captain_id", nullable = false)
    private User captain;

    @ManyToMany
    @JoinTable(name = "team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE)
    private List<TeamApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "team1", cascade = CascadeType.REMOVE)
    private List<Match> matchesAsTeam1 = new ArrayList<>();

    @OneToMany(mappedBy = "team2", cascade = CascadeType.REMOVE)
    private List<Match> matchesAsTeam2 = new ArrayList<>();

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

    public User getCaptain() {
        return captain;
    }

    public void setCaptain(User captain) {
        this.captain = captain;
    }

    public List<User> getMembers() {
        return members;
    }

    public List<TeamApplication> getApplications() {
        return applications;
    }

    public List<Match> getMatchesAsTeam1() {
        return matchesAsTeam1;
    }

    public List<Match> getMatchesAsTeam2() {
        return matchesAsTeam2;
    }
}
