package com.smartsquad.backend.repositories;

import com.smartsquad.backend.models.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    @Transactional
    void deleteByName(String name);


    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE players RESTART IDENTITY", nativeQuery = true)
    void deleteAll();

    PlayerEntity findByName(String name);

    List<PlayerEntity> findAllByNameIn(List<String> names);

}
