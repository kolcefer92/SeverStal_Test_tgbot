package ru.Zanfirov.TgBot.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "User_Notes")
public class Notes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
   // private long chatID;
    private String headLine;
    private String message;
    private String urlPicture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatID", referencedColumnName = "chatID")
    private Users users;


    public Notes(){

    }

//    public Notes(long chatID,String headLine, String message, String urlPicture, Users users) {
//        this.chatID = chatID;
//        this.headLine = headLine;
//        this.message = message;
//        this.urlPicture = urlPicture;
//        this.users = users;
//    }


    public Notes(String headLine, String message, String urlPicture, Users users) {
        this.headLine = headLine;
        this.message = message;
        this.urlPicture = urlPicture;
        this.users = users;
    }
}
