package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UsageItem
import skills.storage.model.UserAchievement

interface UserAchievedLevelRepo extends CrudRepository<UserAchievement, Integer> {

    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)

    Integer countByProjectIdAndSkillIdAndLevel(String projectId, String skillId, int level)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    @Query('''select sdParent.skillId as skillId, sdChild.skillId as childSkillId, sdChild.projectId as childProjectId, ua.skillId as childAchievedSkillId 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1
      where srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    List<ChildWithAchievementsInfo> findChildrenAndTheirAchievements(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    static interface ChildWithAchievementsInfo {
        String getSkillId()

        String getChildProjectId()
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

    @Query('''select sdChild.name as label, count(ua) as count
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      left outer join UserAchievement ua ON sdChild.skillId=ua.skillId 
      where srd.parent=sdParent.id and srd.child=sdChild.id and ua.level is null and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and sdParent.type=?3 group by sdChild.name''')
    List<LabelCountInfo> getSubjectUsageFacetedViaSkill(String projectId, String subjectId, SkillDef.ContainerType subjectType)

    static interface LabelCountInfo {
        String getLabel()
        Integer getCount()
    }

    @Query('''select count(ua)
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and ua.userId=?1 and 
        skillDef.skillId = ua.skillId and skillDef.projectId = ua.projectId and 
        skillDef.projectId=?2 and 
        skillDef.type=?3''')
    int countAchievedForUser(String userId, String projectId, SkillDef.ContainerType containerType)


    @Query('''select count(ua) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    int countAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)


    @Query(value = '''select date(ua.created) day, count(*) numItems
      from skill_definition skillDef, user_achievement ua 
      where 
        ua.level is null and 
        skillDef.skill_id = ua.skill_id and skillDef.project_id = ua.project_id and 
        skillDef.project_id= :projectId and  
        skillDef.skill_id = :badgeId and
        skillDef.type= :#{#type.toString()} and
        ua.created >= :date 
        group by day''', nativeQuery = true)
    List<UsageItem> countAchievementsForProjectPerDay(@Param('projectId') String projectId, @Param('badgeId') String badgeId, @Param('type') SkillDef.ContainerType containerType, @Param('date') Date mustBeAfterThisDate)

    @Query(value = '''select month(ua.created) label, count(*) count
      from skill_definition skillDef, user_achievement ua 
      where 
        ua.level is null and 
        skillDef.skill_id = ua.skill_id and skillDef.project_id = ua.project_id and 
        skillDef.project_id= :projectId and 
        skillDef.skill_id = :badgeId and
        skillDef.type= :#{#type.toString()} and
        ua.created >= :date 
        group by label''', nativeQuery = true)
    List<LabelCountInfo> countAchievementsForProjectPerMonth(@Param('projectId') String projectId, @Param('badgeId') String badgeId, @Param('type') SkillDef.ContainerType containerType, @Param('date') Date mustBeAfterThisDate)
}
