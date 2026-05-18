package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findAllByOrderByCategoryIdAscDisplayOrderAsc();

    List<Scenario> findByCategoryOrderByDisplayOrderAsc(Category category);
}
