package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.DataIntegrityViolationExceptionHandler
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillDefForDependencyRes
import skills.controller.result.model.*
import skills.icons.IconCssNameUtil
import skills.services.admin.DisplayOrderService
import skills.services.admin.SkillsAdminService
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.repos.*
import skills.utils.Props

import java.util.concurrent.atomic.AtomicInteger

@Service
@Slf4j
class AdminProjService {

    static final ALL_SKILLS_PROJECTS = 'ALL_SKILLS_PROJECTS'

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    DisplayOrderService displayOrderService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillDefAccessor skillDefAccessor

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
    DependencyValidator dependencyValidator
    @Autowired
    ProjectSortingService sortingService

    @Value('#{"${skills.config.ui.descriptionMaxLength}"}')
    int maxDescriptionLength

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    GlobalBadgesService globalBadgesService
    @Autowired
    CustomValidator customValidator

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    LockingService lockingService

    private static DataIntegrityViolationExceptionHandler dataIntegrityViolationExceptionHandler =
            new DataIntegrityViolationExceptionHandler([
                    "index_project_definition_name" : "Provided project name already exist.",
                    "index_project_definition_project_id": "Provided project id already exist.",
            ])

    private static DataIntegrityViolationExceptionHandler skillDataIntegrityViolationExceptionHandler = crateSkillDefBasedDataIntegrityViolationExceptionHandler("skill")
    private static DataIntegrityViolationExceptionHandler crateSkillDefBasedDataIntegrityViolationExceptionHandler(String type) {
        new DataIntegrityViolationExceptionHandler([
                "index_skill_definition_project_id_skill_id" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name": "Provided ${type} name already exist.".toString(),
                "index_skill_definition_project_id_skill_id_type" : "Provided ${type} id already exist.".toString(),
                "index_skill_definition_project_id_name_type": "Provided ${type} name already exist.".toString(),
                "index_global_badge_level_definition_proj_skill_level" : "Provided project already has a level assigned for this global badge.",
                "index_skill_relationship_definition_parent_child_type": "Provided skill id has already been added to this global badge.",
        ])
    }

