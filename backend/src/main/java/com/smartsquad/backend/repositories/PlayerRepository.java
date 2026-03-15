package com.smartsquad.backend.repositories;

import com.smartsquad.backend.models.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


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

    /**
     * Finds all players who are currently forced to be with a specific player.
     * Essential for reciprocal updates (e.g., if Bale must be with Messi, finding Bale when Messi is updated).
     */
    List<PlayerEntity> findByHasToBeWith(String name);

    /**
     * Finds all players who are currently forced to stay away from a specific player.
     */
    List<PlayerEntity> findByCannotBeWith(String name);

    /**
     * Updates the skill level of a player.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlayerEntity p SET p.skillLevel = :skillLevel WHERE p.name = :name")
    int updateSkillLevelByName(@Param("name") String name, @Param("skillLevel") int skillLevel);

    /**
     * Updates the 'has_to_be_with' constraint for a player.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlayerEntity p SET p.hasToBeWith = :hasToBeWith WHERE p.name = :name")
    void updateHasToBeWithByName(@Param("name") String name, @Param("hasToBeWith") String hasToBeWith);

    /**
     * Updates the 'cannot_be_with' constraint for a player.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PlayerEntity p SET p.cannotBeWith = :cannotBeWith WHERE p.name = :name")
    void updateCannotBeWithByName(@Param("name") String name, @Param("cannotBeWith") String cannotBeWith);

}

