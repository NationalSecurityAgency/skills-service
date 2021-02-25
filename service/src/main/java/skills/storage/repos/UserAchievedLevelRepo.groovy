/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.AchievedBadgeCount
import skills.storage.model.AchievedSkillsCount
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.DayCountItem
import skills.storage.model.UserAchievement

@CompileStatic
interface UserAchievedLevelRepo extends CrudRepository<UserAchievement, Integer> {

    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillId(String userId, @Nullable String projectId, @Nullable String skillId)
    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(String userId, @Nullable String projectId, @Nullable String skillId)

    List<UserAchievement> findAllByUserIdAndNotifiedOrderByCreatedAsc(String userId, String notified)

    @Nullable
    @Query('''select ua.achievedOn from UserAchievement ua where ua.userId= ?1 and ua.projectId=?2 and ua.skillId=?3''')
    Date getAchievedDateByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)

    @Query('''select ua from UserAchievement ua where ua.userId = ?1 and ua.projectId in ?2''')
    List<UserAchievement> findAllByUserAndProjectIds(String userId, Collection<String> projectId)

    @Query('''select ua from UserAchievement ua where ua.userId= ?1 and ua.level is not null''')
    List<UserAchievement> findAllLevelsByUserId(String userId)

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


    @Query(value = '''select new skills.storage.model.DayCountItem(ua.created, count(ua))
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

    @Query(value = '''select count(ua) as totalCount,
                      sum(case when skillDef.startDate is not null and skillDef.endDate is not null then 1 end) as gemCount,
                      sum(case when skillDef.type='GlobalBadge' then 1 end) as globalCount
        from SkillDef skillDef, UserAchievement ua
        where
            ua.level is null and
            ua.userId= :userId and
            skillDef.skillId = ua.skillId and (
                (skillDef.projectId = ua.projectId and skillDef.projectId IN (
                    select s.projectId
                    from Setting s
                    where s.projectId = skillDef.projectId
                      and s.setting = 'production.mode.enabled'
                      and s.value = 'true')
                ) OR 
                (skillDef.projectId is null and ua.projectId is null)
            ) and
            (skillDef.type='Badge' OR skillDef.type='GlobalBadge')''')
    AchievedBadgeCount countAchievedProductionBadgesForUser(@Param('userId') String userId)

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


    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, notified)
            SELECT eventsByUserId.user_id, :projectId, :skillId, :skillRefId, -1, :notified
            FROM (
                SELECT user_id, count(id) eventCount
                FROM user_performed_skill
                WHERE
                      skill_id = :skillId and
                      project_id = :projectId
                GROUP BY user_id
                ) eventsByUserId
            WHERE
                  eventsByUserId.eventCount >= :numOfOccurrences and
                NOT EXISTS (
                        SELECT id FROM user_achievement WHERE project_id = :projectId and skill_id = :skillId and user_id = eventsByUserId.user_id
                    )''', nativeQuery = true)
    @Modifying
    void insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(@Param('projectId') String projectId,
                                                                            @Param('skillId') String skillId,
                                                                            @Param('skillRefId') Integer skillRefId,
                                                                            @Param('numOfOccurrences') int numOfOccurrences,
                                                                            @Param('notified') String notified)

    static interface AchievementItem {
        Date getAchievedOn()
        String getUserId()
        Integer getLevel()
        String getSkillId()
        String getName()
        SkillDef.ContainerType getType()
        String getUserIdForDisplay()
    }

    @Query('''select ua.achievedOn as achievedOn, ua.userId as userId, ua.level as level, ua.skillId as skillId,
            sd.name as name, sd.type as type, uAttrs.userIdForDisplay as userIdForDisplay
            from UserAchievement ua, UserAttrs uAttrs left join SkillDef sd on ua.skillId = sd.skillId 
            where 
                ua.userId = uAttrs.userId and
                ua.projectId = :projectId and
                ua.achievedOn >= :fromDate and
                ua.achievedOn <= :toDate and 
                upper(uAttrs.userIdForDisplay) like UPPER(CONCAT('%', :userNameFilter, '%')) and
                (upper(sd.name) like UPPER(CONCAT('%', :skillNameFilter, '%')) OR (:skillNameFilter = 'ALL')) and
                (ua.level >= :level OR (:level = -1)) and
                (sd.type in (:types) OR (:disableTypes = 'true') OR (ua.skillId is null AND (:includeOverallType = 'true'))) and 
                (ua.skillId is not null OR (:includeOverallType = 'true'))
                ''')
    List<AchievementItem> findAllForAchievementNavigator(
            @Param("projectId") String projectId,
            @Param("userNameFilter") String userNameFilter,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("skillNameFilter") String skillNameFilter,
            @Param("level") Integer level,
            @Param("types") List<SkillDef.ContainerType> types,
            @Param("disableTypes") String disableTypes,
            @Param("includeOverallType") String includeOverallType,
            @Param("pageable") Pageable pageable)

    @Query('''select count(uAttrs) from UserAchievement ua, UserAttrs uAttrs left join SkillDef sd on ua.skillId = sd.skillId 
            where 
                ua.userId = uAttrs.userId and
                ua.projectId = :projectId and
                ua.achievedOn >= :fromDate and
                ua.achievedOn <= :toDate and 
                upper(uAttrs.userIdForDisplay) like UPPER(CONCAT('%', :userNameFilter, '%')) and
                (upper(sd.name) like UPPER(CONCAT('%', :skillNameFilter, '%')) OR (:skillNameFilter = 'ALL')) and
                (ua.level >= :level OR (:level = -1)) and
                (sd.type in (:types) OR (:disableTypes = 'true') OR (ua.skillId is null AND (:includeOverallType = 'true'))) and 
                (ua.skillId is not null OR (:includeOverallType = 'true'))
                ''')
    Integer countForAchievementNavigator(
            @Param("projectId") String projectId,
            @Param("userNameFilter") String userNameFilter,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("skillNameFilter") String skillNameFilter,
            @Param("level") Integer level,
            @Param("types") List<SkillDef.ContainerType> types,
            @Param("disableTypes") String disableTypes,
            @Param("includeOverallType") String includeOverallType)


    static interface SkillAndLevelUserCount {
        String getSkillId()
        Integer getLevel()
        Long getNumberUsers()
    }

    @Query('''select ua.skillId as skillId, ua.level as level, count(ua.id) as numberUsers 
            from UserAchievement as ua, SkillDef as sd 
            where 
                ua.skillId = sd.skillId and
                ua.projectId = :projectId and
                sd.type = :containerType
            group by ua.skillId, ua.level    
           ''')
    List<SkillAndLevelUserCount> countNumUsersPerContainerTypeAndLevel(
            @Param("projectId") String projectId,
            @Param("containerType") SkillDef.ContainerType containerType
    )

    @Query('''select count(distinct ua.userId) from UserAchievement ua
              where ua.projectId = :projectId and
              ua.skillId = :skillId
    ''')
    Long countDisinctUsersAchievingSkill(@Param("projectId")String projectId, @Param("skillId") String skillId)


    static interface SkillDayUserCount {
        Date getDay()
        Long getNumberUsers()
    }

    static interface SkillLevelDayUserCount extends SkillDayUserCount {
        Integer getLevel()
    }

    @Query('''select ua.achievedOn as day, ua.level as level, count(ua.id) as numberUsers 
            from UserAchievement as ua 
            where 
                ua.skillId = :skillId and
                ua.projectId = :projectId
            group by ua.achievedOn, ua.level     
           ''')
    List<SkillLevelDayUserCount> countNumUsersOverTimeAndLevelByProjectIdAndSkillId(
            @Param("projectId") String projectId,
            @Param("skillId") String skillId
    )

    @Query('''select new skills.storage.model.DayCountItem(ua.achievedOn, count(ua.id))
            from UserAchievement as ua 
            where 
                ua.skillId = :skillId and
                ua.projectId = :projectId
            group by ua.achievedOn     
           ''')
    List<DayCountItem> countNumUsersOverTimeByProjectIdAndSkillId(
            @Param("projectId") String projectId,
            @Param("skillId") String skillId
    )

    static interface SkillUsageItem {
        String getSkillId()
        String getSubjectId()
        String getSkillName()
        Integer getNumUserAchieved()
        Integer getNumUsersInProgress()
        Date getLastReported()
        Date getLastAchieved()
    }

    @Query(value = '''select 
                sd.skillId as skillId, 
                sd.name as skillName, 
                sd.subjId as subjectId,
                achievements.usersAchieved as numUserAchieved, 
                performedSkills.userInProgress as numUsersInProgress, 
                achievements.lastAchieved as lastAchieved, 
                performedSkills.lastPerformed as lastReported
            from (
                select skill.skill_id as skillId, skill.name as name, subj.skill_id as subjId
                from skill_definition skill, skill_definition subj, skill_relationship_definition rel
                where
                      subj.id = rel.parent_ref_id and skill.id = rel.child_ref_id and rel.type = 'RuleSetDefinition' and
                      subj.project_id = :projectId and subj.type = 'Subject' and
                      skill.project_id = :projectId and skill.type = 'Skill'
                ) sd
            left join (
                select skill_id, count(distinct user_id) as usersAchieved, max(achieved_on) as lastAchieved from user_achievement where project_id = :projectId group by skill_id
            ) achievements
                on achievements.skill_id = sd.skillId
            left join (
                select skill_id, count(distinct user_id) as userInProgress, max(performed_on) as lastPerformed from user_performed_skill
                where project_id = :projectId
                group by skill_id
            ) performedSkills
                on sd.skillId = performedSkills.skill_id
           ''', nativeQuery = true)
    List<SkillUsageItem> findAllForSkillsNavigator(@Param("projectId") String projectId)

    static interface SkillStatsItem {
        Integer getNumUsersAchieved()
        Date getLastAchieved()
    }

    @Query(value = '''
select count(distinct ua) as numUsersAchieved, max(ua.achievedOn) as lastAchieved
from UserAchievement ua
where ua.projectId = :projectId and ua.skillId = :skillId
''')
    SkillStatsItem calculateNumAchievedAndLastAchieved(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Query('''select count(ua)
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and ua.userId=?1 and 
        skillDef.skillId = ua.skillId and skillDef.projectId = ua.projectId and
        skillDef.type='Skill' ''')
    int countAchievedSkillsForUser(String userId)

    @Query(value = '''select count(ua) as totalCount,
                      sum(case when ua.achievedOn >= (current_date - 30) then 1 end) as monthCount,
                      sum(case when ua.achievedOn >= (current_date - 7) then 1 end) as weekCount,
                      sum(case when ua.achievedOn >= (current_date - 1) then 1 end) as todayCount,
                      max(ua.achievedOn) as lastAchieved
        from SkillDef skillDef, UserAchievement ua
        where
            ua.level is null and
            ua.userId= :userId and
            skillDef.skillId = ua.skillId and
            skillDef.projectId = ua.projectId and
            skillDef.type='Skill' and skillDef.projectId IN (
                select s.projectId
                from Setting s
                where s.projectId = skillDef.projectId
                  and s.setting = 'production.mode.enabled'
                  and s.value = 'true')''')
    AchievedSkillsCount countAchievedProductionSkillsForUserByDayWeekMonth(@Param('userId') String userId)
}
