package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinedRoomRepository extends JpaRepository<JoinedRoomEntity, Long> {
    @Override
    Optional<JoinedRoomEntity> findById(Long aLong);

    @Override
    <S extends JoinedRoomEntity> S save(S entity);

    boolean existsByRoomAndUser(RoomEntity room, UserEntity user);

    List<JoinedRoomEntity> findAllByUser(UserEntity user);

    Optional<JoinedRoomEntity> findByRoomAndUser(RoomEntity room, UserEntity user);

    List<JoinedRoomEntity> findAllByRoom(RoomEntity room);

    Optional<JoinedRoomEntity> findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(Long roomId, Long userId);

    List<JoinedRoomEntity> findByRoom_RoomIdAndIsParticipatingTrue(Long roomId);

    boolean existsByRoom_RoomIdAndUser_UserIdAndIsAdminTrue(Long roomId, Long userId);

    JoinedRoomEntity findByUserAndRoom_RoomId(UserEntity user, Long roomId);
}
