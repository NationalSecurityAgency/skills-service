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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.*
import skills.services.DependencyValidator
import skills.services.RuleSetDefGraphService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo
import skills.utils.InputSanitizer
import skills.utils.Props

import java.util.concurrent.atomic.AtomicInteger

@Service
@Slf4j
class SkillsDepsService {

    @Value('#{"${skills.circularLearningPathChecker.maxIterations:1000}"}')
    int circularLearningPathCheckerMaxIterations

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    DependencyValidator dependencyValidator

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    ShareSkillsService shareSkillsService

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    SkillsAdminService skillsAdminService


    @Transactional(readOnly = true)
    boolean checkIfSkillInAnotherProjectPartOfLearningPath(String projId, String otherProj, String otherProjSkillId) {
        return skillRelDefRepo.checkIfSkillInAnotherProjectPartOfLearningPath(projId, otherProj, otherProjSkillId)
    }

    @Transactional(readOnly = true)
    List<SkillDepResult> checkIfSkillsHaveDeps(String projectId, List<String> skillIds) {
        List<SkillRelDefRepo.SkillIdAndCount> skillIdsAndCounts = skillRelDefRepo.countChildrenForMultipleSkillIds(projectId, skillIds, [SkillRelDef.RelationshipType.Dependence])
        return skillIds.collect { String skillId ->
            SkillRelDefRepo.SkillIdAndCount found = skillIdsAndCounts.find { it.skillId == skillId }
            new SkillDepResult(skillId: skillId, hasDependency: found != null)
        }?.sort { it.skillId }
    }

