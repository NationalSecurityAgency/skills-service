package skills.storage.repos

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.storage.model.ProjDef

@Service
class ProjDefAccessor {

    @Autowired
    ProjDefRepo projDefRepo

    @Transactional()
    ProjDef getProjDef(String projectId) {
        ProjDef projDef = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (!projDef) {
            throw new SkillException("Failed to find project [$projectId]", projectId, null)
        }
        return projDef
    }
}
