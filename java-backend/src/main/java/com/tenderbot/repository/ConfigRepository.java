package com.tenderbot.repository;

import com.tenderbot.entity.Config;
import com.tenderbot.entity.ConfigGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
    Optional<Config> findByKey(String key);
    List<Config> findByGroup(ConfigGroup group);
    boolean existsByKey(String key);
}
