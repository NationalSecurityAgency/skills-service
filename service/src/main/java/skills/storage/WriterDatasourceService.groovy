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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import skills.services.LockingService
import skills.storage.model.UserAttrs
import skills.storage.model.UserTag
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo
import skills.storage.repos.UserTagRepo

@Service
@Slf4j
class WriterDatasourceService {

    @Autowired
    JdbcTemplate writerJdbcTemplate

    @Autowired
    UserRepo userRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    LockingService lockingService

    private boolean isWriterDataSource() {
        return !ReadOnlyDataSourceContext.readOnly
    }

    void insertUser(User user) {
        if (isWriterDataSource()) {
            userRepo.save(user)
            return
        }
        String sql = "INSERT INTO public.users ( user_id, password) VALUES (${param(user.userId)}, ${param(user.password)} );"
        writerJdbcTemplate.execute(sql)
    }

    Integer getUserRefId(String userId) {
        if (isWriterDataSource()) {
            return userRepo.findIdByUserId(userId)
        }

        String sql = "select id from users where user_id = ${param(userId)}"
        return writerJdbcTemplate.queryForObject(sql, Integer.class)
    }

    void insertUserRoles(List<UserRole> roles) {
        if (isWriterDataSource()) {
            userRoleRepo.saveAll(roles)
            return
        }
        roles.each {
            String sql = "INSERT INTO user_roles (user_ref_id, user_id, role_name, project_id,  quiz_id, admin_group_id) " +
                    "VALUES (" +
                    "${it.userRefId}, ${param(it.userId)}, ${param(it.roleName.toString())}, ${param(it.projectId)}, ${param(it.quizId)}, ${param(it.adminGroupId)});"
            writerJdbcTemplate.execute(sql)
        }
    }

    void insertUserAttrs(UserAttrs userAttrs) {
        if (isWriterDataSource()) {
            userAttrsRepo.save(userAttrs)
            return
        }
        String insertSql = "insert into user_attrs " +
                "(user_id, first_name, last_name, " +
                "dn, email, nickname, " +
                "user_id_for_display, email_verified" +
                ") values (${getUserAttrsSqlParams(userAttrs)})"
        writerJdbcTemplate.execute(insertSql)
    }
    void updateUserAttrs(UserAttrs userAttrs) {
        if (isWriterDataSource()) {
            userAttrsRepo.save(userAttrs)
            return
        }
        String updateSql = "UPDATE user_attrs SET " +
                "user_id = ${param(userAttrs.userId)}, " +
                "first_name = ${param(userAttrs.firstName)}, " +
                "last_name = ${param(userAttrs.lastName)}, " +
                "dn = ${param(userAttrs.dn)}, " +
                "email = ${param(userAttrs.email)}, " +
                "nickname = ${param(userAttrs.nickname)}, " +
                "user_id_for_display = ${param(userAttrs.userIdForDisplay)} " +
                "WHERE id = ${userAttrs.id}"
        writerJdbcTemplate.execute(updateSql)
    }

    @Profile
    void lockUser(String userId) {
        log.debug("locking user [{}]", userId)
        if (isWriterDataSource()) {
            lockingService.lockForUserCreateOrUpdate(userId)
            return
        }
        writerJdbcTemplate.execute("select * from f_select_lock_and_insert('${userId}');".toString())
    }

    void deleteUserTagsByUserId(String userId) {
        if (isWriterDataSource()) {
            userTagRepo.deleteByUserId(userId)
            return
        }
        String sql = "delete from user_tags where lower(user_id) = lower('${userId}')".toString()
        writerJdbcTemplate.execute(sql)
    }

    void saveUserTags(List<UserTag> userTags) {
        if (isWriterDataSource()) {
            userTagRepo.saveAll(userTags)
            return
        }
        userTags.each {
            String sql = "INSERT INTO user_tags (user_id, key, value) VALUES (${param(it.userId)}, ${param(it.key)}, ${param(it.value)});"
            writerJdbcTemplate.execute(sql)
        }
    }

    private static String getUserAttrsSqlParams(UserAttrs userAttrs) {
        List<String> params = [
                userAttrs.userId,
                userAttrs.firstName,
                userAttrs.lastName,
                userAttrs.dn,
                userAttrs.email,
                userAttrs.nickname,
                userAttrs.userIdForDisplay,
                'false'
        ]
        return params.collect { param(it)}.join(", ")
    }

    private static String param(String val) {
        if (val == null) {
            return "null"
        }
        return "'${val}'"
    }

}
