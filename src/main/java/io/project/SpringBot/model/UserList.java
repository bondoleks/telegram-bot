package io.project.SpringBot.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;


@Data
@Entity(name = "UsersList")
public class UserList {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "day")
    private LocalDate dayId;

    @Column(name = "user")
    private String user;

    public UserList(LocalDate dayId, String user) {
        this.dayId = dayId;
        this.user = user;
    }

    public UserList() {
    }

    @Override
    public String toString() {
        return user;
    }
}
