/**
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.openai

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Fixed-window, per-instance admission control; no lock is held while streaming. */
@Component
class OpenAIUsageLimiter {
    private static final long WINDOW = TimeUnit.MINUTES.toNanos(1)
    private static final int MAX_TRACKED_USERS = 10_000
    private final OpenAIUsageLimitsProperties limits
    private final Map<String, Counter> users = [:]
    private Counter global
    private int active

    OpenAIUsageLimiter(OpenAIUsageLimitsProperties limits) {
        this.limits = limits
    }

    protected long nanoTime() { System.nanoTime() }

    synchronized Permit acquire(String userId) {
        long now = nanoTime()
        if (global == null || expired(global, now)) {
            global = new Counter(started: now)
        }
        // Expire only elapsed windows; capacity pressure must never reset a live quota.
        users.entrySet().removeIf { expired(it.value, now) }
        Counter user = users[userId] ?: new Counter(started: now)
        checkRate(global, limits.requestsPerMinuteGlobal, now, 'Global AI request rate exceeded')
        checkRate(user, limits.requestsPerMinutePerUser, now, 'User AI request rate exceeded')
        if (active >= limits.maxConcurrentRequestsGlobal) {
            reject('AI concurrent request limit exceeded', null)
        }
        if (!users.containsKey(userId) && users.size() >= MAX_TRACKED_USERS) {
            reject('AI request tracking capacity exceeded', '60')
        }
        // Commit both allowances only after all checks succeed.
        global.count++
        user.count++
        users[userId] = user
        active++
        return new Permit({ release() })
    }

    private synchronized void release() { active-- }

    private static boolean expired(Counter counter, long now) { now - counter.started >= WINDOW }

    private static void checkRate(Counter counter, int limit, long now, String reason) {
        if (counter.count >= limit) {
            long seconds = Math.max(1L, (long) Math.ceil((WINDOW - (now - counter.started)) / 1_000_000_000d))
            reject(reason, seconds.toString())
        }
    }

    private static void reject(String reason, String retryAfter) {
        throw new OpenAIProviderErrors.ProviderException(HttpStatus.TOO_MANY_REQUESTS, reason, retryAfter)
    }

    private static class Counter {
        long started
        int count
    }

    static class Permit implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean()
        private final Closure release

        Permit(Closure release) { this.release = release }

        @Override
        void close() {
            if (closed.compareAndSet(false, true)) {
                release.call()
            }
        }
    }
}
