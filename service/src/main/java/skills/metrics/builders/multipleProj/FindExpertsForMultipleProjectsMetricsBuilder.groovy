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
package skills.metrics.builders.multipleProj

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillException
import skills.metrics.builders.GlobalMetricsBuilder
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.services.admin.InviteOnlyProjectService
import skills.services.admin.UserCommunityService
import skills.storage.repos.UserAchievedCustomRepo

@Component
class FindExpertsForMultipleProjectsMetricsBuilder implements GlobalMetricsBuilder {

    @Autowired
    UserAchievedCustomRepo userAchievedCustomRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    UserCommunityService userCommunityService

    @Override
    String getId() {
        return "findExpertsForMultipleProjectsChartBuilder"
    }

    private static PARAM_SEP = "AndLevel";

    static class UserAchievementsRes {
        Integer totalNum
        List<UserAchievedCustomRepo.UserAndLevel> data
    }

    @Override
    def build(Map<String, String> props) {
        List<UserAchievedCustomRepo.ProjectAndLevel> params = getParams(props)
        validateProjectAccess(params)
        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(null, id, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc

        Integer totalNum = userAchievedCustomRepo.countUsersWithMaxLevelForMultipleProjects(params)
        List<UserAchievedCustomRepo.UserAndLevel> data = userAchievedCustomRepo.findUsersWithMaxLevelForMultipleProjects(params, currentPage*pageSize, pageSize, !sortDesc)

        return new UserAchievementsRes(totalNum: totalNum, data: data)
    }

    private void validateProjectAccess(List<UserAchievedCustomRepo.ProjectAndLevel> params) {
        String userId = userInfoService.currentUserId
        // Authorize every requested project before querying either counts or rows.
        // Root access bypasses invitation roles, but not community membership.
        params.each { UserAchievedCustomRepo.ProjectAndLevel param ->
            if (userCommunityService.isUserCommunityOnlyProject(param.projectId) &&
                    !userCommunityService.isUserCommunityMember(userId)) {
                throw new AccessDeniedException('Access is denied')
            }
            if (inviteOnlyProjectService.isInviteOnlyProject(param.projectId) &&
                    !userInfoService.isCurrentUserASuperDuperUser() &&
                    !inviteOnlyProjectService.canUserAccess(param.projectId, userId, true)) {
                throw new AccessDeniedException('Access is denied')
            }
        }
    }

    private List<UserAchievedCustomRepo.ProjectAndLevel> getParams(Map<String, String> props) {
        String paramVal = MetricsParams.getParam(props, MetricsParams.P_PROJECT_IDS_AND_LEVEL, id)

        String [] split = paramVal.split(",")
        if(split.size() < 2) {
            throw new SkillException("Metrics[${id}]: must provide at least 2 projects but recieved [${split.size()}]")
        }
        if(split.size() > 5) {
            throw new SkillException("Metrics[${id}]: only supports up to 5 projects but recieved [${split.size()}]")
        }

        List<UserAchievedCustomRepo.ProjectAndLevel> params = split.collect({
            Integer lastIndex = it.lastIndexOf(PARAM_SEP)
            if (lastIndex < 0){
                throw new SkillException("Metrics[${id}]: projectId and level must be separted by '${PARAM_SEP}', full param=[${paramVal}]")
            }
            String id = it.substring(0, lastIndex)
            Integer level = Integer.valueOf(it.substring(lastIndex + PARAM_SEP.size()))
            if (level <= 0) {
                throw new SkillException("Metrics[${id}]: level must be more than 0 in [${paramVal}]")
            }
            new UserAchievedCustomRepo.ProjectAndLevel(projectId: id, level: level)
        })

        return params
    }
}
