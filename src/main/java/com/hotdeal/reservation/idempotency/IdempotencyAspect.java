package com.hotdeal.reservation.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdeal.reservation.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Around("@annotation(Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        String key = extractIdempotencyKey();

        try {
            Optional<ResponseEntity<Object>> cachedResponse = findCachedResponse(key);
            if (cachedResponse.isPresent()) {
                return cachedResponse.get();
            }

            acquireProcessingLock(key);
        } catch (BadRequestException | ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 장애가 발생해 멱등성 확인을 생략합니다.", e);
            return joinPoint.proceed();
        }

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
                try {
                    idempotencyStore.save(key, serialize(responseEntity.getBody()));
                } catch (Exception e) {
                    log.warn("Redis 멱등성 응답 캐싱 실패.", e);
                }
            }

            return result;
        } catch (Exception e) {
            try {
                idempotencyStore.delete(key);
            } catch (Exception redisEx) {
                log.warn("Redis 멱등성 키 삭제 실패.", redisEx);
            }
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