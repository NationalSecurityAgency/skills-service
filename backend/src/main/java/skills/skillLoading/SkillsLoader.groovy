package skills.skillLoading


import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.SerializationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.controller.result.model.SettingsResult
import skills.services.DependencyValidator
import skills.services.GlobalBadgesService
import skills.services.LevelDefinitionStorageService
import skills.services.settings.SettingsService
import skills.skillLoading.model.*
import skills.storage.model.*
import skills.storage.repos.*
import skills.storage.repos.nativeSql.GraphRelWithAchievement
import skills.storage.repos.nativeSql.NativeQueriesRepo

@Component
@CompileStatic
@Slf4j
class SkillsLoader {

    @Value('#{"${skills.subjects.minimumPoints:20}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.project.minimumPoints:20}"}')
    int minimumProjectPoints

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    PointsHistoryBuilder pointsHistoryBuilder

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    SubjectDataLoader subjectDataLoader

    @Autowired
    DependencySummaryLoader dependencySummaryLoader

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    DependencyValidator dependencyValidator

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    GlobalBadgesService globalBadgesService

    private static String PROP_HELP_URL_ROOT = "help.url.root"

    @Transactional(readOnly = true)
    Integer getUserLevel(String projectId, String userId) {
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if (!projDef) {
            // indicates that project doesn't exist at all
            return -1;
        }

        Integer res = 0
        List<UserAchievement> levels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
        if (levels) {
            res = levels.collect({ it.level }).max()
        }
        return res
    }

    @Profile
    @Transactional(readOnly = true)
    OverallSkillSummary loadOverallSummary(String projectId, String userId, Integer version = -1) {
        ProjDef projDef = getProjDef(projectId)

        List<SkillSubjectSummary> subjects = loadSubjectsSummaries(projDef, userId, version)

        int points
        int totalPoints
        int skillLevel
        int levelPoints
        int levelTotalPoints
        LevelDefinitionStorageService.LevelInfo levelInfo
        int todaysPoints = 0

        if (subjects) {
            points = (int) subjects.collect({ it.points }).sum()
            totalPoints = (int) subjects.collect({ it.totalPoints }).sum()
            levelInfo = levelDefService.getOverallLevelInfo(projDef, points)
            todaysPoints = (Integer) subjects?.collect({ it.todaysPoints })?.sum()

            List<UserAchievement> achievedLevels = getProjectsAchievedLevels(userId, projectId)
            if (achievedLevels) {
                achievedLevels = achievedLevels.sort({ it.level })
                levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels.last(), levelInfo, null)
            }

            skillLevel = levelInfo?.level
            levelPoints = levelInfo?.currentPoints
            levelTotalPoints = levelInfo?.nextLevelPoints
        }

        if(totalPoints < minimumProjectPoints){
            skillLevel = 0
        }

