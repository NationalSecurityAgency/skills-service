package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

interface SkillRelDefRepo extends CrudRepository<SkillRelDef, Integer> {
    List<SkillRelDef> findAllByChildAndType(SkillDef child, SkillRelDef.RelationshipType type)
    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)
    List<SkillRelDef> findAllByParentAndType(SkillDef parent, SkillRelDef.RelationshipType type)


    // keep in mind that most of the time you want to ask for a specific relationship type so use this method with caution
    List<SkillRelDef> findAllByParent(SkillDef parent)

    @Query('''select sd1, sd2 from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId=?1''')
    List<Object[]> getGraph(String projectId, SkillRelDef.RelationshipType type)
}
