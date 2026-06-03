package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в собственном событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Запрос на участие в этом событии уже существует");
        }

        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников на данное событие уже исчерпан");
        }

        RequestStatus status = RequestStatus.PENDING;

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        log.info("Создан запрос на участие с id={} для пользователя id={} на событие id={}",
                request.getId(), userId, eventId);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId +
                        " для пользователя id=" + userId + " не найден"));

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            if (event.getConfirmedRequests() > 0) {
                event.setConfirmedRequests(event.getConfirmedRequests() - 1);
                eventRepository.save(event);
            }
        }

        request.setStatus(RequestStatus.CANCELED);
        log.info("Пользователь id={} отменил свой запрос id={}", userId, requestId);
        return RequestMapper.toDto(requestRepository.save(request));
    }
}
