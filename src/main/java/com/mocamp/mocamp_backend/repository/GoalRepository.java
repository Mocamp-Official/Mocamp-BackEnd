package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
}
