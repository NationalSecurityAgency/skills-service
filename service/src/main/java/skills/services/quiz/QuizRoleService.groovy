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
package skills.services.quiz

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserNameService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.storage.model.QuizDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo

import javax.transaction.Transactional

@Service
@Slf4j
class QuizRoleService {
    private static Set<RoleName> quizSupportedRoles = [RoleName.ROLE_QUIZ_ADMIN].toSet()

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    skills.auth.UserInfoService userInfoService

    @Autowired
    UserNameService userNameService

    @Autowired
    QuizDefRepo quizDefRepo

    @Transactional
    void addQuizRole(String userIdParam, String quizId, RoleName roleName) {
        QuizDef quizDef = findQuizDef(quizId)
        ensureValidRole(roleName, quizDef.quizId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillQuizException("Cannot add roles to myself. userId=[${userId}]", quizId, ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.addQuizDefUserRole(userId, quizDef.quizId, roleName)
    }

    List<UserRoleRes> getQuizUserRoles(String quizId) {
        QuizDef quizDef = findQuizDef(quizId)
        return accessSettingsStorageService.findAllQuizRoles(quizDef.quizId)
    }

    @Transactional
    void deleteQuizRole(String userIdParam, String quizId, RoleName roleName) {
        QuizDef quizDef = findQuizDef(quizId)
        ensureValidRole(roleName, quizDef.quizId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillQuizException("Cannot remove roles from myself. userId=[${userId}]", quizId, ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.deleteQuizUserRole(userId, quizDef.quizId, roleName)
    }

    private void ensureValidRole(RoleName roleName, String quizId) {
        if (!quizSupportedRoles.contains(roleName)) {
            throw new SkillQuizException("Provided [${roleName}] is not a quiz role.", quizId, ErrorCode.BadParam)
        }
    }

    private QuizDef findQuizDef(String quizId) {
        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!updatedDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return updatedDef
    }
}