    @Profile
    private SkillDef loadSkillDefForLearningPath(String projectId, String id) {
        return skillDefAccessor.getSkillDef(projectId, id, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup, SkillDef.ContainerType.Badge])
    }

    @Transactional()
    void addLearningPathItem(String projectId, String id, String prereqFromId, String prereqFromProjectId = null, boolean isProjCopy = false) {
        SkillDef skillDef = loadSkillDefForLearningPath(projectId, id)
        SkillDef prereqSkillDef = loadSkillDefForLearningPath(prereqFromProjectId ?: projectId, prereqFromId)

        if (isProjCopy) {
            // handle a special case where copied project has a badge with no skills and that badge is part of a learning path
            // if all the skills are removed from one of those live badges then it will be copied as disabled and cannot be
            // added to the learning path
            boolean isToBadgeDisabled = skillDef.type == SkillDef.ContainerType.Badge && (!skillDef.enabled || !Boolean.parseBoolean(skillDef.enabled))
            boolean isFromBadgeDisabled = prereqSkillDef.type == SkillDef.ContainerType.Badge && (!prereqSkillDef.enabled || !Boolean.parseBoolean(prereqSkillDef.enabled))
            if (isToBadgeDisabled || isFromBadgeDisabled) {
                return
            }
        }

        validateLearningPathItemAndThrowException(skillDef, prereqSkillDef)
        skillRelDefRepo.save(new SkillRelDef(parent: skillDef, child: prereqSkillDef, type: SkillRelDef.RelationshipType.Dependence))

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.LearningPathItem,
                actionAttributes: [
                        fromProjectId: prereqSkillDef.projectId,
                        fromSkillId: prereqSkillDef.skillId,
                        toProjectId: skillDef.projectId,
                        toSkillId: skillDef.skillId,
                ],
                itemId: skillDef.projectId,
                projectId: skillDef.projectId,
        ))
    }

    @Transactional()
    void removeLearningPathItem(String projectId, String dependentSkillId, String dependencyProjectId, String dependencySkillId) {
        ruleSetDefGraphService.removeGraphRelationship(projectId, dependentSkillId, null,
                dependencyProjectId, dependencySkillId, SkillRelDef.RelationshipType.Dependence)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.LearningPathItem,
                actionAttributes: [
                        fromProjectId: dependencyProjectId,
                        fromSkillId: dependencySkillId,
                        toProjectId: projectId,
                        toSkillId: dependentSkillId,
                ],
                itemId: projectId,
                projectId: projectId,
        ))
    }

    @Transactional()
    void removeAllLearningPathItemsBySkillId(String projectId, String skillId) {
        def relationships = skillRelDefRepo.findAllDependenciesForSkillIdAndProjectId(projectId, skillId)
        skillRelDefRepo.deleteAllById(relationships.collect{it.id})
    }

    @Transactional()
    void removeAllLearningPathItemsBySkillIdAndProjectId(String projectId, String skillId, String originalProjectId) {
        def relationships = skillRelDefRepo.findAllDependenciesForSkillIdAndProjectIdForProject(projectId, skillId, originalProjectId)
        skillRelDefRepo.deleteAllById(relationships.collect{it.id})
    }

    static class GraphSkillDefEdge {
        SkillDefGraphRes from
        SkillDefGraphRes to
    }

    @Profile
    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, SkillRelDef.RelationshipType.Dependence)
        return convertToSkillsGraphRes(edges)
    }


    private static Comparator<SkillDefGraphRes> skillDefComparator = new Comparator<SkillDefGraphRes>() {
        @Override
        int compare(SkillDefGraphRes o1, SkillDefGraphRes o2) {
            return o1.id.compareTo(o2.id)
        }
    }

    private SkillsGraphRes convertToSkillsGraphRes(List<GraphSkillDefEdge> edges) {
        AtomicInteger idCounter = new AtomicInteger(0)
        Map<SkillDefGraphRes, Integer> distinctNodesWithIdLookup = new TreeMap(skillDefComparator)
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
            graphRes.projectName = it.key.projectName
            graphRes.containedSkills = it.key.containedSkills
            return graphRes
        }
        SkillsGraphRes res = new SkillsGraphRes(nodes: nodes, edges: edgesRes)
        return res
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

    private Integer getIdInsertIfNeeded(Map<SkillDefGraphRes, Integer> distinctNodesWithIdLookup, SkillDefGraphRes item, AtomicInteger idCounter) {
        Integer resultId = distinctNodesWithIdLookup.get(item)
        if (resultId == null) {
            resultId = idCounter.incrementAndGet()
            distinctNodesWithIdLookup.put(item, resultId)
        }
        return resultId
    }

    @Profile
    SkillDefRes convertToSkillDefRes(SkillDefGraphRes skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.name = InputSanitizer.unsanitizeName(res.name)
        if(skillDef.type != SkillDef.ContainerType.Badge) {
            res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        }
        return res
    }

    @Profile
    SkillDefRes convertToSkillDefRes(SkillDef skillDef) {
        SkillDefRes res = new SkillDefRes()
        Props.copy(skillDef, res)
        res.name = InputSanitizer.unsanitizeName(res.name)
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
        return res
    }

    @Profile
    List<GraphSkillDefEdge> loadGraphEdges(String projectId, SkillRelDef.RelationshipType type) {
        List<Object[]> edges = skillRelDefRepo.getGraph(projectId, type)

        return edges.collect({
            //   mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly

            SkillDefGraphRes from = new SkillDefGraphRes(
                    id: it[0],
                    name: it[1],
                    skillId: it[2],
                    subjectId: it[3],
                    projectId: it[4],
                    projectName: it[5],
                    pointIncrement: it[6],
                    totalPoints: it[7],
                    type: it[8],
                    containedSkills: null,
            )

            if(it[8] == SkillDef.ContainerType.Badge) {
                from.containedSkills = getSkillsForLearningPathItem(projectId, it[5], it[2])
            }

            SkillDefGraphRes to = new SkillDefGraphRes(
                    id: it[9],
                    name: it[10],
                    skillId: it[11],
                    subjectId: it[12],
                    projectId: it[13],
                    projectName: it[14],
                    pointIncrement: it[15],
                    totalPoints: it[16],
                    type: it[17],
                    containedSkills: null,
            )

            if(it[17] == SkillDef.ContainerType.Badge) {
                to.containedSkills = getSkillsForLearningPathItem(projectId, it[14], it[11])
            }

            new GraphSkillDefEdge(from: from, to: to)

        })
    }

    private List<SkillDefGraphRes> getSkillsForLearningPathItem(String projectId, String projectName, String skillId) {
        List<SkillDefPartialRes> badgeSkills = skillsAdminService.getSkillsByProjectSkillAndType(projectId, skillId, SkillDef.ContainerType.Badge, SkillRelDef.RelationshipType.BadgeRequirement)
        List<SkillDefGraphRes> skills = badgeSkills.collect{res -> new SkillDefGraphRes(
                id: null,
                name: res.name,
                skillId: res.skillId,
                subjectId: res.subjectId,
                projectId: res.projectId,
                projectName: projectName,
                pointIncrement: res.pointIncrement,
                totalPoints: res.totalPoints,
                type: res.type,
        ) }
        return skills
    }

    private void validateLearningPathItemAndThrowException(SkillDef skillDef, SkillDef prereqSkillDef) {
        DependencyCheckResult dependencyCheckResult = validateLearningPathItem(skillDef, prereqSkillDef)
        if (!dependencyCheckResult.possible) {
            throw new SkillException(dependencyCheckResult.reason, skillDef.projectId, skillDef.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    @Transactional(readOnly = true)
    DependencyCheckResult validatePossibleLearningPathItem(String projectId, String id, String prereqFromId, String prereqFromProjectId = null) {
        SkillDef skillDef = loadSkillDefForLearningPath(projectId, id)
        SkillDef prereqSkillDef = loadSkillDefForLearningPath(prereqFromProjectId ?: projectId, prereqFromId)

        DependencyCheckResult dependencyCheckResult = validateLearningPathItem(skillDef, prereqSkillDef)
        return dependencyCheckResult
    }

    @Profile
    DependencyCheckResult validateLearningPathItem(SkillDef skillDef, SkillDef prereqSkillDef) {
        assert skillDef.skillId != prereqSkillDef.skillId || skillDef.projectId != prereqSkillDef.projectId

        if ("false" == skillDef.enabled) {
            return new DependencyCheckResult(possible: false,
                    failureType: DependencyCheckResult.FailureType.NotEligible,
                    reason: "Disabled nodes cannot be added. [${skillDef.projectId}-${skillDef.skillId}] is disabled")
        }
        if ("false" == prereqSkillDef.enabled) {
            return new DependencyCheckResult(possible: false,
                    failureType: DependencyCheckResult.FailureType.NotEligible,
                    reason: "Disabled nodes cannot be added. [${prereqSkillDef.projectId}-${prereqSkillDef.skillId}] is disabled")
        }

        if (skillDef.projectId != prereqSkillDef.projectId) {
            try {
                dependencyValidator.validateDependencyEligibility(skillDef.projectId, prereqSkillDef)
            } catch (SkillException e) {
                return new DependencyCheckResult(possible: false,
                        failureType: DependencyCheckResult.FailureType.NotEligible,
                        reason: e.message)
            }
        }

        if (skillCatalogService.isAvailableInCatalog(skillDef)) {
            return new DependencyCheckResult(possible: false,
                    failureType: DependencyCheckResult.FailureType.SkillInCatalog,
                    reason: "Skill [${skillDef.skillId}] was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.")
        }

        if (skillDefRepo.wasThisSkillReusedElsewhere(skillDef.id)) {
            return new DependencyCheckResult(possible: false,
                    failureType: DependencyCheckResult.FailureType.ReusedSkill,
                    reason: "Skill [${skillDef.skillId}] was reused in another subject or group and cannot have prerequisites in the learning path.")
        }
        if (skillDef.version < prereqSkillDef.version) {
            String msg = "Not allowed to depend on skill with a later version. " +
                    "Skill [ID:${skillDef.skillId}, version ${skillDef.version}] can not depend on [ID:${prereqSkillDef.skillId}, version ${prereqSkillDef.version}]".toString()
            return new DependencyCheckResult(possible: false, failureType: DependencyCheckResult.FailureType.SkillVersion, reason: msg)
        }

        SkillsGraphRes existingGraph = getDependentSkillsGraph(skillDef.projectId)
        List<CircularLearningPathChecker.BadgeAndSkills> loadedBadges = loadBadgeSkills(skillDef.projectId)

        if (skillDef.type == SkillDef.ContainerType.Badge || prereqSkillDef.type == SkillDef.ContainerType.Badge) {
            SkillDef badge = skillDef.type == SkillDef.ContainerType.Badge ? skillDef : prereqSkillDef
            SkillDef skill = skillDef.type == SkillDef.ContainerType.Skill ? skillDef : prereqSkillDef
            String msg = "A badge cannot have a dependency with a skill it contains. " +
                    "Badge [ID:${badge.skillId}] can not have a dependency with [ID:${skill.skillId}]".toString()
            CircularLearningPathChecker.BadgeAndSkills badgeToCheck = loadedBadges.find({it.badgeGraphNode.skillId == badge.skillId})
            if(badgeToCheck?.skills?.find({ it.skillId == skill.skillId})) {
                return new DependencyCheckResult(possible: false, failureType: DependencyCheckResult.FailureType.SkillExistsInBadge, reason: msg, violatingSkillId: skill.skillId, violatingSkillInBadgeId: badge.skillId)
            }
        }

        CircularLearningPathChecker circularLearningPathChecker = new CircularLearningPathChecker(
                circularLearningPathCheckerMaxIterations: circularLearningPathCheckerMaxIterations,
                skillDef: skillDef,
                prereqSkillDef: prereqSkillDef,
                existingGraph: existingGraph,
                badgeAndSkills: loadedBadges)
        return circularLearningPathChecker.check()
    }

    @Profile
    List<CircularLearningPathChecker.BadgeAndSkills> loadBadgeSkills(String projectId) {
        List<SkillRelDefRepo.ParentChildSkillIds> parentChildSkillIds = skillRelDefRepo.findParentAndChildrenSkillIdsForProject(projectId, SkillRelDef.RelationshipType.BadgeRequirement)
        if (!parentChildSkillIds) {
            return []
        }
        Map<String, List<SkillRelDefRepo.ParentChildSkillIds>> byBadgeId = parentChildSkillIds.groupBy { it.parentSkillId }
        return byBadgeId.collect {
            List<CircularLearningPathChecker.SkillInfo> badgeSkillInfos = it.value?.collect {
                new CircularLearningPathChecker.SkillInfo(projectId: projectId, skillId: it.childSkillId, name: it.childSkillName, type: SkillDef.ContainerType.Skill, belongsToBadge: true, belongsToBadgeId: it.parentSkillId)
            }
            return new CircularLearningPathChecker.BadgeAndSkills(
                    badgeGraphNode: new CircularLearningPathChecker.SkillInfo(projectId: projectId, skillId: it.key, name: it.value[0].parentSkillName, type: SkillDef.ContainerType.Badge),
                    skills: badgeSkillInfos
            )
        }
    }
}