package ru.practicum.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EndpointHitDto {

    @NotBlank(message = "Имя приложения (app) не может быть пустым")
    String app;

    @NotBlank(message = "URI запроса не может быть пустым")
    String uri;

    @NotBlank(message = "IP-адрес пользователя не может быть пустым")
    String ip;

    @NotNull(message = "Время запроса (timestamp) должно быть указано")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp;
}
