package skills.service.skillLoading

import org.apache.commons.lang3.SerializationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.service.controller.exceptions.SkillException
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.skillLoading.model.*
import skills.storage.model.*
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

@Component
class SkillsLoader {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    @Autowired
    PointsHistoryBuilder pointsHistoryBuilder

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    SubjectDataLoader subjectDataLoader


    @Transactional(readOnly = true)
    Integer getUserLevel(String projectId, String userId) {
        Integer res = 0
        List<UserAchievement> levels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
        if (levels) {
            res = levels.collect({ it.level }).max()
        }
        return res
    }

    @Transactional(readOnly = true)
    OverallSkillSummary loadOverallSummary(String projectId, String userId) {
        ProjDef projDef = getProjDef(projectId)
        List<SkillSubjectSummary> subjects = projDef.getSubjects().collect { SkillDef subjectDefinition ->
            loadSubjectSummary(projDef, userId, subjectDefinition)
        }
        List<SkillBadgeSummary> badges = projDef.getBadges().collect { SkillDef badgeDefinition ->
            loadBadgeSummary(projDef, userId, badgeDefinition)
        }

        int points
        int totalPoints
        int skillLevel
        int levelPoints
        int levelTotalPoints
        LevelDefinitionStorageService.LevelInfo levelInfo
        List<SkillsLevelDefinition> levelDefinitions
        int todaysPoints = 0

        if (subjects) {
            points = (int) subjects.collect({ it.points }).sum()
            totalPoints = (int) subjects.collect({ it.totalPoints }).sum()
            levelInfo = levelDefService.getOverallLevelInfo(projDef, points)
            todaysPoints = (Integer) subjects?.collect({ it.todaysPoints })?.sum()

            List<UserAchievement> achievedLevels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
            if (achievedLevels) {
                achievedLevels = achievedLevels.sort({ it.created })
                levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels.last(), levelInfo, null)
            }

            skillLevel = levelInfo?.level
            levelPoints = levelInfo?.currentPoints
            levelTotalPoints = levelInfo?.nextLevelPoints
        }
        OverallSkillSummary res = new OverallSkillSummary(
                projectName: projDef.name,
                skillsLevel: skillLevel,
                points: points,
                totalPoints: totalPoints,
                todaysPoints: todaysPoints,
                levelPoints: levelPoints,
                levelTotalPoints: levelTotalPoints,
                subjects: subjects,
                badges: badges,
        )

