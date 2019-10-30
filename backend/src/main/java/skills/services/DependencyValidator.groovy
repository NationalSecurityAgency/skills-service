package skills.services

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillShareDef
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo

@Component
@CompileStatic
class DependencyValidator {

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    void validateDependencyEligibility(String projectId, SkillDefParent skill) {
        SkillShareDef skillShareDef = skillShareDefRepo.findBySharedToProjectIdAndSkillId(projectId, skill.id)
        if (!skillShareDef) {
            // check if the dependency is shared with ALL projects (null shared_to_project_id)
            skillShareDef = skillShareDefRepo.findBySkillIdAndSharedToProjectIsNull(skill.id)
        }

        if (!skillShareDef){
            SkillDef globalBadge = skillRelDefRepo.findGlobalBadgeByChildSkillId(skill.skillId)
            if(globalBadge) {
                //if the skillId is a child of a global badge, then we expect there not to be an explicit
                //SkillShareDef
                return
            }
        }

        if (!skillShareDef) {
            throw new SkillException("Skill [${skill.projectId}:${skill.skillId}] is not shared (or does not exist) to [$projectId] project", projectId)
        }
    }
}
