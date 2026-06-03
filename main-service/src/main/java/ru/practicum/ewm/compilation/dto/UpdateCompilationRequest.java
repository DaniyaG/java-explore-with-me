package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompilationRequest {
    Set<Long> events;
    Boolean pinned;

    @Size(min = 1, max = 50)
    String title;
}