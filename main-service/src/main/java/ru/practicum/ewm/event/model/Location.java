package ru.practicum.ewm.event.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private Float lat;
    private Float lon;
}
