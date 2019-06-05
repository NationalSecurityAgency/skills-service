package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

interface SkillRelDefRepo extends CrudRepository<SkillRelDef, Integer> {
    List<SkillRelDef> findAllByChildAndType(SkillDef child, SkillRelDef.RelationshipType type)
    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)
    List<SkillRelDef> findAllByParentAndType(SkillDef parent, SkillRelDef.RelationshipType type)

    @Query('''select sd2 from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.skillId=?1''')
    List<SkillDef> getChildren(String parentSkillId, SkillRelDef.RelationshipType type)

    // keep in mind that most of the time you want to ask for a specific relationship type so use this method with caution
    List<SkillRelDef> findAllByParent(SkillDef parent)

    @Query('''select sd1, sd2 from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId=?1''')
    List<Object[]> getGraph(String projectId, SkillRelDef.RelationshipType type)

    @Query(value = '''WITH RECURSIVE skill_deps_path AS (
  select sd.project_id as parentProjectId, sd.skill_id as parentSkillId, sd.id as parentId, sd.name as parentName,
         sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
  from skill_definition sd,
       skill_relationship_definition srd,
       skill_definition sd1
  where sd.id = srd.parent_id
    and sd1.id = srd.child_id
    and srd.type = 'Dependence'
    and sd.project_id=?1 and sd.skill_id=?2 
  UNION ALL
  select skill_deps_path.childProjectId as parentProjectId, skill_deps_path.childSkillId as parentSkillId, skill_deps_path.childId as parentId, skill_deps_path.childName as parentName,
         sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
  from  skill_deps_path,
       skill_relationship_definition srd,
       skill_definition sd1
  where skill_deps_path.childId = srd.parent_id
    and sd1.id = srd.child_id
    and srd.type = 'Dependence'
    and skill_deps_path.childProjectId=?1
)
select pd.project_id as parentProjectId, pd.name as parentProjectName, skill_deps_path.parentId, skill_deps_path.parentSkillId, skill_deps_path.parentName,
       skill_deps_path.childProjectId, pd1.name as childProjectName, skill_deps_path.childId, skill_deps_path.childSkillId, skill_deps_path.childName,
       ua.id as achievementId
from skill_deps_path
  join project_definition pd on skill_deps_path.parentProjectId = pd.project_id
  join project_definition pd1 on skill_deps_path.childProjectId = pd1.project_id
  left join user_achievement ua
    ON ua.skill_ref_id = skill_deps_path.childId AND ua.user_id=?3''', nativeQuery = true)
    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId)


    static interface GraphRelWithAchievement {
        String getParentProjectId()
        String getParentProjectName()
        Integer getParentId()
        String getParentSkillId()
        String getParentName()

        String getChildProjectId()
        String getChildProjectName()
        Integer getChildId()
        String getChildSkillId()
        String getChildName()

        Integer getAchievementId()
    }
}
