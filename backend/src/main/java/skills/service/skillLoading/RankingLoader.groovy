package skills.service.skillLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.service.controller.result.model.LevelDefinitionRes
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.skillLoading.model.UsersPerLevel
import skills.service.skillLoading.model.SkillsRanking
import skills.service.skillLoading.model.SkillsRankingDistribution

@Component
@Slf4j
class RankingLoader {

    @Autowired
    UserPointsRepo userPointsRepository

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    SkillsRanking getUserSkillsRanking(String projectId, String userId, String subjectId = null){
        UserPoints usersPoints = userPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, subjectId, null)
        return doGetUserSkillsRanking(projectId, usersPoints, subjectId)
    }

    private  SkillsRanking doGetUserSkillsRanking(String projectId, UserPoints usersPoints, String subjectId = null) {
        int numUsers = userPointsRepository.countByProjectIdAndSkillIdAndDay(projectId, null, null)
        // always calculate total number of users
        SkillsRanking ranking
        if (usersPoints) {
            int numUsersWithLessScore = subjectId ? userPointsRepository.calculateNumUsersWithLessScore(projectId, subjectId, usersPoints.points)
                    : userPointsRepository.calculateNumUsersWithLessScore(projectId, usersPoints.points)
            int position = numUsers - numUsersWithLessScore
            ranking = new SkillsRanking(numUsers: numUsers, position: position)
        } else {
            // last one
            ranking = new SkillsRanking(numUsers: numUsers+1, position: numUsers+1)
        }

        return ranking
    }

    SkillsRankingDistribution getRankingDistribution(String projectId, String userId, String subjectId = null) {
        UserPoints usersPoints = userPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, subjectId, null)
        SkillsRanking skillsRanking = doGetUserSkillsRanking(projectId, usersPoints, subjectId)

        List<UsersPerLevel> usersPerLevel = getUserCountsPerLevel(projectId, false, subjectId)

        List<UserAchievement> myLevels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, subjectId)
        int myLevel = myLevels ? myLevels.collect({it.level}).max() : 0

        Integer pointsToPassNextUser = -1
        Integer pointsAnotherUserToPassMe = -1

        if(usersPoints?.points){
            List<UserPoints> next = userPointsRepository.findHigherUserPoints(projectId, usersPoints.points, new PageRequest(0 , 1))
            pointsToPassNextUser = next ? next.first().points - usersPoints.points : -1

            List<UserPoints> previous = userPointsRepository.findPreviousUserPoints(projectId, usersPoints.points, new PageRequest(0 , 1))
            pointsAnotherUserToPassMe = previous ? usersPoints.points - previous.first().points : -1
        }

        return new SkillsRankingDistribution(totalUsers: skillsRanking.numUsers, myPosition: skillsRanking.position,
                myLevel: myLevel, myPoints: usersPoints?.points ?: 0, usersPerLevel: usersPerLevel,
                pointsToPassNextUser: pointsToPassNextUser, pointsAnotherUserToPassMe: pointsAnotherUserToPassMe)
    }

    List<UsersPerLevel> getUserCountsPerLevel(String projectId, boolean includeZeroLevel = false, String subjectId = null) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId, subjectId)
        List<UsersPerLevel> usersPerLevel = !levels ? [] : levels.sort({
            it.level
        }).collect { LevelDefinitionRes levelMeta ->
            Integer numUsers = achievedLevelRepository.countByProjectIdAndSkillIdAndLevel(projectId, subjectId, levelMeta.level)
            new UsersPerLevel(level: levelMeta.level, numUsers: numUsers ?: 0)
        }

        if(includeZeroLevel){
            Integer numUsers = achievedLevelRepository.countByProjectIdAndSkillIdAndLevel(projectId, subjectId, 0)
            usersPerLevel.add(0, new UsersPerLevel(level: 0, numUsers: numUsers ?: 0))
        }

        return usersPerLevel
    }
}
