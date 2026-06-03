package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventPrivateController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Пользователь id={} создает событие title={}", userId, newEventDto.getTitle());
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> getEventsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Запрос списка событий пользователя id={} (from={}, size={})", userId, from, size);
        return eventService.getEventsByUserId(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByUserIdAndEventId(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Запрос полной информации о событии id={} пользователем id={}", eventId, userId);
        return eventService.getEventByUserIdAndEventId(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByUserIdAndEventId(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        log.info("Пользователь id={} обновляет событие id={}", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, updateRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsOnEventByUserId(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("Инициатор id={} запрашивает заявки на участие в событии id={}", userId, eventId);
        return eventService.getRequestsOnEventByUserId(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("Инициатор id={} меняет статус заявок для события id={}", userId, eventId);
        return eventService.updateRequestsStatus(userId, eventId, updateRequest);
    }
}
