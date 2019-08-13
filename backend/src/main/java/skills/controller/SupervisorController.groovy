package skills.controller


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.result.model.*
import skills.services.AccessSettingsStorageService
import skills.services.AdminProjService
import skills.services.AdminUsersService
import skills.services.GlobalSkillsStorageService
import skills.services.LevelDefinitionStorageService
import skills.storage.model.auth.UserRole

import java.nio.charset.StandardCharsets

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/supervisor")
@Slf4j
@skills.profile.EnableCallStackProf
class SupervisorController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    GlobalSkillsStorageService globalSkillsStorageService

    @Autowired
    AdminUsersService adminUsersService

    @RequestMapping(value = "/badges/name/{badgeName}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeNameExist(@PathVariable("badgeName") String badgeName) {
        SkillsValidator.isNotBlank(badgeName, "Badge Name")
        String decodedName = URLDecoder.decode(badgeName,  StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsByBadgeName(null, decodedName)
    }

    @RequestMapping(value = "/badges/id/{badgeId}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeIdExist(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        String decodedName = URLDecoder.decode(badgeId,  StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsBySkillId(null, decodedName)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        SkillsValidator.isFirstOrMustEqualToSecond(badgeRequest.badgeId, badgeId, "Badge Id")
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name")
        SkillsValidator.isTrue((badgeRequest.startDate && badgeRequest.endDate) || (!badgeRequest.startDate && !badgeRequest.endDate),
                "If one date is provided then both start and end dates must be provided")

        globalSkillsStorageService.saveBadge(badgeRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/badges/{badgeId}/projects/{projectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignSkillToBadge(@PathVariable("badgeId") String badgeId,
                                     @PathVariable("projectId") String projectId,
                                     @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id")

        globalSkillsStorageService.addSkillToBadge(badgeId, projectId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/badges/{badgeId}/projects/{projectId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeSkillFromBadge(@PathVariable("badgeId") String badgeId,
                                       @PathVariable("projectId") String projectId,
                                       @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        globalSkillsStorageService.removeSkillFromBadge(badgeId, projectId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        globalSkillsStorageService.deleteBadge(badgeId)
    }

    @RequestMapping(value = "/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<BadgeResult> getBadges() {
        return globalSkillsStorageService.getBadges()
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult getBadge(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        return globalSkillsStorageService.getBadge(badgeId)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        SkillsValidator.isNotNull(badgePatchRequest.action, "Action must be provided")

        globalSkillsStorageService.setBadgeDisplayOrder(badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @GetMapping(value = "/badges/{badgeId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getBadgeUsers(@PathVariable("badgeId") String badgeId,
                              @RequestParam String query,
                              @RequestParam int limit,
                              @RequestParam int page,
                              @RequestParam String orderBy,
                              @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> badgeSkills = getBadgeSkills(badgeId)
        List<String> skillIds = badgeSkills.collect { it.skillId }
        if (!skillIds) {
            return new TableResult()
        }
        return adminUsersService.loadUsersPage(null, skillIds, query, pageRequest)
    }

    @RequestMapping(value = "/badges/{badgeId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefRes> getBadgeSkills(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        return globalSkillsStorageService.getSkillsForBadge(badgeId)
    }

    @RequestMapping(value = "/badges/{badgeId}/skills/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefRes> suggestBadgeSkills(@PathVariable("badgeId") String badgeId,
                                         @RequestParam String query) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        if (query) {
            String q = query
            log.info("Optional query param is [${q}]")
        }
        return globalSkillsStorageService.getAvailableSkillsForGlobalBadge(badgeId ,"")
    }

//    @GetMapping('/supervisorUsers')
//    @ResponseBody
//    List<UserRole> getSupervisorUsers() {
//        return accessSettingsStorageService.getSupervisorUsers()
//    }
}
