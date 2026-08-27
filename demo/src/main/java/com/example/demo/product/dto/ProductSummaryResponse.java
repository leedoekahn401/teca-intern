package com.example.demo.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {
    private UUID id;
    private String name;
    private String description;
    private String primaryImageUrl;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private List<CategoryResponse> categories;
    private List<TagResponse> tags;
}
