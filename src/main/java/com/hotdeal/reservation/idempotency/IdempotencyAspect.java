package com.hotdeal.reservation.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdeal.reservation.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Around("@annotation(Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        String key = extractIdempotencyKey();

        Optional<ResponseEntity<Object>> cachedResponse = findCachedResponse(key);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        acquireProcessingLock(key);

        return executeAndCacheResponse(key, joinPoint);
    }

    private Optional<ResponseEntity<Object>> findCachedResponse(String key) {
        Optional<String> existing = idempotencyStore.find(key);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        String value = existing.get();
        if (idempotencyStore.isProcessing(value)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "요청을 처리 중입니다.");
        }

        return Optional.of(ResponseEntity.ok(deserialize(value)));
    }

    private void acquireProcessingLock(String key) {
        boolean acquired = idempotencyStore.markProcessing(key);
        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "요청을 처리 중입니다.");
        }
    }

    private Object executeAndCacheResponse(String key, ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();

            if (result instanceof ResponseEntity<?> responseEntity) {
                idempotencyStore.save(key, serialize(responseEntity.getBody()));
            }

            return result;
        } catch (Exception e) {
            idempotencyStore.delete(key);
            throw e;
        }
    }

    private String extractIdempotencyKey() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("요청 컨텍스트를 찾을 수 없습니다.");
        }

        HttpServletRequest request = attrs.getRequest();
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Idempotency-Key 헤더가 필요합니다.");
        }

        return key;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("응답 직렬화 실패", e);
        }
    }

    private Object deserialize(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("응답 역직렬화 실패", e);
        }
    }
}
