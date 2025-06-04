package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.GoalEntity;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
    @Override
    Optional<GoalEntity> findById(Long aLong);

    List<GoalEntity> findAllByJoinedRoom(JoinedRoomEntity joinedRoom);

    @Override
    <S extends GoalEntity> S save(S entity);
}
