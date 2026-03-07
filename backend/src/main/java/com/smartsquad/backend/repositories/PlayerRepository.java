package com.smartsquad.backend.repositories;

import com.smartsquad.backend.models.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    @Transactional
    public void deleteByName(String name);
}
