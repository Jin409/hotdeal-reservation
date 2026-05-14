package com.hotdeal.reservation.queue;

import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.common.exception.DuplicateEntryException;
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
            return doEnter(productId, userId);
        } catch (DuplicateEntryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("대기열 진입에 실패하여 건너뜁니다.", e);
            return null;
        }
    }

    private Long doEnter(Long productId, Long userId) {
        boolean entered = queueRedisRepository.tryEnter(productId, userId);
        if (!entered) {
            throw new DuplicateEntryException("이미 대기열에 진입한 사용자입니다.");
        }

        queueRedisRepository.addToQueue(productId, userId);
        queueRedisRepository.addActiveProduct(productId);
        Long rank = queueRedisRepository.getRank(productId, userId);

        markReadyIfFirstInLine(productId, userId, rank);

        return rank;
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

    public void leave(Long productId, Long userId) {
        try {
            queueRedisRepository.removeFromQueue(productId, userId);
            queueRedisRepository.removeEntry(productId, userId);
            queueRedisRepository.removeReady(productId, userId);
            markNextUserReady(productId);
        } catch (Exception e) {
            log.warn("대기열 제거에 실패하여 건너뜁니다.", e);
        }
    }

    public void evictReadyExpiredUser(Long productId) {
        try {
            Long firstUserId = queueRedisRepository.getFirstUserId(productId);
            if (firstUserId == null) {
                return;
            }

            if (queueRedisRepository.isReady(productId, firstUserId)) {
                return;
            }

            log.info("대기열 1번 사용자(userId={}) 결제 시간 만료. 대기열에서 제거합니다.", firstUserId);
            leave(productId, firstUserId);
        } catch (Exception e) {
            log.warn("만료된 대기열 사용자 제거에 실패했습니다.", e);
        }
    }

    private void markReadyIfFirstInLine(Long productId, Long userId, Long rank) {
        if (rank != null && rank == FIRST_IN_LINE) {
            queueRedisRepository.markReady(productId, userId);
        }
    }

    private void markNextUserReady(Long productId) {
        Long nextUserId = queueRedisRepository.getFirstUserId(productId);
        if (nextUserId != null) {
            queueRedisRepository.markReady(productId, nextUserId);
            return;
        }
        queueRedisRepository.removeActiveProduct(productId);
    }
}