package com.hotdeal.reservation.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final EntityManager entityManager;

    @Transactional
    public void clear() {
        entityManager.flush();

        List<String> tableNames = entityManager.getMetamodel().getEntities().stream()
                .map(this::getTableName)
                .toList();

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private String getTableName(EntityType<?> entity) {
        Table table = entity.getJavaType().getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name();
        }
        return entity.getName().toLowerCase();
    }
}