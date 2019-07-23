package skills.skillLoading.model

class SkillBadgeSummary {

    String badge
    String badgeId
    String description
    boolean badgeAchieved = false
    Date dateAchieved
    int numSkillsAchieved
    int numTotalSkills

    Date startDate
    Date endDate
    boolean isGem() { return startDate && endDate }

    List<SkillSummary> skills

    String iconClass
}
