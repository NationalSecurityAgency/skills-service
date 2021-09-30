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
package skills.controller

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.AddMyProjectRequest
import skills.controller.result.model.AvailableProjectResult
import skills.controller.result.model.ProjectNameResult
import skills.controller.result.model.RequestResult
import skills.metrics.MetricsService
import skills.profile.EnableCallStackProf
import skills.services.admin.ProjAdminService
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.MyProgressSummary
import skills.skillLoading.model.SkillBadgeSummary

import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api")
@Slf4j
@EnableCallStackProf
class MyProgressController {

    @Autowired
    MetricsService metricsServiceNew

    @Autowired
    private SkillsLoader skillsLoader

    @Autowired
    private UserInfoService userInfoService

    @Autowired
    private PublicProps publicProps

    @Autowired
    ProjAdminService projAdminService


    @Value('#{"${skills.config.ui.rankingAndProgressViewsEnabled}"}')
    Boolean rankingAndProgressViewsEnabled

    @RequestMapping(value = "/metrics/{metricsId}", method =  RequestMethod.GET, produces = "application/json")
    def getChartData(@PathVariable("metricsId") String metricsId,
                     @RequestParam Map<String,String> metricsProps) {
        return metricsServiceNew.loadGlobalMetrics(metricsId, metricsProps)
    }

    @RequestMapping(value = "/myProgressSummary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    @Profile
    MyProgressSummary getMyProgressSummary() {

        validateRankingAndProgressViewsEnabled()
        String userId = userInfoService.getCurrentUserId()
        return skillsLoader.loadMyProgressSummary(userId)
    }

    @RequestMapping(value = "/availableForMyProjects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    @Profile
    List<AvailableProjectResult> getAvailableForMyProjects() {
        validateRankingAndProgressViewsEnabled()

        String currentUserIdLower = userInfoService.getCurrentUserId().toLowerCase()
        return skillsLoader.getAvailableForMyProjects(currentUserIdLower);
    }

    @PostMapping('/myprojects/{projectId}')
    RequestResult addMyProject(@PathVariable("projectId") String projectId, @RequestBody(required = false) AddMyProjectRequest addMyProjectRequest) {
        if (addMyProjectRequest?.newSortIndex != null){
            SkillsValidator.isTrue(addMyProjectRequest.newSortIndex >=0, "Provided [newSortIndex=${addMyProjectRequest.newSortIndex}] is less than 0", projectId)
        }
        validateRankingAndProgressViewsEnabled()
        projAdminService.addMyProject(projectId, addMyProjectRequest)
        return new RequestResult(success: true)
    }

    @DeleteMapping('/myprojects/{projectId}')
    RequestResult removeMyProject(@PathVariable("projectId") String projectId) {
        validateRankingAndProgressViewsEnabled()
        projAdminService.removeMyProject(projectId)
        return new RequestResult(success: true)
    }

    @GetMapping(value='/mybadges', produces = "application/json")
    List<? extends SkillBadgeSummary> getMyBadges() {
        String userId = userInfoService.getCurrentUserId()
        return skillsLoader.getBadgesForUserMyProjects(userId)
    }

    @GetMapping('/myprojects/{projectId}/name')
    ProjectNameResult getProjectName(@PathVariable("projectId") String projectId) {
        String name = projAdminService.lookupMyProjectName(userInfoService.getCurrentUserId().toLowerCase(), projectId)
        return new ProjectNameResult(projectId: projectId, name: name)
    }

    private void validateRankingAndProgressViewsEnabled() {
        if (!rankingAndProgressViewsEnabled) {
            throw new SkillException("Progress and Ranking Views are disabled for this installation of the SkillTree", null, null, ErrorCode.AccessDenied)
        }
    }
}
