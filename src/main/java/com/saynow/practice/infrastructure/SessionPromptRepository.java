package com.saynow.practice.infrastructure;

import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PromptType;
import com.saynow.practice.domain.SessionPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionPromptRepository extends JpaRepository<SessionPrompt, Long> {

    Optional<SessionPrompt> findFirstBySessionOrderByPromptIndexDesc(PracticeSession session);

    List<SessionPrompt> findBySessionOrderByPromptIndexAsc(PracticeSession session);

    long countBySessionAndPromptType(PracticeSession session, PromptType promptType);
}
