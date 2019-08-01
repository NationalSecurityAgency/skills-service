package skills.services.events.pointsAndAchievements

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsSaver {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Profile
    void save(DataToSave dataToSave) {
        saveNewPoints(dataToSave)
        addToExistingPoints(dataToSave)
        saveAchievements(dataToSave)
    }

    @Profile
    private Iterable<UserAchievement> saveAchievements(DataToSave dataToSave) {
        userAchievedLevelRepo.saveAll(dataToSave.userAchievements)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> addToExistingPoints(DataToSave dataToSave) {
        dataToSave.toAddPointsTo.each {
            skillEventsSupportRepo.addUserPoints(it.id, dataToSave.pointIncrement)
        }
    }
    @Profile
    private Iterable<UserPoints> saveNewPoints(DataToSave dataToSave) {
        userPointsRepo.saveAll(dataToSave.toSave)
    }
}
