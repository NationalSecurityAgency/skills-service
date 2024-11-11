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
package skills.skillLoading

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.settings.ClientPrefKey
import skills.services.settings.ClientPrefService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.skillLoading.model.SkillDependencySummary
import skills.skillLoading.model.SkillTag
import skills.storage.model.*
import skills.storage.repos.ExpiredUserAchievementRepo
import skills.storage.repos.QuizToSkillDefRepo
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.UserQuizAttemptRepo

@Component
@Slf4j
@CompileStatic
class SubjectDataLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    DependencySummaryLoader dependencySummaryLoader

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    ClientPrefService clientPrefService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    ExpiredUserAchievementRepo expiredUserAchievementRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints
        String copiedFromProjectName
        String description
        Boolean isLastViewed
        String quizId
        QuizDefParent.QuizType quizType
        String quizName
        Integer quizNumQuestions
        UserQuizAttempt.QuizAttemptStatus lastQuizAttemptStatus
        Date lastQuizAttemptDate
        Integer lastQuizAttemptId

        SkillDependencySummary dependencyInfo

        List<SkillsAndPoints> children = []
        SkillApproval approval
        List<SimpleBadgeRes> badges = []
        List<SkillTag> tags = []
        SkillAttributesDef attributes
        Date expiredOn
        Date achievedOn
        String approverUserIdForDisplay
    }

    static class SkillsData {
        List<SkillsAndPoints> childrenWithPoints
    }

    @Profile
    SkillsData loadData(String userId,  String projectId, SkillDefParent skillDefWithExtra, Integer version = Integer.MAX_VALUE, List<SkillRelDef.RelationshipType> relationshipTypes) {
        String skillId = skillDefWithExtra.skillId
        List<SkillDefAndUserPoints> childrenWithUserPoints = loadChildren(userId, projectId, skillId, relationshipTypes, version)
        childrenWithUserPoints = childrenWithUserPoints?.sort({ it.skillDef.displayOrder })

        List<UserPointsRepo.SkillRefIdWithPoints> todaysUserPoints = loadChildrenDayPoints(userId, skillDefWithExtra.id, relationshipTypes, new Date().clearTime())

        List<UserPointsRepo.SkillWithChildAndAchievementIndicator> allProjectDepsAndAchievements = loadAllDepsWithAchievementIndicator(userId, projectId, version)
        Map<Integer, List<UserPointsRepo.SkillWithChildAndAchievementIndicator>> byParentId = allProjectDepsAndAchievements.groupBy { it.parentId }

        List<SkillsAndPoints> skillsAndPoints = childrenWithUserPoints.collect { SkillDefAndUserPoints skillDefAndUserPoints ->
            UserPointsRepo.SkillRefIdWithPoints todaysPoints = todaysUserPoints.find({
                it.skillRefId == skillDefAndUserPoints.skillDef.id
            })

            int todayPoints = todaysPoints?.points ? todaysPoints.points : 0
            int points = skillDefAndUserPoints?.points ? skillDefAndUserPoints.points.points : 0

            if (skillDefAndUserPoints.skillDef.copiedFrom != null && skillDefAndUserPoints.skillDef.selfReportingType) {
                // because of the catalog's async nature when self-approval honor skill is submitted todaysPoints and points are not consistent on the imported side
                // this is because todaysPoints are calculated from UserPerformedSkill but points come from UserPoints; UserPerformedSkill
                // is shared in the catalog exported/imported skills but UserPoints are duplicated and asynchronously synced
                if (todayPoints > points) {
                    // this will at least account for 1 event that have not been propagated and make it a bit more consistent
                    // it mostly likely will account for the first event only unless multiple skill events are submitted in the same day
                    points = points + skillDefAndUserPoints.skillDef.pointIncrement
                }
            }

            List<UserPointsRepo.SkillWithChildAndAchievementIndicator> dependents = byParentId[skillDefAndUserPoints.skillDef.id]
            SkillDependencySummary dependencyInfo
            if (dependents) {
                dependencyInfo = dependents ? new SkillDependencySummary(
                        numDirectDependents: dependents.size(),
                        achieved: !dependents.find { it.getAchievementId() == null }
                ) : null
            }
            new SkillsAndPoints(skillDef: skillDefAndUserPoints.skillDef, points: points, todaysPoints: todayPoints, dependencyInfo: dependencyInfo,
                    copiedFromProjectName: skillDefAndUserPoints.copiedFromProjectName, approval: skillDefAndUserPoints.approval, attributes: skillDefAndUserPoints.attributes,
                    approverUserIdForDisplay: skillDefAndUserPoints.approverUserIdForDisplay)
        }

        updateLastViewedSkill(skillsAndPoints, userId, projectId)

        skillsAndPoints = handleGroupSkills(skillsAndPoints, relationshipTypes)
        skillsAndPoints = handleGroupDescriptions(projectId, skillsAndPoints, relationshipTypes)
        skillsAndPoints = handleBadges(projectId, skillsAndPoints)
        skillsAndPoints = handleSkillTags(projectId, skillsAndPoints)
        skillsAndPoints = handleSkillQuizInfo(projectId, skillsAndPoints, userId)
        skillsAndPoints = handleAchievements(projectId, userId, skillsAndPoints)
        skillsAndPoints = handleSkillExpirations(projectId, userId, skillsAndPoints)

        new SkillsData(childrenWithPoints: skillsAndPoints)
    }

    @Profile
    private void updateLastViewedSkill(List<SkillsAndPoints> skillsAndPoints, String userId, String projectId) {
        String lastViewedSkillId = null
        if (projectId) {
            ClientPref clientPref = clientPrefService.findPref(ClientPrefKey.LastViewedSkill, userId, projectId)
            lastViewedSkillId = clientPref?.value
        }
        skillsAndPoints.each {
            it.isLastViewed = it.skillDef.skillId == lastViewedSkillId
        }
    }

    @Profile
    private List<SkillsAndPoints> handleBadges(String projectId, List<SkillsAndPoints> skillsAndPoints) {
        if(projectId) {
            List<String> skillIds = collectSkillIds(skillsAndPoints)
            def badges = skillDefRepo.findAllBadgesForSkill(skillIds, projectId);
            def badgesById = badges.groupBy{ it.skillId }
            skillsAndPoints.forEach{ it ->
                it.badges = badgesById[it.skillDef.skillId]
                it.children.forEach{ child ->
                    child.badges = badgesById[child.skillDef.skillId]
                }
            }
        }
        return skillsAndPoints;
    }

    private List<SkillsAndPoints> handleAchievements(String projectId, String userId, List<SkillsAndPoints> skillsAndPoints) {
        if(projectId) {
            List<String> skillIds = collectSkillIds(skillsAndPoints)
            List<UserAchievement> achievedSkills = achievedLevelRepository.getAchievedDateByUserIdAndProjectIdAndSkillBatch(userId, projectId, skillIds)
            if (achievedSkills) {
                skillsAndPoints.each { it ->
                    List<UserAchievement> achievements = achievedSkills.findAll{skill -> skill.skillId == it.skillDef.skillId}
                    if(achievements) {
                        achievements?.sort { skill -> skill.achievedOn }
                        it.achievedOn = achievements.first()?.achievedOn
                    }
                }
            }
        }
        return skillsAndPoints
    }

    private List<SkillsAndPoints> handleSkillExpirations(String projectId, String userId, List<SkillsAndPoints> skillsAndPoints) {
        if(projectId) {
            List<String> skillIds = collectUnachievedSkillIds(skillsAndPoints)
            def expiredSkills = expiredUserAchievementRepo.findMostRecentExpirationForAllSkills(projectId, userId, skillIds)
            if (expiredSkills) {
                skillsAndPoints.each { it ->
                    def expirations = expiredSkills.findAll{skill -> skill.skillId == it.skillDef.skillId}
                    if(expirations) {
                        expirations?.sort { skill -> skill.expiredOn }
                        it.expiredOn = expirations.first()?.expiredOn
                    }
                }
            }
        }
        return skillsAndPoints
    }

    @Profile
    private List<SkillsAndPoints> handleSkillTags(String projectId, List<SkillsAndPoints> skillsAndPoints) {
        if(projectId) {
            List<String> skillIds = collectSkillIds(skillsAndPoints)
            def tagWithSkillIds = skillDefRepo.getTagsForSkillsWithSkillId(projectId, skillIds);
            def tagsById = tagWithSkillIds.groupBy{ it.skillId }
            skillsAndPoints.forEach{ it ->
                it.tags = tagsById[it.skillDef.skillId]?.collect { new SkillTag(tagId: it.tagId, tagValue: it.tagValue)}?.sort { a, b -> a.tagValue <=> b.tagValue }
                it.children.forEach{ child ->
                    child.tags = tagsById[child.skillDef.skillId]?.collect { new SkillTag(tagId: it.tagId, tagValue: it.tagValue)}?.sort { a, b -> a.tagValue <=> b.tagValue }
                }
            }
        }
        return skillsAndPoints;
    }

    @Profile
    private List<SkillsAndPoints> handleSkillQuizInfo(String projectId, List<SkillsAndPoints> skillsAndPoints, String userId) {
        if(projectId) {
            List<SkillsAndPoints> allSkillAndPoints = (List<SkillsAndPoints>)skillsAndPoints
                    .collect { SkillsAndPoints skAndPts -> (skAndPts.skillDef.type == SkillDef.ContainerType.SkillsGroup) ? skAndPts.children : skAndPts }
                    .flatten()
            List<SkillsAndPoints> quizBasedSkills = allSkillAndPoints.findAll { it.skillDef.selfReportingType == SkillDef.SelfReportingType.Quiz}
            if (quizBasedSkills) {
                List<Integer> skillRefIds = quizBasedSkills.collect { it.skillDef.copiedFrom ?: it.skillDef.id }
                List<QuizToSkillDefRepo.QuizNameAndId> quizInfo = quizToSkillDefRepo.getQuizInfoSkillIdRef(skillRefIds)

                List<Integer> quizIds = quizInfo.findAll { it.getNumTextInputQuestions() > 0 }?.collect { it.getQuizRefId() }?.unique()?.toList()
                Map<Integer, List<QuizToSkillDefRepo.QuizAttemptInfo>> latestAttemptsByQuizRefId = [:]
                if (quizIds) {
                    Integer[] quizRefIdArray = quizIds.toArray(new Integer[0]);
                    List<QuizToSkillDefRepo.QuizAttemptInfo> quizAttempts = userQuizAttemptRepo.getLatestQuizAttemptsForUserByQuizIds(quizRefIdArray, userId)
                    if (quizAttempts) {
                        latestAttemptsByQuizRefId = quizAttempts.groupBy { it.quizDefRefId }
                    }
                }
                Map<Integer, List<QuizToSkillDefRepo.QuizNameAndId>> bySkillRefId = quizInfo.groupBy() { it.getSkillRefId() }
                quizBasedSkills.each {
                    List<QuizToSkillDefRepo.QuizNameAndId> found = bySkillRefId[it.skillDef.copiedFrom ?: it.skillDef.id]
                    if (found) {
                        QuizToSkillDefRepo.QuizNameAndId quizNameAndId = found.first()
                        it.quizId = quizNameAndId.quizId
                        it.quizName = quizNameAndId.quizName
                        it.quizType = quizNameAndId.quizType
                        it.quizNumQuestions = quizNameAndId.numQuestions

                        List<QuizToSkillDefRepo.QuizAttemptInfo> attempts = latestAttemptsByQuizRefId[quizNameAndId.quizRefId]
                        if (attempts) {
                            QuizToSkillDefRepo.QuizAttemptInfo lastAttempt = attempts.first()
                            it.lastQuizAttemptStatus = lastAttempt.status
                            it.lastQuizAttemptId = lastAttempt.attemptId
                            it.lastQuizAttemptDate = lastAttempt.updated
                        }

                    } else {
                        log.error("Failed to find quiz for skill ref id [{}]. This is likely an issue with the data and a record is missing in the QuizToSkillDef or SkillDef's SelfReportingType.Quiz is not correct.", it.skillDef.id)
                    }
                }
            }
        }
        return skillsAndPoints;
    }

    private List<String> collectSkillIds(List<SkillsAndPoints> skillsAndPoints) {
        List<String> skillIds = []
        skillsAndPoints.forEach { it ->
            if(it.skillDef.type == SkillDef.ContainerType.SkillsGroup) {
                if(it.children) {
                    skillIds.addAll(it.children.collect{ child -> child.skillDef.skillId })
                }
            }
            else if(it.skillDef.type == SkillDef.ContainerType.Skill) {
                skillIds.add(it.skillDef.skillId)
            }
        }
        return skillIds
    }

    private List<String> collectUnachievedSkillIds(List<SkillsAndPoints> skillsAndPoints) {
        List<String> skillIds = []
        skillsAndPoints.forEach { it ->
            if(!it.achievedOn) {
                if (it.skillDef.type == SkillDef.ContainerType.SkillsGroup) {
                    if (it.children) {
                        skillIds.addAll(it.children.findAll{ child -> !child.achievedOn }.collect { child -> child.skillDef.skillId })
                    }
                } else if (it.skillDef.type == SkillDef.ContainerType.Skill) {
                    skillIds.add(it.skillDef.skillId)
                }
            }
        }
        return skillIds
    }

    private List<SkillsAndPoints> handleGroupDescriptions(String projectId, List<SkillsAndPoints> skillsAndPoints, List<SkillRelDef.RelationshipType> relationshipTypes) {
        if (relationshipTypes.containsAll([SkillRelDef.RelationshipType.SkillsGroupRequirement, SkillRelDef.RelationshipType.GroupSkillToSubject])) {
            List<SkillsAndPoints> groups = skillsAndPoints.findAll({ it.skillDef.type == SkillDef.ContainerType.SkillsGroup })
            List<String> groupIds = groups.collect { it.skillDef.skillId }
            if (groupIds) {
                Boolean groupDescriptionsOn = settingsService.getProjectSetting(projectId, Settings.GROUP_DESCRIPTIONS.settingName)?.value?.toBoolean()
                if (groupDescriptionsOn) {
                    Map<String, List<SkillsAndPoints>> bySkillId = groups.groupBy { it.skillDef.skillId}
                    List<SkillDefWithExtraRepo.SkillIdAndDesc> loadedDescriptions = skillDefWithExtraRepo.findDescriptionBySkillIdIn(projectId, groupIds)
                    loadedDescriptions.each {
                        SkillsAndPoints group = bySkillId[it.getSkillId()]?.first()
                        if (!group) {
                            throw new IllegalStateException("Failed locate the group. This is a logic error. lookedFor=[${it.getSkillId()}]")
                        }
                        group.description = it.description
                    }
                }
            }
        }
        return skillsAndPoints;
    }

    private List<SkillsAndPoints> handleGroupSkills(List<SkillsAndPoints> skillsAndPoints, List<SkillRelDef.RelationshipType> relationshipTypes) {
        if (relationshipTypes.containsAll([SkillRelDef.RelationshipType.SkillsGroupRequirement, SkillRelDef.RelationshipType.GroupSkillToSubject])) {
            List<SkillsAndPoints> res = skillsAndPoints
            List<SkillsAndPoints> childrenOfGroups = skillsAndPoints.findAll({ it.skillDef.groupId })
            if (childrenOfGroups) {
                res = skillsAndPoints.findAll({ !it.skillDef.groupId })
                Map<String, List<SkillsAndPoints>> bySkillId = res.groupBy { it.skillDef.skillId }
                childrenOfGroups.each {
                    SkillsAndPoints parent = bySkillId[it.skillDef.groupId]?.first()
                    if (!parent) {
                        throw new IllegalStateException("Failed to find group for a skill under that group. groupSkillId=[${it.skillDef.groupId}], childSkillId=[${it.skillDef.skillId}]")
                    }
                    parent.children.add(it)
                }
            }

            return res
        }
        return skillsAndPoints
    }

    @Profile
    private List<UserPointsRepo.SkillWithChildAndAchievementIndicator> loadAllDepsWithAchievementIndicator(String userId, String projectId, int version) {
        if (projectId) {
            userPointsRepo.findAllChildrenAndTheirAchievementsForProject(userId, projectId, SkillRelDef.RelationshipType.Dependence, version)
        } else {
            userPointsRepo.findAllChildrenAndTheirAchievementsForGlobal(userId, SkillRelDef.RelationshipType.Dependence, version)
        }
    }

    private static class SkillDefAndUserPoints {
        SkillDef skillDef
        UserPoints points
        String copiedFromProjectName
        SkillApproval approval
        SkillAttributesDef attributes
        String approverUserIdForDisplay
    }

    @Profile
    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, Integer version = Integer.MAX_VALUE) {

        List<Object[]> childrenWithUserPoints = findChildrenPoints(userId, projectId, skillId, relationshipTypes, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            SkillAttributesDef attributes = (it.length > 1 ? it[1] : null) as SkillAttributesDef
            UserPoints userPoints = (it.length > 2 ? it[2] : null) as UserPoints
            SkillApproval skillApproval = (projectId ? (it.length > 4 ? it[4] : null) : (it.length > 3 ? it[3] : null)) as SkillApproval
            return new SkillDefAndUserPoints(
                    skillDef: it[0] as SkillDef, points: userPoints, copiedFromProjectName: it.length > 3 ? (String)it[3] : null, approval: skillApproval, attributes: attributes, approverUserIdForDisplay: it.length > 5 ? it[5] : null
            )
        }

        return res?.findAll {it.skillDef.type != SkillDef.ContainerType.SkillsGroup || it.skillDef.totalPoints > 0 }.sort { it.skillDef.displayOrder }
    }

    @Profile
    private List<UserPointsRepo.SkillRefIdWithPoints> loadChildrenDayPoints(String userId, Integer skillRefId, List<SkillRelDef.RelationshipType> relationshipTypes, Date day) {
        List<UserPointsRepo.SkillRefIdWithPoints> res = userPointsRepo.calculatePointsForChildSkillsForADay(userId, skillRefId, relationshipTypes.collect { it.toString()}, day)
        return res
    }

    @Profile
    private List<Object[]> findChildrenPoints(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, int version) {
        if (projectId) {
            return userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipTypes, version)
        } else {
            return userPointsRepo.findGlobalChildrenAndTheirUserPoints(userId, skillId, relationshipTypes, version)
        }
    }

}
