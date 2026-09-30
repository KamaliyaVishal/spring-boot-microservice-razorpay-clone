package com.vault_service.repository;

import com.vault_service.entity.VaultCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultCardRepository extends JpaRepository<VaultCard, Long> {
}