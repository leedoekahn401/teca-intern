package com.example.demo.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private List<CategoryResponse> categories;
    private List<TagResponse> tags;
    private List<ProductImageResponse> images;
    private List<String> availableColors;
    private List<String> availableSizes;
    private List<ProductVariantResponse> variants;
}
