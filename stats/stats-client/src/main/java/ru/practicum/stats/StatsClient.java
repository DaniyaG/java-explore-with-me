package ru.practicum.stats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

@Service
public class StatsClient {
    private final RestTemplate rest;

    public StatsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.rest = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .build();
    }

    public void hit(EndpointHitDto hitDto) {
        rest.postForEntity("/hit", hitDto, Void.class);
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(String start, String end, List<String> uris, boolean unique) {
        String path = String.format("/stats?start=%s&end=%s&unique=%b", start, end, unique);
        if (uris != null && !uris.isEmpty()) {
            path += "&uris=" + String.join(",", uris);
        }
        return rest.exchange(path, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }
}
