package skills.service.skillLoading.model

class SkillsRankingDistribution {

    Integer myPosition
    Integer myPoints
    Integer myLevel

    Integer pointsToPassNextUser
    Integer pointsAnotherUserToPassMe

    Integer totalUsers
    List<UsersPerLevel> usersPerLevel
}
