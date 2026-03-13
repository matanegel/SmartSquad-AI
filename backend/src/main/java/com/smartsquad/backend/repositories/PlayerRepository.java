package com.smartsquad.backend.repositories;

import com.smartsquad.backend.models.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    @Transactional
    void deleteByName(String name);

    PlayerEntity findByName(String name);

    List<PlayerEntity> findAllByNameIn(List<String> names);

}
