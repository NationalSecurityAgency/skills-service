package skills.service.datastore.services.settings.listeners

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.controller.exceptions.ErrorCode
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.SettingsRequest
import skills.service.controller.result.model.LevelDefinitionRes
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.datastore.services.settings.SettingChangedListener
import skills.service.datastore.services.settings.Settings
import skills.storage.model.LevelDef
import skills.storage.model.ProjDef
import skills.storage.model.Setting
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo

import javax.transaction.Transactional

@Slf4j
@Component
class LevelPointsSettingListener implements SettingChangedListener{

    static final int MIN_TOTAL_POINTS_REQUIRED_TO_SWITCH = 100

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Override
    boolean supports(SettingsRequest setting) {
        return setting.setting == Settings.LEVEL_AS_POINTS.settingName
    }

    @Transactional
    @Override
    void execute(Setting previousValue, SettingsRequest setting) {
        ProjDef project = projDefRepo.findByProjectId(setting.projectId)

        if(setting.isEnabled() && (!previousValue?.isEnabled())){
            log.info("converting all levels for project [${setting.projectId}] (including skill levels) to points")
            if(project?.totalPoints < MIN_TOTAL_POINTS_REQUIRED_TO_SWITCH){
                throw new SkillException("Project has [${project.totalPoints}] total points. " +
                        "[$MIN_TOTAL_POINTS_REQUIRED_TO_SWITCH] total points required to switch to points based levels",
                        project.projectId,
                        "N/A",
                        ErrorCode.InsufficientPointsToConvertLevels)
            }
            convertToPoints(project.levelDefinitions, project.totalPoints)
            project.subjects?.each{
                convertToPoints(it.levelDefinitions, it.totalPoints)
            }
        }else if(!setting.isEnabled()){
            log.info("converting all levels for project [${setting.projectId}] (including skill levels) to percentages")
            convertToPercentage(project.levelDefinitions, project.totalPoints)
            project.subjects?.each{
                convertToPercentage(it.levelDefinitions, it.totalPoints)
            }
        }
    }

    private void convertToPoints(List<LevelDef> levelDefs, int totalPoints){
        List<Integer> levelScores = levelDefs.sort({ it.level }).collect {
            return (int) (totalPoints * (it.percent / 100d))
        }
        levelDefs.eachWithIndex{ LevelDef entry, int i ->
            Integer fromPts = levelScores.get(i)
            /*if(i > 0){
                fromPts+=1
            }*/
            Integer toPts = (i != levelScores.size() - 1) ? levelScores.get(i + 1) : null
            entry.pointsFrom = fromPts
            entry.pointsTo = toPts
            levelDefRepo.save(entry)
        }
    }

    private void convertToPercentage(List<LevelDef> levelDefs, int totalPoints){
        levelDefs.sort({ it.level })

        Integer highestLevelPoints = levelDefs?.last()?.pointsFrom
        if(highestLevelPoints > totalPoints){
            //this means that skills were deleted since the levels were converted to points/edited
            //if we convert as-is to percentages, we'll wind up with invalid percentage values
            log.warn("totalPoints [$totalPoints] are lower " +
                    "then the highest level's pointsFrom [${highestLevelPoints}], " +
                    "this would create invalid percentage values. Using [${highestLevelPoints}] as totalPoints for purposes of conversion")
            //since we don't know what the total was before deletion, let's model the percentages off the highest level points being 92%
            //since that's what we do for the default, otherwise the last level would be 100%
            totalPoints = highestLevelPoints*1.08
        }
        levelDefs.eachWithIndex { LevelDef entry, int i ->
            entry.percent = (int) (((double) entry.pointsFrom / totalPoints) * 100d)
            levelDefRepo.save(entry)
        }
    }
}
