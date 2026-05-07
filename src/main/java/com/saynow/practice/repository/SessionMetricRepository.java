package com.saynow.practice.repository;

import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.SessionMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionMetricRepository extends JpaRepository<SessionMetric, Long> {

    Optional<SessionMetric> findBySessionAndMetricKey(PracticeSession session, String metricKey);
}
