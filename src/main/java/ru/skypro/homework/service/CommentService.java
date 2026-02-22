package ru.skypro.homework.service;

import ru.skypro.homework.dto.comment.CommentDto;
import ru.skypro.homework.dto.comment.CommentsDto;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDto;

/**
 * Сервис для работы с комментариями к объявлениям. Содержит методы для получения, добавления,
 * обновления и удаления комментариев.
 */
public interface CommentService {

    /**
     * Получение всех комментариев к объявлению
     *
     * @param adId идентификатор объявления
     * @return CommentsDto со списком комментариев
     */
    CommentsDto getComments(Integer adId);

    /**
     * Добавление комментария к объявлению
     *
     * @param adId идентификатор объявления
     * @param createComment данные нового комментария
     * @return созданный комментарий
     */
    CommentDto addComment(Integer adId, CreateOrUpdateCommentDto createComment);

    /**
     * Удаление комментария
     *
     * @param adId идентификатор объявления
     * @param commentId идентификатор комментария
     */
    void deleteComment(Integer adId, Integer commentId);

    /**
     * Обновление комментария
     *
     * @param adId идентификатор объявления
     * @param commentId идентификатор комментария
     * @param updateComment новые данные комментария
     * @return обновлённый комментарий
     */
    CommentDto updateComment(
            Integer adId, Integer commentId, CreateOrUpdateCommentDto updateComment);
}
