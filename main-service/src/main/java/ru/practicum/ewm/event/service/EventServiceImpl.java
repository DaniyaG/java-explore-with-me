package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
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
import ru.practicum.stats.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата события должна быть минимум через 2 часа от текущего момента");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

        Event event = EventMapper.toEvent(dto, initiator, category);
        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long userId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return eventRepository.findAllByInitiatorId(userId, page).stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventByUserIdAndEventId(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Изменять можно только неопубликованные события");
        }

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new IllegalArgumentException("Дата события должна быть минимум через 2 часа");
            }
            event.setEventDate(request.getEventDate());
        }

        updateEventFields(event, request.getAnnotation(), request.getDescription(), request.getTitle(),
                request.getPaid(), request.getParticipantLimit(), request.getRequestModeration(), request.getCategory());

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("SEND_TO_REVIEW")) {
                event.setState(EventState.PENDING);
            } else if (request.getStateAction().equals("CANCEL_REVIEW")) {
                event.setState(EventState.CANCELED);
            }
        }

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsOnEventByUserId(Long userId, Long eventId) {
        eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников на событие уже исчерпан");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно менять только у заявок в состоянии PENDING");
            }

            if (updateRequest.getStatus().equals("CONFIRMED")) {
                if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toDto(requestRepository.save(request)));
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    confirmed.add(RequestMapper.toDto(requestRepository.save(request)));
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(requestRepository.save(request)));
            }
        }
        eventRepository.save(event);

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<EventFullDto> searchEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);

        List<EventState> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.searchEventsByAdminWithFilters(
                users, stateEnums, categories, rangeStart, rangeEnd, page
        );

        loadViewsForEvents(events);

        return events.stream()
                .map(EventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("Дата события должна быть минимум за 1 час до публикации");
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("PUBLISH_EVENT")) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Опубликовать можно только событие в состоянии PENDING");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals("REJECT_EVENT")) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить уже опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }
        updateEventFields(event, request.getAnnotation(), request.getDescription(),
                request.getTitle(),request.getPaid(), request.getParticipantLimit(),
                request.getRequestModeration(), request.getCategory());

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    // --- PUBLIC API ---

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size, HttpServletRequest request) {
        sendHit(request);

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart не может быть позже rangeEnd");
        }

        int pageNumber = from / (size > 0 ? size : 10);
        PageRequest page = sort != null && sort.equals("EVENT_DATE")
                ? PageRequest.of(pageNumber, size, Sort.by("eventDate").ascending())
                : PageRequest.of(pageNumber, size);

        List<Event> events = eventRepository.searchPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                EventState.PUBLISHED, page
        );

        loadViewsForEvents(events);
        if (sort != null && sort.equals("VIEWS")) {
            events.sort(Comparator.comparing(Event::getViews, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventByIdPublic(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с id=" + id + " не найдено"));
        sendHit(request);
        loadViewsForEvents(List.of(event));

        return EventMapper.toEventFullDto(event);
    }

    private void updateEventFields(Event event, String annotation, String description, String title,
                                   Boolean paid, Integer limit, Boolean moderation, Long catId) {
        if (annotation != null) event.setAnnotation(annotation);
        if (description != null) event.setDescription(description);
        if (title != null) event.setTitle(title);
        if (paid != null) event.setPaid(paid);
        if (limit != null) event.setParticipantLimit(limit);
        if (moderation != null) event.setRequestModeration(moderation);
        if (catId != null) {
            Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
            event.setCategory(category);
        }
    }

    private void sendHit(HttpServletRequest request) {
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app(APP_NAME)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.hit(hit);
        } catch (Exception e) {
            log.error("Ошибка отправки статистики просмотра: {}", e.getMessage());
        }
    }

    private void loadViewsForEvents(List<Event> events) {
        if (events.isEmpty()) return;

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(10));

        try {
            ResponseEntity<List<ViewStatsDto>> response = statsClient.getStats(
                    start.format(formatter),
                    LocalDateTime.now().format(formatter),
                    uris,
                    true
            );

            List<ViewStatsDto> stats = response.getBody();

            if (stats != null) {
                Map<String, Long> viewsMap = stats.stream()
                        .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));

                for (Event event : events) {
                    event.setViews(viewsMap.getOrDefault("/events/" + event.getId(), 0L));
                }
            }
        } catch (Exception e) {
            log.error("Не удалось получить просмотры из статистики: {}", e.getMessage());
            for (Event event : events) {
                event.setViews(0L);
            }
        }
    }
}
