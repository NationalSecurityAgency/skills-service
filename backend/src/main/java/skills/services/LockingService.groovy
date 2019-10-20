package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.ProjDef
import skills.storage.model.UserAttrs
import skills.storage.repos.LockTransactionRepo

@Service
class LockingService {

    @Autowired
    LockTransactionRepo lockTransactionRepo

    ProjDef lockProject(String projectId) {
        return lockTransactionRepo.findByProjectIdIgnoreCase(projectId)
    }

    UserAttrs lockUser(String userId) {
        return lockTransactionRepo.findUserAttrsByUserId(userId?.toLowerCase())
    }
}
