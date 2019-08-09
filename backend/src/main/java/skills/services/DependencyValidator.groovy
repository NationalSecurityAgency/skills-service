package skills.services

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillShareDef
import skills.storage.repos.SkillShareDefRepo

@Component
@CompileStatic
class DependencyValidator {

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    void validateDependencyEligibility(String projectId, SkillDefParent skill) {
        SkillShareDef skillShareDef = skillShareDefRepo.findBySharedToProjectIdAndSkillId(projectId, skill.id)
        if (!skillShareDef) {
            // check if the dependency is shared with ALL projects (null shared_to_project_id)
            skillShareDef = skillShareDefRepo.findBySkillIdAndSharedToProjectIsNull(skill.id)
        }

        if (!skillShareDef) {
            throw new skills.controller.exceptions.SkillException("Skill [${skill.projectId}:${skill.skillId}] is not shared (or does not exist) to [$projectId] project", projectId)
        }
    }
}
