
package com.shopmind.backend.service;

import com.shopmind.backend.client.AiRecommendationClient;
import com.shopmind.backend.dto.AiRecommendationResponse;
import com.shopmind.backend.dto.ProductCardResponse;
import com.shopmind.backend.dto.RecommendedProductResponse;
import com.shopmind.backend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRecommendationService {

    private final AiRecommendationClient aiClient;
    private final ProductRepository productRepository;

    public List<RecommendedProductResponse> getRecommendations(Long productId,int limit,boolean sameCategory) {

        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid product ID");
        }

        if (limit < 1 || limit > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Limit must be between 1 and 20");
        }

        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
        }

        // 2. Ask FastAPI for similar products
        AiRecommendationResponse aiResponse = aiClient.getRecommendations(productId,limit,sameCategory);

        List<AiRecommendationResponse.Recommendation> aiRecommendations = aiResponse.recommendations();

        if (aiRecommendations == null || aiRecommendations.isEmpty()) {
            return List.of();
        }

        // 3. Extract IDs recommended by AI
        List<Long> recommendedIds = aiRecommendations.stream().map(AiRecommendationResponse.Recommendation::productId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (recommendedIds.isEmpty()) {
            return List.of();
        }

        // 4. Fetch product cards in one SQL query
        List<ProductCardResponse> cards = productRepository.findActiveCardsByIds(recommendedIds);

        Map<Long, ProductCardResponse> cardsById = cards.stream().collect( Collectors.toMap(ProductCardResponse::getId,Function.identity()));

        // 5. Preserve the AI ranking
        List<RecommendedProductResponse> results = new ArrayList<>();

        for ( AiRecommendationResponse.Recommendation recommendation : aiRecommendations) {

            ProductCardResponse card = cardsById.get(recommendation.productId());

            // Skip missing or inactive products
            if (card == null) {
                continue;
            }

            results.add(new RecommendedProductResponse(card,recommendation.similarityScore())
            );
        }

        return results;
    }
}
