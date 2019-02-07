package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillShareDef

interface SkillShareDefRepo extends CrudRepository<SkillShareDef, Long> {


    @Query('''select share from SkillShareDef share, SkillDef skill
        where share.skill = skill and skill.projectId=?1''')
    List<SkillShareDef> getSkillShareDefsByProjectId(String projectId)

    SkillShareDef findBySkillAndSharedToProject(SkillDef skill, ProjDef sharedToProject)
}
