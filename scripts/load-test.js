import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const checkoutSuccess = new Counter('checkout_success');
const checkoutFail = new Counter('checkout_fail');
const bookingSuccess = new Counter('booking_success');
const bookingFail = new Counter('booking_fail');

export const options = {
    scenarios: {
        concurrent_checkout: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 100,
            maxDuration: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
    },
};

export default function () {
    const userId = __VU;

    // 1. Checkout
    const checkoutRes = http.get(`${BASE_URL}/checkout?productId=1`, {
        headers: { userId: `${userId}` },
    });

    if (checkoutRes.status === 200) {
        checkoutSuccess.add(1);
        const body = JSON.parse(checkoutRes.body);
        const bookingId = body.bookingId;
        const idempotencyKey = body.idempotencyKey;
        const rank = body.rank;

        // 2. Polling (rank 1이 될 때까지)
        let ready = rank === 1;
        let attempts = 0;
        while (!ready && attempts < 60) {
            sleep(1);
            const statusRes = http.get(`${BASE_URL}/queue-status?productId=1`, {
                headers: { userId: `${userId}` },
            });
            if (statusRes.status === 200) {
                const status = JSON.parse(statusRes.body);
                ready = status.status === 'READY';
            }
            attempts++;
        }

        if (!ready) {
            bookingFail.add(1);
            return;
        }

        // 3. Booking
        const bookingPayload = JSON.stringify({
            productId: 1,
            paymentMethods: [{ type: 'CREDIT_CARD', amount: 100000 }],
        });

        const headers = {
            'Content-Type': 'application/json',
            userId: `${userId}`,
        };
        if (idempotencyKey) {
            headers['Idempotency-Key'] = idempotencyKey;
        }

        const bookingRes = http.post(`${BASE_URL}/bookings/${bookingId}`, bookingPayload, {
            headers: headers,
        });

        if (bookingRes.status === 200) {
            bookingSuccess.add(1);
        } else {
            bookingFail.add(1);
        }
    } else {
        checkoutFail.add(1);
    }
}