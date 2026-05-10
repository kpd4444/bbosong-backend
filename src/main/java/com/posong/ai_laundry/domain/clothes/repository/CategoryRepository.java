package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByName(String name);
}
