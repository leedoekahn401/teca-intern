package com.example.demo.product.repository;

import com.example.demo.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentIsNull();
    Optional<Category> findByNameIgnoreCase(String name);
}
