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
package skills.metricsNew.builders.multipleProj

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.metricsNew.builders.GlobalMetricsBuilder
import skills.metricsNew.builders.MetricsPagingParamsHelper
import skills.metricsNew.builders.MetricsParams
import skills.storage.repos.UserAchievedCustomRepo

@Component
class FindExpertsForMultipleProjectsChartBuilder implements GlobalMetricsBuilder {

    @Autowired
    UserAchievedCustomRepo userAchievedCustomRepo

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
        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(null, id, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc

        if(params.size() < 2) {
            throw new SkillException("Chart[${id}]: must provide at least 2 projects but recieved [${params.size()}]")
        }
        if(params.size() > 5) {
            throw new SkillException("Chart[${id}]: only supports up to 5 projects but recieved [${params.size()}]")
        }

        Integer totalNum = userAchievedCustomRepo.countUsersWithMaxLevelForMultipleProjects(params)
        List<UserAchievedCustomRepo.UserAndLevel> data = userAchievedCustomRepo.findUsersWithMaxLevelForMultipleProjects(params, currentPage*pageSize, pageSize, !sortDesc)

        return new UserAchievementsRes(totalNum: totalNum, data: data)
    }

    private List<UserAchievedCustomRepo.ProjectAndLevel> getParams(Map<String, String> props) {
        String paramVal = MetricsParams.getParam(props, MetricsParams.P_PROJECT_IDS_AND_LEVEL, id)

        List<UserAchievedCustomRepo.ProjectAndLevel> params = paramVal.split(",").collect({
            Integer lastIndex = it.lastIndexOf(PARAM_SEP)
            if (lastIndex < 0){
                throw new SkillException("Chart[${id}]: projectId and level must be separted by '${PARAM_SEP}', full param=[${paramVal}]")
            }
            String id = it.substring(0, lastIndex)
            Integer level = Integer.valueOf(it.substring(lastIndex + PARAM_SEP.size()))
            if (level <= 0) {
                throw new SkillException("Chart[${id}]: level must be more than 0 in [${paramVal}]")
            }
            new UserAchievedCustomRepo.ProjectAndLevel(projectId: id, level: level)
        })
        return params
    }
}
