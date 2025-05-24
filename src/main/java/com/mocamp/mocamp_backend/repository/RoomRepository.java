package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    @Override
    Optional<RoomEntity> findById(Long aLong);

    Optional<RoomEntity> findByRoomSeq(String roomSeq);

    @Override
    <S extends RoomEntity> S save(S entity);
}
