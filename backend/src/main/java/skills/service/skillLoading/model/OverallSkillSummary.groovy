package skills.service.skillLoading.model

import groovy.transform.Canonical

@Canonical
class OverallSkillSummary {

    String projectName

    int skillsLevel

    int points
    int totalPoints

    int levelPoints
    int levelTotalPoints

    int todaysPoints

    List<SkillSubjectSummary> subjects

    List<SkillBadgeSummary> badges
}
