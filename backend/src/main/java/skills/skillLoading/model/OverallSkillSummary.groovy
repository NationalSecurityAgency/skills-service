package skills.skillLoading.model

import groovy.transform.Canonical

@Canonical
class OverallSkillSummary {

    String projectName

    int skillsLevel
    int totalLevels

    int points
    int totalPoints

    int levelPoints
    int levelTotalPoints

    int todaysPoints

    List<SkillSubjectSummary> subjects

    BadgeStats badges

    public static class BadgeStats {
        int numBadgesCompleted = 0
        boolean enabled = false
    }
}

