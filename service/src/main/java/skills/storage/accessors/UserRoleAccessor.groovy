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
package skills.storage.accessors


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRoleRepo

@Service
class UserRoleAccessor {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    static class ProjectsAndQuizzes {
        List<String> projectIds
        List<String> quizIds
    }

    @Transactional
    ProjectsAndQuizzes getCurrentUserAdminProjectsAndQuizzes() {
        String userId = userInfoService.currentUser.username
        List<UserRole> allUserRoles = userRoleRepo.findAllByUserId(userId)
        List<String> projectIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_PROJECT_ADMIN})?.projectId?.unique()
        List<String> quizIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_QUIZ_ADMIN})?.quizId?.unique()

        return new ProjectsAndQuizzes(projectIds: projectIds, quizIds: quizIds)
    }
}
