package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.ScenarioCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioCategoryRepository extends JpaRepository<ScenarioCategory, Long> {

    List<ScenarioCategory> findAllByOrderByIdAsc();

    Optional<ScenarioCategory> findByCategoryKey(String categoryKey);
}
