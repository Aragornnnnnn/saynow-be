package com.saynow.scenario.repository;

import com.saynow.scenario.domain.ContentStatus;
import com.saynow.scenario.domain.ScenarioCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioCategoryRepository extends JpaRepository<ScenarioCategory, Long> {

    List<ScenarioCategory> findByStatusOrderBySortOrderAsc(ContentStatus status);

    Optional<ScenarioCategory> findByCategoryKeyAndStatus(String categoryKey, ContentStatus status);
}
