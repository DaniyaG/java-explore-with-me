package ru.practicum.ewm.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список идентификаторов заявок (requestIds) не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Статус (status) должен быть указано")
    private String status;
}
