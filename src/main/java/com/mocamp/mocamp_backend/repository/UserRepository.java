package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findUserByUserSeq(String userSeq);

    Optional<UserEntity> findUserByEmail(String email);

    Optional<UserEntity> findUserByUserId(Long userId);

    @Override
    <S extends UserEntity> S save(S entity);

    @Override
    void delete(UserEntity entity);
}
