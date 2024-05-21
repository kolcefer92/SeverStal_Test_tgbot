package ru.Zanfirov.TgBot.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.Zanfirov.TgBot.entity.Notes;
import ru.Zanfirov.TgBot.entity.Users;

import java.util.List;

@Repository
public interface NotesRepo extends JpaRepository<Notes,Integer> {

    List<Notes> findByUsersChatID(long chatID);
}
