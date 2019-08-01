package skills.services.events.pointsAndAchievements

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillEventsSupportRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsDataLoader {

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    LoadedDataValidator validator

    @Profile
    LoadedData loadData(String projectId, String userId, Date incomingSkillDate, SkillEventsSupportRepo.SkillDefMin skillDef){
        List<SkillEventsSupportRepo.TinySkillDef> parentDefs = loadParents(skillDef)

        List<Integer> skillRefIds = [skillDef.id]
        skillRefIds.addAll(parentDefs.collect { it.id })
        List<SkillEventsSupportRepo.TinyUserPoints> tinyUserPoints = loadPoints(projectId, userId, skillRefIds, incomingSkillDate)

        SkillEventsSupportRepo.TinyProjectDef tinyProjectDef = loadProject(projectId)
        List<Integer> parentIds = parentDefs.collect { it.id }
        List<LevelDefInterface> tinyLevels = loadLevels(parentIds, tinyProjectDef)

        List<SkillEventsSupportRepo.TinyUserAchievement> tinyUserAchievements = loadAchievements(userId, projectId, skillRefIds)

        LoadedData res = new LoadedData(projectId: projectId, parentDefs: parentDefs, tinyUserPoints: tinyUserPoints,
                levels: tinyLevels, tinyUserAchievements: tinyUserAchievements, tinyProjectDef:tinyProjectDef)
        validator.validate(res)

        return res
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserAchievement> loadAchievements(String userId, String projectId, List<Integer> skillRefIds) {
        skillEventsSupportRepo.findTinyUserAchievementsByUserIdAndProjectIdAndSkillIds(userId, projectId, skillRefIds)
    }

    @Profile
    private List<LevelDefInterface> loadLevels(List<Integer> parentIds, SkillEventsSupportRepo.TinyProjectDef tinyProjectDef) {
        skillEventsSupportRepo.findLevelsBySkillIdsOrByProjectId(parentIds, tinyProjectDef.id)
    }

    @Profile
    private SkillEventsSupportRepo.TinyProjectDef loadProject(String projectId) {
        skillEventsSupportRepo.getTinyProjectDef(projectId)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> loadPoints(String projectId, String userId, List<Integer> skillRefIds, Date incomingSkillDate) {
        skillEventsSupportRepo.findTinyUserPointsProjectIdAndUserIdAndSkillsAndDay(projectId, userId, skillRefIds, incomingSkillDate)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinySkillDef> loadParents(SkillEventsSupportRepo.SkillDefMin skillDef) {
        skillEventsSupportRepo.findTinySkillDefsParentsByChildIdAndType(skillDef.id, SkillRelDef.RelationshipType.RuleSetDefinition)
    }
}
