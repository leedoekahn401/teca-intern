package com.example.demo.product.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.product.dto.CategoryResponse;
import com.example.demo.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for retrieving product categories")
public class CategoryController {

    private final ProductService productService;

    @Operation(summary = "Get all categories", description = "Retrieves a complete list of product categories.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", response));
    }
}
