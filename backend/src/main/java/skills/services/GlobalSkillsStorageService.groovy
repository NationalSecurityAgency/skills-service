package skills.services

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.DataIntegrityViolationExceptionHandler
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.controller.result.model.*
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo
import skills.utils.Props

@Service
@Slf4j
class GlobalSkillsStorageService {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    UserAchievementsAndPointsManagement userPointsManagement

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    SortingService sortingService

    private static DataIntegrityViolationExceptionHandler badgeDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("badge")
    private static DataIntegrityViolationExceptionHandler crateSkillDefBasedDataIntegrityViolationExceptionHandler(String type) {
        new DataIntegrityViolationExceptionHandler([
                "index_skill_definition_project_id_skill_id" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name": "Provided ${type} name already exist.".toString(),
                "index_skill_definition_project_id_skill_id_type" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name_type": "Provided ${type} name already exist.".toString(),
        ])
    }


    @Transactional()
    void saveBadge(String originalBadgeId, BadgeRequest badgeRequest) {
        IdFormatValidator.validate(badgeRequest.badgeId)
        if(badgeRequest.name.length() > 50){
            throw new SkillException("Bad Name [${badgeRequest.name}] - must not exceed 50 chars.")
        }

        SkillDefWithExtra skillDefinition = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, originalBadgeId, SkillDef.ContainerType.Badge)

