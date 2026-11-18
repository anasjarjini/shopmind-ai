package com.shopmind.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmind.backend.dto.ProductImageRequest;
import com.shopmind.backend.dto.ProductRequest;
import com.shopmind.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private static final int MAX_PRODUCTS = 500;
    private static final long MAX_FILE_BYTES = 5_000_000L;

    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final ProductService productService;

 
    @Transactional
    public ImportResult importProducts(MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            throw badRequest("Select a non-empty JSON file");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw badRequest("JSON file is too large (maximum 5 MB)");
        }

        JsonNode root;
        try (InputStream input = file.getInputStream()) {
            root = objectMapper.readTree(input);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid or unreadable JSON file", ex);
        }

        if (root == null || !root.isObject() || !root.path("products").isArray()) {
            throw badRequest("Expected a JSON object containing a 'products' array");
        }

        JsonNode products = root.get("products");
        if (products.isEmpty() || products.size() > MAX_PRODUCTS) {
            throw badRequest("The 'products' array must contain between 1 and 500 items");
        }

        // Convert the whole file before performing any database writes.
        List<ProductRequest> requests = new ArrayList<>();
        Set<String> seenAsins = new HashSet<>();
        int duplicatesInFile = 0;

        for (int i = 0; i < products.size(); i++) {
            ProductRequest request = mapAmazonProduct(products.get(i), i + 1);
            String asin = request.getAmazonAsin().trim().toUpperCase(Locale.ROOT);

            if (!asin.matches("[A-Z0-9]{10}")) {
                throw badRequest("Product " + (i + 1) + ": invalid Amazon ASIN");
            }
            request.setAmazonAsin(asin);

            if (!seenAsins.add(asin)) {
                duplicatesInFile++;
                continue;
            }
            requests.add(request);
        }

        int imported = 0;
        int alreadyExists = 0;

        for (ProductRequest request : requests) {
            if (productRepository.existsByAmazonAsin(request.getAmazonAsin())) {
                alreadyExists++;
                continue;
            }

            // Reuses the existing business rules: category lookup, validation,
            // 3 distinct images, date conversion and JPA cascade persistence.
            productService.createProduct(request);
            imported++;
        }

        return new ImportResult(
                products.size(),
                imported,
                alreadyExists,
                duplicatesInFile,
                "Import completed successfully"
        );
    }

    private ProductRequest mapAmazonProduct(JsonNode node, int index) {
        if (node == null || !node.isObject()) {
            throw badRequest("Product " + index + ": expected a JSON object");
        }

        ProductRequest request = new ProductRequest();
        request.setAmazonAsin(requiredText(node, "amazon_id", index));
        request.setName(requiredText(node, "name", index));
        request.setBrand(requiredText(node, "brand", index));
        request.setCategory(requiredText(node, "category", index));
        request.setStore(optionalText(node, "store"));
        request.setDescription(optionalText(node, "description"));
        request.setPrice(requiredDecimal(node, "price", index));
        request.setAmazonRating(optionalDecimal(node, "amazon_rating", index));
        request.setRatingCount(optionalInteger(node, "rating_count", index));
        request.setDateFirstAvailable(optionalText(node, "date_first_available"));

        // The Amazon dataset has no inventory data. Do not invent stock.
        request.setStock(0);
        request.setActive(true);

        JsonNode imageNodes = node.path("images");
        if (!imageNodes.isArray() || imageNodes.size() != 3) {
            throw badRequest("Product " + index + ": exactly 3 images are required");
        }

        List<ProductImageRequest> images = new ArrayList<>(3);
        Set<Integer> positions = new HashSet<>();
        Set<String> urls = new HashSet<>();

        for (JsonNode imageNode : imageNodes) {
            String url = requiredText(imageNode, "image_url", index);
            int position = requiredInteger(imageNode, "position", index);

            if (!positions.add(position) || !urls.add(url)) {
                throw badRequest("Product " + index + ": duplicated image position or URL");
            }

            ProductImageRequest image = new ProductImageRequest();
            image.setImageUrl(url);
            image.setPosition(position);
            image.setVariant(optionalText(imageNode, "variant"));
            image.setSourceResolution(optionalText(imageNode, "source_resolution"));
            images.add(image);
        }

        if (!positions.equals(Set.of(1, 2, 3))) {
            throw badRequest("Product " + index + ": image positions must be 1, 2 and 3");
        }

        request.setImages(images);
        // source_file/source_line/source_categories are provenance metadata,
        // not fields in the current Product entity, so they are not persisted.
        return request;
    }

    private String requiredText(JsonNode object, String field, int productIndex) {
        JsonNode value = object.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw badRequest("Product " + productIndex + ": missing field '" + field + "'");
        }
        return value.asText().trim();
    }

    private String optionalText(JsonNode object, String field) {
        JsonNode value = object.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private BigDecimal requiredDecimal(JsonNode object, String field, int productIndex) {
        JsonNode value = object.path(field);
        if (!value.isNumber()) {
            throw badRequest("Product " + productIndex + ": invalid numeric field '" + field + "'");
        }
        return value.decimalValue();
    }

    private BigDecimal optionalDecimal(JsonNode object, String field, int productIndex) {
        JsonNode value = object.path(field);
        if (value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (!value.isNumber()) {
            throw badRequest("Product " + productIndex + ": invalid numeric field '" + field + "'");
        }
        return value.decimalValue();
    }

    private Integer optionalInteger(JsonNode object, String field, int productIndex) {
        JsonNode value = object.path(field);
        if (value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw badRequest("Product " + productIndex + ": invalid integer field '" + field + "'");
        }
        return value.intValue();
    }

    private int requiredInteger(JsonNode object, String field, int productIndex) {
        Integer value = optionalInteger(object, field, productIndex);
        if (value == null) {
            throw badRequest("Product " + productIndex + ": missing field '" + field + "'");
        }
        return value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record ImportResult(
            int totalInFile,
            int imported,
            int skippedExisting,
            int skippedDuplicatesInFile,
            String message
    ) {}
}
