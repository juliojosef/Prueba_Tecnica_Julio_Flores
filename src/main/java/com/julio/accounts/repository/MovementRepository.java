package com.julio.accounts.repository;

import com.julio.accounts.domain.Movement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MovementRepository
        extends JpaRepository<Movement, Long> {

    Optional<Movement> findByAccount_IdAndIdempotencyKey(
        UUID accountId,
        String idempotencyKey
    );
}