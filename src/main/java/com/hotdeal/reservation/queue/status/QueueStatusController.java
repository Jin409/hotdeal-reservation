package com.hotdeal.reservation.queue.status;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QueueStatusController {

    private final QueueStatusService queueStatusService;

    @GetMapping("/queue-status")
    public ResponseEntity<QueueStatusResponse> getStatus(
            @RequestParam Long productId,
            @RequestHeader("userId") Long userId) {
        QueueStatusResponse response = queueStatusService.getStatus(productId, userId);
        return ResponseEntity.ok(response);
    }
}
