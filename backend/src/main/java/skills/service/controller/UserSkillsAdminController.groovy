package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.AddSkillRequest
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillLoading.model.*
import skills.service.skillsManagement.SkillsManagementFacade
import skills.utils.Constants

@RestController
@RequestMapping("/admin")
@Slf4j
@CompileStatic
class UserSkillsAdminController {

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillsManagementFacade skillsManagementFacade


    @RequestMapping(value = "/projects/{projectId}/level", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Integer getUserLevel(@PathVariable(name = "projectId") String projectId, @RequestParam(name = "userId") String userId) {
        return skillsLoader.getUserLevel(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    OverallSkillSummary getSkillsSummary(@PathVariable("projectId") String projectId, @RequestParam(name = "userId") String userId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadOverallSummary(projectId, userId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectsSkillsSummary(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId, @RequestParam(name = "userId") String userId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubject(projectId, userId, subjectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getSkillSummary(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId, @RequestParam(name = "userId") String userId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, userId, skillId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId, @RequestParam(name = "userId") String userId) {
        return skillsLoader.loadPointHistorySummary(projectId, userId, 365)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId, @RequestParam(name = "userId") String userId) {
        return skillsLoader.loadPointHistorySummary(projectId, userId, 365, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId, @RequestParam(name = "userId") String userId) {
        return skillsLoader.loadSkillDependencyInfo(projectId, userId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRanking(@PathVariable("projectId") String projectId, @RequestParam(name = "userId") String userId) {
        return rankingLoader.getUserSkillsRanking(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId, @RequestParam(name = "userId") String userId) {
        return rankingLoader.getUserSkillsRanking(projectId, userId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistribution(@PathVariable("projectId") String projectId, @RequestParam(name = "userId") String userId) {
        return rankingLoader.getRankingDistribution(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId, @RequestParam(name = "userId") String userId) {
        return rankingLoader.getRankingDistribution(projectId, userId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsManagementFacade.AddSkillResult addSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("skillId") String skillId,
            @RequestBody AddSkillRequest skillRequest) {
        try {
            assert skillRequest?.userId
            Date incomingDate = skillRequest.timestamp != null ? new Date(skillRequest.timestamp) : null

            return skillsManagementFacade.addSkill(projectId, skillId, userInfoService.lookupUserId(skillRequest.userId), incomingDate)
        } catch (Exception e) {
            log.error("Failed for projetId=[$projectId], skillId=[$skillId]", e)
            throw new SkillException(e.message, projectId, skillId)
        }
    }
}
