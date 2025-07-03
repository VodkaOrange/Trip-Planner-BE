package com.tripplanner.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleCseService {

  @org.springframework.beans.factory.annotation.Value("${google.cse.apiKey}")
  private String apiKey;

  @org.springframework.beans.factory.annotation.Value("${google.cse.cx}")
  private String cx;

  private final RestTemplate restTemplate = new RestTemplate();

  public List<String> searchImages(String query) {

    String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/customsearch/v1")
        .queryParam("key", apiKey)
        .queryParam("cx", cx)
        .queryParam("q", query)
        .queryParam("searchType", "image")
        .queryParam("num", 1)
        .queryParam("fields", "items(link)")
        .build()
        .toUriString();

    ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
      List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
      if (items == null) {
        return Collections.emptyList();
      }

      return items.stream()
          .map(item -> (String) item.get("link"))
          .toList();
    }
    return Collections.emptyList();
  }
}

