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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.storage.model.UserAttrs

@Service
class UserAttrsTestServiceWrapper {

    @Autowired
    UserAttrsService userAttrsService

    @Transactional
    UserAttrs saveUserAttrs(String userId, UserInfo userInfo) {
        return userAttrsService.saveUserAttrs(userId, userInfo)
    }
}
