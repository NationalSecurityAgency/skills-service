package skills.services

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.ProjDef
import skills.storage.model.SkillsDBLock
import skills.storage.model.UserAttrs
import skills.storage.model.UserPoints
import skills.storage.repos.SkillsDBLockRepo

@Service
@CompileStatic
class LockingService {

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    SkillsDBLock lockGlobally() {
        return skillsDBLockRepo.findByLock("global_lock")
    }

    ProjDef lockProject(String projectId) {
        return skillsDBLockRepo.findByProjectIdIgnoreCase(projectId)
    }

    UserAttrs lockUser(String userId) {
        return skillsDBLockRepo.findUserAttrsByUserId(userId?.toLowerCase())
    }

    UserPoints lockUserPoints(String projectId, String userId) {
        return skillsDBLockRepo.findUserPointsByProjectIdAndUserId(projectId, userId)
    }



}
