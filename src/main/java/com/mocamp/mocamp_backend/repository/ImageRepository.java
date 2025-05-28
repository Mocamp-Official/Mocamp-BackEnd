package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    @Override
    Optional<ImageEntity> findById(Long aLong);

    Optional<ImageEntity> findByPath(String path);

    @Override
    <S extends ImageEntity> S save(S entity);
}