    @Transactional()
    ProjDef getProjDef(String projectId) {
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [$projectId]", projectId, null)
        }
        return projDef
    }

    @Transactional()
    List<Integer> getUniqueVersionList(String projectId) {
        skillDefRepo.getUniqueVersionList(projectId)
    }

    private SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType = SkillDef.ContainerType.Skill) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillException("Failed to find skillId [$skillId] for [$projectId] with type [${containerType}]", projectId, skillId)
        }
        return skillDef
    }


    @Transactional()
    void shareSkillToExternalProject(String projectId, String skillId, String sharedToProjectId) {
        if (projectId?.equalsIgnoreCase(sharedToProjectId)) {
            throw new SkillException("Can not share skill to itself. Requested project [$sharedToProjectId] is itself!", projectId, skillId)
        }
        SkillDef skill = getSkillDef(projectId, skillId)

        ProjDef sharedToProject = null
        if (sharedToProjectId != ALL_SKILLS_PROJECTS) {
            sharedToProject = getProjDef(sharedToProjectId)
        }

        SkillShareDef skillShareDef = new SkillShareDef(skill: skill, sharedToProject: sharedToProject)
        skillShareDefRepo.save(skillShareDef)
    }

    @Transactional()
    void deleteSkillShare(String projectId, String skillId, String sharedToProjectId) {
        SkillDef skill = getSkillDef(projectId, skillId)
        SkillShareDef skillShareDef
        if (sharedToProjectId == ALL_SKILLS_PROJECTS) {
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProjectIsNull(skill)
        } else {
            ProjDef sharedToProject = getProjDef(sharedToProjectId)
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProject(skill, sharedToProject)
        }

        if (!skillShareDef){
            throw new SkillException("Failed to find skill share definition for project [$projectId] skill [$skillId] => [$sharedToProjectId] project", projectId, skillId)
        }
        skillShareDefRepo.delete(skillShareDef)
    }


    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsWithOtherProjects(String projectId) {
        List<SkillShareDef> shareDefs = skillShareDefRepo.getSkillShareDefsWithOtherProjectsByProjectId(projectId)
        return shareDefs.collect { SkillShareDef shareDef ->
            new SharedSkillResult(
                    skillName: shareDef.skill.name, skillId: shareDef.skill.skillId,
                    projectName: shareDef.sharedToProject?.name, projectId: shareDef.sharedToProject?.projectId,
                    sharedWithAllProjects: shareDef.sharedToProject == null
            )
        }
    }

    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsFromOtherProjects(String projectId) {
        ProjDef projDef = getProjDef(projectId)
        List<SkillShareDefRepo.SkillSharedMeta> sharedMetas = skillShareDefRepo.getSkillDefsSharedFromOtherProjectsByProjectId(projDef)
        return sharedMetas.collect { SkillShareDefRepo.SkillSharedMeta meta ->
            new SharedSkillResult(
                    skillName: meta.skillName, skillId: meta.skillId,
                    projectName: meta.projectName, projectId: meta.projectId
            )
        }
    }

    @Profile
    NumUsersRes getNumUsersByProjectId(String projectId) {
        int numUsers = projDefRepo.calculateDistinctUsers(projectId)
        return  new NumUsersRes(numUsers: numUsers)
    }


    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId, String skillId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, RelationshipType.Dependence)

        // must only keep the provide skill id and its descendants
        List<GraphSkillDefEdge> collectedRes = []
        List<GraphSkillDefEdge> startEdges = edges.findAll { it.from.skillId == skillId }
        if(startEdges){
            collectDescendants(edges, startEdges, collectedRes)
        }

        return convertToSkillsGraphRes(collectedRes)
    }

    @Transactional(readOnly = true)
    long countNumberOfSkills(String projectId) {
        return skillDefRepo.countByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
    }

    @Profile
    private void collectDescendants(List<GraphSkillDefEdge> allEdges, List<GraphSkillDefEdge> currentLevel, List<GraphSkillDefEdge> collectedRes){
        if(currentLevel){
            collectedRes.addAll(currentLevel)
            for(GraphSkillDefEdge edge: currentLevel) {
                List<GraphSkillDefEdge> nextLevel = allEdges.findAll({ edge.to.id == it.from.id })
                collectDescendants(allEdges, nextLevel, collectedRes)
            }
        }
    }

    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, RelationshipType.Dependence)
        return convertToSkillsGraphRes(edges)
    }

    private static Comparator<SkillDef> skillDefComparator = new Comparator<SkillDef>() {
        @Override
        int compare(SkillDef o1, SkillDef o2) {
            return o1.id.compareTo(o2.id)
        }
    }

    private SkillsGraphRes convertToSkillsGraphRes(List<GraphSkillDefEdge> edges) {
        AtomicInteger idCounter = new AtomicInteger(0)
        Map<SkillDef, Integer> distinctNodesWithIdLookup = new TreeMap(skillDefComparator)
        List<SkillsGraphRes.Edge> edgesRes = []
        edges.each {
            int fromId = getIdInsertIfNeeded(distinctNodesWithIdLookup, it.from, idCounter)
            int toId = getIdInsertIfNeeded(distinctNodesWithIdLookup, it.to, idCounter)
            edgesRes.add(new SkillsGraphRes.Edge(fromId: fromId, toId: toId))
        }

        List<SkillDefGraphRes> nodes = distinctNodesWithIdLookup.collect {
            SkillDefRes skillDefRes = convertToSkillDefRes(it.key)
            SkillDefGraphRes graphRes = new SkillDefGraphRes()
            Props.copy(skillDefRes, graphRes)
            graphRes.id = it.value
            return graphRes
        }
        SkillsGraphRes res = new SkillsGraphRes(nodes: nodes, edges: edgesRes)
        return res
    }

    private Integer getIdInsertIfNeeded(Map<SkillDef, Integer> distinctNodesWithIdLookup, SkillDef item, AtomicInteger idCounter) {
        Integer resultId = distinctNodesWithIdLookup.get(item)
        if (resultId == null) {
            resultId = idCounter.incrementAndGet()
            distinctNodesWithIdLookup.put(item, resultId)
        }
        return resultId
    }

    private static class GraphSkillDefEdge {
        SkillDef from
        SkillDef to
    }
    @Profile
    private List<GraphSkillDefEdge> loadGraphEdges(String projectId, RelationshipType type){
        List<Object[]> edges = skillRelDefRepo.getGraph(projectId, type)

        return edges.collect({
            //   mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly

            SkillDef from = new SkillDef(
                    id: it[0],
                    name: it[1],
                    skillId: it[2],
                    projectId: it[3],
                    pointIncrement: it[4],
                    totalPoints: it[5],
                    type: it[6],
            )

            SkillDef to = new SkillDef(
                    id: it[7],
                    name: it[8],
                    skillId: it[9],
                    projectId: it[10],
                    pointIncrement: it[11],
                    totalPoints: it[12],
                    type: it[13],
            )

            new GraphSkillDefEdge(from: from, to: to)

        })
    }

    @Transactional(readOnly = true)
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(String projectId) {
        List<SkillDefRepo.SkillDefSkinny> res = skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
        List<SkillDefForDependencyRes> finalRes = res.collect {
            new SkillDefForDependencyRes(
                    skillId: it.skillId, name: it.name, projectId: it.projectId, version: it.version
            )
        }
        List<SharedSkillResult> sharedSkills = getSharedSkillsFromOtherProjects(projectId)
        sharedSkills.each {
            finalRes.add(
                    new SkillDefForDependencyRes(
                            skillId: it.skillId, name: it.skillName, projectId: projectId, otherProjectId: it.projectId, otherProjectName: it.projectName
                    )
            )
        }

        return finalRes
    }

    @Transactional()
    void assignSkillDependency(String projectId, String skillId, String dependentSkillId, String dependentProjectId = null) {
        SkillDef skill1 = getSkillDef(projectId, skillId)
        SkillDef skill2 = getSkillDef(dependentProjectId ?: projectId, dependentSkillId)

        if (dependentProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, skill2)
        }

        validateDependencyVersions(skill1, skill2)
        checkForCircularGraphAndThrowException(skill1, skill2, RelationshipType.Dependence)
        try {
            skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: RelationshipType.Dependence))
        } catch (DataIntegrityViolationException e) {
            String msg = "Skill dependency [${skill1.projectId}:${skill1.skillId}]=>[${skill2.projectId}:${skill2.skillId}] already exist.".toString()
            log.error(msg, e)
            throw new SkillException(msg, skill1.projectId, skill1.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private void validateDependencyVersions(SkillDef skill, SkillDef dependOnSkill) {
        if (skill.version < dependOnSkill.version) {
            throw new SkillException("Not allowed to depend on skill with a later version. " +
                    "Skill [ID:${skill.skillId}, version ${skill.version}] can not depend on [ID:${dependOnSkill.skillId}, version ${dependOnSkill.version}]",
                    skill.projectId, skill.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    @Transactional()
    void removeSkillDependency(String projectId, String skillId, String dependentSkillId, String dependentProjectId = null) {
        ruleSetDefGraphService.removeGraphRelationship(projectId, skillId, SkillDef.ContainerType.Skill,
                dependentProjectId ?: projectId, dependentSkillId, RelationshipType.Dependence)
    }


    private void checkForCircularGraphAndThrowException(SkillDef skill1, SkillDef skill2, RelationshipType type) {
        assert skill1.skillId != skill2.skillId || skill1.projectId != skill2.projectId

        DependencyCheckResult dependencyCheckResult = checkForCircularGraph(skill1, skill2, type)
        if (!dependencyCheckResult.possible) {
            throw new SkillException(dependencyCheckResult.reason, skill1.projectId, skill1.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private DependencyCheckResult checkForCircularGraph(SkillDef proposedParent, SkillDef proposedChild, RelationshipType type) {
        try {
            recursiveCircularDependenceCheck(proposedChild, proposedParent, [getDependencyCheckId(proposedParent), getDependencyCheckId(proposedChild)], type)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, proposedParent.projectId, proposedParent.skillId)
        }
    }

    private String getDependencyCheckId(SkillDef skill) {
        return skill.projectId + ":" + skill.skillId
    }

    private DependencyCheckResult recursiveCircularDependenceCheck(SkillDef parent, SkillDef originalParent, List<String> idPath, RelationshipType type, int currentIter = 0, int maxIter = 100) {
        if (currentIter > maxIter) {
            throw new IllegalStateException("Number of [$maxIter] iterations exceeded when checking for circular dependency for [${originalParent.skillId}]")
        }

        List<SkillRelDef> relationships = skillRelDefRepo.findAllByParentAndType(parent, type)
        if (relationships) {
            if (relationships.find { it.child.skillId == originalParent.skillId }) {
                return new DependencyCheckResult(skillId: originalParent.skillId, dependentSkillId: idPath.last(), possible: false, reason: "Discovered circular dependency [${idPath.join(" -> ")} -> ${getDependencyCheckId(originalParent)}]".toString())
            }
            for ( SkillRelDef skillRelDef : relationships ) {
                List<String> idPathCopy = new ArrayList<>(idPath)
                idPathCopy.add(getDependencyCheckId(skillRelDef.child))
                DependencyCheckResult res = recursiveCircularDependenceCheck(skillRelDef.child, originalParent, idPathCopy, type, currentIter++, maxIter)
                if (!res.possible) {
                    return res
                }
            }
        }

        return new DependencyCheckResult(skillId: originalParent.skillId, dependentSkillId: idPath.last())
    }


    @Profile
    private skills.controller.result.model.SkillDefRes convertToSkillDefRes(SkillDef skillDef) {
        skills.controller.result.model.SkillDefRes res = new skills.controller.result.model.SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    @Profile
    private skills.controller.result.model.SkillDefRes convertToSkillDefRes(SkillDefWithExtra skillDef) {
        skills.controller.result.model.SkillDefRes res = new skills.controller.result.model.SkillDefRes()
        Props.copy(skillDef, res)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    @Transactional(readOnly = true)
    boolean existsBySkillId(String projectId, String skillId) {
        return skillDefRepo.existsByProjectIdAndSkillIdAllIgnoreCase(projectId, skillId)
    }

    @Transactional(readOnly = true)
    String getProjectSecret(String projectId) {
        ProjDef projectDefinition = getProjDef(projectId)
        return projectDefinition.clientSecret
    }

    @Transactional
    void updateClientSecret(String projectId, String clientSecret) {
        ProjDef projDef = getProjDef(projectId)
        projDef.clientSecret = clientSecret
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getCustomIcons(String projectId){
        ProjDef project = getProjDef(projectId)
        return project.getCustomIcons().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass(icon.projectId, icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }
}
