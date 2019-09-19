package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.DayCountItem
import skills.storage.model.UserAchievement

@CompileStatic
interface UserAchievedLevelRepo extends CrudRepository<UserAchievement, Integer> {

    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillId(String userId, @Nullable String projectId, @Nullable String skillId)

    List<UserAchievement> findAllByUserIdAndSkillId(String userId, @Nullable String skillId)

    Integer countByProjectIdAndSkillIdAndLevel(String projectId, @Nullable String skillId, int level)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)
    void deleteByProjectIdAndSkillIdAndUserIdAndLevel(String projectId, @Nullable String skillId, String userId, @Nullable Integer level)

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
    left join UserAchievement ua on sdParent.id = ua.skillRefId and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdChild.projectId=?2 and sdChild.skillId=?3 and ua.id is null and srd.type=?4''')
    List<SkillDef> findNonAchievedParents(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select sdChild
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and ua.id is null and srd.type=?4''')
    List<SkillDef> findNonAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select count(sdChild.id)
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and ua.id is null and srd.type=?4''')
    Long countNonAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select count(sdChild.id)
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdParent.projectId is null and sdParent.skillId=?2 and ua.id is null and srd.type=?3''')
    Long countNonAchievedGlobalSkills(String userId, String skillId, SkillRelDef.RelationshipType type)

    @Query('''select sdParent.name as label, count(ua) as countRes
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where srd.parent=sdParent.id and srd.child=sdChild.id and sdChild.skillId=ua.skillId and ua.level is null and 
      sdParent.projectId=?1 and sdParent.type=?2 group by sdParent.name''')
    List<LabelCountInfo> getUsageFacetedViaSubject(String projectId, SkillDef.ContainerType subjectType, Pageable pageable)

    @Query('''select sdChild.name as label, count(ua) as countRes
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      left outer join UserAchievement ua ON sdChild.skillId=ua.skillId 
      where srd.parent=sdParent.id and srd.child=sdChild.id and ua.level is null and 
      sdParent.projectId=?1 and sdParent.skillId=?2 and sdParent.type=?3 group by sdChild.name''')
    List<LabelCountInfo> getSubjectUsageFacetedViaSkill(String projectId, String subjectId, SkillDef.ContainerType subjectType, Pageable pageable)

    static interface LabelCountInfo {
        String getLabel()
        Integer getCountRes()
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
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and ua.userId=?1 and 
        skillDef.skillId = ua.skillId and 
        skillDef.projectId is null and 
        skillDef.type=?2''')
    int countAchievedGlobalForUser(String userId, SkillDef.ContainerType containerType)


    @Query('''select count(ua) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    int countAchievedChildren(String userId, @Nullable String projectId, String skillId, SkillRelDef.RelationshipType type)


    @Query('''select count(ua) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent=sdParent.id and  srd.child=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type=?3''')
    int countAchievedGlobalSkills(String userId, String skillId, SkillRelDef.RelationshipType type)


    @Query(value = '''select ua.created AS day, count(ua) AS count
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and 
        skillDef.skillId = ua.skillId 
        and skillDef.projectId = ua.projectId and 
        skillDef.projectId= :projectId and  
        skillDef.skillId = :badgeId and
        skillDef.type= :type and
        ua.created >= :date 
        group by ua.created''')
    List<DayCountItem> countAchievementsForProjectPerDay(@Param('projectId') String projectId, @Param('badgeId') String badgeId, @Param('type') SkillDef.ContainerType containerType, @Param('date') Date mustBeAfterThisDate)

    @Query(value = '''select EXTRACT(MONTH FROM ua.created) as label, count(*) countRes
      from skill_definition skillDef, user_achievement ua 
      where 
        ua.level is null and 
        skillDef.skill_id = ua.skill_id and skillDef.project_id = ua.project_id and 
        skillDef.project_id= :projectId and 
        skillDef.skill_id = :badgeId and
        skillDef.type= :#{#type.toString()} and
        ua.created >= :date 
        group by EXTRACT(MONTH FROM ua.created)''', nativeQuery = true)
    List<LabelCountInfo> countAchievementsForProjectPerMonth(@Param('projectId') String projectId, @Param('badgeId') String badgeId, @Param('type') SkillDef.ContainerType containerType, @Param('date') Date mustBeAfterThisDate)
}
