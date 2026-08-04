package com.dbwb.platform.testsupport;

import com.dbwb.platform.common.entity.BaseEntity;

import java.util.UUID;

/**
 * BaseEntity.id is DB-generated in production (no public setter), but unit
 * tests that mock repositories need entities with a stable, known id to
 * return from findById(...)/equality checks. Centralized here so every test
 * that needs it shares one implementation instead of duplicating reflection.
 */
public final class TestEntities {

    private TestEntities() {
    }

    public static <T extends BaseEntity> T withId(T entity, UUID id) {
        try {
            var field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