        return res
    }


    @Transactional(readOnly = true)
    UserPointHistorySummary loadPointHistorySummary(String projectId, String userId, int showHistoryForNumDays, String skillId = null) {
        List<SkillHistoryPoints> historyPoints = pointsHistoryBuilder.buildHistory(projectId, userId, showHistoryForNumDays, skillId)
        return new UserPointHistorySummary(
                pointsHistory: historyPoints
        )
    }

    @Transactional(readOnly = true)
    SkillSubjectSummary loadSubject(String projectId, String userId, String subjectId) {
        ProjDef projDef = getProjDef(projectId)
        SkillDef subjectDef = getSkillDef(projectId, subjectId, SkillDef.ContainerType.Subject)

        return loadSubjectSummary(projDef, userId, subjectDef, true)
    }

    @Transactional(readOnly = true)
    SkillBadgeSummary loadBadge(String projectId, String userId, String subjectId) {
        ProjDef projDef = getProjDef(projectId)
        SkillDef badgeDef = getSkillDef(projectId, subjectId, SkillDef.ContainerType.Badge)

        return loadBadgeSummary(projDef, userId, badgeDef, true)
    }

    private SkillSubjectSummary loadSubjectSummary(ProjDef projDef, String userId, SkillDef subjectDefinition, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef.projectId, subjectDefinition.skillId)
            skillsRes = createSkillSummaries(groupChildrenMeta.childrenWithPoints)
        }

        UserPoints overallPts = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(projDef.projectId, userId, subjectDefinition.skillId, null)
        int points = overallPts ? overallPts.points : 0

        UserPoints todayPts = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(projDef.projectId, userId, subjectDefinition.skillId, new Date().clearTime())
        int todaysPoints = todayPts ? todayPts.points : 0

        LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(subjectDefinition, points)

        List<UserAchievement> achievedLevels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef.projectId, subjectDefinition.skillId)
        if (achievedLevels) {
            achievedLevels = achievedLevels.sort({ it.created })
            levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels?.last(), levelInfo, subjectDefinition)
        }

        return new SkillSubjectSummary(
                subject: subjectDefinition.name,
                subjectId: subjectDefinition.skillId,
                description: subjectDefinition.description,
                points: points,

                skillsLevel: levelInfo.level,
                levelPoints: levelInfo.currentPoints,
                levelTotalPoints: levelInfo.nextLevelPoints,

                totalPoints: subjectDefinition.totalPoints,
                todaysPoints: todaysPoints,

                skills: skillsRes,

                iconClass: subjectDefinition.iconClass
        )
    }
    private SkillBadgeSummary loadBadgeSummary(ProjDef projDef, String userId, SkillDef badgeDefinition, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef.projectId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeDependence)
            skillsRes = createSkillSummaries(groupChildrenMeta.childrenWithPoints)
        }

        List<UserAchievement> achievements = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef.projectId, badgeDefinition.skillId)
        if (achievements) {
            // for badges, there should only be one UserAchievment
            assert achievements.size() == 1
        }

        return new SkillBadgeSummary(
                badge: badgeDefinition.name,
                badgeId: badgeDefinition.skillId,
                description: badgeDefinition.description,
                badgeAchieved: achievements,
                startDate: badgeDefinition.startDate,
                endDate: badgeDefinition.endDate,
                skills: skillsRes,
                iconClass: badgeDefinition.iconClass
        )
    }

    private List<SkillSummary> createSkillSummaries(List<SubjectDataLoader.SkillsAndPoints> childrenWithPoints) {
        List<SkillSummary> skillsRes = []
        childrenWithPoints.each { SubjectDataLoader.SkillsAndPoints skillDefAndUserPoints ->
            SkillDef skillDef = skillDefAndUserPoints.skillDef
            int points = skillDefAndUserPoints.points
            int todayPoints = skillDefAndUserPoints.todaysPoints

            skillsRes << new SkillSummary(
                    skillId: skillDef.skillId,
                    skill: skillDef.name, points: points, todaysPoints: todayPoints,
                    pointIncrement: skillDef.pointIncrement, totalPoints: skillDef.totalPoints,
                    description: new SkillDescription(description: skillDef.description, href: skillDef.helpUrl),
                    mustAchieveTheseFirst: skillDefAndUserPoints.mustAchieveTheseFirst
            )
        }

        return skillsRes
    }

    /**
     * see if the user already achieved the next level, this can happen if user achieved the next level and then we added new skill with more points
     * which re-balanced how many points required for that level;
     * in that case we'll assign already achieved level and add the points from the previous levels + new level (total number till next level achievement)
     */
    private LevelDefinitionStorageService.LevelInfo updateLevelBasedOnLastAchieved(ProjDef projDef, int points, UserAchievement lastAchievedLevel, LevelDefinitionStorageService.LevelInfo calculatedLevelInfo, SkillDef subjectDef) {
        LevelDefinitionStorageService.LevelInfo res = SerializationUtils.clone(calculatedLevelInfo)

        if (lastAchievedLevel && lastAchievedLevel?.level > calculatedLevelInfo.level) {
            if (lastAchievedLevel.level >= 5) {
                res.currentPoints = 0
                res.nextLevelPoints = -1
                res.level = 5
            } else {
                int nextLevelPointsToAchievel

                if (subjectDef) {
                    nextLevelPointsToAchievel = levelDefService.getPointsRequiredForLevel(subjectDef, lastAchievedLevel.level + 1)
                } else {
                    nextLevelPointsToAchievel = levelDefService.getPointsRequiredForOverallLevel(projDef, lastAchievedLevel.level + 1)
                }

                int numPtsLeftInPreviousLevel = nextLevelPointsToAchievel - points

                res.level = lastAchievedLevel?.level
                res.currentPoints = 0
                res.nextLevelPoints = numPtsLeftInPreviousLevel
            }
        }

        return res
    }

    private ProjDef getProjDef(String projectId) {
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if(!projDef){
            throw new SkillException("Failed to find project [$projectId]", projectId)
        }
        return projDef
    }

    private SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillException("Skill with id [${skillId}] doesn't exist", projectId, skillId)
        }
        return skillDef
    }
}
