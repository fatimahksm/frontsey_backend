package com.dbwb.platform.theme.repository;

import com.dbwb.platform.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {
    List<Theme> findByActiveTrue();
}
