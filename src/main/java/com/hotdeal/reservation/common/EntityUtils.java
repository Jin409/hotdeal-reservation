package com.hotdeal.reservation.common;

import com.hotdeal.reservation.common.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EntityUtils {

    private final EntityManager entityManager;

    public <T> T getEntity(Long id, Class<T> entityType) {
        return Optional.ofNullable(entityManager.find(entityType, id))
                .orElseThrow(() -> new NotFoundException(entityType.getSimpleName()));
    }
}
