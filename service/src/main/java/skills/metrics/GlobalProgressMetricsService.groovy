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
package skills.metrics


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import skills.auth.UserInfoService
import skills.controller.result.model.globalMetrics.GlobalMetricsResult
import skills.controller.result.model.globalMetrics.GlobalMetricsUserItem
import skills.storage.model.QuizDefParent
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.GlobalProgressMetricsRepo
import skills.storage.repos.UserRoleRepo

@Service
@Slf4j
class GlobalProgressMetricsService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    MetricsService metricsServiceNew

    GlobalMetricsResult loadMetrics(String userQuery, PageRequest pageRequest) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds

        Page<GlobalProgressMetricsRepo.UserProgressMetric> userProgressMetricPage = globalProgressMetricsRepo.findUsersOverallProgress(projectIds, quizIds, pageRequest)
        List<GlobalMetricsUserItem> metricItemsPage = userProgressMetricPage.getContent().collect {
            new GlobalMetricsUserItem(
                    userId: it.userId,
                    numProjects: it.numProjects,
                    numProjectLevelsEarned: it.projectLevelsEarned,
                    numSubjectLevelsEarned: it.subjectLevelsEarned,
                    numSkillsEarned: it.skillsAccomplished,
                    numBadgesEarned: it.badgesEarned,
                    numGlobalBadgesEarned: it.globalBadgesEarned,
                    numQuizzes: it.numQuizzes,
                    numQuizzesPassed: it.numQuizzesPassed,
                    numQuizzesFailed: it.numQuizzesFailed,
                    numQuizzesInProgress: it.numQuizzesInProgress,
                    numSurveys: it.numSurveys,
                    numSurveysCompleted: it.numSurveyCompleted,
                    numSurveysInProgress: it.numSurveyInProgress
            )
        }

        GlobalProgressMetricsRepo.ProjDefCounts projDefCounts = globalProgressMetricsRepo.findProjectDefCounts(projectIds)
        Integer totalGlobalBadgeCount = globalProgressMetricsRepo.getTotalGlobalBadgeCountForProjects(projectIds)
        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(quizIds)
        return new GlobalMetricsResult(
                numTotalProjects: projectIds.size(),
                numTotalSkills: projDefCounts?.numSkills ?: 0,
                numTotalBadges: projDefCounts?.numBadges ?: 0,
                numTotalGlobalBadges: totalGlobalBadgeCount,
                numTotalQuizzes: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Quiz),
                numTotalSurveys: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Survey),
                numTotalMetricItems: userProgressMetricPage.getTotalElements(),
                metricItemsPage: metricItemsPage
        )
    }

    GlobalMetricsResult loadOverallMetrics(List<String> selectedProjectIds, List<String> selectedQuizIds) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds
//        selectedProjectIds = selectedProjectIds ? projectIdsAndQuizIds.projectIds.intersect(selectedProjectIds) : projectIdsAndQuizIds.projectIds
//        selectedQuizIds = selectedQuizIds ? projectIdsAndQuizIds.quizIds.intersect(selectedQuizIds) : projectIdsAndQuizIds.quizIds

        GlobalProgressMetricsRepo.ProjDefCounts projDefCounts = globalProgressMetricsRepo.findProjectDefCounts(projectIds)
        Integer totalGlobalBadgeCount = globalProgressMetricsRepo.getTotalGlobalBadgeCountForProjects(projectIds)
        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(quizIds)

        return new GlobalMetricsResult(
                numTotalProjects: projectIds.size(),
                numTotalSkills: projDefCounts?.numSkills ?: 0,
                numTotalBadges: projDefCounts?.numBadges ?: 0,
                numTotalGlobalBadges: totalGlobalBadgeCount,
                numTotalQuizzes: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Quiz),
                numTotalSurveys: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Survey),
//                numTotalMetricItems: userProgressMetricPage.getTotalElements(),
//                metricItemsPage: metricItemsPage
        )
    }
    @RequestMapping(value = "/metrics/{metricsId}", method =  RequestMethod.GET, produces = "application/json")
    def getChartData(@PathVariable("metricsId") String metricsId,
                     @RequestParam Map<String,String> metricsProps) {

        // props: start, projIds
        return metricsServiceNew.loadGlobalMetrics(metricsId, metricsProps)
    }

    private ProjectIdsAndQuizIds getProjectIdsAndQuizIdsForCurrentUser() {
        String userId = userInfoService.currentUser.username
        List<UserRole> allUserRoles = userRoleRepo.findAllByUserId(userId)
        List<String> projectIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_PROJECT_ADMIN})?.projectId
        List<String> quizIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_QUIZ_ADMIN})?.quizId
        return new ProjectIdsAndQuizIds(projectIds: projectIds, quizIds: quizIds)
    }

    private static int countQuizzesByType(List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes, QuizDefParent.QuizType quizType) {
        return quizIdAndTypes?.findAll { it.quizType == quizType }?.size() ?: 0
    }

    static final class ProjectIdsAndQuizIds {
        List<String> projectIds
        List<String> quizIds
    }
}
