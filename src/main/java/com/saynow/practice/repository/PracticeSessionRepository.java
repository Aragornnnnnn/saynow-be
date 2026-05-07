package com.saynow.practice.repository;

import com.saynow.practice.domain.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    Optional<PracticeSession> findByPublicId(String publicId);
}
