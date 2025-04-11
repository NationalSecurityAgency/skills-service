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

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.storage.model.UserAttrs

interface UserAttrsRepo extends JpaRepository<UserAttrs, Integer> {

    @Nullable
    UserAttrs findByUserIdIgnoreCase(String userId)

    @Nullable
    @Query('''select attrs from UserAttrs attrs, User user where attrs.userId = user.userId and user.id = ?1''')
    UserAttrs findUserAttrsByUserTableRefId(Integer userTableRefId)

    @Query('''select attrs 
        from User u, UserAttrs attrs 
        where
            u.userId = attrs.userId and
            (lower(CONCAT(attrs.firstName, ' ', attrs.lastName, ' (',  attrs.userIdForDisplay, ')')) like lower(CONCAT('%', ?1, '%')) OR 
             lower(attrs.userIdForDisplay) like lower(CONCAT('%', ?1, '%')))
        order by attrs.firstName asc''')
    List<UserAttrs> searchForUser(String userIdQuery, Pageable pageable)

    @Nullable
    @Query(value='''select attrs.email from user_attrs attrs where attrs.user_id = ?1''', nativeQuery = true)
    String findEmailByUserId(String userId)

    @Query("SELECT DISTINCT(ua.userId) from UserAttrs ua where lower(ua.userId) LIKE %?1% order by ua.userId asc" )
    List<String> findDistinctUserIdForDisplay(String userUserIdForDisplayQuery, Pageable pageable)

}