        int numBadgesAchieved = achievedLevelRepository.countAchievedForUser(userId, projectId, SkillDef.ContainerType.Badge)
        long numTotalBadges = skillDefRepo.countByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)

        // add in global badge counts
        numBadgesAchieved += achievedLevelRepository.countAchievedGlobalForUser(userId, SkillDef.ContainerType.GlobalBadge)
        numTotalBadges += skillDefRepo.countByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)


        OverallSkillSummary res = new OverallSkillSummary(
                projectName: projDef.name,
                skillsLevel: skillLevel,
                totalLevels: levelInfo?.totalNumLevels ?: 0,
                points: points,
                totalPoints: totalPoints,
                todaysPoints: todaysPoints,
                levelPoints: levelPoints,
                levelTotalPoints: levelTotalPoints,
                subjects: subjects,
                badges: new OverallSkillSummary.BadgeStats(numBadgesCompleted: numBadgesAchieved, enabled: numTotalBadges > 0)
        )

        return res
    }

    @Profile
    private List<SkillSubjectSummary> loadSubjectsSummaries(ProjDef projDef, String userId, int version) {
        List<SkillDef> subjectsToConvert = loadSubjectsFromDB(projDef)?.sort({ it.displayOrder })
        List<SkillSubjectSummary> subjects = subjectsToConvert?.collect { SkillDef subjectDefinition ->
            return loadSubjectSummary(projDef, userId, subjectDefinition, version)
        }
        subjects
    }

    @Profile
    private List<UserAchievement> getProjectsAchievedLevels(String userId, String projectId) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
    }

    @Profile
    private List<SkillDef> loadSubjectsFromDB(ProjDef projDef) {
        return skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Subject)
    }

    @Transactional(readOnly = true)
    List<SkillBadgeSummary> loadBadgeSummaries(String projectId, String userId, Integer version = Integer.MAX_VALUE){
        ProjDef projDef = getProjDef(projectId)
        List<SkillDefWithExtra> badgeDefs = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        if ( version >= 0 ) {
            badgeDefs = badgeDefs.findAll { it.version <= version }
        }
        List<SkillBadgeSummary> badges = badgeDefs.sort({ it.displayOrder }).collect { SkillDefWithExtra badgeDefinition ->
            loadBadgeSummary(projDef, userId, badgeDefinition, version)
        }
        return badges
    }

    @Transactional(readOnly = true)
    List<SkillGlobalBadgeSummary> loadGlobalBadgeSummaries(String userId, String projectId, Integer version = Integer.MAX_VALUE){
        List<SkillDefWithExtra> badgeDefs = skillDefWithExtraRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        if ( version >= 0 ) {
            badgeDefs = badgeDefs.findAll { it.version <= version }
        }
        List<SkillGlobalBadgeSummary> globalBadges = badgeDefs.sort({ it.displayOrder }).collect { SkillDefWithExtra badgeDefinition ->
            loadGlobalBadgeSummary(userId, badgeDefinition, version, true)
        }
        globalBadges = globalBadges.findAll { it.projectLevelsAndSkillsSummaries.find { it.projectId == projectId } }
        return globalBadges
    }

    @Transactional(readOnly = true)
    UserPointHistorySummary loadPointHistorySummary(String projectId, String userId, int showHistoryForNumDays, String skillId = null, Integer version = Integer.MAX_VALUE) {
        List<SkillHistoryPoints> historyPoints = pointsHistoryBuilder.buildHistory(projectId, userId, showHistoryForNumDays, skillId, version)
        return new UserPointHistorySummary(
                pointsHistory: historyPoints
        )
    }

    @Transactional(readOnly = true)
    SkillSummary loadSkillSummary(String projectId, String userId, String crossProjectId, String skillId) {
        ProjDef projDef = getProjDef(crossProjectId ?: projectId)
        SkillDefWithExtra skillDef = getSkillDefWithExtra(crossProjectId ?: projectId, skillId, SkillDef.ContainerType.Skill)

        if(crossProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, skillDef)
        }

        UserPoints points = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(crossProjectId ?: projectId, userId, skillId, null)
        UserPoints todayPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(crossProjectId ?: projectId, userId, skillId, new Date().clearTime())

        SkillDependencySummary skillDependencySummary
        if (!crossProjectId) {
            skillDependencySummary = dependencySummaryLoader.loadDependencySummary(userId, projectId, skillId)
        }

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projectId, PROP_HELP_URL_ROOT)

        return new SkillSummary(
                projectId: skillDef.projectId, projectName: projDef.name,
                skillId: skillDef.skillId, skill: skillDef.name,
                points: points?.points ?: 0, todaysPoints: todayPoints?.points ?: 0,
                pointIncrement: skillDef.pointIncrement, pointIncrementInterval: skillDef.pointIncrementInterval,
                maxOccurrencesWithinIncrementInterval: skillDef.numMaxOccurrencesIncrementInterval,
                totalPoints: skillDef.totalPoints,
                description: new SkillDescription(
                        skillId: skillDef.skillId,
                        description: skillDef.description,
                        href: getHelpUrl(helpUrlRootSetting, skillDef.helpUrl)),
                dependencyInfo: skillDependencySummary,
                crossProject: crossProjectId != null
        )
    }

    @Transactional(readOnly = true)
    SkillSubjectSummary loadSubject(String projectId, String userId, String subjectId, Integer version = -1) {
        ProjDef projDef = getProjDef(projectId)
        SkillDefWithExtra subjectDef = getSkillDefWithExtra(projectId, subjectId, SkillDef.ContainerType.Subject)

        if (version == -1 || subjectDef.version <= version) {
            return loadSubjectSummary(projDef, userId, subjectDef, version, true)
        } else {
            return null
        }
    }

    @Transactional(readOnly = true)
    List<SkillDescription> loadSubjectDescriptions(String projectId, String subjectId, Integer version = -1) {
        return loadDescriptions(projectId, subjectId, SkillRelDef.RelationshipType.RuleSetDefinition, version)
    }
    @Transactional(readOnly = true)
    List<SkillDescription> loadBadgeDescriptions(String projectId, String badgeId, Integer version = -1) {
        return loadDescriptions(projectId, badgeId, SkillRelDef.RelationshipType.BadgeRequirement, version)
    }

    private List<SkillDescription> loadDescriptions(String projectId, String subjectId, SkillRelDef.RelationshipType relationshipType, int version) {
        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projectId, PROP_HELP_URL_ROOT)

        List<SkillDefWithExtraRepo.SkillDescDBRes> dbRes = skillDefWithExtraRepo.findAllChildSkillsDescriptions(projectId, subjectId, relationshipType, version)
        List<SkillDescription> res = dbRes.collect {
            new SkillDescription(
                    skillId: it.getSkillId(),
                    description: it.getDescription(),
                    href: getHelpUrl(helpUrlRootSetting, it.getHelpUrl())
            )
        }
        return res;
    }

    @Transactional(readOnly = true)
    SkillBadgeSummary loadBadge(String projectId, String userId, String subjectId, Integer version = Integer.MAX_VALUE) {
        ProjDef projDef = getProjDef(projectId)
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(projectId, subjectId, SkillDef.ContainerType.Badge)

        return loadBadgeSummary(projDef, userId, badgeDef, version,true)
    }


    @Transactional(readOnly = true)
    SkillGlobalBadgeSummary loadGlobalBadge(String userId, String badgeSkillId, Integer version = Integer.MAX_VALUE) {
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(null, badgeSkillId, SkillDef.ContainerType.GlobalBadge)

        return loadGlobalBadgeSummary(userId, badgeDef, version,true)
    }

    @Transactional(readOnly = true)
    SkillDependencyInfo loadSkillDependencyInfo(String projectId, String userId, String skillId) {
        List<GraphRelWithAchievement> graphDBRes = nativeQueriesRepo.getDependencyGraphWithAchievedIndicator(projectId, skillId, userId)

        List<SkillDependencyInfo.SkillRelationshipItem> deps = graphDBRes.collect {
            new SkillDependencyInfo.SkillRelationship(
                    skill: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.parentProjectId, projectName: it.parentProjectName, skillId: it.parentSkillId, skillName: it.parentName),
                    dependsOn: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.childProjectId, projectName: it.childProjectName, skillId: it.childSkillId, skillName: it.childName),
                    achieved: it.achievementId != null,
                    crossProject: projectId != it.childProjectId
            )
        }?.sort({ a,b ->
            a.skill.skillId <=> b.skill.skillId ?: a.dependsOn.skillId <=> b.dependsOn.skillId
        }) as List<SkillDependencyInfo.SkillRelationshipItem>
        return new SkillDependencyInfo(dependencies: deps)
    }

    @Profile
    private SkillSubjectSummary loadSubjectSummary(ProjDef projDef, String userId, SkillDefParent subjectDefinition, Integer version, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        // must compute total points so the provided version is taken into advantage
        // subjectDefinition.totalPoints is total overall regardless of the version
        int totalPoints
        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef.projectId, subjectDefinition.skillId, version)
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints)
            totalPoints = skillsRes ? skillsRes.collect({it.totalPoints}).sum() as Integer: 0
        } else {
            totalPoints = calculateTotalForSkillDef(projDef, subjectDefinition, version)
        }

        Integer points = calculatePoints(projDef, userId, subjectDefinition, version)
        Integer todaysPoints = calculateTodayPoints(projDef, userId, subjectDefinition, version)

        // convert null result to 0
        points = points ?: 0
        todaysPoints = todaysPoints ?: 0

        LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(subjectDefinition, points)

        List<UserAchievement> achievedLevels = locateAchievedLevels(userId, projDef, subjectDefinition)
        if (achievedLevels) {
            achievedLevels = achievedLevels.sort({ it.created })
            levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels?.last(), levelInfo, subjectDefinition)
        }

        if(subjectDefinition.totalPoints < minimumSubjectPoints){
            levelInfo.level = 0
        }

        String helpUrl = null
        if(subjectDefinition instanceof SkillDefWithExtra) {
            SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projDef.projectId, PROP_HELP_URL_ROOT)
            helpUrl = getHelpUrl(helpUrlRootSetting, subjectDefinition.helpUrl)
        }

        return new SkillSubjectSummary(
                subject: subjectDefinition.name,
                subjectId: subjectDefinition.skillId,
                description: subjectDefinition instanceof SkillDefWithExtra ? subjectDefinition.description : null,
                points: points,

                skillsLevel: levelInfo.level,
                totalLevels: levelInfo.totalNumLevels,

                levelPoints: levelInfo.currentPoints,
                levelTotalPoints: levelInfo.nextLevelPoints,

                totalPoints: totalPoints,
                todaysPoints: todaysPoints,

                skills: skillsRes,

                iconClass: subjectDefinition.iconClass,

                helpUrl: helpUrl
        )
    }

    @Profile
    private int calculateTotalForSkillDef(ProjDef projDef, SkillDefParent subjectDefinition, int version) {
        Integer res = skillDefRepo.calculateTotalPointsForSkill(projDef.projectId, subjectDefinition.skillId, SkillRelDef.RelationshipType.RuleSetDefinition, version)
        return res ?: 0
    }

    @Profile
    private List<UserAchievement> locateAchievedLevels(String userId, ProjDef projDef, SkillDefParent subjectDefinition) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef.projectId, subjectDefinition.skillId)
    }

    @Profile
    private Integer calculateTodayPoints(ProjDef projDef, String userId, SkillDefParent subjectDefinition, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserIdAndSkillRefIdAndDay(projDef.projectId, userId, subjectDefinition.id, new Date().clearTime())
        return res ?: 0
    }

    @Profile
    private Integer calculatePoints(ProjDef projDef, String userId, SkillDefParent subjectDefinition, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserIdAndSkillRefId(projDef.projectId, userId, subjectDefinition.id)
        return res ?: 0
    }

    @Profile
    private SkillBadgeSummary loadBadgeSummary(ProjDef projDef, String userId, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef?.projectId, badgeDefinition.skillId, version, SkillRelDef.RelationshipType.BadgeRequirement)
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints)
        }

        List<UserAchievement> achievements = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef?.projectId, badgeDefinition.skillId)
        if (achievements) {
            // for badges, there should only be one UserAchievement
            assert achievements.size() == 1
        }

        int numAchievedSkills = achievedLevelRepository.countAchievedChildren(userId, projDef?.projectId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
        int numChildSkills = skillDefRepo.countChildren(projDef?.projectId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projDef.projectId, PROP_HELP_URL_ROOT)
        String helpUrl = getHelpUrl(helpUrlRootSetting, badgeDefinition.helpUrl)

        return new SkillBadgeSummary(
                badge: badgeDefinition.name,
                badgeId: badgeDefinition.skillId,
                description: badgeDefinition.description,
                badgeAchieved: achievements?.size() > 0,
                dateAchieved: achievements ? achievements.first().created : null,
                numSkillsAchieved: numAchievedSkills,
                numTotalSkills: numChildSkills,
                startDate: badgeDefinition.startDate,
                endDate: badgeDefinition.endDate,
                skills: skillsRes,
                iconClass: badgeDefinition.iconClass,
                helpUrl: helpUrl
        )
    }

    @Profile
    private SkillGlobalBadgeSummary loadGlobalBadgeSummary(String userId, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, null, badgeDefinition.skillId, version, SkillRelDef.RelationshipType.BadgeRequirement)
            skillsRes = createSkillSummaries(null, groupChildrenMeta.childrenWithPoints)
        }

        List<UserAchievement> achievements = achievedLevelRepository.findAllByUserIdAndSkillId(userId, badgeDefinition.skillId)
        if (achievements) {
            // for badges, there should only be one UserAchievement
            assert achievements.size() == 1
        }

        int numAchievedSkills = achievedLevelRepository.countAchievedGlobalSkills(userId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
        int numChildSkills = skillDefRepo.countGlobalChildren(badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)

        List<SkillEventsSupportRepo.TinyProjectDef> projectsForUser = skillEventsSupportRepo.getTinyProjectDefForUserId(userId)
        List<GlobalBadgeLevelRes> requiredLevels = globalBadgesService.getGlobalBadgeLevels(badgeDefinition.skillId)
        List<ProjectLevelSummary> projectLevels = []
        for (GlobalBadgeLevelRes requiredLevel : requiredLevels) {
            ProjectLevelSummary projectLevelSummary = new ProjectLevelSummary(projectId: requiredLevel.projectId, projectName: requiredLevel.projectName, requiredLevel: requiredLevel.level)
            projectLevels.add(projectLevelSummary)
            if (projectsForUser.find { it.projectId == requiredLevel.projectId }) {
                Integer achievedProjectLevel = skillsLoader.getUserLevel(requiredLevel.projectId, userId)
                projectLevelSummary.achievedLevel = achievedProjectLevel
                if (achievedProjectLevel >= requiredLevel.level) {
                    numAchievedSkills++
                }
            }
        }
        numChildSkills += requiredLevels.size()

        Map<String, ProjectLevelsAndSkillsSummary> byProject = [:]
        skillsRes.groupBy { it.projectId }.each { projectId, skills ->
            byProject[projectId] = new ProjectLevelsAndSkillsSummary(projectId: projectId, projectName: skills.first().projectName, skills: skills)
        }
        projectLevels.groupBy { it.projectId }.each { projectId, levels ->
            assert levels.size() == 1
            ProjectLevelSummary projectLevel = levels.first()
            ProjectLevelsAndSkillsSummary projectLevelsAndSkillsSummary = byProject.get(projectId)
            if (projectLevelsAndSkillsSummary) {
                projectLevelsAndSkillsSummary.projectLevel = projectLevel
            } else {
                byProject[projectId] = new ProjectLevelsAndSkillsSummary(projectId: projectId, projectName: projectLevel.projectName, projectLevel: projectLevel)
            }
        }

        return new SkillGlobalBadgeSummary(
                badge: badgeDefinition.name,
                badgeId: badgeDefinition.skillId,
                description: badgeDefinition.description,
                badgeAchieved: achievements?.size() > 0,
                dateAchieved: achievements ? achievements.first().created : null,
                numSkillsAchieved: numAchievedSkills,
                numTotalSkills: numChildSkills,
                startDate: badgeDefinition.startDate,
                endDate: badgeDefinition.endDate,
                skills: skillsRes,
                iconClass: badgeDefinition.iconClass,
                projectLevelsAndSkillsSummaries: byProject.values(),
                helpUrl: badgeDefinition.helpUrl
        )
    }

    private String getHelpUrl(SettingsResult helpUrlRootSetting, String helpUrl) {
        String res = helpUrl

        if (helpUrl && helpUrlRootSetting && !helpUrl.toLowerCase().startsWith("http")) {
            String rootUrl = helpUrlRootSetting.value
            if (rootUrl.endsWith("/") && res.startsWith("/")) {
                rootUrl = rootUrl.substring(0, rootUrl.length() - 1)
            }
            res = rootUrl + res
        }

        return res
    }

    @Profile
    private List<SkillSummary> createSkillSummaries(ProjDef thisProjDef, List<SubjectDataLoader.SkillsAndPoints> childrenWithPoints) {
        List<SkillSummary> skillsRes = []

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(thisProjDef?.projectId, PROP_HELP_URL_ROOT)

        Map<String,ProjDef> projDefMap = [:]
        childrenWithPoints.each { SubjectDataLoader.SkillsAndPoints skillDefAndUserPoints ->
            SkillDef skillDef = skillDefAndUserPoints.skillDef
            int points = skillDefAndUserPoints.points
            int todayPoints = skillDefAndUserPoints.todaysPoints

            ProjDef projDef = projDefMap[skillDef.projectId]
            if(!projDef){
                projDef = projDefRepo.findByProjectId(skillDef.projectId)
                projDefMap[skillDef.projectId] = projDef
            }

            skillsRes << new SkillSummary(
                    projectId: skillDef.projectId, projectName: projDef.name,
                    skillId: skillDef.skillId, skill: skillDef.name,
                    points: points, todaysPoints: todayPoints,
                    pointIncrement: skillDef.pointIncrement, pointIncrementInterval: skillDef.pointIncrementInterval,
                    maxOccurrencesWithinIncrementInterval: skillDef.numMaxOccurrencesIncrementInterval,
                    totalPoints: skillDef.totalPoints,
                    dependencyInfo: skillDefAndUserPoints.dependencyInfo
            )
        }

        return skillsRes
    }

    /**
     * see if the user already achieved the next level, this can happen if user achieved the next level and then we added new skill with more points
     * which re-balanced how many points required for that level;
     * in that case we'll assign already achieved level and add the points from the previous levels + new level (total number till next level achievement)
     */
    @Profile
    private LevelDefinitionStorageService.LevelInfo updateLevelBasedOnLastAchieved(ProjDef projDef, int points, UserAchievement lastAchievedLevel, LevelDefinitionStorageService.LevelInfo calculatedLevelInfo, SkillDefParent subjectDef) {
        LevelDefinitionStorageService.LevelInfo res = SerializationUtils.clone(calculatedLevelInfo)

        int maxLevel = Integer.MAX_VALUE
        if(subjectDef){
            maxLevel = subjectDef.levelDefinitions.size()
        }else{
            maxLevel = levelDefService.maxProjectLevel(projDef)
        }

        if (lastAchievedLevel && lastAchievedLevel?.level > calculatedLevelInfo.level) {
            if (lastAchievedLevel.level >= maxLevel) {
                res.currentPoints = 0
                res.nextLevelPoints = -1
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
            throw new skills.controller.exceptions.SkillException("Failed to find project [$projectId]", projectId)
        }
        return projDef
    }

    private SkillDef getSkillDef(String projectId, String skillId, SkillDef.ContainerType containerType) {
        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new skills.controller.exceptions.SkillException("Skill with id [${skillId}] doesn't exist", projectId, skillId)
        }
        return skillDef
    }

    private SkillDefWithExtra getSkillDefWithExtra(String projectId, String skillId, SkillDef.ContainerType containerType) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new skills.controller.exceptions.SkillException("Skill with id [${skillId}] doesn't exist", projectId, skillId)
        }
        return skillDef
    }
}
