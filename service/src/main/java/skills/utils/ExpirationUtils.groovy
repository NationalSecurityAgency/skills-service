/**
 * Copyright 2020 SkillTree
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
package skills.utils

import org.apache.commons.lang3.time.DurationFormatUtils

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ExpirationUtils {

    public static Expiration getExpiration(String iso8601Duration) {
        Duration validityPeriod = Duration.parse(iso8601Duration)
        LocalDateTime expires = LocalDateTime.now().plus(validityPeriod)

        Date expiresOn = Date.from(expires.atZone(ZoneId.systemDefault()).toInstant())
        String validFor = DurationFormatUtils.formatDurationWords(validityPeriod.toMillis(), true, true)

        Expiration expiration = new Expiration(expiresOn: expiresOn, validFor: validFor)

        return expiration
    }
}
