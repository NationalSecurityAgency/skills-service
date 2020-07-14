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
package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.auth.PasswordResetToken

interface PasswordResetTokenRepo extends CrudRepository<PasswordResetToken, Integer> {

    @Nullable
    PasswordResetTokenRepo findByToken(String token)

    @Nullable
    @Query("select p from PasswordResetToken p where p.user.userId = ?1")
    PasswordResetToken findByUserId(String userId)

    @Nullable
    @Query("select p from PasswordResetToken  p where p.token = ?1 and p.user.userId = ?2")
    PasswordResetToken findByTokenAndUserId(String token, String userId)

    void deleteByToken(String token)

}
