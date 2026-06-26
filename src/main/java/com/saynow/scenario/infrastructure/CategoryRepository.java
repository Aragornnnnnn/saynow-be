// 시나리오 카테고리를 조회하는 JPA 저장소
package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByDisplayOrderAsc();
}
