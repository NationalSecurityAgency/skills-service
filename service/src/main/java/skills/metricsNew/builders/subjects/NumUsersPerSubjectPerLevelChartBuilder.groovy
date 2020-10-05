package skills.metricsNew.builders.subjects

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.UserAchievedLevelRepo

@Component
@Slf4j
class NumUsersPerSubjectPerLevelChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Override
    String getId() {
        return "NumUsersPerSubjectPerLevelChartBuilder"
    }

    static class SubjectLevelCounts {
        String subject
        List<NumUsersPerLevel> numUsersPerLevels
    }

    static class NumUsersPerLevel {
        Integer level
        Long numberUsers
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        List<UserAchievedLevelRepo.SkillAndLevelUserCount> skillAndLevelUserCounts = userAchievedRepo.countNumUsersPerContainerTypeAndLevel(projectId, SkillDef.ContainerType.Subject)
        Map<String, List<UserAchievedLevelRepo.SkillAndLevelUserCount>> bySubjectId =  skillAndLevelUserCounts.groupBy {it.skillId}
        List<LevelDefRepo.SubjectLevelCount> subjectLevel = levelDefRepo.countNumLevelsPerSubject(projectId)
        return subjectLevel.collect{
            int numberLevels = it.getNumberLevels()
            List<UserAchievedLevelRepo.SkillAndLevelUserCount> userCountsForLevels = bySubjectId.get(it.getSubject())
            List<NumUsersPerLevel> numUsersPerLevels = (1..numberLevels).collect {Integer level ->
                UserAchievedLevelRepo.SkillAndLevelUserCount found = userCountsForLevels.find({it.level == level})
                new NumUsersPerLevel(level: level, numberUsers: found?.getNumberUsers() ?: 0)
            }

            adjustCountsToOnlyCountLastLevel(numUsersPerLevels)

            return new SubjectLevelCounts(subject: it.subject, numUsersPerLevels: numUsersPerLevels)
        }
    }

    private void adjustCountsToOnlyCountLastLevel(List<NumUsersPerLevel> numUsersPerLevels) {
        List<NumUsersPerLevel> sorted = numUsersPerLevels.sort({ it.level }).reverse()
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                sorted[j].numberUsers = sorted[j].numberUsers - sorted[i].numberUsers
            }
        }
    }
}
