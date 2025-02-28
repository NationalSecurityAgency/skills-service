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
import skills.utils.Props

class CircularLearningPathChecker {

    // inject
    int circularLearningPathCheckerMaxIterations
    SkillDef skillDef
    SkillDef prereqSkillDef
    SkillsGraphRes existingGraph
    List<BadgeAndSkills> badgeAndSkills
    boolean performAlreadyExistCheck = true

    // private
    private BadgeAndSkillsLookup badgeAndSkillsLookup = new BadgeAndSkillsLookup()
    private PrerequisiteNodeLookup prerequisiteNodeLookup = new PrerequisiteNodeLookup()
    private Set<String> allItemIdsOnFinalLearningPath
    private SkillInfo start

    // contains all of the badges by following start node in the opposite direction of prerequisite path
    List<SkillInfo> startNodeBadgesOnParentPath

    static class BadgeAndSkillsLookup {
        List<BadgeAndSkills> badgeAndSkills = []
        Map<String, BadgeAndSkills> badgeAndSkillsByBadgeId = [:]
        Map<String, List<SkillInfo>> badgesBySkillId = [:]
        void addAll(List<BadgeAndSkills> badgeAndSkillsInput) {
            badgeAndSkills.addAll(badgeAndSkillsInput)
            badgeAndSkills.each {
                badgeAndSkillsByBadgeId[it.badgeGraphNode.skillId] = it
            }
            badgeAndSkills.each { BadgeAndSkills badgeAndSkillsItem ->
                badgeAndSkillsItem.skills.each {
                    String skillIdLookup = it.skillId
                    List<SkillInfo> badges = badgesBySkillId[skillIdLookup]
                    if (badges) {
                        if (!badges.find { it.skillId == badgeAndSkillsItem.badgeGraphNode.skillId }) {
                            badges.add(badgeAndSkillsItem.badgeGraphNode)
                        }
                    } else {
                        badgesBySkillId[it.skillId] = [badgeAndSkillsItem.badgeGraphNode]
                    }
                }
            }
        }

        BadgeAndSkills getBadgeByBadgeId(String badgeId) {
            return badgeAndSkillsByBadgeId[badgeId];
        }

        List<SkillInfo> findBadgesThisSkillBelongsTo(String skillId) {
            return badgesBySkillId[skillId]
        }
        boolean isEmpty() {
            return badgeAndSkills.isEmpty()
        }
    }

    static class PrerequisiteNodeLookup {
        Map<String, List<SkillInfo>> prerequisiteNodeLookup = [:]
        Map<String, List<SkillInfo>> prerequisiteParentNodeLookup = [:]

        void addEdgePairs(List<SkillDefGraphResPair> edgePairs) {
            edgePairs.groupBy { getMapKey(it.node.projectId, it.node.skillId) }.each {
                List<SkillInfo> values = it.value.collect { new SkillInfo(projectId: it.prerequisite.projectId, skillId: it.prerequisite.skillId, name: it.prerequisite.name, type: it.prerequisite.type) }
                prerequisiteNodeLookup[it.key] = values
            }
            edgePairs.groupBy { getMapKey(it.prerequisite.projectId, it.prerequisite.skillId) }.each {
                prerequisiteParentNodeLookup[it.key] = it.value.collect { new SkillInfo(projectId: it.node.projectId, skillId: it.node.skillId, name: it.node.name, type: it.node.type) }
            }
        }
        List<SkillInfo> get(SkillInfo current) {
            return prerequisiteNodeLookup[getMapKey(current)]
        }
        List<SkillInfo> getParents(SkillInfo current) {
            return prerequisiteParentNodeLookup[getMapKey(current)]
        }
        private String getMapKey(SkillInfo s) {
            return getMapKey(s.projectId, s.skillId)
        }
        private String getMapKey(String projectId, String skillId) {
            return "${projectId}-${skillId}".toString()
        }
    }

    private boolean isEmpty() {
        return !existingGraph.nodes && badgeAndSkillsLookup.isEmpty();
    }

    private static class SkillDefGraphResPair {
        SkillDefGraphRes node
        SkillDefGraphRes prerequisite
    }
    protected static class BadgeAndSkills {
        SkillInfo badgeGraphNode
        List<SkillInfo> skills
        private Set<String> cachedSkillIds

