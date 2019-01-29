package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement

interface UserAchievedLevelRepo extends CrudRepository<UserAchievement, Integer> {

    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)

    Integer countByProjectIdAndSkillIdAndLevel(String projectId, String skillId, int level)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    @Query('''select sdParent.skillId as skillId, sdChild.skillId as childSkillId, ua.skillId as childAchievedSkillId 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    List<ChildWithAchievementsInfo> findChildrenAndTheirAchievements(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    static interface ChildWithAchievementsInfo {
        String getSkillId()

        String getChildSkillId()

        String getChildAchievedSkillId()
    }


    @Query(''' select sdParent
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdParent.id = ua.skillDef and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdChild.projectId=?2 and sdChild.skillId=?3 and ua.id is null and srd.type=?4''')
    List<SkillDef> findNonAchievedParents(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select sdChild
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillDef and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and ua.id is null and srd.type=?4''')
    List<SkillDef> findNonAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query('''select sdParent.name as label, count(ua) as count
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where srd.parent=sdParent.id and srd.child=sdChild.id and sdChild.skillId=ua.skillId and ua.level is null and 
      sdParent.projectId=?1 and sdParent.type=?2 group by sdParent.skillId''')
    List<LabelCountInfo> getUsageFacetedViaSubject(String projectId, SkillDef.ContainerType subjectType)

    static interface LabelCountInfo {
        String getLabel()
        Integer getCount()
    }
}
