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
            Long rank = queueRedisRepository.getRank(productId, userId);

            if (rank != null && rank == FIRST_IN_LINE) {
                queueRedisRepository.markReady(productId, userId);
            }

            return rank;
        } catch (DuplicateEntryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("대기열 진입에 실패하여 건너뜁니다.", e);
            return null;
        }
    }

    public void validateIsReady(Long productId, Long userId) {
        try {
            if (!queueRedisRepository.isReady(productId, userId)) {
                throw new BadRequestException("아직 결제할 수 있는 순번이 아닙니다.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("결제 가능 상태 검증에 실패하여 건너뜁니다.", e);
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
            queueRedisRepository.removeReady(productId, userId);
            markNextUserReady(productId);
        } catch (Exception e) {
            log.warn("대기열 제거에 실패하여 건너뜁니다.", e);
        }
    }

    private void markNextUserReady(Long productId) {
        Long nextUserId = queueRedisRepository.getFirstUserId(productId);
        if (nextUserId != null) {
            queueRedisRepository.markReady(productId, nextUserId);
        }
    }
}