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
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.result.model.QuizDefResult
import skills.storage.repos.QuizDefRepo

@Service
@Slf4j
class QuizDefService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    QuizDefRepo quizDefRepo

    @Transactional(readOnly = true)
    List<QuizDefResult> getCurrentUsersTestDefs() {
        boolean isRoot = userInfoService.isCurrentUserASuperDuperUser()

        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<QuizDefResult> res = []

        List<QuizDefRepo.QuizDefSummaryResult> fromDb = quizDefRepo.getQuizDefSummariesByUser(userId)
        if (fromDb) {
            res.addAll(fromDb.collect {convert(it) })
        }


//        Map<String, Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)
//        List<ProjectResult> finalRes
//        if (isRoot) {
//            finalRes = loadProjectsForRoot(projectIdSortOrder, userId)
//        } else {
//            // sql join with UserRoles and there is 1-many relationship that needs to be normalized
//            List<ProjSummaryResult> projects = projDefRepo.getProjectSummariesByUser(userId)
//            finalRes = projects?.unique({ it.projectId })?.collect({
//                ProjectResult res = convert(it, projectIdSortOrder)
//                return res
//            })
//        }
//
//        finalRes.sort() { it.displayOrder }
//
//        if (finalRes) {
//            finalRes.first().isFirst = true
//            finalRes.last().isLast = true

        return res
    }

    private QuizDefResult convert(QuizDefRepo.QuizDefSummaryResult quizDefSummaryResult) {
        new QuizDefResult(
                quizId: quizDefSummaryResult.getQuizId(),
                name: quizDefSummaryResult.getName(),
                created: quizDefSummaryResult.getCreated(),
                displayOrder: 0 // todo
        )
    }
}
