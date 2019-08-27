package skills.services

import callStack.profiler.Profile
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.EditLevelRequest
import skills.controller.request.model.NextLevelRequest
import skills.controller.result.model.LevelDefinitionRes
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

@Service
@Slf4j
class LevelDefinitionStorageService {

    @Value('#{"${skills.levels.max:25}"}')
    private int maxLevels

    @Autowired
    LevelDefRepo levelDefinitionRepository

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    // this could also come from DB.. eventually
    Map<String,Integer> defaultPercentages = ["White Belt":10, "Blue Belt":25, "Purple Belt":45, "Brown Belt":67, "Black Belt":92]


    @Profile
    LevelInfo getLevelInfo(SkillDefParent skillDefinition, int currentScore) {
        return getLevelInfo(skillDefinition.projectId, skillDefinition.levelDefinitions, skillDefinition.totalPoints, currentScore)
    }

    @Profile
    LevelInfo getOverallLevelInfo(ProjDef projDef, int currentScore) {
        List<LevelDef> levelDefinitions = levelDefinitionRepository.findAllByProjectId(projDef.id)
        LevelInfo levelInfo = getLevelInfo(projDef.projectId, levelDefinitions, projDef.totalPoints, currentScore)
        return levelInfo
    }

    LevelInfo getLevelInfo(String projectId, List<? extends LevelDefInterface> levelDefinitions, int totalPoints, int currentScore) {
        SettingsResult setting = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)

