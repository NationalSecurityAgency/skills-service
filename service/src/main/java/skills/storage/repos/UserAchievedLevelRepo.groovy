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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.BadgeCount
import skills.storage.model.AchievedSkillsCount
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.DayCountItem
import skills.storage.model.UserAchievement
import skills.storage.model.UserTagCount

import java.util.stream.Stream

@CompileStatic
interface UserAchievedLevelRepo extends CrudRepository<UserAchievement, Integer>, JpaSpecificationExecutor<UserAchievement> {

    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillId(String userId, @Nullable String projectId, @Nullable String skillId)
    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(String userId, @Nullable String projectId, @Nullable String skillId)
    List<UserAchievement> findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNullAndAchievedOnAfter(String userId, @Nullable String projectId, @Nullable String skillId, Date achievedOn)

    List<UserAchievement> findAllByUserIdAndNotifiedOrderByCreatedAsc(String userId, String notified)

    @Nullable
    @Query('''select ua.userId from UserAchievement ua where ua.projectId = ?1 and ua.skillId = ?2''')
    List<String> findAllAchievementsForProjectAndSkill(String projectId, String skillId, Pageable pageable)

    @Nullable
    @Query('''select ua.achievedOn from UserAchievement ua where ua.userId= ?1 and ua.projectId=?2 and ua.skillId=?3''')
    Date getAchievedDateByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)

    @Nullable
    @Query('''select ua from UserAchievement ua where ua.userId = ?1 and ua.projectId = ?2 and ua.skillId in ?3''')
    List<UserAchievement> getAchievedDateByUserIdAndProjectIdAndSkillBatch(String userId, String projectId, List<String> skillId)

    @Query('''select ua from UserAchievement ua where ua.userId = ?1 and ua.projectId in ?2''')
    List<UserAchievement> findAllByUserAndProjectIds(String userId, Collection<String> projectId)

    @Query('''select ua from UserAchievement ua where ua.userId= ?1 and ua.level is not null''')
    List<UserAchievement> findAllLevelsByUserId(String userId)

    @Query('''select ua from UserAchievement ua where ua.userId= ?1 and ua.level is not null and ua.skillId is null''')
    List<UserAchievement> findAllProjectLevelsByUserId(String userId)

    List<UserAchievement> findAllByUserIdAndSkillId(String userId, @Nullable String skillId)

    @Query(value='''
    SELECT COUNT(distinct ua.userId)
    FROM UserAchievement ua
    WHERE ua.projectId =?1 AND
          ua.skillId = ?2 AND
          ua.level = ?3 AND
          not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = ?1)
    ''')
    Integer countByProjectIdAndSkillIdAndLevel(String projectId, @Nullable String skillId, int level)

    @Query(value='''
    SELECT COUNT(distinct ua.userId)
    FROM UserAchievement ua
    WHERE ua.projectId =?1 AND
          ua.skillId is null AND
          ua.level = ?2 AND
          not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = ?1)
    ''')
    Integer countByProjectIdAndLevel(String projectId, int level)

    @Query(value='''
    SELECT COUNT(distinct ua.userId)
    FROM UserAchievement ua
        INNER JOIN UserTag AS ut ON ua.userId = ut.userId
    WHERE ua.projectId =?1 AND
          ua.level = ?2 AND
          ut.key = ?3 AND
          ut.value = ?4 AND
          ua.skillId is null AND
          not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = ?1)
    ''')
    Integer countByProjectIdAndLevelAndUserTag(String projectId, int level, String userTagKey, String userTagValue)

    void deleteAllByProjectIdAndUserId(String projectId, String userId)
    void deleteByProjectIdAndSkillId(String projectId, String skillId)
    void deleteByProjectIdAndSkillIdAndUserIdAndLevel(String projectId, @Nullable String skillId, String userId, @Nullable Integer level)

    @Query(value = '''
                select sdParent.skill_id  as skillId,
                       sdChild.skill_id   as childSkillId,
                       sdChild.project_id as childProjectId,
                       ua.skill_id        as childAchievedSkillId
                from skill_definition sdParent,
                     skill_relationship_definition srd,
                     skill_definition sdChild
                         left join user_achievement ua
                                   on sdChild.project_id = ua.project_id and sdChild.skill_id = ua.skill_id and
                                      ua.user_id = :userId
                where srd.parent_ref_id = sdParent.id
                  and srd.child_ref_id = sdChild.id
                  and (
                    (sdParent.project_id = :projectId and sdParent.skill_id = :skillId)
                    OR
                    sdParent.id in (
                        select badge.id
                        from skill_definition badge,
                            skill_relationship_definition badge_to_skill,
                            skill_definition skill
                        where badge_to_skill.parent_ref_id = badge.id
                         and badge_to_skill.child_ref_id = skill.id
                         and badge_to_skill.type = 'BadgeRequirement'
                         and skill.project_id = :projectId
                         and skill.skill_id = :skillId)
                  )
                  and srd.type = :relationshipType
    ''', nativeQuery = true)
    List<ChildWithAchievementsInfo> findChildrenAndTheirAchievements(@Param("userId") String userId,
                                                                     @Param("projectId") String projectId,
                                                                     @Param("skillId") String skillId,
                                                                     @Param("relationshipType") String relationshipType)



    static interface ChildWithAchievementsInfo {
        String getSkillId()

        String getChildProjectId()
        String getChildSkillId()

        String getChildAchievedSkillId()
    }

    @Query(''' select sdChild
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and ua.id is null and srd.type=?4''')
    List<SkillDef> findNonAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select count(sdChild.id)
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and
      sdParent.projectId=?2 and sdParent.skillId=?3 and ua.id is null and srd.type=?4''')
    Long countNonAchievedChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query(''' select count(sdChild.id)
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
    left join UserAchievement ua on sdChild.id = ua.skillRefId and ua.userId=?1
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and
      sdParent.projectId is null and sdParent.skillId=?2 and ua.id is null and srd.type=?3''')
    Long countNonAchievedGlobalSkills(String userId, String skillId, SkillRelDef.RelationshipType type)

    @Query('''select sdParent.name as label, count(ua) as countRes
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and sdChild.skillId=ua.skillId and ua.level is null and 
      sdParent.projectId=?1 and sdParent.type=?2 group by sdParent.name''')
    List<LabelCountInfo> getUsageFacetedViaSubject(String projectId, SkillDef.ContainerType subjectType, Pageable pageable)

    @Query('''select sdChild.name as label, count(ua) as countRes
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
      left outer join UserAchievement ua ON sdChild.skillId=ua.skillId 
      where srd.parent.id = sdParent.id and srd.child.id=sdChild.id and ua.level is null and 
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

    static interface AchievementInfo {
        String getName()
        String getId()
        Date getAchievedOn()
        SkillDef.ContainerType getType()
    }

    @Query('''select skillDef.name as name, 
        skillDef.skillId as id, 
        ua.achievedOn as achievedOn,
        skillDef.type as type
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and 
        ua.userId=?1 and
        ua.achievedOn > ?4 and 
        skillDef.skillId = ua.skillId and 
        skillDef.projectId = ua.projectId and 
        skillDef.projectId=?2 and 
        skillDef.type in ?3''')
    List<AchievementInfo> getUserAchievementsAfterDate(String userId, String projectId, List<SkillDef.ContainerType> containerTypes, Date mustBeAfterThisDate)

    @Query('''select badgeDef.name as name, 
        badgeDef.skillId as id, 
        ua.achievedOn as achievedOn,
        badgeDef.type as type
      from SkillDef badgeDef, SkillDef skillDef, SkillRelDef rel, UserAchievement ua
      where 
        ua.level is null and 
        ua.userId=?1 and
        ua.achievedOn > ?3 and 
        badgeDef = rel.parent and
        skillDef = rel.child and 
        rel.type = 'BadgeRequirement' and
        ua.projectId is null and
        ua.skillRefId = badgeDef.id and 
        skillDef.projectId=?2 and 
        badgeDef.type = 'GlobalBadge' ''')
    List<AchievementInfo> getUserGlobalBadgeAchievementsAfterDate(String userId, String projectId, Date mustBeAfterThisDate)

    @Query('''select count(ua)
      from SkillDef skillDef, UserAchievement ua 
      where 
        ua.level is null and ua.userId=?1 and 
        skillDef.skillId = ua.skillId and 
        skillDef.projectId is null and 
        skillDef.type=?2''')
    int countAchievedGlobalForUser(String userId, SkillDef.ContainerType containerType)

    @Query(value='''
        SELECT COUNT(ua.id)
        FROM user_achievement ua
        JOIN skill_definition skillDef on ua.skill_ref_id = skillDef.id
        WHERE
            ua.level IS null AND
            ua.user_id = ?1 AND (
                skillDef.type='GlobalBadge'
                AND (
                     exists (
                         SELECT true
                         FROM global_badge_level_definition gbld
                         WHERE gbld.skill_ref_id = skillDef.id AND gbld.project_id = ?2
                     )
                OR (
                        skillDef.id IN (
                         SELECT srd.parent_ref_id FROM skill_relationship_definition srd JOIN skill_definition ssd ON srd.child_ref_id = ssd.id AND ssd.project_id = ?2
                        )
                     )
                )
            )''', nativeQuery = true)
    int countAchievedGlobalBadgeForUserIntersectingProjectId(String userId, String projectId)

    @Query(value='''
        SELECT ua.id FROM user_achievement ua
        JOIN skill_definition skillDef on ua.skill_ref_id = skillDef.id
        WHERE
            ua.level IS null AND
            ua.user_id = ?1 AND (
                skillDef.type='GlobalBadge'
                AND (
                     exists (
                         SELECT true
                         FROM global_badge_level_definition gbld
                         WHERE gbld.skill_ref_id = skillDef.id AND gbld.project_id = ?2
                     )
                OR (
                        skillDef.id IN (
                         SELECT srd.parent_ref_id FROM skill_relationship_definition srd JOIN skill_definition ssd ON srd.child_ref_id = ssd.id AND ssd.project_id = ?2
                        )
                     )
                )
            )''', nativeQuery = true)
    List<Integer> getAchievedGlobalBadgeForUserIntersectingProjectId(String userId, String projectId)

    @Query('''select count(ua) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent.id = sdParent.id and  srd.child.id=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    int countAchievedChildren(String userId, @Nullable String projectId, String skillId, SkillRelDef.RelationshipType type)

    @Query('''select sum(ua.pointsWhenAchieved) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent.id = sdParent.id and  srd.child.id=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId=?2 and sdParent.skillId=?3 and srd.type=?4''')
    int sumAchievedChildrenPoints(String userId, @Nullable String projectId, String skillId, SkillRelDef.RelationshipType type)


    @Query('''select count(ua) 
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserAchievement ua
      where 
      srd.parent.id = sdParent.id and  srd.child.id=sdChild.id and
      sdChild.projectId = ua.projectId and sdChild.skillId = ua.skillId and ua.userId=?1 and 
      sdParent.projectId is null and sdParent.skillId=?2 and srd.type=?3''')
    int countAchievedGlobalSkills(String userId, String skillId, SkillRelDef.RelationshipType type)


    @Query(value = '''select ua.created as day, count(ua) as count
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

    @Query(value = '''WITH mp AS (
            SELECT s.project_id AS project_id
            FROM settings s, users uu, settings s1
            WHERE s.setting = 'my_project'
              AND uu.user_id=?1
              AND uu.id = s.user_ref_id
              AND s.project_id = s1.project_id
              AND s1.setting = 'production.mode.enabled'
              AND s1.value = 'true'
        )
        SELECT COUNT(ua.id) AS totalCount,
               COALESCE(SUM(CASE WHEN skillDef.start_date IS NOT null and skillDef.end_date IS NOT null THEN 1 END), 0) AS gemCount,
               COALESCE(SUM(CASE WHEN skillDef.type='GlobalBadge' THEN 1 END), 0) AS globalCount
        FROM user_achievement ua
        JOIN skill_definition skillDef on ua.skill_ref_id = skillDef.id
        WHERE
            ua.level IS null AND
            ua.user_id = ?1 AND (
            (
                skillDef.type = 'Badge' AND
                skillDef.project_id IN (
                 SELECT project_id FROM mp
                )
            ) OR
            (
                skillDef.type='GlobalBadge'
                AND (
                     exists (
                         SELECT true
                         FROM global_badge_level_definition gbld
                         WHERE gbld.skill_ref_id = skillDef.id AND gbld.project_id in (select project_id from mp)
                     )
                OR (
                        skillDef.id IN (
                         SELECT srd.parent_ref_id FROM skill_relationship_definition srd JOIN skill_definition ssd ON srd.child_ref_id = ssd.id AND ssd.project_id IN (select project_id FROM mp)
                        )
                     )
                )
            )
    )''', nativeQuery = true)
    BadgeCount countAchievedProductionBadgesForUser(@Param('userId') String userId)

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

    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, notified)
            SELECT achievementsByUserId.user_id, :projectId, :groupSkillId, :groupSkillRefId, -1, :notified
            FROM (
                SELECT user_id, count(id) achievementCount
                FROM user_achievement
                WHERE
                      skill_id IN :childSkillIds and
                      project_id = :projectId
                GROUP BY user_id
                ) achievementsByUserId
            WHERE
                  achievementsByUserId.achievementCount >= :numSkillsRequired and
                NOT EXISTS (
                        SELECT id FROM user_achievement WHERE project_id = :projectId and skill_id = :groupSkillId and user_id = achievementsByUserId.user_id
                    )''', nativeQuery = true)
    @Modifying
    void insertUserAchievementWhenDecreaseOfNumSkillsRequiredCausesUsersToAchieve(@Param('projectId') String projectId,
                                                                                  @Param('groupSkillId') String groupSkillId,
                                                                                  @Param('groupSkillRefId') Integer groupSkillRefId,
                                                                                  @Param('childSkillIds') List<String> childSkillIds,
                                                                                  @Param('numSkillsRequired') int numSkillsRequired,
                                                                                  @Param('notified') String notified)

    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, notified)
            SELECT achievementsByUserId.user_id, :projectId, :groupSkillId, :groupSkillRefId, -1, :notified
            FROM (
                SELECT user_id, count(id) achievementCount
                FROM user_achievement
                WHERE
                      skill_id IN (select sd.skill_id
                                    from skill_relationship_definition srd, skill_definition sd
                                    where srd.parent_ref_id = :groupSkillRefId and sd.id = srd.child_ref_id and srd.type='SkillsGroupRequirement') and
                      project_id = :projectId
                GROUP BY user_id
                ) achievementsByUserId
            WHERE
                  achievementsByUserId.achievementCount >= :numSkillsRequired and
                NOT EXISTS (
                        SELECT id FROM user_achievement WHERE project_id = :projectId and skill_id = :groupSkillId and user_id = achievementsByUserId.user_id
                    )''', nativeQuery = true)
    @Modifying
    void identifyAndAddGroupAchievements(@Param('projectId') String projectId,
                                         @Param('groupSkillId') String groupSkillId,
                                         @Param('groupSkillRefId') Integer groupSkillRefId,
                                         @Param('numSkillsRequired') int numSkillsRequired,
                                         @Param('notified') String notified)

    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, notified)
            SELECT achievementsByUserId.user_id, :projectId, :groupSkillId, :groupSkillRefId, -1, :notified
            FROM (
                SELECT user_id, count(id) achievementCount
                FROM user_achievement
                WHERE
                      skill_id IN (select sd.skill_id
                                    from skill_relationship_definition srd, skill_definition sd
                                    where srd.parent_ref_id = :groupSkillRefId and sd.id = srd.child_ref_id and srd.type='SkillsGroupRequirement') and
                      user_id = :userId and
                      project_id = :projectId
                GROUP BY user_id
                ) achievementsByUserId
            WHERE
                  achievementsByUserId.achievementCount >= :numSkillsRequired and
                NOT EXISTS (
                        SELECT id FROM user_achievement WHERE project_id = :projectId and skill_id = :groupSkillId and user_id = achievementsByUserId.user_id
                    )''', nativeQuery = true)
    @Modifying
    void identifyAndAddGroupAchievementsForSingleUser(@Param('userId') String userId,
                                         @Param('projectId') String projectId,
                                         @Param('groupSkillId') String groupSkillId,
                                         @Param('groupSkillRefId') Integer groupSkillRefId,
                                         @Param('numSkillsRequired') int numSkillsRequired,
                                         @Param('notified') String notified)

    @Modifying
    @Query(value = '''
        WITH skills as (
            select case when s.copied_from_skill_ref is not null then s.copied_from_skill_ref else s.id end as id,
                s.point_increment as point_increment
            from skill_definition s
            where s.project_id = :projectId
                and s.type = 'Skill'
        )
        INSERT INTO user_achievement (user_id, level, achieved_on, points_when_achieved, project_id, notified)
        SELECT user_id, :level, min(performed_on), min(runningSum) points_when_achieved, :projectId, 'false'
        FROM (
                 SELECT ups.user_id,
                        ups.performed_on,
                        ups.created,
                        SUM(skills.point_increment) over (partition by user_id order by ups.performed_on, ups.created) as runningSum
                 FROM user_performed_skill ups, skills
                 WHERE skills.id = ups.skill_ref_id
                   and not exists (select 1 from user_achievement ua where ua.user_id = ups.user_id and ua.project_id = :projectId and ua.skill_ref_id is null and ua.level = :level)
             ) as t
        where t.runningSum >= :fromPoints
        group by user_id''', nativeQuery = true)
    int identifyAndAddProjectLevelAchievementsForALevel(
            @Param('projectId') String projectId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    @Query(value = '''
        WITH skills as (
            select case when s.copied_from_skill_ref is not null then s.copied_from_skill_ref else s.id end as id,
                s.point_increment as point_increment
            from skill_definition s
            where s.project_id = :projectId
                and s.type = 'Skill'
        )
        INSERT INTO user_achievement (user_id, level, achieved_on, points_when_achieved, project_id, notified)
        SELECT user_id, :level, min(performed_on), min(runningSum) points_when_achieved, :projectId, 'false'
        FROM (
                 SELECT ups.user_id,
                        ups.performed_on,
                        ups.created,
                        SUM(skills.point_increment) over (partition by user_id order by ups.performed_on, ups.created) as runningSum
                 FROM user_performed_skill ups, skills
                 WHERE skills.id = ups.skill_ref_id and ups.user_id = :userId
                   and not exists (select 1 from user_achievement ua where ua.user_id = ups.user_id and ua.project_id = :projectId and ua.skill_ref_id is null and ua.level = :level)
             ) as t
        where t.runningSum >= :fromPoints
        group by user_id''', nativeQuery = true)
    int identifyAndAddProjectLevelAchievementsForALevelAndSingleUser(
            @Param('userId') String userId,
            @Param('projectId') String projectId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)


    @Modifying
    @Query(value = '''
        WITH skills as (
          select case when child.copied_from_skill_ref is not null then child.copied_from_skill_ref else child.id end as id,
                 child.point_increment as point_increment
                    from skill_relationship_definition rel,
                         skill_definition child
                    where rel.parent_ref_id = :subjectRefId
                      and rel.child_ref_id = child.id
                      and rel.type in ('RuleSetDefinition', 'GroupSkillToSubject')
                      and child.type = 'Skill'
        )
        INSERT INTO user_achievement (user_id, level, achieved_on, points_when_achieved, project_id, skill_id, skill_ref_id, notified)
        SELECT user_id, :level, min(performed_on), min(runningSum) points_when_achieved, :projectId, :subjectId, :subjectRefId, 'false'
        FROM (
                 SELECT ups.user_id,
                        ups.performed_on,
                        ups.created,
                        SUM(skills.point_increment) over (partition by user_id order by ups.performed_on, ups.created) as runningSum
                 FROM user_performed_skill ups, skills
                 WHERE skills.id = ups.skill_ref_id
                   and not exists (select 1 from user_achievement ua where ua.user_id = ups.user_id and ua.project_id = :projectId and ua.skill_ref_id = :subjectRefId and ua.level = :level)
             ) as t
        where t.runningSum >= :fromPoints
        group by user_id''', nativeQuery = true)
    int identifyAndAddSubjectLevelAchievementsForALevel(
            @Param('projectId') String projectId,
            @Param('subjectId') String subjectId,
            @Param('subjectRefId') Integer subjectRefId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    @Query(value = '''
        WITH skills as (
          select case when child.copied_from_skill_ref is not null then child.copied_from_skill_ref else child.id end as id,
                 child.point_increment as point_increment
                    from skill_relationship_definition rel,
                         skill_definition child
                    where rel.parent_ref_id = :subjectRefId
                      and rel.child_ref_id = child.id
                      and rel.type in ('RuleSetDefinition', 'GroupSkillToSubject')
                      and child.type = 'Skill'
        )
        INSERT INTO user_achievement (user_id, level, achieved_on, points_when_achieved, project_id, skill_id, skill_ref_id, notified)
        SELECT user_id, :level, min(performed_on), min(runningSum) points_when_achieved, :projectId, :subjectId, :subjectRefId, 'false'
        FROM (
                 SELECT ups.user_id,
                        ups.performed_on,
                        ups.created,
                        SUM(skills.point_increment) over (partition by user_id order by ups.performed_on, ups.created) as runningSum
                 FROM user_performed_skill ups, skills
                 WHERE skills.id = ups.skill_ref_id and ups.user_id = :userId
                   and not exists (select 1 from user_achievement ua where ua.user_id = ups.user_id and ua.project_id = :projectId and ua.skill_ref_id = :subjectRefId and ua.level = :level)
             ) as t
        where t.runningSum >= :fromPoints
        group by user_id''', nativeQuery = true)
    int identifyAndAddSubjectLevelAchievementsForALevelForASingleUser(
            @Param('userId') String userId,
            @Param('projectId') String projectId,
            @Param('subjectId') String subjectId,
            @Param('subjectRefId') Integer subjectRefId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)


    @Modifying
    @Query(value = '''delete
                from user_achievement uaOuter
                where uaOuter.id = ANY (select ua.id
                        from user_achievement ua
                                 left join user_points up
                                           on (ua.skill_ref_id = up.skill_ref_id and ua.user_id = up.user_id)
                        where ua.skill_ref_id = :subjectRefId
                          and ua.level = :level
                          and (up.points < :fromPoints or up.points is null)
                        )''', nativeQuery = true)
    int removeSubjectLevelAchievementsIfUsersDoNotQualify(
            @Param('subjectRefId') Integer subjectRefId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    @Query(value = '''delete
                from user_achievement uaOuter
                where uaOuter.id = ANY (select ua.id
                        from user_achievement ua
                                 left join user_points up
                                           on (ua.skill_ref_id = up.skill_ref_id and ua.user_id = up.user_id)
                        where ua.skill_ref_id = :subjectRefId
                          and ua.user_id = :userId
                          and ua.level = :level
                          and (up.points < :fromPoints or up.points is null)
                        )''', nativeQuery = true)
    int removeSubjectLevelAchievementsIfThisUserDoesNotQualify(
            @Param('userId') String userId,
            @Param('subjectRefId') Integer subjectRefId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    @Query(value = '''delete
                from user_achievement uaOuter
                where uaOuter.id = ANY (select ua.id
                        from user_achievement ua
                                 left join user_points up
                                           on (ua.project_id = up.project_id 
                                                   and ua.skill_ref_id is null 
                                                   and up.skill_ref_id is null
                                                   and ua.user_id = up.user_id)
                        where ua.project_id = :projectId 
                          and ua.skill_ref_id is null
                          and ua.level = :level
                          and (up.points < :fromPoints or up.points is null)
                        )''', nativeQuery = true)
    int removeProjectLevelAchievementsIfUsersDoNotQualify(
            @Param('projectId') String projectId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    @Query(value = '''delete
                from user_achievement uaOuter
                where uaOuter.id = ANY (select ua.id
                        from user_achievement ua
                                 left join user_points up
                                           on (ua.project_id = up.project_id 
                                                   and ua.skill_ref_id is null 
                                                   and up.skill_ref_id is null
                                                   and ua.user_id = :userId
                                                   and ua.user_id = up.user_id)
                        where ua.project_id = :projectId 
                          and ua.user_id = :userId
                          and ua.skill_ref_id is null
                          and ua.level = :level
                          and (up.points < :fromPoints or up.points is null)
                        )''', nativeQuery = true)
    int removeProjectLevelAchievementsIfUserDoesNotQualify(
            @Param('userId') String userId,
            @Param('projectId') String projectId,
            @Param('level') Integer level,
            @Param('fromPoints') Integer fromPointsExclusive)

    @Modifying
    int deleteAllBySkillRefId(Integer skillRefId)

    @Modifying
    int deleteAllBySkillRefIdAndUserId(Integer skillRefId, String userId)

    @Modifying
    int deleteAllBySkillRefIdInAndUserId(List<Integer> skillRefId, String userId)

    static interface AchievementItem {
        Date getAchievedOn()

        String getUserId()

        Integer getLevel()

        String getSkillId()

        String getName()

        SkillDef.ContainerType getType()

        String getUserIdForDisplay()

        String getFirstName()

        String getLastName()

        String getUserTag()
    }

    @Query('''select ua.achievedOn as achievedOn, ua.userId as userId, ua.level as level, ua.skillId as skillId,
            sd.name as name, sd.type as type, uAttrs.userIdForDisplay as userIdForDisplay, uAttrs.firstName as firstName, uAttrs.lastName as lastName, ut.value as userTag
            from UserAttrs uAttrs
            JOIN  UserAchievement ua ON uAttrs.userId = ua.userId 
                left join SkillDef sd on ua.skillRefId = sd.id 
            LEFT JOIN (SELECT ut.userId userId, max(ut.value) AS value FROM UserTag ut WHERE ut.key = :usersTableAdditionalUserTagKey group by ut.userId) ut ON ut.userId=ua.userId
            where 
                ua.userId = uAttrs.userId and
                ua.projectId = :projectId and
                ua.achievedOn >= :fromDate and
                ua.achievedOn <= :toDate and 
                (ua.skillId not like '%STREUSESKILLST%' OR ua.skillId is null) and
                lower(uAttrs.userIdForDisplay) like lower(CONCAT('%', :userNameFilter, '%')) and
                (lower(sd.name) like lower(CONCAT('%', :skillNameFilter, '%')) OR (:skillNameFilter = 'ALL')) and
                (ua.level >= :level OR (:level = -1)) and
                (sd.type in (:types) OR (:disableTypes = 'true') OR (ua.skillId is null AND (:includeOverallType = 'true'))) and 
                (ua.skillId is not null OR (:includeOverallType = 'true')) and
                not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
                ''')
    Stream<AchievementItem> findAllForAchievementNavigator(
            @Param("projectId") String projectId,
            @Param("userNameFilter") String userNameFilter,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("skillNameFilter") String skillNameFilter,
            @Param("level") Integer level,
            @Param("types") List<SkillDef.ContainerType> types,
            @Param("disableTypes") String disableTypes,
            @Param("includeOverallType") String includeOverallType,
            @Param("usersTableAdditionalUserTagKey") String usersTableAdditionalUserTagKey,
            @Param("pageable") Pageable pageable)

    @Query('''
            SELECT COUNT(uAttrs)
            FROM  UserAttrs uAttrs
            JOIN  UserAchievement ua ON uAttrs.userId = ua.userId
            LEFT JOIN SkillDef sd ON ua.skillRefId = sd.id
            where 
                ua.projectId = :projectId and
                ua.achievedOn >= :fromDate and
                ua.achievedOn <= :toDate and 
                (ua.skillId not like '%STREUSESKILLST%' OR ua.skillId is null) and
                lower(uAttrs.userIdForDisplay) like lower(CONCAT('%', :userNameFilter, '%')) and
                (lower(sd.name) like lower(CONCAT('%', :skillNameFilter, '%')) OR (:skillNameFilter = 'ALL')) and
                (ua.level >= :level OR (:level = -1)) and
                (sd.type in (:types) OR (:disableTypes = 'true') OR (ua.skillId is null AND (:includeOverallType = 'true'))) and 
                (ua.skillId is not null OR (:includeOverallType = 'true')) and
                 not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
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

    static interface LevelAndTagCount {
        String getSkillId()
        String getUserTag()
        Integer getLevel()
        Long getNumberUsers()
    }

    @Query('''SELECT ua.skillId AS skillId, ua.level AS level, COUNT(ua.id) AS numberUsers
            FROM UserAchievement AS ua
            INNER JOIN SkillDef AS sd ON ua.skillId = sd.skillId AND ua.projectId = sd.projectId
            WHERE ua.projectId = :projectId AND sd.type = :containerType AND 
            not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
            GROUP BY ua.skillId, ua.level   
           ''')
    List<SkillAndLevelUserCount> countNumUsersPerContainerTypeAndLevel(
            @Param("projectId") String projectId,
            @Param("containerType") SkillDef.ContainerType containerType
    )

    @Query('''select ua.skillId as skillId, ua.level as level, count(ua.id) as numberUsers, ut.value as userTag 
            from UserAchievement as ua 
            join UserTag ut on ut.userId = ua.userId
            where 
                ua.skillId = :subjectId and
                ua.projectId = :projectId and
                ut.key = :userTagKey and
                ua.level is not null and
                not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
            group by ua.skillId, ua.level, ut.value
           ''')
    List<LevelAndTagCount> countNumUsersPerSubjectTagAndLevel(
            @Param("projectId") String projectId,
            @Param("subjectId") String subjectId,
            @Param("userTagKey") String userTagKey
    )

    @Query('''
        select count(distinct ua.userId) 
        from UserAchievement ua
        where ua.projectId = :projectId and
              ua.skillId = :skillId and
              not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
    ''')
    Long countDistinctUsersAchievingSkill(@Param("projectId") String projectId, @Param("skillId") String skillId)


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
                ua.projectId = :projectId and
                not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
            group by ua.achievedOn, ua.level     
           ''')
    List<SkillLevelDayUserCount> countNumUsersOverTimeAndLevelByProjectIdAndSkillId(
            @Param("projectId") String projectId,
            @Param("skillId") String skillId
    )

    @Query('''select ua.achievedOn as day, count(ua.id) as count
            from UserAchievement as ua 
            where 
                ua.skillId = :skillId and
                ua.projectId = :projectId and
                not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId)
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
                      subj.id = rel.parent_ref_id and skill.id = rel.child_ref_id and rel.type in ('RuleSetDefinition', 'GroupSkillToSubject') and
                      subj.project_id = :projectId and subj.type = 'Subject' and
                      skill.project_id = :projectId and skill.type = 'Skill' and 
                      skill.skill_id not like '%STREUSESKILLST%'
                ) sd
            left join (
                select ua.skill_id, count(distinct ua.user_id) as usersAchieved, max(ua.achieved_on) as lastAchieved 
                from user_achievement ua
                where ua.project_id = :projectId 
                AND not exists (select 1 from archived_users au where au.user_id = ua.user_id and au.project_id = :projectId) 
                group by ua.skill_id
            ) achievements
                on achievements.skill_id = sd.skillId
            left join (
                select skill.skill_id, count(distinct ups.user_id) as userInProgress, max(ups.performed_on) as lastPerformed
                from skill_definition skill,
                     user_performed_skill ups
                where skill.project_id = :projectId
                  and skill.type = 'Skill'
                  and skill.enabled = 'true'
                  and ups.skill_ref_id = case when skill.copied_from_skill_ref is not null then skill.copied_from_skill_ref else skill.id end
                  and not exists (select 1 from archived_users au where au.user_id = ups.user_id and au.project_id = :projectId)
                group by skill.skill_id
            ) performedSkills
                on sd.skillId = performedSkills.skill_id order by sd.skillId
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

    @Query(value = '''
select count(distinct ua) as userCount, ut.value as tagValue
from UserAchievement ua
join UserTag ut on ut.userId = ua.userId
where ua.projectId = :projectId and ua.skillId = :skillId and ut.key = :userTagKey
and not exists (select 1 from ArchivedUser au where au.userId = ua.userId and au.projectId = :projectId) 
group by ut.value
''')
    List<UserTagCount> countNumAchievedByUserTag(@Param("projectId") String projectId, @Param("skillId") String skillId, @Param("userTagKey") String userTagKey)

    @Nullable
    @Query(value = '''
select count(distinct ua) from UserAchievement ua where ua.projectId = :projectId and ua.skillId = :skillId
''')
    int countNumAchievedForSkill(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Query(value = '''select count(ua) as totalCount,
                      sum(case when ua.achieved_on >= (current_date - 30) then 1 end) as monthCount,
                      sum(case when ua.achieved_on >= (current_date - 7) then 1 end) as weekCount,
                      sum(case when ua.achieved_on >= (current_date - 1) then 1 end) as todayCount,
                      max(ua.achieved_on) as lastAchieved
        from skill_definition skillDef, user_achievement ua
        where
            ua.level is null and
            ua.user_id= :userId and
            skillDef.skill_id = ua.skill_id and
            skillDef.project_id = ua.project_id and
            skillDef.type='Skill' and 
            skillDef.project_id IN 
            (
                select s.project_id
                from settings s
                where s.project_id = skillDef.project_id
                  and s.setting = 'production.mode.enabled'
                  and s.value = 'true'
            ) and 
            skillDef.project_id IN (
                select s.project_id
                from settings s, users uu
                where (s.setting = 'my_project' and uu.user_id=:userId and uu.id = s.user_ref_id and s.project_id = skillDef.project_id)
            ) 
    ''', nativeQuery = true)
    AchievedSkillsCount countAchievedProductionSkillsForUserByDayWeekMonth(@Param('userId') String userId)

    @Modifying
    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, achieved_on)
            SELECT ua.user_id, toDef.project_id, toDef.skill_id, toDef.id, ua.points_when_achieved, ua.achieved_on
            FROM user_achievement ua,
                 skill_definition toDef
            WHERE toDef.copied_from_skill_ref = ua.skill_ref_id
              and ua.skill_ref_id in (:fromSkillRefIds)
              and (not exists(
                    select 1
                    from settings s
                    where s.project_id = toDef.project_id
                      and s.setting = 'user_community' and s.value = 'true'
                   ) or (exists(
                    select 1
                    from user_tags ut
                    where ut.user_id = ua.user_id
                      and ut.key = :userCommunityUserTagKey and ut.value = :userCommunityUserTagValue
                   )))     
              and not exists(
                    select 1
                    from user_achievement innerTable
                    where toDef.project_id = innerTable.project_id
                      and ua.user_id = innerTable.user_id
                      and toDef.skill_id = innerTable.skill_id
                )
            ''', nativeQuery = true)
    void copySkillAchievementsToTheImportedProjects(@Param('fromSkillRefIds') List<Integer> fromSkillRefIds, @Param('userCommunityUserTagKey') String userCommunityUserTagKey, @Param('userCommunityUserTagValue') String userCommunityUserTagValue)

    @Modifying
    @Query(value = '''INSERT INTO user_achievement(user_id, project_id, skill_id, skill_ref_id, points_when_achieved, achieved_on)
            SELECT ua.user_id, toDef.project_id, toDef.skill_id, toDef.id, ua.points_when_achieved, ua.achieved_on
            FROM user_achievement ua,
                 skill_definition toDef
            WHERE toDef.copied_from_skill_ref = ua.skill_ref_id
              and ua.skill_ref_id in (:fromSkillRefIds)
              and ua.user_id = :userId
              and not exists(
                    select 1
                    from user_achievement innerTable
                    where toDef.project_id = innerTable.project_id
                      and ua.user_id = innerTable.user_id
                      and toDef.skill_id = innerTable.skill_id
                )
            ''', nativeQuery = true)
    void copyForSingleUserSkillAchievementsToTheImportedProjects(@Param('userId') userId, @Param('fromSkillRefIds') List<Integer> fromSkillRefIds)

    @Modifying
    @Query('''delete from UserAchievement ua where not exists (select 1 from UserPoints up where up.userId = ua.userId and up.projectId = ua.projectId) and ua.projectId = :projectId''')
    void deleteAchievementsWithNoPoints(@Param("projectId") String projectId)
}
