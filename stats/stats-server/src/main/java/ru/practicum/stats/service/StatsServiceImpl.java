package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final EndpointHitRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        repository.save(StatsMapper.toEntity(hitDto));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (uris == null || uris.isEmpty()) {
            return unique ? repository.getStatsWithUniqueIpWithoutUris(start, end)
                    : repository.getStatsWithAllIpWithoutUris(start, end);
        } else {
            return unique ? repository.getStatsWithUniqueIpAndUris(start, end, uris)
                    : repository.getStatsWithAllIpAndUris(start, end, uris);
        }
    }
}

