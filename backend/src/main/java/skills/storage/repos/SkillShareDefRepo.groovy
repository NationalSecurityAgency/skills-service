package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillShareDef

interface SkillShareDefRepo extends CrudRepository<SkillShareDef, Long> {


    @Query('''select share from SkillShareDef share, SkillDef skill
        where share.skill = skill and skill.projectId=?1''')
    List<SkillShareDef> getSkillShareDefsWithOtherProjectsByProjectId(String projectId)

    @Query('''select skill.id as id, skill.skillId as skillId, skill.name as skillName, proj.projectId as projectId, proj.name as projectName
        from SkillShareDef share, SkillDef skill, ProjDef proj
        where share.skill = skill and skill.projectId=proj.projectId 
            and (share.sharedToProject=?1 or share.sharedToProject is null) and proj <> ?1''')
    List<SkillSharedMeta> getSkillDefsSharedFromOtherProjectsByProjectId(ProjDef sharedToProject)

    static interface SkillSharedMeta {
        Integer getId()
        String getSkillId()
        String getSkillName()
        String getProjectId()
        String getProjectName()
    }

    @Nullable
    SkillShareDef findBySkillAndSharedToProject(SkillDef skill, ProjDef sharedToProject)

    @Nullable
    SkillShareDef findBySkillAndSharedToProjectIsNull(SkillDef skill)

    @Nullable
    SkillShareDef findBySkillIdAndSharedToProjectIsNull(Integer skillId)

    @Nullable
    @Query('''select share from SkillShareDef share, ProjDef proj
        where share.sharedToProject = proj and proj.projectId=?1 and share.skill.id=?2''')
    SkillShareDef findBySharedToProjectIdAndSkillId(String projectId, Integer skillId)
}
