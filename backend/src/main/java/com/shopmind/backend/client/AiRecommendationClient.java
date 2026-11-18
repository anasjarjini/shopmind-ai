
package com.shopmind.backend.client;

import com.shopmind.backend.dto.AiRecommendationResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;


@Component
public class AiRecommendationClient {

    private final RestClient restClient;

    public AiRecommendationClient(RestClient.Builder builder, @Value("${shopmind.ai.base-url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public AiRecommendationResponse getRecommendations(Long productId,int limit,boolean sameCategory) {

        try {
            AiRecommendationResponse response =
                restClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path( "/api/v1/recommendations/{productId}")
                        .queryParam("limit", limit)
                        .queryParam("same_category",sameCategory)
                        .build(productId)
                    ).retrieve().body(AiRecommendationResponse.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Empty response from AI service");
            }

            return response;

        } catch (HttpClientErrorException.NotFound ex) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found in AI index",
                ex
            );

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI service is unavailable",
                ex
            );

        } catch (RestClientResponseException ex) {

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI service returned HTTP "
                    + ex.getStatusCode().value(),
                ex
            );
        }
    }
}
