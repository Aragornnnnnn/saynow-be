package com.saynow.practice.repository;

import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeTurnRepository extends JpaRepository<PracticeTurn, Long> {

    long countBySession(PracticeSession session);

    List<PracticeTurn> findBySessionOrderByTurnIndexAsc(PracticeSession session);
}
