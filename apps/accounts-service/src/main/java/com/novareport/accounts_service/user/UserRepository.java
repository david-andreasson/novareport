package com.novareport.accounts_service.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByIsActiveTrue();

    @Override
    @NonNull
    <S extends User> S save(@NonNull S entity);
}
