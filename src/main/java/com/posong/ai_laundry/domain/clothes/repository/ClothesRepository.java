package com.posong.ai_laundry.domain.clothes.repository;

import com.posong.ai_laundry.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {
}
