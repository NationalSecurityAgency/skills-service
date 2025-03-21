/**
 * Copyright 2025 SkillTree
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
package skills.storage

import java.util.concurrent.atomic.AtomicInteger

class ReadOnlyDataSourceContext {

    private static final ThreadLocal<AtomicInteger> READ_ONLY_LEVEL = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    static boolean isReadOnly() {
        return READ_ONLY_LEVEL.get().get() > 0;
    }

    static void start() {
        READ_ONLY_LEVEL.get().incrementAndGet();
    }

    static void end() {
        READ_ONLY_LEVEL.get().decrementAndGet();
    }
}
