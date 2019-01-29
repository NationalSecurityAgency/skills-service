package skills.service.datastore.services

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import skills.service.controller.exceptions.SkillException
import skills.service.controller.result.model.LevelDefinitionRes
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
        List<Integer> levelScores = levelDefinitions.sort({ it.level }).collect {
            return (int) (totalPoints * (it.percent / 100d))
        }

        List<LevelDefinitionRes> finalRes = []
        levelDefinitions.eachWithIndex { LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            finalRes << new LevelDefinitionRes(projectId: projectId, skillId: skillId, level: entry.level, percent: entry.percent, pointsFrom: fromPts, pointsTo: toPts)
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

    private List<LevelDefinitionRes> convertAndCalculateScores(LevelDefRes levelDefRes) {
        Integer totalScore = levelDefRes.skillId ? skillDefRepo.calculateTotalScore(levelDefRes.projectId, levelDefRes.skillId) : skillDefRepo.calculateTotalScore(levelDefRes.projectId)
        List<Integer> levelScores = levelDefRes.levels.sort({ it.level }).collect {
            if (totalScore == null) {
                return 0
            }
            return (int) (totalScore * (it.percent / 100d))
        }

        List<LevelDefinitionRes> finalRes = []
        levelDefRes.levels.eachWithIndex { LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            finalRes << new LevelDefinitionRes(projectId: levelDefRes.projectId, skillId: levelDefRes.skillId, level: entry.level, percent: entry.percent, pointsFrom: fromPts, pointsTo: toPts)
        }
        finalRes
    }

    /**
     * deletes the highest level and returns its definition
     * @param projectId
     * @return
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    LevelDef deleteLastLevel(String projectId, String skillId = null) {
        LevelDef removed
        List<LevelDef> existingDefinitions = getLevelDefs(projectId, skillId)
        if (existingDefinitions) {
            removed = existingDefinitions.sort({ it.level }).last()
            levelDefinitionRepository.deleteById(removed.id)
            log.info("Deleted last level [{}]", removed)
        }

        return removed
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    LevelDef addNextLevel(String projectId, int percent, String skillId = null) {
        LevelDef created
        List<LevelDef> existingDefinitions = getLevelDefs(projectId, skillId)
        assert existingDefinitions.collect({ it.percent }).max() < percent

        if (existingDefinitions) {
            LevelDef lastOne = existingDefinitions.sort({ it.level }).last()
            created = new LevelDef(level: lastOne.level + 1, projectId: lastOne.projectId, percent: percent, subjectId: skillId)
            levelDefinitionRepository.save(created)
            log.info("Added new level [{}]", created)
        }

        return created
    }

    List<LevelDef> crateDefault() {
        List<LevelDef> res = []
        defaultPercentages.eachWithIndex { int entry, int i ->
            res << new LevelDef(level: i + 1, percent: entry)
        }

        levelDefinitionRepository.saveAll(res)
    }
}
