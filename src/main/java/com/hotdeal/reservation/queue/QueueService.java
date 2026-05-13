package com.hotdeal.reservation.queue;

import com.hotdeal.reservation.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final long FIRST_IN_LINE = 1;

    private final QueueRedisRepository queueRedisRepository;

    public Long enter(Long productId, Long userId) {
        try {
            boolean entered = queueRedisRepository.tryEnter(productId, userId);
            if (!entered) {
                throw new DuplicateEntryException("이미 대기열에 진입한 사용자입니다.");
            }

            queueRedisRepository.addToQueue(productId, userId);
            return queueRedisRepository.getRank(productId, userId);
        } catch (DuplicateEntryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 장애로 대기열 진입을 건너뜁니다.", e);
            return null;
        }
    }

    public void validateIsFirstInLine(Long productId, Long userId) {
        try {
            Long rank = queueRedisRepository.getRank(productId, userId);
            if (rank != null && rank != FIRST_IN_LINE) {
                throw new BadRequestException("아직 결제할 수 있는 순번이 아닙니다.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 장애로 순번 검증을 건너뜁니다.", e);
        }
    }

    public void removeEntry(Long productId, Long userId) {
        queueRedisRepository.removeEntry(productId, userId);
    }

    public Long getRank(Long productId, Long userId) {
        return queueRedisRepository.getRank(productId, userId);
    }

    public void leave(Long productId, Long userId) {
        try {
            queueRedisRepository.popFirst(productId);
            queueRedisRepository.removeEntry(productId, userId);
        } catch (Exception e) {
            log.warn("Redis 장애로 대기열 제거를 건너뜁니다.", e);
        }
    }
}