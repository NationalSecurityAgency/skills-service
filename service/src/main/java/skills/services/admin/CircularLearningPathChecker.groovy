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

import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.DependencyCheckResult
import skills.controller.result.model.SkillDefGraphRes
import skills.controller.result.model.SkillsGraphRes
import skills.storage.model.SkillDef

class CircularLearningPathChecker {

    // inject
    SkillDef skillDef
    SkillDef prereqSkillDef
    SkillsGraphRes existingGraph

    //private
    List<BadgeAndSkills> badgeAndSkills = []

    CircularLearningPathChecker addBadgeAndSkills(BadgeAndSkills b) {
        badgeAndSkills.add(b)
        return this
    }
    CircularLearningPathChecker addAllBadgeAndSkills(List<BadgeAndSkills> b) {
        badgeAndSkills.addAll(b)
        return this
    }
    private boolean isEmpty() {
        return !existingGraph.nodes && !badgeAndSkills;
    }

    private static class SkillDefGraphResPair {
        SkillDefGraphRes node
        SkillDefGraphRes prerequisite
    }
    protected static class BadgeAndSkills {
        DependencyCheckResult.SkillInfo badgeGraphNode
        List<DependencyCheckResult.SkillInfo> skills
    }

    DependencyCheckResult check() {
        if (isEmpty()) {
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

        DependencyCheckResult.SkillInfo skillInfo = new DependencyCheckResult.SkillInfo(skillId: skillDef.skillId, name: skillDef.name, type: skillDef.type)
        DependencyCheckResult.SkillInfo prereqSkillInfo = new DependencyCheckResult.SkillInfo(skillId: prereqSkillDef.skillId, name: prereqSkillDef.name, type: prereqSkillDef.type)
        List<DependencyCheckResult.SkillInfo> path = [skillInfo, prereqSkillInfo]
        try {
            return recursiveCircularPrerequisiteCheck(prereqSkillInfo, skillInfo, path, byNodeLookup, badgeAndSkills, 1)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, skillDef.projectId, skillDef.skillId)
        }
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
                List<DependencyCheckResult.SkillInfo> pathCopy = new ArrayList<>(path)
                pathCopy.add(sameNodeFound)
                return new DependencyCheckResult(possible: false, reason: buildCircularLearningPathErrMsg(pathCopy))
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

    private String buildCircularLearningPathErrMsg(List<DependencyCheckResult.SkillInfo> path) {
        StringBuilder builder = new StringBuilder()
        path.eachWithIndex { DependencyCheckResult.SkillInfo item, int index ->
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
        return "Discovered circular prerequisite [${builder.toString()}]".toString()
    }

}