        private Set<String> getSkillIds() {
            if (!cachedSkillIds) {
                cachedSkillIds = new HashSet<>(skills.collect { it.skillId })
            }
            return cachedSkillIds
        }
        boolean badgeHasSkillId(String skillId) {
            return skillIds.contains(skillId)
        }
    }
    protected static class SkillInfo {
        String projectId
        String skillId
        String name
        SkillDef.ContainerType type
        Boolean belongsToBadge = false
        // only set if belongsToBadge = true
        String belongsToBadgeId

        // circular check indicator that examined skill came
        // because logic decided to follow badge's skills for circular check
        Boolean circularCheckProvidedBecauseFollowingSkillsUnderBadge = false

        Boolean circularCheckBadgeLoadedDueToPreviousSkill = false
        // when following all of badges for a given skill, keep track which badge Id is follow
        String circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId = null

        @Override
        protected Object clone() throws CloneNotSupportedException {
            SkillInfo copy = new SkillInfo()
            Props.copy(this, copy)
            return copy
        }
    }
    @Profile
    DependencyCheckResult check() {
        badgeAndSkillsLookup.addAll(this.badgeAndSkills)
        if (isEmpty()) {
            return new DependencyCheckResult()
        }

        List<SkillDefGraphResPair> edgePairs = existingGraph.edges.collect { SkillsGraphRes.Edge edge ->
            new SkillDefGraphResPair(
                    node: existingGraph.nodes.find { it.id == edge.fromId },
                    prerequisite: existingGraph.nodes.find { it.id == edge.toId },
            )
        }
        if (performAlreadyExistCheck) {
            for (SkillDefGraphResPair edgePair : edgePairs) {
                if (edgePair.prerequisite.skillId == prereqSkillDef.skillId
                        && edgePair.prerequisite.projectId == prereqSkillDef.projectId
                        && edgePair.node.skillId == skillDef.skillId
                        && edgePair.node.projectId == skillDef.projectId) {
                    return new DependencyCheckResult(possible: false,
                            failureType: DependencyCheckResult.FailureType.AlreadyExist,
                            violatingSkillId: prereqSkillDef.skillId,
                            violatingSkillName: prereqSkillDef.name,
                            reason: "Learning path from [${prereqSkillDef.name}] to [${skillDef.name}] already exists.")
                }
            }
        }
        prerequisiteNodeLookup.addEdgePairs(edgePairs)
        SkillInfo skillInfo = new SkillInfo(projectId: skillDef.projectId, skillId: skillDef.skillId, name: skillDef.name, type: skillDef.type)
        SkillInfo prereqSkillInfo = new SkillInfo(projectId: prereqSkillDef.projectId, skillId: prereqSkillDef.skillId, name: prereqSkillDef.name, type: prereqSkillDef.type)
        List<SkillInfo> path = [skillInfo]
        allItemIdsOnFinalLearningPath = constructAllItemIdsOnFinalLearningPath(edgePairs, skillInfo, prereqSkillInfo)

        startNodeBadgesOnParentPath = []
        collectParentBadges(skillInfo, startNodeBadgesOnParentPath, 0)
        start = skillInfo
        try {
            return recursiveCircularPrerequisiteCheck(prereqSkillInfo, path, 1)
        } catch (Throwable t) {
            throw new SkillException(t.message, t, skillDef.projectId, skillDef.skillId)
        }
    }

