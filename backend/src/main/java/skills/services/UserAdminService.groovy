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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.result.model.TableResult
import skills.controller.result.model.UserInfoRes
import skills.controller.result.model.UserSkillsStats
import skills.skillLoading.model.SkillPerfomed
import skills.storage.model.UserAttrs
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo

@Service
@Slf4j
class UserAdminService {

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserInfoService userInfoService

    @Transactional(readOnly = true)
    skills.controller.result.model.TableResult loadUserPerformedSkillsPage(String projectId, String userId, String query, PageRequest pageRequest){
        TableResult result = new TableResult()
        Long totalPerformedSkills = performedSkillRepository.countByUserIdAndProjectId(userId, projectId)
        if(totalPerformedSkills) {
            Long filteredPerformedSkillsCount = performedSkillRepository.countByUserIdAndProjectIdAndSkillIdIgnoreCaseContaining(userId, projectId, query)
            List<UserPerformedSkill> performedSkills = performedSkillRepository.findByUserIdAndProjectIdAndSkillIdIgnoreCaseContaining(userId, projectId, query, pageRequest)
            result.data = performedSkills.collect({
                new SkillPerfomed(skillId: it.skillId, performedOn: it.performedOn)
            })
            result.count = filteredPerformedSkillsCount
        }
        result.totalCount = totalPerformedSkills
        return result
    }


    @Transactional(readOnly = true)
    List<UserInfoRes> suggestDashboardUsers(String query, boolean includeSelf) {
        query = query ? query.toLowerCase() : ""

        List<UserAttrs> userAttrs = userAttrsRepo.searchForUser(query, new PageRequest(0, 6))
        List<UserInfoRes> results = userAttrs.collect { new UserInfoRes(it) }

        if (!includeSelf) {
            String currentUserId = userInfoService.currentUser.username
            results = results.findAll { it.userId != currentUserId }
        }
        results?.sort() { it.userId }
        return results.take(5)
    }

    @Transactional(readOnly = true)
    List<String> suggestUsersForProject(String projectId, String userQuery, PageRequest pageRequest) {
        return performedSkillRepository.findDistinctUserIdsForProject(projectId, userQuery?.toLowerCase(), pageRequest)
    }

    @Transactional(readOnly = true)
    List<String> suggestUsers(String userQuery, PageRequest pageRequest) {
        return performedSkillRepository.findDistinctUserIds(userQuery?.toLowerCase(), pageRequest)
    }

    @Transactional(readOnly = true)
    Boolean isValidExistingUserIdForProject(String projectId, String userId) {
        return performedSkillRepository.existsByProjectIdAndUserId(projectId, userId)
    }

    @Transactional(readOnly = true)
    Boolean isValidExistingUserId(String userId) {
        return performedSkillRepository.existsByUserId(userId)
    }

    @Transactional(readOnly = true)
    UserSkillsStats getUserSkillsStats(String projectId, String userId) {
        int numSkills =  performedSkillRepository.countDistinctSkillIdByProjectIdAndUserId(projectId, userId)
        UserPoints userPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, null, null)
        return new UserSkillsStats(numSkills: numSkills, userTotalPoints: userPoints?.points ?: 0 )
    }
}
