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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.SkillDefForDependencyRes
import skills.controller.result.model.DependencyCheckResult
import skills.controller.result.model.SharedSkillResult
import skills.controller.result.model.SkillDefGraphRes
import skills.controller.result.model.SkillDefRes
import skills.controller.result.model.SkillDepResult
import skills.controller.result.model.SkillsGraphRes
import skills.services.DependencyValidator
import skills.services.RuleSetDefGraphService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefSkinny
import skills.storage.model.SkillRelDef
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo
import skills.utils.InputSanitizer
import skills.utils.Props

import java.util.concurrent.atomic.AtomicInteger

@Service
@Slf4j
class SkillsDepsService {

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

    @Transactional(readOnly = true)
    List<SkillDepResult> checkIfSkillsHaveDeps(String projectId, List<String> skillIds) {
        List<SkillRelDefRepo.SkillIdAndCount> skillIdsAndCounts = skillRelDefRepo.countChildrenForMultipleSkillIds(projectId, skillIds, [SkillRelDef.RelationshipType.Dependence])
        return skillIds.collect { String skillId ->
            SkillRelDefRepo.SkillIdAndCount found = skillIdsAndCounts.find { it.skillId == skillId }
            new SkillDepResult(skillId: skillId, hasDependency: found != null)
        }?.sort { it.skillId }
    }