        List<Integer> levelScores
        if(setting?.isEnabled()){
            levelScores = loadPointsLevels(levelDefinitions)
        } else {
            levelScores = loadPercentLevels(levelDefinitions, totalPoints)
        }
        LevelInfo levelInfo = calculateLevel(levelScores, currentScore)
        return levelInfo
    }

    int getPointsRequiredForLevel(SkillDefParent skillDefinition, int level) {
        SettingsResult setting = settingsService.getProjectSetting(skillDefinition.projectId, Settings.LEVEL_AS_POINTS.settingName)

        List<Integer> levelScores = []
        if(setting?.isEnabled()){
            levelScores = loadPointsLevels(skillDefinition.levelDefinitions)
        } else {
            levelScores = loadPercentLevels(skillDefinition.levelDefinitions, skillDefinition.totalPoints)
        }
        return calculatePointsRequiredForLevel(levelScores, level)
    }

    int getPointsRequiredForOverallLevel(ProjDef projDef, int level) {
        SettingsResult setting = settingsService.getProjectSetting(projDef.projectId, Settings.LEVEL_AS_POINTS.settingName)

        List<LevelDef> levelDefinitions = getProjLevelDefs(projDef)
        List<Integer> levelScores = []
        if(setting?.isEnabled()){
            levelScores = loadPointsLevels(levelDefinitions)
        } else {
            levelScores = loadPercentLevels(levelDefinitions, projDef.totalPoints)
        }
        return calculatePointsRequiredForLevel(levelScores, level)
    }

    @Profile
    private List<LevelDef> getProjLevelDefs(ProjDef projDef) {
        levelDefinitionRepository.findAllByProjectId(projDef.id)
    }

    private List<Integer> loadPercentLevels(List<LevelDefInterface> levelDefinitions, int currentScore) {
        List<Integer> levelScores = levelDefinitions.sort({ it.level }).collect {
            return (int) (currentScore * (it.percent / 100d))
        }
        return levelScores
    }

    private List<Integer> loadPointsLevels(List<LevelDefInterface> levelDefinitions){
        List<Integer> levelScores = levelDefinitions.sort({ it.level }).collect {
            return it.pointsFrom
        }
        return levelScores
    }

    private int calculatePointsRequiredForLevel(List<Integer> levelScores, int level) {
        if (level == 0) {
            return 0
        }

        //this can occur when a user has achieved a level
        //but levels have been deleted since their achievement
        if(level > levelScores.size()){
            level = levelScores.size()
        }
        assert level <= levelScores.size()
        return levelScores[level - 1]
    }

    private LevelInfo calculateLevel(List<Integer> levelScores, int currentScore) {
        Integer found

        levelScores.each {
            if (it <= currentScore) {
                found = it
            }
        }

        int index = -1
        if (found != null) {
            index = levelScores.indexOf(found)
        }

        int substract = 0
        if (index >= 0) {
            substract = levelScores.get(index)
        }

        // next level points are the different between my level points and previous level points
        int nextLevelPoints = -1
        if (index + 1 < levelScores.size()) {
            nextLevelPoints = levelScores.get(index + 1)
            if (index >= 0) {
                nextLevelPoints -= levelScores.get(index)
            }
        }

        LevelInfo levelInfo = new LevelInfo(
                level: index + 1,
                currentPoints: currentScore - substract,
                nextLevelPoints: nextLevelPoints,
                totalNumLevels: levelScores.size()
        )
        return levelInfo
    }

    @EqualsAndHashCode
    @ToString
    static class LevelInfo implements Serializable {
        int level
        int totalNumLevels
        int currentPoints
        int nextLevelPoints
    }


    @Transactional
    List<LevelDefinitionRes> getLevels(String projectId, String skillId = null) {
        LevelDefRes levelDefRes = getLevelDefs(projectId, skillId)
        return doGetLevelsDefRes(levelDefRes.levels, levelDefRes.totalPoints, levelDefRes.projectId, levelDefRes.skillId)
    }

    @Transactional
    List<LevelDefinitionRes> getLevelsDefRes(SkillDef skillDef) {
        return doGetLevelsDefRes(skillDef.levelDefinitions, skillDef.totalPoints, skillDef.projectId, skillDef.skillId)
    }

    private List<LevelDefinitionRes> doGetLevelsDefRes(List<LevelDef> levelDefinitions, int totalPoints, String projectId, String skillId = null) {

        SettingsResult setting = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)

        List<Integer> levelScores = []
        if(!setting?.isEnabled()) {
            levelScores = levelDefinitions.sort({ it.level }).collect {
                return totalPoints > 0 ? (int) (totalPoints * (it.percent / 100d)) : null
            }
        }

        List<LevelDefinitionRes> finalRes = []
        levelDefinitions.eachWithIndex { LevelDef entry, int i ->
            Integer fromPts =  entry.pointsFrom
            Integer toPts = entry.pointsTo
            if(!setting?.isEnabled()) {
                fromPts = levelScores.get(i)
                toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            }
            finalRes << new LevelDefinitionRes(projectId: projectId,
                    skillId: skillId,
                    level: entry.level,
                    percent: entry.percent,
                    pointsFrom: fromPts,
                    pointsTo: toPts,
                    name: entry.name,
                    iconClass: entry.iconClass,
                    achievable: fromPts <= totalPoints
                    )
        }
        return finalRes
    }

    private static class LevelDefRes {
        List<LevelDef> levels
        String projectId
        String skillId
        int totalPoints
    }

    private LevelDefRes getLevelDefs(String projectId, String skillId = null) {
        LevelDefRes res

        if (skillId) {
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Subject)
            if(!skillDef){
                throw new skills.controller.exceptions.SkillException("Failed to find Ability for project", projectId, skillId)
            }
            res = new LevelDefRes(levels: skillDef.levelDefinitions?.sort({it.level}), projectId: skillDef.projectId, skillId: skillDef.skillId, totalPoints: skillDef.totalPoints)
        } else {
            ProjDef projDef = projDefRepo.findByProjectId(projectId)
            if(!projDef){
                throw new skills.controller.exceptions.SkillException("Failed to find project", projectId, null)
            }
            res = new LevelDefRes(levels: getProjLevelDefs(projDef)?.sort({it.level}), projectId: projDef.projectId, totalPoints: projDef.totalPoints)
        }
        return res
    }

    /**
     * deletes the highest level and returns its definition
     * @param projectId
     * @return
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    LevelDef deleteLastLevel(String projectId, String skillId = null) {
        LevelDef removed
        LevelDefRes result = getLevelDefs(projectId, skillId)
        List<LevelDef> existingDefinitions = result?.levels

        if (existingDefinitions.size() == 1) {
            throw new skills.controller.exceptions.SkillException("A minimum of one level is required", projectId, skillId)
        }

        if (existingDefinitions) {
            existingDefinitions = existingDefinitions.sort({ it.level })
            removed = existingDefinitions.last()

            int usersAtLevel = achievedLevelRepository.countByProjectIdAndSkillIdAndLevel(projectId, skillId, removed.level)
            if(usersAtLevel > 0){
                throw new skills.controller.exceptions.SkillException("Unable to delete level ${removed.level}, $usersAtLevel ${usersAtLevel > 1 ? 'users have' : 'user has'} achieved this level")
            }

            levelDefinitionRepository.deleteById(removed.id)
            log.debug("Deleted last level [{}]", removed)
            //now we have to null out the toPoints of the 2nd to last level
            if(existingDefinitions.size() > 1){
                LevelDef alter = existingDefinitions.get(existingDefinitions.size()-2)
                alter.pointsTo = null
                levelDefinitionRepository.save(alter)
            }
        }

        return removed
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    LevelDef editLevel(String projectId, EditLevelRequest editLevelRequest, Integer level, String skillId = null) {
        SettingsResult setting = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)
        assert editLevelRequest.name?.length() <= 50
        boolean asPoints = false
        if(setting?.isEnabled()){
            asPoints = true
        }
        LevelValidator.validateEditRequest(editLevelRequest, asPoints)

        LevelDefRes existingDefinitions = getLevelDefs(projectId, skillId)
        existingDefinitions.levels.sort({it.level})

        int toEditIndex = existingDefinitions.levels.findIndexOf { it.level == level }
        if (toEditIndex < 0) {
            throw new SkillException("Failed to find level [${level}]", projectId, skillId)
        }
        LevelDef toEdit = null
        int levelToEditIdx = 0

        //validate that the edit doesn't break the consistency of the other levels.
        for(int i=0; i < existingDefinitions.levels.size(); i++){
            def levelDef = existingDefinitions.levels.get(i)
            if(i < toEditIndex){
                LevelValidator.validateLevelsBefore(levelDef, editLevelRequest, asPoints)
            }else if(i == toEditIndex){
                toEdit = levelDef
                levelToEditIdx = i
            }else if(i > toEditIndex){
                LevelValidator.validateLevelsAfter(levelDef, editLevelRequest, asPoints)
            }
        }

        assert toEdit != null
        if(asPoints){
            toEdit.pointsFrom = editLevelRequest.pointsFrom
            toEdit.pointsTo = editLevelRequest.pointsTo

            //prevent gaps
            List<LevelDef> modified = new LevelUtils().fixGaps(existingDefinitions.levels, toEdit, levelToEditIdx)
            modified.each {
                levelDefinitionRepository.save(it)
            }
        }else{
            toEdit.percent = editLevelRequest.percent
        }

        toEdit.level = editLevelRequest.level
        toEdit.name = editLevelRequest.name
        toEdit.iconClass = editLevelRequest.iconClass
        toEdit = levelDefinitionRepository.save(toEdit)

        return toEdit
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    LevelDef addNextLevel(String projectId, NextLevelRequest nextLevelRequest, String skillId = null) {
        SettingsResult setting = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)
        assert nextLevelRequest.name?.length() <= 50
        boolean asPoints = false
        if(setting?.isEnabled()){
            asPoints = true
            assert nextLevelRequest?.points > 0
        }

        LevelDef created
        LevelDefRes existingDefinitions = getLevelDefs(projectId, skillId)

        if(existingDefinitions.levels.size() == maxLevels){
            throw new SkillException("No more then $maxLevels levels are allowed", projectId, skillId)
        }

        if(asPoints) {
            assert existingDefinitions.levels.collect({ it.pointsTo }).max() < nextLevelRequest.points
        }else{
            assert existingDefinitions.levels.collect({ it.percent }).max() < nextLevelRequest.percent
        }

        if (existingDefinitions) {
            LevelDef lastOne = existingDefinitions.levels.sort({ it.level }).last()
            SkillDef skill = null
            ProjDef project = null
            if(skillId) {
                skill = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Subject)
                assert skill
            }else{
                project = projDefRepo.findByProjectId(projectId)
                assert project
            }

            if(asPoints){
                //if the system is configured to use points instead of percentages for levels
                //then we need to explicitly update the toRange of the previous highest level
                lastOne.pointsTo = nextLevelRequest.points
                levelDefinitionRepository.save(lastOne)
            }

            created = new LevelDef(level: lastOne.level + 1,
                    projectId: project?.id,
                    percent: nextLevelRequest.percent,
                    skillRefId: skill?.id,
                    name: nextLevelRequest.name,
                    pointsFrom: nextLevelRequest.points,
                    iconClass: nextLevelRequest.iconClass
            )

            created = levelDefinitionRepository.save(created)
            log.debug("Added new level [{}]", created)
        }

        return created
    }

    /**
     * Levels belong to either project or skill; so only project OR skill must be provided
     */
    List<LevelDef> createDefault(String projectId, ProjDef projDef, SkillDefParent skillDef = null) {
        SettingsResult setting = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)

        assert projDef || skillDef

        List<LevelDef> res = []
        int i=0
        defaultPercentages.each { String name, Integer percentage ->
            def levelDef = new LevelDef(
                    projectId: skillDef ? null : projDef?.id,
                    skillRefId: skillDef?.id,
                    level: ++i,
                    percent: percentage,
                    name: name,
                    iconClass: "fas fa-user-ninja")
            log.debug("creating default level {}", levelDef)
            res << levelDef
        }
        if(setting?.isEnabled()){
            new LevelUtils().convertToPoints(res, LevelUtils.defaultTotalPointsGuess)
        }

        levelDefinitionRepository.saveAll(res)
    }

    int maxProjectLevel(ProjDef projDef) {
        return getProjLevelDefs(projDef).size()
    }
}
