
package com.shopmind.backend.dto;

import java.util.List;


public record AiRecommendationResponse(Long productId,String algorithm,Integer total,List<Recommendation> recommendations) {

    public record Recommendation(Long productId,String name,String brand,String category,Double similarityScore) {}
}
