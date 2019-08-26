package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.UserSkillsStats
import skills.skillLoading.model.SkillPerfomed
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo

@Service
@Slf4j
class UserAdminService {

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    UserPointsRepo userPointsRepo

    @Transactional(readOnly = true)
    skills.controller.result.model.TableResult loadUserPerformedSkillsPage(String projectId, String userId, String query, PageRequest pageRequest){
        skills.controller.result.model.TableResult result = new skills.controller.result.model.TableResult()
        Long totalPerformedSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillIdContaining(userId, projectId, query)
        if(totalPerformedSkills) {
            List<UserPerformedSkill> performedSkills = performedSkillRepository.findByUserIdAndProjectIdAndSkillIdContaining(userId, projectId, query, pageRequest)
            result.data = performedSkills.collect({
                new SkillPerfomed(skillId: it.skillId, performedOn: it.performedOn)
            })
            result.count = totalPerformedSkills
        }
        return result
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
