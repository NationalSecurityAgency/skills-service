package skills.service.datastore.services

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.NextLevelRequest
import skills.service.controller.result.model.LevelDefinitionRes
import skills.service.controller.result.model.SettingsResult
import skills.service.datastore.services.settings.Settings
import skills.service.datastore.services.settings.SettingsService
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.model.LevelDef
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef

@Service
@Slf4j
class LevelDefinitionStorageService {

    @Autowired
    LevelDefRepo levelDefinitionRepository

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingsService settingsService

    // this could also come from DB.. eventually
    List<Integer> defaultPercentages = [10, 25, 45, 67, 92]


    LevelInfo getLevelInfo(SkillDef skillDefinition, int currentScore) {
        List<Integer> levelScores = loadPercentLevels(skillDefinition.levelDefinitions, skillDefinition.totalPoints)
        return calculateLevel(levelScores, currentScore)
    }

    LevelInfo getOverallLevelInfo(ProjDef projDef, int currentScore) {
        List<Integer> levelScores = loadPercentLevels(projDef.levelDefinitions, projDef.totalPoints)
        return calculateLevel(levelScores, currentScore)
    }

    int getPointsRequiredForLevel(SkillDef skillDefinition, int level) {
        List<Integer> levelScores = loadPercentLevels(skillDefinition.levelDefinitions, skillDefinition.totalPoints)
        return calculatePointsRequiredForLevel(levelScores, level)
    }

    int getPointsRequiredForOverallLevel(ProjDef projDef, int level) {
        List<Integer> levelScores = loadPercentLevels(projDef.levelDefinitions, projDef.totalPoints)
        return calculatePointsRequiredForLevel(levelScores, level)
    }

    private List<Integer> loadPercentLevels(List<LevelDef> levelDefinitions, int currentScore) {
        List<Integer> levelScores = levelDefinitions.sort({ it.level }).collect {
            return (int) (currentScore * (it.percent / 100d))
        }
        return levelScores
    }

    private int calculatePointsRequiredForLevel(List<Integer> levelScores, int level) {
        if (level == 0) {
            return 0
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

        return new LevelInfo(
                level: index + 1,
                currentPoints: currentScore - substract,
                nextLevelPoints: nextLevelPoints
        )
    }

    @EqualsAndHashCode
    @ToString
    static class LevelInfo implements Serializable {
        int level
        int currentPoints
        int nextLevelPoints
    }


    @Transactional
    List<LevelDefinitionRes> getLevels(String projectId, String skillId = null) {
        LevelDefRes levelDefRes = getLevelDefs(projectId, skillId)
        return doGetLevelsDefRes(levelDefRes.levels, levelDefRes.totalPoints, levelDefRes.projectId, levelDefRes.skillId)
    }

    @Transactional
    List<LevelDefinitionRes> getLevelsDefRes(ProjDef projDef) {
        return doGetLevelsDefRes(projDef.levelDefinitions, projDef.totalPoints, projDef.projectId)
    }

    @Transactional
    List<LevelDefinitionRes> getLevelsDefRes(SkillDef skillDef) {
        return doGetLevelsDefRes(skillDef.levelDefinitions, skillDef.totalPoints, skillDef.projectId, skillDef.skillId)
    }

    private List<LevelDefinitionRes> doGetLevelsDefRes(List<LevelDef> levelDefinitions, int totalPoints, String projectId, String skillId = null) {

        SettingsResult setting = settingsService.getSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)

        List<Integer> levelScores = []
        if(!setting?.isEnabled()) {
            levelScores = levelDefinitions.sort({ it.level }).collect {
                return (int) (totalPoints * (it.percent / 100d))
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
                    iconClass: entry.iconClass
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
                throw new SkillException("Failed to find Ability for project", projectId, skillId)
            }
            res = new LevelDefRes(levels: skillDef.levelDefinitions, projectId: skillDef.projectId, skillId: skillDef.skillId, totalPoints: skillDef.totalPoints)
        } else {
            ProjDef projDef = projDefRepo.findByProjectId(projectId)
            if(!projDef){
                throw new SkillException("Failed to find project", projectId, null)
            }
            res = new LevelDefRes(levels: projDef.levelDefinitions, projectId: projDef.projectId, totalPoints: projDef.totalPoints)
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
        if (existingDefinitions) {
            existingDefinitions = existingDefinitions.sort({ it.level })
            removed = existingDefinitions.last()
            levelDefinitionRepository.deleteById(removed.id)
            log.info("Deleted last level [{}]", removed)
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
    LevelDef addNextLevel(String projectId, NextLevelRequest nextLevelRequest, String skillId = null) {
        SettingsResult setting = settingsService.getSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)

        boolean asPoints = false
        if(setting?.isEnabled()){
            asPoints = true
            assert nextLevelRequest?.points > 0
        }

        LevelDef created
        LevelDefRes existingDefinitions = getLevelDefs(projectId, skillId)
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
                skill = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
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
                    projDef: project,
                    percent: nextLevelRequest.percent,
                    skillDef: skill,
                    name: nextLevelRequest.name,
                    pointsFrom: nextLevelRequest.points
            )

            created = levelDefinitionRepository.save(created)

            if(skill){
                skill.addLevel(created)
                skillDefRepo.save(skill)
            }else{
                project.addLevel(created)
                projDefRepo.save(project)
            }
            log.info("Added new level [{}]", created)
        }

        return created
    }

    List<LevelDef> createDefault() {
        List<LevelDef> res = []
        defaultPercentages.eachWithIndex { int entry, int i ->
            res << new LevelDef(level: i + 1, percent: entry)
        }

        levelDefinitionRepository.saveAll(res)
    }
}
