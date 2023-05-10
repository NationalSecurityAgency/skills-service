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
    Map<String, BadgeAndSkills> badgeAndSkillsByBadgeId = [:]
    Map<String, List<SkillInfo>> byNodeLookup

    // methods
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
        SkillInfo badgeGraphNode
        List<SkillInfo> skills
    }
    protected static class SkillInfo {
        String skillId
        String name
        SkillDef.ContainerType type
        Boolean belongsToBadge = false
    }
    @Profile
    DependencyCheckResult check() {
        if (isEmpty()) {
            return new DependencyCheckResult()
        }

        badgeAndSkills.each {
            badgeAndSkillsByBadgeId[it.badgeGraphNode.skillId] = it
        }

        List<SkillDefGraphResPair> edgePairs = existingGraph.edges.collect { SkillsGraphRes.Edge edge ->
            new SkillDefGraphResPair(
                    node: existingGraph.nodes.find { it.id == edge.fromId },
                    prerequisite: existingGraph.nodes.find { it.id == edge.toId },
            )
        }
        // only project local skills dependencies can cause a circular path
        edgePairs = edgePairs?.findAll({ it.prerequisite.projectId == skillDef.projectId })
        byNodeLookup = [:]
        edgePairs.groupBy { it.node.skillId }.each {
            byNodeLookup[it.key] = it.value.collect { new SkillInfo(skillId: it.prerequisite.skillId, name: it.prerequisite.name, type: it.prerequisite.type) }
        }

        SkillInfo skillInfo = new SkillInfo(skillId: skillDef.skillId, name: skillDef.name, type: skillDef.type)
        SkillInfo prereqSkillInfo = new SkillInfo(skillId: prereqSkillDef.skillId, name: prereqSkillDef.name, type: prereqSkillDef.type)
        List<SkillInfo> path = [skillInfo, prereqSkillInfo]
        try {
            return recursiveCircularPrerequisiteCheck(prereqSkillInfo, skillInfo, path, 1)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, skillDef.projectId, skillDef.skillId)
        }
    }

    private DependencyCheckResult recursiveCircularPrerequisiteCheck(SkillInfo current,
                                                                     SkillInfo start,
                                                                     List<SkillInfo> path,
                                                                     int currentIter = 0) {
        if (currentIter > 1000) {
            throw new IllegalStateException("Number of [1000] iterations exceeded when checking for circular dependency for [${start.skillId}]")
        }

        if (current.type == SkillDef.ContainerType.Badge) {
            BadgeAndSkills badge = badgeAndSkillsByBadgeId[current.skillId]

            // step back through the path and see if any of skills are present in the badges on this path
            List<SkillInfo> badgesOnPath = path.findAll { it.type == SkillDef.ContainerType.Badge && it.skillId != badge.badgeGraphNode.skillId }
            for (SkillInfo badgeOnPathSkillInfo : badgesOnPath) {
                BadgeAndSkills badgeOnPath = badgeAndSkillsByBadgeId[badgeOnPathSkillInfo.skillId]
                SkillInfo found = badgeOnPath.skills.find { SkillInfo searchFor -> badge.skills.find { searchFor.skillId == it.skillId } }
                if (found) {
                    return new DependencyCheckResult(possible: false,
                            failureType: DependencyCheckResult.FailureType.BadgeOverlappingSkills,
                            violatingSkillInBadgeId: badgeOnPath.badgeGraphNode.skillId,
                            violatingSkillInBadgeName: badgeOnPath.badgeGraphNode.name,
                            violatingSkillId: found.skillId,
                            violatingSkillName: found.name,
                            reason: "Multiple badges on the same Learning path cannot have overlapping skills. There is already a badge [${badgeOnPath.badgeGraphNode.name}] on this learning path that has the same skill as [${current.name}] badge. The skill in conflict is [${found.name}].")
                }
            }

            DependencyCheckResult res = handlePrerequisiteNodes(badge.skills, start, path, currentIter)
            if (!res.possible) {
                return res
            }
        }
        List<SkillInfo> prereqNodes = byNodeLookup.get(current.skillId)
        return handlePrerequisiteNodes(prereqNodes, start, path, currentIter)
    }

    private DependencyCheckResult handlePrerequisiteNodes(List<SkillInfo> prereqNodes,
                                                          SkillInfo start,
                                                          List<SkillInfo> path,
                                                          int currentIter) {
        if (prereqNodes) {
            SkillInfo sameNodeFound = prereqNodes.find { it.skillId == start.skillId }
            if (sameNodeFound) {
                List<SkillInfo> pathCopy = new ArrayList<>(path)
                pathCopy.add(sameNodeFound)
                return buildCircularLearningPathErr(pathCopy)
            }
            for ( SkillInfo pNode : prereqNodes ) {
                List<SkillInfo> pathCopy = new ArrayList<>(path)
                pathCopy.add(pNode)
                DependencyCheckResult res = recursiveCircularPrerequisiteCheck(pNode, start, pathCopy, currentIter+1)
                if (!res.possible) {
                    return res
                }
            }
        }
        return new DependencyCheckResult()
    }

    private DependencyCheckResult buildCircularLearningPathErr(List<SkillInfo> path) {
        String violatingSkillInBadgeId
        String violatingSkillInBadgeName

        StringBuilder builder = new StringBuilder()
        path.eachWithIndex { SkillInfo item, int index ->
            String itemStr = "${item.type}:${item.skillId}".toString()
            if (item.belongsToBadge) {
                builder.append("(${itemStr})".toString())
                if (item.skillId == skillDef.skillId && index > 0) {
                    SkillInfo myBadge = path[index-1]
                    if (myBadge.type == SkillDef.ContainerType.Badge) {
                        violatingSkillInBadgeId = myBadge.skillId
                        violatingSkillInBadgeName = myBadge.name
                    }
                }
            } else {
                if (index > 0) {
                    builder.append(" -> ")
                }
                builder.append(itemStr)
            }
        }
        String msg = "Discovered circular prerequisite [${builder.toString()}]".toString()
        new DependencyCheckResult(possible: false,
                failureType: DependencyCheckResult.FailureType.CircularLearningPath,
                reason: msg,
                violatingSkillInBadgeId: violatingSkillInBadgeId,
                violatingSkillInBadgeName: violatingSkillInBadgeName)
    }

}
