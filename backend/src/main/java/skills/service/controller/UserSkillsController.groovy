package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.icons.CustomIconFacade
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillLoading.model.*
import skills.service.skillsManagement.SkillsManagementFacade
import skills.utils.Constants

@CrossOrigin(allowCredentials = 'true')
@RestController
@RequestMapping("/api")
@Slf4j
@CompileStatic
class UserSkillsController {

    @Autowired
    SkillsManagementFacade skillsManagementFacade

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    RankingLoader rankingLoader

    @RequestMapping(value = "/projects/{projectId}/level", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Integer getUserLevel(@PathVariable(name = "projectId") String projectId) {
        return skillsLoader.getUserLevel(projectId, userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    OverallSkillSummary getSkillsSummary(@PathVariable("projectId") String projectId) {
        return skillsLoader.loadOverallSummary(projectId, userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectsSkillsSummary(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubject(projectId, userInfoService.currentUser.username, subjectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getSkillSummary(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, userInfoService.currentUser.username, skillId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillBadgeSummary>  getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadgeSummaries(projectId, userInfoService.currentUser.username, version)
    }


    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        return skillsLoader.loadBadge(projectId, userInfoService.currentUser.username, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId) {
        return skillsLoader.loadPointHistorySummary(projectId, userInfoService.currentUser.username, 365)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return skillsLoader.loadPointHistorySummary(projectId, userInfoService.currentUser.username, 365, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        return skillsLoader.loadSkillDependencyInfo(projectId, userInfoService.currentUser.username, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsManagementFacade.AddSkillResult addSkill(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        skillsManagementFacade.addSkill(projectId, skillId, userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRanking(@PathVariable("projectId") String projectId) {
        return rankingLoader.getUserSkillsRanking(projectId, userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return rankingLoader.getUserSkillsRanking(projectId, userInfoService.currentUser.username, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistribution(@PathVariable("projectId") String projectId) {
        return rankingLoader.getRankingDistribution(projectId, userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return rankingLoader.getRankingDistribution(projectId, userInfoService.currentUser.username, subjectId)
    }

    @RequestMapping(value = "/projects/{id}/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    String getCustomIconCss(@PathVariable("id") String projectId) {
        return customIconFacade.generateCss(projectId)
    }

}
