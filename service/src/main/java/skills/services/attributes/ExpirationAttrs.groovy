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
package skills.services.attributes

import groovy.transform.Canonical

@Canonical
class ExpirationAttrs {

    // expiration type
    static final String NEVER = 'NEVER'
    static final String YEARLY = 'YEARLY'
    static final String MONTHLY = 'MONTHLY'
    static final String DAILY = 'DAILY'

    // type specific attributes
    static final String LAST_DAY_OF_MONTH = 'LAST_DAY_OF_MONTH'

    String expirationType = NEVER
    Integer every
    String monthlyDay
    Date nextExpirationDate
}