    private DependencyCheckResult recursiveCircularPrerequisiteCheck(SkillInfo current,
                                                                     List<SkillInfo> path,
                                                                     int currentIter) {
        if (currentIter > 1000) {
            throw new IllegalStateException("Number of [1000] iterations exceeded when checking for circular dependency for [${start.skillId}]")
        }

        SkillInfo sameNodeFound = path.find { SkillInfo checkItem ->
            boolean sameItem = checkItem.skillId == current.skillId && checkItem.projectId == current.projectId && checkItem.type == current.type;
            boolean skillLoadedDueToBadge = current.type == SkillDef.ContainerType.Skill && current.circularCheckBadgeLoadedDueToPreviousSkill
            if (sameItem && skillLoadedDueToBadge) {
                List<SkillInfo> checkTheseBadges = badgeAndSkillsLookup.findBadgesThisSkillBelongsTo(checkItem.skillId)
                if (checkTheseBadges) {
                    SkillInfo compliantBadge = checkTheseBadges.find {it.skillId == current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId }
                    sameItem = (compliantBadge == null)
                }
            }

            return sameItem
        }
        if (sameNodeFound) {
            List<SkillInfo> pathCopy = new ArrayList<>(path)
            pathCopy.add(current)
            return buildCircularLearningPathErr(pathCopy)
        }

        DependencyCheckResult prerequisitesResult = followPrequisites(current, path, currentIter)
        if (!prerequisitesResult.possible) {
            return prerequisitesResult
        }

        if (current.type == SkillDef.ContainerType.Badge) {
            BadgeAndSkills badge = badgeAndSkillsLookup.getBadgeByBadgeId(current.skillId)

            // step back through the path and see if any of skills are present in the badges on this path
            List<SkillInfo> badgesOnPath = new ArrayList<>(path).findAll { it.type == SkillDef.ContainerType.Badge && it.skillId != badge.badgeGraphNode.skillId }
            DependencyCheckResult overlapRes = checkBadgesForSkillOverlap(badge, badgesOnPath)
            if (!overlapRes.possible) {
                return overlapRes
            }

            List<SkillInfo> skillsUnderBadgeToCheck = badge.skills.collect {
                SkillInfo clone = it.clone()
                clone.circularCheckProvidedBecauseFollowingSkillsUnderBadge = true
                return clone
            }
            if (current.circularCheckBadgeLoadedDueToPreviousSkill) {
                String lastSkillId = path.last().skillId
                skillsUnderBadgeToCheck = skillsUnderBadgeToCheck.findAll { it.skillId != lastSkillId }
            }
            List<SkillInfo> pathCopy = new ArrayList<>(path)
            pathCopy.add(current)
            for ( SkillInfo pNode : skillsUnderBadgeToCheck ) {
                SkillInfo skillCopy = pNode.clone()
                skillCopy.circularCheckBadgeLoadedDueToPreviousSkill = current.circularCheckBadgeLoadedDueToPreviousSkill
                skillCopy.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId = current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId
                DependencyCheckResult res = recursiveCircularPrerequisiteCheck(skillCopy, pathCopy, currentIter+1)
                if (!res.possible) {
                    return res
                }
            }
            // check overlap against nodes that were retrieved by starting at initial parent node and walking up the chain; must happen after following prerequisites graph
            DependencyCheckResult overlapResInOtherDirection = checkBadgesForSkillOverlap(badge, startNodeBadgesOnParentPath)
            if (!overlapResInOtherDirection.possible) {
                return overlapResInOtherDirection
            }
        }
        if (current.type == SkillDef.ContainerType.Skill) {
            List<SkillInfo> badgesOnPath = new ArrayList<>(path)
            // do not consider badge for violation if this skill came from the badge itself
            if (!badgesOnPath.isEmpty() && path.last().skillId == current.belongsToBadgeId) {
                badgesOnPath.removeLast()
            }
            if (!current.circularCheckProvidedBecauseFollowingSkillsUnderBadge) {
                // do not try to traverse badges the other direction when skill was provided by a badge
                badgesOnPath.addAll(startNodeBadgesOnParentPath)
            }

            badgesOnPath = badgesOnPath.findAll { it.type == SkillDef.ContainerType.Badge }
            for (SkillInfo badgeOnPathSkillInfo : badgesOnPath) {
                BadgeAndSkills badge = badgeAndSkillsLookup.getBadgeByBadgeId(badgeOnPathSkillInfo.skillId)
                if (isBadgeOnCurrentLearningPath(badge) &&
                        badge.badgeHasSkillId(current.skillId) &&
                        badge.badgeGraphNode.skillId != current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId) {
                    return new DependencyCheckResult(possible: false,
                            failureType: DependencyCheckResult.FailureType.BadgeSkillIsAlreadyOnPath,
                            violatingSkillId: current.skillId,
                            violatingSkillName: current.name,
                            reason: "Badge [${badge.badgeGraphNode.name}] has skill [${current.name}] which already exists on the Learning Path.")
                }
            }

            if (!current.circularCheckProvidedBecauseFollowingSkillsUnderBadge) {
                List<SkillInfo> badgesIBelongTo = badgeAndSkillsLookup.findBadgesThisSkillBelongsTo(current.skillId)
                if (badgesIBelongTo) {
                    // do not follow try to walk down badge that is currently being checked
                    if (current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId) {
                        badgesIBelongTo = badgesIBelongTo.findAll( { it.skillId != current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId })
                    }
                    for (SkillInfo badgeIBelongTo : badgesIBelongTo) {
                        SkillInfo myBadge = badgeIBelongTo.clone()
                        myBadge.circularCheckBadgeLoadedDueToPreviousSkill = true
                        myBadge.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId = myBadge.skillId
                        List<SkillInfo> pathCopy = new ArrayList<>(path)
                        pathCopy.add(current)
                        DependencyCheckResult res = recursiveCircularPrerequisiteCheck(myBadge, pathCopy, currentIter + 1)
                        if (!res.possible) {
                            return res
                        }
                    }
                }
            }
        }
        return new DependencyCheckResult()
    }

