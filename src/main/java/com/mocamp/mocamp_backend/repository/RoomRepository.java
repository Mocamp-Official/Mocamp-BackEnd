package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

}
