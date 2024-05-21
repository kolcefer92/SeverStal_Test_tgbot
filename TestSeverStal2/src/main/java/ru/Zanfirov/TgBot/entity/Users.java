package ru.Zanfirov.TgBot.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "UserTable")
public class Users {
    @Id
    private long chatID;
    private  String name;


    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notes> notes;


    public Users(){

    }

    public Users(long chatID, String name) {
        this.chatID = chatID;
        this.name = name;
    }
}
