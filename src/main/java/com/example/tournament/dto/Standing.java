package com.example.tournament.dto;

import com.example.tournament.model.Team;

public class Standing {

    public Team team;
    public boolean withdrawn;
    public int played;
    public int won;
    public int drawn;
    public int lost;
    public int goalsFor;
    public int goalsAgainst;
    public int points;

    public Standing(Team team, boolean withdrawn) {
        this.team = team;
        this.withdrawn = withdrawn;
    }

    public void addGame(int scored, int missed) {
        played++;
        goalsFor += scored;
        goalsAgainst += missed;
        if (scored > missed) {
            won++;
            points += 3;
        } else if (scored == missed) {
            drawn++;
            points += 1;
        } else {
            lost++;
        }
    }
}
