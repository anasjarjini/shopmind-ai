package com.shopmind.backend.controller;

import com.shopmind.backend.service.ProductImportService;
import com.shopmind.backend.service.ProductImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService productImportService;
    
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResult> importProducts(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productImportService.importProducts(file));
    }
}
