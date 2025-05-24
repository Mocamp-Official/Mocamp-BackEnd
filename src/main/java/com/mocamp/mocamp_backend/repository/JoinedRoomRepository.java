package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JoinedRoomRepository extends JpaRepository<JoinedRoomEntity, Long> {
    @Override
    Optional<JoinedRoomEntity> findById(Long aLong);

    @Override
    <S extends JoinedRoomEntity> S save(S entity);

    boolean existsByRoomAndUser(RoomEntity room, UserEntity user);
    Optional<JoinedRoomEntity> findByRoomAndUser(RoomEntity room, UserEntity user);
}
