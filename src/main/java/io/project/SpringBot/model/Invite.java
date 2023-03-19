package io.project.SpringBot.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;


@Data
@Entity(name = "Invites")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "dayInvite")
    private LocalDate day;

    @Column(name = "userInitiator")
    private String name;

    public Invite(LocalDate dayId, String user) {
        this.day = dayId;
        this.name = user;
    }

    public Invite() {
    }
}