        if (!skillDefinition || !skillDefinition.skillId.equalsIgnoreCase(badgeRequest.badgeId)) {
            SkillDef idExists = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeRequest.badgeId, SkillDef.ContainerType.Badge)
            if (idExists) {
                throw new SkillException("Badge with id [${badgeRequest.badgeId}] already exists! Sorry!", null, null, ErrorCode.ConstraintViolation)
            }
        }
        if (!skillDefinition || !skillDefinition.name.equalsIgnoreCase(badgeRequest.name)) {
            SkillDef nameExists = skillDefRepo.findByProjectIdAndNameIgnoreCaseAndType(null, badgeRequest.name, SkillDef.ContainerType.Badge)
            if (nameExists) {
                throw new SkillException("Badge with name [${badgeRequest.name}] already exists! Sorry!", null, null, ErrorCode.ConstraintViolation)
            }
        }

        if (skillDefinition) {
            Props.copy(badgeRequest, skillDefinition)
            skillDefinition.skillId = badgeRequest.badgeId
        } else {
//            ProjDef projDef = getProjDef(projectId)

            Integer lastDisplayOrder = getBadges()?.collect({ it.displayOrder })?.max()
            int displayOrder = lastDisplayOrder != null ? lastDisplayOrder + 1 : 0

            skillDefinition = new SkillDefWithExtra(
                    type: SkillDef.ContainerType.GlobalBadge,
//                    projectId: projectId,
                    skillId: badgeRequest.badgeId,
                    name: badgeRequest?.name,
                    description: badgeRequest?.description,
                    iconClass: badgeRequest?.iconClass ?: "fa fa-question-circle",
                    startDate: badgeRequest.startDate,
                    endDate: badgeRequest.endDate,
//                    projDef: projDef,
                    displayOrder: displayOrder,
            )
            log.info("Saving [{}]", skillDefinition)
        }

        SkillDefWithExtra savedSkill

        badgeDataIntegrityViolationExceptionHandler.handle(null) {
            savedSkill = skillDefWithExtraRepo.save(skillDefinition)
        }

        log.info("Saved [{}]", savedSkill)
    }

    @Transactional()
    void addSkillToBadge(String badgeId, String projectId, String skillId) {
        assignGraphRelationship(badgeId, SkillDef.ContainerType.GlobalBadge, projectId, skillId, RelationshipType.BadgeDependence)
    }

    @Transactional()
    void removeSkillFromBadge(String badgeId, projectId, String skillId) {
        removeGraphRelationship(badgeId, SkillDef.ContainerType.GlobalBadge, projectId, skillId, RelationshipType.BadgeDependence)
    }
    @Transactional
    void assignGraphRelationship(String badgeSkillId, SkillDef.ContainerType skillType, String projectId,
                                 String relationshipSkillId, RelationshipType relationshipType) {
        SkillDef skill1 = getSkillDef(null, badgeSkillId, skillType)
        SkillDef skill2 = getSkillDef(projectId, relationshipSkillId)
        skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: relationshipType))
    }

    @Transactional
    void removeGraphRelationship(String skillId, SkillDef.ContainerType skillType, String projectId,
                                 String relationshipSkillId, RelationshipType relationshipType){
        SkillDef skill1 = getSkillDef(null, skillId, skillType)
        SkillDef skill2 = getSkillDef(projectId, relationshipSkillId)
        SkillRelDef relDef = skillRelDefRepo.findByChildAndParentAndType(skill2, skill1, relationshipType)
        if (!relDef) {
            throw new SkillException("Failed to find relationship [$relationshipType] between [$skillId] and [$relationshipSkillId] for [$projectId]", projectId, skillId)
        }
        skillRelDefRepo.delete(relDef)
    }

    private SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType = SkillDef.ContainerType.Skill) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with type [${containerType}]", projectId, skillId)
        }
        return skillDef
    }
    @Transactional
    void deleteBadge(String badgeId) {
        log.info("Deleting global badge with badge id [{}]", badgeId)
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeId, SkillDef.ContainerType.GlobalBadge)
        assert badgeDefinition, "DELETE FAILED -> no badge with badge id [$badgeId]"
        assert badgeDefinition.type == SkillDef.ContainerType.GlobalBadge

        deleteSkillWithItsDescendants(badgeDefinition)

        // reset display order attribute - make sure the order is continuous - 0...N
        List<SkillDef> badges = getBadges()
        badges = badges?.findAll({ it.id != badgeDefinition.id }) // need to remove because of JPA level caching?
        resetDisplayOrder(badges)
        log.info("Deleted badge with id [{}]", badgeDefinition)
    }

    private void deleteSkillWithItsDescendants(SkillDef skillDef) {
        List<SkillDef> toDelete = []

        List<SkillDef> currentChildren = ruleSetDefGraphService.getChildrenSkills(skillDef)
        while (currentChildren) {
            toDelete.addAll(currentChildren)
            currentChildren = currentChildren?.collect {
                ruleSetDefGraphService.getChildrenSkills(it)
            }?.flatten()
        }
        toDelete.add(skillDef)
        log.info("Deleting [{}] skill definitions (descendants + me) under [{}]", toDelete.size(), skillDef.skillId)
        skillDefRepo.deleteAll(toDelete)
    }

    private void resetDisplayOrder(List<SkillDef> skillDefs) {
        if(skillDefs) {
            List <SkillDef> copy = new ArrayList<>(skillDefs)
            List<SkillDef> toSave = []
            copy = copy.sort({ it.displayOrder })
            copy.eachWithIndex { SkillDef entry, int i ->
                if (entry.displayOrder != i) {
                    toSave.add(entry)
                    entry.displayOrder = i
                }
            }
            if (toSave) {
                skillDefRepo.saveAll(toSave)
            }
        }
    }

    @Transactional(readOnly = true)
    List<BadgeResult> getBadges() {
        List<SkillDefWithExtra> badges = skillDefWithExtraRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        List<BadgeResult> res = badges.collect { convertToBadge(it) }
        return res?.sort({ it.displayOrder })
    }

    @Transactional(readOnly = true)
    BadgeResult getBadge(String badgeId) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, badgeId, SkillDef.ContainerType.GlobalBadge)
        return convertToBadge(skillDef, true)
    }

    @Transactional
    void setBadgeDisplayOrder(String badgeId, ActionPatchRequest badgePatchRequest) {
        updateDisplayOrder(badgeId, getBadges(), badgePatchRequest)
    }

    @Transactional
    List<SkillDef> getAvailableSkillsForGlobalBadge(String badgeId, String query) {
        List<SkillDef> allSkillDefs = skillDefRepo.findAllByTypeAndNameLike(SkillDef.ContainerType.Skill, query)
        Set<String> existingBadgeSkillIds = getSkillsForBadge(badgeId).collect { "${it.projectId}${it.skillId}" }
        return allSkillDefs.findAll { !("${it.projectId}${it.skillId}" in existingBadgeSkillIds) }.sort().take(10)
    }

    @Transactional
    List<SkillDefPartialRes> getSkillsForBadge(String badgeId) {
        return getSkillsByProjectSkillAndType(null, badgeId, SkillDef.ContainerType.GlobalBadge, RelationshipType.BadgeDependence)
    }

    private List<SkillDefPartialRes> getSkillsByProjectSkillAndType(String projectId, String skillId, SkillDef.ContainerType type, RelationshipType relationshipType) {
        SkillDef parent = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, type)
        if (!parent) {
            throw new SkillException("There is no skill id [${skillId}] doesn't exist.", projectId, null)
        }

        List<SkillDefRepo.SkillDefPartial> res = skillRelDefRepo.getGlobalChildrenPartial(parent.skillId, relationshipType)
        return res.collect { convertToSkillDefPartialRes(it) }
    }

    @CompileStatic
    @Profile
    private SkillDefPartialRes convertToSkillDefPartialRes(SkillDefRepo.SkillDefPartial partial, boolean loadNumUsers = false) {
        SkillDefPartialRes res = new SkillDefPartialRes(
                skillId: partial.skillId,
                projectId: partial.projectId,
                name: partial.name,
                pointIncrement: partial.pointIncrement,
                pointIncrementInterval: partial.pointIncrementInterval,
                numMaxOccurrencesIncrementInterval: partial.numMaxOccurrencesIncrementInterval,
                numPerformToCompletion: partial.numMaxOccurrencesIncrementInterval,
                totalPoints: partial.totalPoints,
                version: partial.version,
                type: partial.skillType,
                displayOrder: partial.displayOrder,
                created: partial.created,
                updated: partial.updated,
        )

        res.numPerformToCompletion = (Integer)(res.totalPoints / res.pointIncrement)
        res.totalPoints = partial.totalPoints
        res.numMaxOccurrencesIncrementInterval = partial.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
            res.numUsers = calculateDistinctUsersForSkill((SkillDefRepo.SkillDefPartial)partial)
        }

        return res;
    }

    @Profile
    private int calculateDistinctUsersForSkill(SkillDefRepo.SkillDefPartial partial) {
        skillDefRepo.calculateDistinctUsersForASingleSkill(partial.projectId, partial.skillId)
    }

    private void updateDisplayOrder(String skillId, List<SkillDef> skills, ActionPatchRequest patchRequest) {
        SkillDef toUpdate = skills.find({ it.skillId == skillId })

        SkillDef switchWith

        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                skills = skills.sort({ it.displayOrder })
                switchWith = skills.find({ it.displayOrder > toUpdate.displayOrder })
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                skills = skills.sort({ it.displayOrder }).reverse()
                switchWith = skills.find({ it.displayOrder < toUpdate.displayOrder })
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${patchRequest.action}")
        }

        if (!switchWith) {
            assert switchWith, "Failed to find project definition to switch with [${toUpdate}] for action [$patchRequest.action]"
        }
        assert switchWith.skillId != toUpdate.skillId

        int switchWithDisplayOrderTmp = toUpdate.displayOrder

        toUpdate.displayOrder = switchWith.displayOrder
        switchWith.displayOrder = switchWithDisplayOrderTmp
        skillDefRepo.saveAll([toUpdate, switchWith])

        log.info("Switched order of [{}] and [{}]", toUpdate.skillId, switchWith.skillId)
    }

    @Profile
    private BadgeResult convertToBadge(SkillDefWithExtra skillDef, boolean loadRequiredSkills = false) {
        BadgeResult res = new BadgeResult(
                badgeId: skillDef.skillId,
                projectId: skillDef.projectId,
                name: skillDef.name,
                description: skillDef.description,
                displayOrder: skillDef.displayOrder,
                iconClass: skillDef.iconClass,
                startDate: skillDef.startDate,
                endDate: skillDef.endDate,
        )

        if (loadRequiredSkills) {
            List<SkillDef> dependentSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            res.requiredSkills = dependentSkills?.collect { convertToSkillDefRes(it) }
            res.numSkills = dependentSkills ? dependentSkills.size() : 0
            res.totalPoints = dependentSkills ? dependentSkills?.collect({ it.totalPoints })?.sum() : 0
        } else {
            res.numSkills = skillDefRepo.countChildSkillsByIdAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            if (res.numSkills > 0) {
                res.totalPoints = skillDefRepo.sumChildSkillsTotalPointsBySkillAndRelationshipType(skillDef.id, SkillRelDef.RelationshipType.BadgeDependence)
            } else {
                res.totalPoints = 0
            }
        }
        return res
    }
    @Profile
    private SkillDefRes convertToSkillDefRes(SkillDef skillDef, boolean loadNumUsers = false) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        res.totalPoints = skillDef.totalPoints
        res.numMaxOccurrencesIncrementInterval = skillDef.numMaxOccurrencesIncrementInterval

        if (loadNumUsers) {
//            res.numUsers = calculateDistinctUsersForSkill(skillDef.projectId, skillDef.skillId)
        }
        return res
    }
}
