package ru.skypro.homework.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.skypro.homework.model.AdsDao;

@Repository
public interface AdRepository extends JpaRepository<AdsDao, Integer> {

    /**
     * Получение объявлений конкретного пользователя (автора) с пагинацией.
     *
     * @param authorId идентификатор автора (поле user_id в таблице ads)
     * @param pageable параметры пагинации
     * @return страница объявлений пользователя
     */
    Page<AdsDao> findByAuthorId(Integer authorId, Pageable pageable);

    /**
     * Подсчёт количества объявлений пользователя.
     * Может быть полезен для сервисного слоя.
     *
     * @param authorId идентификатор автора
     * @return количество объявлений
     */
    long countByAuthorId(Integer authorId);
}