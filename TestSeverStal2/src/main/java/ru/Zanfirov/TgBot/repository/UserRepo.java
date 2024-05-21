package ru.Zanfirov.TgBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.Zanfirov.TgBot.entity.Users;

@Repository
public interface UserRepo extends JpaRepository<Users,Long> {
}
