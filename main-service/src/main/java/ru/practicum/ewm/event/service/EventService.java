package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getEventsByUserId(Long userId, int from, int size);

    EventFullDto getEventByUserIdAndEventId(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<ParticipationRequestDto> getRequestsOnEventByUserId(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);

    List<EventFullDto> searchEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);


    List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        String sort, int from, int size, HttpServletRequest request);

    EventFullDto getEventByIdPublic(Long id, HttpServletRequest request);
}

