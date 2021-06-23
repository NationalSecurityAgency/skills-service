/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.skillLoading

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import skills.controller.UserInfoController
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.AccessSettingsStorageService
import skills.services.LevelDefinitionStorageService
import skills.services.settings.SettingsDataAccessor
import skills.skillLoading.model.*
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserPointsRepo

import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
@Slf4j
@CompileStatic
class RankingLoader {

    static final String PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF = 'project-admins_rank_and_leaderboard_optOut'

    @Autowired
    UserPointsRepo userPointsRepository

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    SkillsRanking getUserSkillsRanking(String projectId, String userId, String subjectId = null) {
        Integer points = subjectId ?
                userPointsRepository.findPointsByProjectIdAndUserIdAndSkillId(projectId, userId, subjectId) :
                userPointsRepository.findPointsByProjectIdAndUserId(projectId, userId)

        return doGetUserSkillsRanking(userId, projectId, points, subjectId)
    }

    private Sort getPointsSort(boolean ptsAsc, boolean timestampAsc = true) {
        Sort pointsSort = Sort.by(ptsAsc ? Sort.Direction.ASC : Sort.Direction.DESC, "points")
        Sort userFirstSeenTimestampSort = Sort.by(timestampAsc ? Sort.Direction.ASC : Sort.Direction.DESC, "userFirstSeenTimestamp")
        return pointsSort.and(userFirstSeenTimestampSort)
    }

    @Profile
    LeaderboardRes getLeaderboard(String projectId, String userId, LeaderboardRes.Type type, String subjectId = null) {
        UserAttrs userAttrs = userAttrsRepo.findByUserId(userId)
        LocalDateTime userCreatedDate = userAttrs.created
        OptOutInfo optOutInfo = getOptOutInfo(userId, projectId)

        List<RankedUserRes> res
        if (type == LeaderboardRes.Type.tenAroundMe) {
            if (optOutInfo.isOptOut()) {
                throw new SkillException("Leaderboard type of [${LeaderboardRes.Type.tenAroundMe}] is not supported for opted-out users. Requested user is [${userId}]", projectId)
            }
            int myPoints = userPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, subjectId, null)?.points ?: 0
            int numWithHigherScore = subjectId ?
                    userPointsRepository.calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(projectId, subjectId, myPoints, userAttrs.created) :
                    userPointsRepository.calculateNumUsersWithHigherScoreAndIfScoreTheSameThenAfterUserCreateDate(projectId, myPoints, userAttrs.created)
            int rank = numWithHigherScore + 1
            if (rank <= 5) {
                res = getTop10Users(projectId, userAttrs, optOutInfo, subjectId)
            } else {
                res = []

                // 5 above
                PageRequest pageRequestForAbove = PageRequest.of(0, 5, getPointsSort(true, false))
                List<UserPointsRepo.RankedUserRes> above = subjectId ?
                        userPointsRepository.findUsersForLeaderboardPointsMoreOrEqual(projectId, subjectId, myPoints, userCreatedDate, pageRequestForAbove) :
                        userPointsRepository.findUsersForLeaderboardPointsMoreOrEqual(projectId, myPoints, userCreatedDate, pageRequestForAbove)
                res.addAll(convertToRankedUserRes(above.reverse(), rank - 5, userId))

                // requested user
                res.add(createRankedUserForThisUser(rank, userAttrs, myPoints))

                // 5 below
                PageRequest pageRequestForBelow = PageRequest.of(0, 5, getPointsSort(false))
                List<UserPointsRepo.RankedUserRes> below = subjectId ?
                        userPointsRepository.findUsersForLeaderboardPointsLessOrEqual(projectId, subjectId, myPoints, userCreatedDate, pageRequestForBelow) :
                        userPointsRepository.findUsersForLeaderboardPointsLessOrEqual(projectId, myPoints, userCreatedDate, pageRequestForBelow)
                res.addAll(convertToRankedUserRes(below, rank + 1, userId))
            }
        } else {
            res = getTop10Users(projectId, userAttrs, optOutInfo, subjectId)
        }

