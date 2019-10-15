package skills.controller


import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import skills.PublicProps
import skills.controller.exceptions.InvalidContentTypeException
import skills.controller.exceptions.MaxIconSizeExceeded
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.result.model.*
import skills.icons.CustomIconFacade
import skills.icons.UploadedIcon
import skills.services.AccessSettingsStorageService
import skills.services.AdminProjService
import skills.services.AdminUsersService
import skills.services.GlobalBadgesService
import skills.services.IdFormatValidator
import skills.services.LevelDefinitionStorageService
import skills.utils.InputSanitizer

import java.nio.charset.StandardCharsets

import static skills.services.GlobalBadgesService.*

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
    GlobalBadgesService globalBadgesService

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    CustomIconFacade iconFacade

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @RequestMapping(value = "/badges/name/{badgeName}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeNameExist(@PathVariable("badgeName") String badgeName) {
        SkillsValidator.isNotBlank(badgeName, "Badge Name")
        String decodedName = URLDecoder.decode(badgeName,  StandardCharsets.UTF_8.toString())
        return globalBadgesService.existsByBadgeName(decodedName)
    }

    @RequestMapping(value = "/badges/id/{badgeId}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeIdExist(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        String decodedId = URLDecoder.decode(badgeId,  StandardCharsets.UTF_8.toString())
        return globalBadgesService.existsByBadgeId(decodedId)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name")

        IdFormatValidator.validate(badgeRequest.badgeId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Badge Id", badgeRequest.badgeId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Badge Id", badgeRequest.badgeId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxBadgeNameLength, "Badge Name", badgeRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Badge Name", badgeRequest.name)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Badge Description", badgeRequest.description)

        badgeRequest.name = InputSanitizer.sanitize(badgeRequest.name)
        badgeRequest.badgeId = InputSanitizer.sanitize(badgeRequest.badgeId)
        badgeRequest.description = InputSanitizer.sanitize(badgeRequest.description)
        badgeRequest.helpUrl = InputSanitizer.sanitize(badgeRequest.helpUrl)

        globalBadgesService.saveBadge(badgeId, badgeRequest)
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

        globalBadgesService.addSkillToBadge(badgeId, projectId, skillId)
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

        globalBadgesService.removeSkillFromBadge(badgeId, projectId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        globalBadgesService.deleteBadge(badgeId)
    }

    @RequestMapping(value = "/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<GlobalBadgeResult> getBadges() {
        return globalBadgesService.getBadges()
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    GlobalBadgeResult getBadge(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        return globalBadgesService.getBadge(badgeId)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(@PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        SkillsValidator.isNotNull(badgePatchRequest.action, "Action must be provided")

        globalBadgesService.setBadgeDisplayOrder(badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/badges/{badgeId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefRes> getBadgeSkills(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")

        return globalBadgesService.getSkillsForBadge(badgeId)
    }

    @RequestMapping(value = "/badges/{badgeId}/skills/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    AvailableSkillsResult suggestBadgeSkills(@PathVariable("badgeId") String badgeId,
                                             @RequestParam String query) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        return globalBadgesService.getAvailableSkillsForGlobalBadge(badgeId, query)
    }

    @RequestMapping(value = "/badges/{badgeId}/projects/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<ProjectResult> getAllProjects(@PathVariable("badgeId") String badgeId) {
        return globalBadgesService.getAllProjectsForBadge(badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevelsForProject(
            @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @RequestMapping(value = "/badges/{badgeId}/projects/{projectId}/level/{level}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignProjectLevelToBadge(@PathVariable("badgeId") String badgeId,
                                            @PathVariable("projectId") String projectId,
                                            @PathVariable("level") Integer level) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(level, "Level")

        globalBadgesService.addProjectLevelToBadge(badgeId, projectId, level)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/badges/{badgeId}/projects/{projectId}/level/{level}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeProjectLevelFromBadge(@PathVariable("badgeId") String badgeId,
                                              @PathVariable("projectId") String projectId,
                                              @PathVariable("level") Integer level) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(level, "Level")

        globalBadgesService.removeProjectLevelFromBadge(badgeId, projectId, level)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/icons/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<CustomIconResult> getGlobalCustomIcons() {
        return iconFacade.getGlobalCustomIcons()
    }

    @RequestMapping(value = "/icons/upload", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    UploadedIcon addGlobalCustomIcon(@RequestParam("customIcon") MultipartFile icon) {
        String iconFilename = icon.originalFilename
        byte[] file = icon.bytes
        icon.contentType

        if (!icon.contentType?.toLowerCase()?.startsWith("image/")) {
            throw new InvalidContentTypeException("content-type [${icon.contentType}] is unacceptable, only image/ content-types are allowed")
        }

        if (file.length > CustomIconAdminController.maxIconFileSize) {
            throw new MaxIconSizeExceeded("[${file.length}] exceeds the maximum icon size of [${FileUtils.byteCountToDisplaySize(maxIconFileSize)}]")
        }

        UploadedIcon result = iconFacade.saveIcon(null, iconFilename, icon.contentType, file)

        return result
    }

    @RequestMapping(value = "/icons/{filename}", method = RequestMethod.DELETE)
    ResponseEntity<Boolean> deleteGlobal(@PathVariable("filename") String filename) {
        iconFacade.deleteGlobalIcon(filename)
        return ResponseEntity.ok(true)
    }
}
