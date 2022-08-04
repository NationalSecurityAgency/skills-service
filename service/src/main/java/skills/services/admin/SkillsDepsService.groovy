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
    void assignSkillDependency(String projectId, String dependentSkillId, String dependencySkillId, String dependendencyProjectId = null) {
        SkillDef dependent = skillDefAccessor.getSkillDef(projectId, dependentSkillId)
        SkillDef dependency = skillDefAccessor.getSkillDef(dependendencyProjectId ?: projectId, dependencySkillId)

        if (skillCatalogService.isAvailableInCatalog(dependent)) {
            throw new SkillException("Skill [${dependent.skillId}] has been shared to the catalog. Dependencies cannot be added to a skill shared to the catalog.", projectId, dependentSkillId, ErrorCode.DependenciesNotAllowed)
        }
        if (skillDefRepo.wasThisSkillReusedElsewhere(dependent.id)) {
            throw new SkillException("Skill [${dependent.skillId}] was reused in another subject or group. Dependencies cannot be added to a skill that was reused.", projectId, dependentSkillId, ErrorCode.DependenciesNotAllowed)
        }

        if (dependendencyProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, dependency)
        }

        validateDependencyVersions(dependent, dependency)
        checkForCircularGraphAndThrowException(dependent, dependency, SkillRelDef.RelationshipType.Dependence)
        try {
            skillRelDefRepo.save(new SkillRelDef(parent: dependent, child: dependency, type: SkillRelDef.RelationshipType.Dependence))
        } catch (DataIntegrityViolationException e) {
            String msg = "Skill dependency [${dependent.projectId}:${dependent.skillId}]=>[${dependency.projectId}:${dependency.skillId}] already exist.".toString()
            log.error(msg, e)
            throw new SkillException(msg, dependent.projectId, dependent.skillId, ErrorCode.FailedToAssignDependency)
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
        ruleSetDefGraphService.removeGraphRelationship(projectId, dependentSkillId, SkillDef.ContainerType.Skill,
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
        res.numPerformToCompletion = skillDef.totalPoints / res.pointIncrement
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

    private void checkForCircularGraphAndThrowException(SkillDef skill1, SkillDef skill2, SkillRelDef.RelationshipType type) {
        assert skill1.skillId != skill2.skillId || skill1.projectId != skill2.projectId

        DependencyCheckResult dependencyCheckResult = checkForCircularGraph(skill1, skill2, type)
        if (!dependencyCheckResult.possible) {
            throw new SkillException(dependencyCheckResult.reason, skill1.projectId, skill1.skillId, ErrorCode.FailedToAssignDependency)
        }
    }

    private DependencyCheckResult checkForCircularGraph(SkillDef proposedParent, SkillDef proposedChild, SkillRelDef.RelationshipType type) {
        try {
            recursiveCircularDependenceCheck(proposedChild, proposedParent, [getDependencyCheckId(proposedParent), getDependencyCheckId(proposedChild)], type)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, proposedParent.projectId, proposedParent.skillId)
        }
    }

    private String getDependencyCheckId(SkillDef skill) {
        return skill.projectId + ":" + skill.skillId
    }

    private DependencyCheckResult recursiveCircularDependenceCheck(SkillDef parent, SkillDef originalParent, List<String> idPath, SkillRelDef.RelationshipType type, int currentIter = 0, int maxIter = 100) {
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
}
