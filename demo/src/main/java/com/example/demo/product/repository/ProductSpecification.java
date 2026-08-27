package com.example.demo.product.repository;

import com.example.demo.product.entity.Category;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            UUID categoryId,
            UUID tagId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // Keyword filter
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            // Category filter
            if (categoryId != null) {
                Join<Product, Category> categoryJoin = root.join("categories", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("id"), categoryId));
            }

            // Tag filter
            if (tagId != null) {
                Join<Product, Tag> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(tagJoin.get("id"), tagId));
            }

            // Min & Max Price filter on variants
            if (minPrice != null || maxPrice != null) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(variantJoin.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(variantJoin.get("price"), maxPrice));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
