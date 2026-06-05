package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import java.util.List;

public interface CommentService {
    // Private API
    CommentDto addComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto);

    void deleteCommentByUser(Long userId, Long commentId);

    // Public API
    List<CommentDto> getCommentsPublic(Long eventId, int from, int size);

    // Admin API
    CommentDto moderateComment(Long commentId, String action);

    List<CommentDto> getCommentsByAdmin(Long eventId, int from, int size);
}

