package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {
    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;

    @NotNull
    Long category;

    @NotBlank
    @Size(min = 20, max = 7000)
    String description;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    @NotNull
    @Valid
    Location location;

    private Boolean paid = false;

    @PositiveOrZero
    Integer participantLimit = 0;

    Boolean requestModeration = true;

    @NotBlank
    @Size(min = 3, max = 120)
    String title;
}