    private DependencyCheckResult followPrequisites(SkillInfo current, List<SkillInfo> path, int currentIter) {
        List<SkillInfo> prereqNodes = prerequisiteNodeLookup.get(current)
        List<SkillInfo> pathCopy = new ArrayList<>(path)
        pathCopy.add(current)
        for ( SkillInfo pNode : prereqNodes ) {
            SkillInfo nodeCopy = pNode.clone()
            nodeCopy.circularCheckBadgeLoadedDueToPreviousSkill = current.circularCheckBadgeLoadedDueToPreviousSkill
            nodeCopy.circularCheckProvidedBecauseFollowingSkillsUnderBadge = current.circularCheckProvidedBecauseFollowingSkillsUnderBadge
            nodeCopy.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId = current.circularCheckBadgeLoadedDueToPreviousSkillFollowingRouteOfBadgeId
            DependencyCheckResult res = recursiveCircularPrerequisiteCheck(nodeCopy, pathCopy, currentIter+1)
            if (!res.possible) {
                return res
            }
        }

        return new DependencyCheckResult()
    }

    private DependencyCheckResult checkBadgesForSkillOverlap(BadgeAndSkills badge, List<SkillInfo> checkAgainst) {
        for (SkillInfo badgeOnPathSkillInfo : checkAgainst) {
            BadgeAndSkills badgeOnPath = badgeAndSkillsLookup.getBadgeByBadgeId(badgeOnPathSkillInfo.skillId)
            if (isBadgeOnCurrentLearningPath(badgeOnPath)) {
                SkillInfo found = badgeOnPath.skills.find { SkillInfo searchFor -> badge.skills.find { searchFor.skillId == it.skillId } }
                if (found) {
                    return new DependencyCheckResult(possible: false,
                            failureType: DependencyCheckResult.FailureType.BadgeOverlappingSkills,
                            violatingSkillInBadgeId: badge.badgeGraphNode.skillId,
                            violatingSkillInBadgeName:  badge.badgeGraphNode.name,
                            violatingSkillId: found.skillId,
                            violatingSkillName: found.name,
                            reason: "Multiple badges on the same Learning path cannot have overlapping skills. Both badge [${badge.badgeGraphNode.name}] and [${badgeOnPath.badgeGraphNode.name}] badge have [${found.name}] skill.")
                }
            }
        }

        return new DependencyCheckResult()
    }

    private Set<String> constructAllItemIdsOnFinalLearningPath(List<SkillDefGraphResPair> edgePairs, SkillInfo skillInfo, SkillInfo prereqSkillInfo) {
        Set res = edgePairs.collect {
            ["${it.node.projectId}-${it.node.skillId}".toString(), "${it.prerequisite.projectId}-${it.prerequisite.skillId}".toString()]
        }.flatten().toSet()
        res.add("${skillInfo.projectId}-${skillInfo.skillId}".toString())
        res.add("${prereqSkillInfo.projectId}-${prereqSkillInfo.skillId}".toString())
        return res
    }
    private boolean isBadgeOnCurrentLearningPath(BadgeAndSkills badge) {
        boolean isBadgeOnPath = allItemIdsOnFinalLearningPath.contains("${badge.badgeGraphNode.projectId}-${badge.badgeGraphNode.skillId}".toString())
        return isBadgeOnPath
    }

    private void collectParentBadges(SkillInfo start, List<SkillInfo> badges, int currentIteration) {
        if (currentIteration > 1000) {
            throw new IllegalStateException("Number of [1000] iterations exceeded when checking for circular dependency for [${start.skillId}]")
        }

        List<SkillInfo> skillInfos = prerequisiteNodeLookup.getParents(start)
        for (SkillInfo skillInfo : skillInfos) {
            if (skillInfo.type == SkillDef.ContainerType.Badge) {
                badges.add(skillInfo)
            }
            collectParentBadges(skillInfo, badges, currentIteration + 1)
        }
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
