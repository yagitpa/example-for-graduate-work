package ru.skypro.homework.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.skypro.homework.model.UsersDao;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UsersDao, Integer> {
    Optional<UsersDao> findByEmail(String email);

    boolean existsByEmail(String email);
}