        return new LeaderboardRes(rankedUsers: res, availablePoints: getAvailablePoints(projectId, subjectId), optedOut: optOutInfo.isPersonalOptOut())
    }

    private RankedUserRes createRankedUserForThisUser(int rank, UserAttrs userAttrs, int myPoints) {
        new RankedUserRes(rank: rank, userId: userAttrs.userIdForDisplay, firstName: userAttrs.firstName, lastName: userAttrs.lastName,
                isItMe: true, points: myPoints, userFirstSeenTimestamp: userAttrs.created.toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    private Integer getAvailablePoints(String projectId, String subjectId) {
        if (subjectId) {
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
            if (!skillDef) {
                throw new SkillException("Failed to find project [$projectId] with skillId [${subjectId}]", projectId, subjectId, ErrorCode.SkillNotFound)
            }
            return skillDef.totalPoints
        }

        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        return projDef.totalPoints
    }

    private List<RankedUserRes> getTop10Users(String projectId, UserAttrs userAttrs, OptOutInfo optOut, String subjectId = null) {
        int size = 10
        PageRequest pageRequest = PageRequest.of(0, size, getPointsSort(false))
        List<String> excludedIds = optOut.admins ?: ['$%^&*']
        List<UserPointsRepo.RankedUserRes> rankedUserRes = subjectId ?
                userPointsRepository.findUsersForLeaderboard(projectId, subjectId, excludedIds, pageRequest) :
                userPointsRepository.findUsersForLeaderboard(projectId, excludedIds, pageRequest)

        List<RankedUserRes> res = convertToRankedUserRes(rankedUserRes, 1, userAttrs.userId)

        // if user is NOT in the top 10 then artificially add the user on the bottom
        if (!optOut.isPersonalOptOut() && res.size() > 0 && res.size() < 10 && !res.find { it.isItMe }) {
            int myPoints = userPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userAttrs.userId, subjectId, null)?.points ?: 0
            res.add(createRankedUserForThisUser(res.size() + 1, userAttrs, myPoints))
        }
        return res
    }

    private List<RankedUserRes> convertToRankedUserRes(List<UserPointsRepo.RankedUserRes> rankedUserRes, int startRank, String userId) {
        int count = startRank
        List<RankedUserRes> res = rankedUserRes.collect {
            new RankedUserRes(
                    rank: count++,
                    userId: it.getUserIdForDisplay(),
                    firstName: it.getUserFirstName(),
                    lastName: it.getUserLastName(),
                    points: it.getPoints(),
                    userFirstSeenTimestamp: it.getUserFirstSeenTimestamp()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                    isItMe: it.getUserId() == userId,
            )
        }
        return res
    }

    @Profile
    private SkillsRanking doGetUserSkillsRanking(String userId, String projectId, Integer points, String subjectId = null) {
        // always calculate total number of users
        int numUsers = findNumberOfUsers(projectId, subjectId) as int

        OptOutInfo optOut = getOptOutInfo(userId, projectId)

        SkillsRanking ranking
        if (points) {
            List<String> excludedIds = optOut.admins ?: ['$%^&*']
            int numUsersWithMorePoints = calculateNumberOfUsersWithGreaterPoints(subjectId, projectId, points, excludedIds)
            int position = numUsersWithMorePoints + 1
            ranking = new SkillsRanking(numUsers: numUsers, position: position, optedOut: optOut.isPersonalOptOut())
        } else {
            // last one
            ranking = new SkillsRanking(numUsers: numUsers + 1, position: numUsers + 1, optedOut: optOut.isPersonalOptOut())
        }

        return ranking
    }

    static class OptOutInfo {
        boolean personalOptOut
        boolean allAdminOptOut
        List<String> admins = []

        boolean isOptOut() {
            personalOptOut || allAdminOptOut
        }
    }

    @Profile
    private OptOutInfo getOptOutInfo(String userId, String projectId) {
        OptOutInfo res = new OptOutInfo()
        String optOutValue = settingsDataAccessor.getUserSettingValue(userId, UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF)
        res.personalOptOut = StringUtils.isNotBlank(optOutValue) ? Boolean.valueOf(optOutValue) : false
        Setting adminOptOutSetting = settingsDataAccessor.getProjectSetting(projectId, PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF)
        res.allAdminOptOut = adminOptOutSetting && Boolean.valueOf(adminOptOutSetting.value)
        if (res.allAdminOptOut) {
            res.admins = accessSettingsStorageService.getProjectAdminIds(projectId)
            if (res.admins.contains(userId)){
                res.personalOptOut = true
            }
        }
        return res
    }

    @Profile
    private int calculateNumberOfUsersWithGreaterPoints(String subjectId, String projectId, Integer usersPoints, List<String> excludeUserIds) {
        subjectId ? userPointsRepository.calculateNumUsersWithLessScore(projectId, subjectId, usersPoints, excludeUserIds)
                : userPointsRepository.calculateNumUsersWithLessScore(projectId, usersPoints, excludeUserIds)
    }

    @Profile
    private long findNumberOfUsers(String projectId, String subjectId) {
        userPointsRepository.countByProjectIdAndSkillIdAndDay(projectId, subjectId, null)
    }

    SkillsRankingDistribution getRankingDistribution(String projectId, String userId, String subjectId = null) {
        UserPoints usersPoints = loadUserPoints(projectId, userId, subjectId)

        List<UserAchievement> myLevels = loadUserAchievements(userId, projectId, subjectId)
        int myLevel = myLevels ? myLevels.collect({ it.level }).max() : 0

        final int currentPts = usersPoints?.points ?: 0
        List<UserPoints> next = findHighestUserPoints(projectId, currentPts, subjectId)
        Integer pointsToPassNextUser = next ? next.first().points - currentPts : -1

        Integer pointsAnotherUserToPassMe = -1
        if (currentPts) {
            List<UserPoints> previous = findLowestUserPoints(projectId, currentPts, subjectId)
            pointsAnotherUserToPassMe = previous ? currentPts - previous.first().points : -1
        }

        return new SkillsRankingDistribution(myLevel: myLevel, myPoints: usersPoints?.points ?: 0,
                pointsToPassNextUser: pointsToPassNextUser, pointsAnotherUserToPassMe: pointsAnotherUserToPassMe)
    }

    @CompileStatic
    @Profile
    private UserPoints loadUserPoints(String projectId, String userId, String subjectId) {
        userPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, subjectId, null)
    }

    @CompileStatic
    @Profile
    private List<UserAchievement> loadUserAchievements(String userId, String projectId, String subjectId) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, subjectId)
    }

    @CompileStatic
    @Profile
    private List<UserPoints> findLowestUserPoints(String projectId, int points, String subjectId) {
        List<UserPoints> previous = userPointsRepository.findByProjectIdAndSkillIdAndPointsLessThanAndDayIsNull(projectId, subjectId, points, PageRequest.of(0, 1, Sort.Direction.DESC, "points"))
        previous
    }

    @CompileStatic
    @Profile
    private List<UserPoints> findHighestUserPoints(String projectId, int points, String subjectId) {
        List<UserPoints> next = userPointsRepository.findByProjectIdAndSkillIdAndPointsGreaterThanAndDayIsNull(projectId, subjectId, points, PageRequest.of(0, 1, Sort.Direction.ASC, "points"))
        next
    }

    @CompileStatic
    @Profile
    List<UsersPerLevel> getUserCountsPerLevel(String projectId, boolean includeZeroLevel = false, String subjectId = null) {
        List<skills.controller.result.model.LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId, subjectId)
        List<UsersPerLevel> usersPerLevel = !levels ? [] : levels.sort({
            it.level
        }).collect { skills.controller.result.model.LevelDefinitionRes levelMeta ->
            Integer numUsers = achievedLevelRepository.countByProjectIdAndSkillIdAndLevel(projectId, subjectId, levelMeta.level)
            new UsersPerLevel(level: levelMeta.level, numUsers: numUsers ?: 0)
        }

        // when level completed by a user a UserAchievement record is stored,
        // a user that achieved level 1, 2 and 3 will have three UserAchievement records, therefore
        // the sql logic ends up double counting for lower levels; as a fix let's remove
        // number of users of higher levels from lower levels
        usersPerLevel = usersPerLevel.sort({ it.level })
        usersPerLevel.eachWithIndex { UsersPerLevel entry, int i ->
            if (i + 1 < usersPerLevel.size()) {
                entry.numUsers -= usersPerLevel[i + 1].numUsers
            }
        }

        if (includeZeroLevel) {
            Integer numUsers = achievedLevelRepository.countByProjectIdAndSkillIdAndLevel(projectId, subjectId, 0)
            usersPerLevel.add(0, new UsersPerLevel(level: 0, numUsers: numUsers ?: 0))
        }

        return usersPerLevel
    }
}
