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
package skills.quizLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserInfoService
import skills.controller.result.model.QuizAttemptRowResult
import skills.services.admin.InviteOnlyProjectService
import skills.services.admin.UserCommunityService
import skills.storage.repos.QuizToSkillDefRepo

@Service
@Slf4j
class QuizAssociatedSkillsService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    List<QuizAttemptRowResult.AssociatedSkill> getAssociatedSkills(List<String> quizIds) {
        String userId = userInfoService.currentUserId
        List<QuizToSkillDefRepo.AssociatedSkill> associatedSkills = quizToSkillDefRepo.findAssociatedSkillsWhereQuizIdIn(quizIds)

        List<QuizAttemptRowResult.AssociatedSkill> res = null
        if (associatedSkills) {
            List<String> userCommunityProjects = associatedSkills.findAll { it.userCommunityProj }?.collect { it.projectId }?.unique()
            if (userCommunityProjects) {
                Boolean isUserCommunityMember = userCommunityService.isUserCommunityMember(userId)
                if (!isUserCommunityMember) {
                    List<String> projectsUserCannotAccess = userCommunityProjects.findAll { userCommunityService.isUserCommunityOnlyProject(it) }
                    associatedSkills = associatedSkills.findAll { !projectsUserCannotAccess.contains(it.projectId) }
                }
            }

            List<String> inviteOnlyProjectIds = associatedSkills.findAll { it.inviteOnlyProj }.collect { it.projectId }.unique()
            if (inviteOnlyProjectIds) {
                List<String> projectsUserCannotAccess = inviteOnlyProjectIds.findAll { !inviteOnlyProjectService.canUserAccess(it, userId) }
                associatedSkills = associatedSkills.findAll { !projectsUserCannotAccess.contains(it.projectId) }
            }

            res = associatedSkills.collect {
                new QuizAttemptRowResult.AssociatedSkill(
                        quizId: it.quizId,
                        skillId: it.skillId,
                        skillName: it.skillName,
                        projectId: it.projectId,
                        projectName: it.projectName,
                        subjectId: it.subjectId
                )
            }?.sort { it.skillName }
        }

        return res
    }
}