    @Transactional()
    void addLearningPathItem(String projectId, String id, String prereqFromId, String prereqFromProjectId = null) {
        SkillDef skillDef = skillDefAccessor.getSkillDef(projectId, id, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup, SkillDef.ContainerType.Badge])
        SkillDef prereqSkillDef = skillDefAccessor.getSkillDef(prereqFromProjectId ?: projectId, prereqFromId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup, SkillDef.ContainerType.Badge])

        if (skillCatalogService.isAvailableInCatalog(skillDef)) {
            throw new SkillException("Skill [${skillDef.skillId}] has been shared to the catalog. Dependencies cannot be added to a skill shared to the catalog.", projectId, id, ErrorCode.DependenciesNotAllowed)
        }
        if (skillDefRepo.wasThisSkillReusedElsewhere(skillDef.id)) {
            throw new SkillException("Skill [${skillDef.skillId}] was reused in another subject or group. Dependencies cannot be added to a skill that was reused.", projectId, id, ErrorCode.DependenciesNotAllowed)
        }
        if ("false" == skillDef.enabled) {
            throw new SkillException("Disabled nodes cannot be added. [${skillDef.projectId}-${skillDef.skillId}] is disabled", projectId, null, ErrorCode.BadParam)
        }
        if ("false" == prereqSkillDef.enabled) {
            throw new SkillException("Disabled nodes cannot be added. [${prereqSkillDef.projectId}-${prereqSkillDef.skillId}] is disabled", projectId, null, ErrorCode.BadParam)
        }

        if (prereqFromProjectId && projectId != prereqFromProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, prereqSkillDef)
        }

        validateDependencyVersions(skillDef, prereqSkillDef)
        checkForCircularGraphAndThrowException(skillDef, prereqSkillDef)
        try {
            skillRelDefRepo.save(new SkillRelDef(parent: skillDef, child: prereqSkillDef, type: SkillRelDef.RelationshipType.Dependence))
        } catch (DataIntegrityViolationException e) {
            String msg = "Skill dependency [${skillDef.projectId}:${skillDef.skillId}]=>[${prereqSkillDef.projectId}:${prereqSkillDef.skillId}] already exist.".toString()
            log.error(msg, e)
            throw new SkillException(msg, skillDef.projectId, skillDef.skillId, ErrorCode.FailedToAssignDependency)
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
    void removeSkillDependency(String projectId, String dependentSkillId, String dependencySkillId, String dependencyProjectId = null) {
        ruleSetDefGraphService.removeGraphRelationship(projectId, dependentSkillId, null,
                dependencyProjectId ?: projectId, dependencySkillId, SkillRelDef.RelationshipType.Dependence)
    }

    @Transactional(readOnly = true)
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(String projectId) {
        List<SkillDefSkinny> res = skillDefRepo.findAllSkinnySelectByProjectIdAndType(projectId, SkillDef.ContainerType.Skill, "", Boolean.TRUE.toString(), Boolean.FALSE.toString())
        // remove reused skills
        res = res.findAll { !SkillReuseIdUtil.isTagged(it.skillId) }
        List<SkillDefForDependencyRes> finalRes = res.collect {
            new SkillDefForDependencyRes(
                    skillId: it.skillId,
                    name: InputSanitizer.unsanitizeName(it.name),
                    projectId: it.projectId,
                    version: it.version
            )
        }
        List<SharedSkillResult> sharedSkills = shareSkillsService.getSharedSkillsFromOtherProjects(projectId)
        sharedSkills.each {
            finalRes.add(
                new SkillDefForDependencyRes(
                    skillId: it.skillId,
                    name: InputSanitizer.unsanitizeName(it.skillName),
                    projectId: projectId,
                    otherProjectId: it.projectId,
                    otherProjectName: InputSanitizer.unsanitizeName(it.projectName)
                )
            )
        }

        return finalRes
    }

    static class GraphSkillDefEdge {
        SkillDefGraphRes from
        SkillDefGraphRes to
    }

    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, SkillRelDef.RelationshipType.Dependence)
        return convertToSkillsGraphRes(edges)
    }


    @Transactional(readOnly = true)
    SkillsGraphRes getDependentSkillsGraph(String projectId, String skillId) {
        List<GraphSkillDefEdge> edges = loadGraphEdges(projectId, SkillRelDef.RelationshipType.Dependence)

        // must only keep the provide skill id and its descendants
        List<GraphSkillDefEdge> collectedRes = []
        List<GraphSkillDefEdge> startEdges = edges.findAll { it.from.skillId == skillId }
        if(startEdges){
            collectDescendants(edges, startEdges, collectedRes)
        }

        return convertToSkillsGraphRes(collectedRes)
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
                    pointIncrement: it[5],
                    totalPoints: it[6],
                    type: it[7],
            )

            SkillDefGraphRes to = new SkillDefGraphRes(
                    id: it[8],
                    name: it[9],
                    skillId: it[10],
                    subjectId: it[11],
                    projectId: it[12],
                    pointIncrement: it[13],
                    totalPoints: it[14],
                    type: it[15],
            )

            new GraphSkillDefEdge(from: from, to: to)

        })
    }

    private void checkForCircularGraphAndThrowException(SkillDef skillDef, SkillDef prereqSkillDef) {
        DependencyCheckResult dependencyCheckResult = checkForCircularGraph(skillDef, prereqSkillDef)
        if (!dependencyCheckResult.possible) {
            String reason = dependencyCheckResult.reason
            if (dependencyCheckResult.circularPath) {
                StringBuilder builder = new StringBuilder()
                dependencyCheckResult.circularPath.eachWithIndex { DependencyCheckResult.SkillInfo item, int index ->
                    String itemStr = "${item.type}:${item.skillId}".toString()
                    if (item.belongsToBadge) {
                        builder.append("(${itemStr})".toString())
                    } else {
                        if (index > 0) {
                            builder.append(" -> ")
                        }
                        builder.append(itemStr)
                    }
                }
                reason = "Discovered circular prerequisite [${builder.toString()}]".toString()
            }
            throw new SkillException(reason, skillDef.projectId, skillDef.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private BadgeAndSkills loadBadgeSkills(Integer badgeRefId, String badgeId, String badgeName) {
        List<SkillDef> badgeSkills = skillRelDefRepo.findChildrenByParent(badgeRefId, [SkillRelDef.RelationshipType.BadgeRequirement])
        List<DependencyCheckResult.SkillInfo> badgeSkillInfos = badgeSkills?.collect { new DependencyCheckResult.SkillInfo(skillId: it.skillId, name: it.name, type: it.type, belongsToBadge: true) }
        return new BadgeAndSkills(
                badgeGraphNode: new DependencyCheckResult.SkillInfo(skillId: badgeId, name: badgeName, type: SkillDef.ContainerType.Badge),
                skills: badgeSkillInfos
        )
    }

    private DependencyCheckResult checkForCircularGraph(SkillDef skillDef, SkillDef prereqSkillDef) {
        assert skillDef.skillId != prereqSkillDef.skillId || skillDef.projectId != prereqSkillDef.projectId

        SkillsGraphRes existingGraph = getDependentSkillsGraph(skillDef.projectId)
        List<BadgeAndSkills> badgeAndSkills = []
        if (prereqSkillDef.type == SkillDef.ContainerType.Badge) {
            badgeAndSkills.add(loadBadgeSkills(prereqSkillDef.id, prereqSkillDef.skillId, prereqSkillDef.name))
        }
        if (skillDef.type == SkillDef.ContainerType.Badge) {
            badgeAndSkills.add(loadBadgeSkills(skillDef.id, skillDef.skillId, skillDef.name))
        }
        if (!existingGraph.nodes && !badgeAndSkills) {
            return new DependencyCheckResult()
        }
        List<SkillDefGraphResPair> edgePairs = existingGraph.edges.collect { SkillsGraphRes.Edge edge ->
            new SkillDefGraphResPair(
                    node: existingGraph.nodes.find { it.id == edge.fromId },
                    prerequisite: existingGraph.nodes.find { it.id == edge.toId },
            )
        }
        // only project local skills dependencies can cause a circular path
        edgePairs = edgePairs?.findAll({ it.prerequisite.projectId == skillDef.projectId })
        Map<String, List<DependencyCheckResult.SkillInfo>> byNodeLookup = [:]
        edgePairs.groupBy { it.node.skillId }.each {
            byNodeLookup[it.key] = it.value.collect { new DependencyCheckResult.SkillInfo(skillId: it.prerequisite.skillId, name: it.prerequisite.name, type: it.prerequisite.type) }
        }

        List<SkillDefGraphRes> badgeNodes = existingGraph.nodes.findAll({ it.type == SkillDef.ContainerType.Badge})
        badgeAndSkills.addAll(badgeNodes.collect {
            Integer badgeId = skillDefRepo.getIdByProjectIdAndSkillIdAndType(it.projectId, it.skillId, SkillDef.ContainerType.Badge)
            return loadBadgeSkills(badgeId, it.skillId, it.name)
        })

        DependencyCheckResult.SkillInfo skillInfo = new DependencyCheckResult.SkillInfo(skillId: skillDef.skillId, name: skillDef.name, type: skillDef.type)
        DependencyCheckResult.SkillInfo prereqSkillInfo = new DependencyCheckResult.SkillInfo(skillId: prereqSkillDef.skillId, name: prereqSkillDef.name, type: prereqSkillDef.type)
        List<DependencyCheckResult.SkillInfo> path = [skillInfo, prereqSkillInfo]
        try {
            return recursiveCircularPrerequisiteCheck(prereqSkillInfo, skillInfo, path, byNodeLookup, badgeAndSkills, 1)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, skillDef.projectId, skillDef.skillId)
        }
    }

    private String getProjectId(SkillDef skill, SkillDef original) {
        return skill.projectId != original.projectId ? "${skill.projectId}:".toString() : ""
    }

    private static class SkillDefGraphResPair {
        SkillDefGraphRes node
        SkillDefGraphRes prerequisite
    }
    private static class BadgeAndSkills {
        DependencyCheckResult.SkillInfo badgeGraphNode
        List<DependencyCheckResult.SkillInfo> skills
    }
    private DependencyCheckResult recursiveCircularPrerequisiteCheck(DependencyCheckResult.SkillInfo current,
                                                                     DependencyCheckResult.SkillInfo start,
                                                                     List<DependencyCheckResult.SkillInfo> path,
                                                                     Map<String, List<DependencyCheckResult.SkillInfo>> byNodeLookup,
                                                                     List<BadgeAndSkills> badgeAndSkills,
                                                                     int currentIter = 0) {
        if (currentIter > 1000) {
            throw new IllegalStateException("Number of [1000] iterations exceeded when checking for circular dependency for [${start.skillId}]")
        }

        if (current.type == SkillDef.ContainerType.Badge) {
            BadgeAndSkills badge = badgeAndSkills.find { it.badgeGraphNode.skillId == current.skillId }

            // step back through the path and see if any of skills are present in the badges on this path
            List<DependencyCheckResult.SkillInfo> badgesOnPath = path.findAll { it.type == SkillDef.ContainerType.Badge && it.skillId != badge.badgeGraphNode.skillId }
            for (DependencyCheckResult.SkillInfo badgeOnPathSkillInfo : badgesOnPath) {
                BadgeAndSkills badgeOnPath = badgeAndSkills.find { it.badgeGraphNode.skillId == badgeOnPathSkillInfo.skillId }
                DependencyCheckResult.SkillInfo found = badgeOnPath.skills.find { DependencyCheckResult.SkillInfo searchFor -> badge.skills.find { searchFor.skillId == it.skillId } }
                if (found) {
                    return new DependencyCheckResult(possible: false, reason: "Multiple badges on the same Learning path cannot have overlapping skills. There is already a badge [${badgeOnPath.badgeGraphNode.name}] on this learning path that has the same skill as [${current.name}] badge. The skill in conflict is [${found.name}].")
                }
            }

            DependencyCheckResult res = handlePrerequisiteNodes(badge.skills, start, path, byNodeLookup, badgeAndSkills, currentIter)
            if (!res.possible) {
                return res
            }
        }
        List<DependencyCheckResult.SkillInfo> prereqNodes = byNodeLookup.get(current.skillId)
        return handlePrerequisiteNodes(prereqNodes, start, path, byNodeLookup, badgeAndSkills, currentIter)
    }

    private DependencyCheckResult handlePrerequisiteNodes(List<DependencyCheckResult.SkillInfo> prereqNodes,
                                                          DependencyCheckResult.SkillInfo start,
                                                          List<DependencyCheckResult.SkillInfo> path,
                                                          Map<String, List<DependencyCheckResult.SkillInfo>> byNodeLookup,
                                                          List<BadgeAndSkills> badgeAndSkills,
                                                          int currentIter) {
        if (prereqNodes) {
            DependencyCheckResult.SkillInfo sameNodeFound = prereqNodes.find { it.skillId == start.skillId }
            if (sameNodeFound) {
                List<String> pathCopy = new ArrayList<>(path)
                pathCopy.add(sameNodeFound)
                return new DependencyCheckResult(possible: false, circularPath: pathCopy)
            }
            for ( DependencyCheckResult.SkillInfo pNode : prereqNodes ) {
                List<DependencyCheckResult.SkillInfo> pathCopy = new ArrayList<>(path)
                pathCopy.add(pNode)
                DependencyCheckResult res = recursiveCircularPrerequisiteCheck(pNode, start, pathCopy, byNodeLookup, badgeAndSkills, currentIter+1)
                if (!res.possible) {
                    return res
                }
            }
        }
        return new DependencyCheckResult()
    }

}
