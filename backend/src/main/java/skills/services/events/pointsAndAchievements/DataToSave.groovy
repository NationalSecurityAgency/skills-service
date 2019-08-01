package skills.services.events.pointsAndAchievements


import groovy.transform.Canonical
import groovy.transform.CompileStatic
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillEventsSupportRepo

@CompileStatic
@Canonical
class DataToSave {
    List<UserPoints> toSave = []
    List<SkillEventsSupportRepo.TinyUserPoints> toAddPointsTo = []
    int pointIncrement
    List<UserAchievement> userAchievements = []
}
