package com.tripnova.tripnova.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    private final RestClient restClient;

    // Constructor: configure connection and response timeouts
    public GeminiService() {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public String generateItinerary(String prompt) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key missing. Check application.properties."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "Gemini model missing. Check application.properties."
            );
        }

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", 1400,
                        "temperature", 0.4
                )
        );

        long startTime = System.currentTimeMillis();

        System.out.println("TripNova: Sending request to Gemini...");
        System.out.println("Gemini model: " + model);

        try {

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .header(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE
                    )
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            long elapsed =
                    System.currentTimeMillis() - startTime;

            System.out.println(
                    "TripNova: Gemini response received in "
                    + elapsed + " ms"
            );

            if (response == null) {
                throw new IllegalStateException(
                        "Gemini returned an empty response."
                );
            }

            Object candidatesObject = response.get("candidates");

            if (!(candidatesObject instanceof List<?> candidates)
                    || candidates.isEmpty()) {

                System.err.println(
                        "Gemini did not return any candidates."
                );

                throw new IllegalStateException(
                        "Gemini could not generate the itinerary. "
                        + "Check the prompt, API quota, and model."
                );
            }

            Object candidateObject = candidates.get(0);

            if (!(candidateObject instanceof Map<?, ?> candidate)) {
                throw new IllegalStateException(
                        "Invalid candidate received from Gemini."
                );
            }

            Object contentObject = candidate.get("content");

            if (!(contentObject instanceof Map<?, ?> content)) {
                throw new IllegalStateException(
                        "Gemini response does not contain content."
                );
            }

            Object partsObject = content.get("parts");

            if (!(partsObject instanceof List<?> parts)
                    || parts.isEmpty()) {
                throw new IllegalStateException(
                        "Gemini response does not contain text."
                );
            }

            Object partObject = parts.get(0);

            if (!(partObject instanceof Map<?, ?> part)) {
                throw new IllegalStateException(
                        "Invalid text part received from Gemini."
                );
            }

            Object textObject = part.get("text");

            if (textObject == null
                    || textObject.toString().isBlank()) {
                throw new IllegalStateException(
                        "Gemini returned empty itinerary text."
                );
            }

            return textObject.toString().trim();

        } catch (RestClientResponseException e) {

            System.err.println(
                    "Gemini API HTTP status: "
                    + e.getStatusCode()
            );

            System.err.println(
                    "Gemini API error response: "
                    + e.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "Gemini API request failed with HTTP status "
                    + e.getStatusCode()
                    + ". Check the API key, model, and quota."
            );

        } catch (ResourceAccessException e) {

            System.err.println(
                    "Gemini connection/timeout error: "
                    + e.getMessage()
            );

            throw new IllegalStateException(
                    "Gemini request timed out or could not connect. "
                    + "Please try again."
            );

        } catch (RestClientException e) {

            System.err.println(
                    "Gemini API communication error: "
                    + e.getMessage()
            );

            throw new IllegalStateException(
                    "Unable to communicate with the Gemini API."
            );
        }
    }
}