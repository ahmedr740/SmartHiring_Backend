package com.smarthiring.repository;

import com.smarthiring.model.AiMatchCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiMatchCacheRepository extends JpaRepository<AiMatchCache, Long> {
    Optional<AiMatchCache> findByCacheKey(String cacheKey);
}
