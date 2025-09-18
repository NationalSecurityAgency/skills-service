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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserTagCount

@CompileStatic
interface UserPerformedSkillRepo extends JpaRepository<UserPerformedSkill, Integer> {

    // find an "exact" performed event;
    // may have more than 1 event with the same exact timestamp, this happens when multiple events may fall
    // within configured time window and client send the same timestamp (example UI calendar control)
    @Query('''select u from UserPerformedSkill u
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?1 and
                s.skillId = ?2 and
                s.enabled = 'true'
              ) and
              u.userId = ?3 and
              u.performedOn = ?4''')
    @Nullable
    List<UserPerformedSkill> findAllByProjectIdAndSkillIdAndUserIdAndPerformedOn(String projectId, String skillId, String userId, Date performedOn)

    @Query('''select u from UserPerformedSkill u
              where
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?1 and
                s.enabled = 'true'
              ) and
              u.userId = ?2 and
              u.id in ?3''')
    @Nullable
    List<UserPerformedSkill> findAllByProjectIdAndUserIdAndIdList(String projectId, String userId, List<Integer> ids)

    @Query('''select count(distinct(u.userId)) as userCount, ut.value as tagValue 
              from UserPerformedSkill u
              join UserTag ut on ut.userId = u.userId
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?1 and
                s.skillId = ?2 and
                s.enabled = 'true'
              ) and ut.key = ?3
              and not exists (select 1 from ArchivedUser au where au.userId = u.userId and au.projectId = ?1)
              group by ut.value order by userCount desc''')
    @Nullable
    List<UserTagCount> findAllByProjectIdAndSkillIdAndUserTag(String projectId, String skillId, String userTagKey, Pageable pageable)

    @Nullable
    @Query('''select min(u.performedOn) from UserPerformedSkill u where u.projectId = ?1 and u.userId = ?2 and u.skillId in ?3''')
    Date findFirstPerformedSkill(String projectId, String userId, List<String> skillIds)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)
    Long deleteBySkillRefId(Integer skillRefId)
    void deleteAllByUserIdAndProjectId(String userId, String projectId)
    void deleteAllByUserIdAndSkillRefIdIn(String userId, List<Integer> skillRefIds)

    @Query('''select count(u.id) from UserPerformedSkill u
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?2 and
                s.enabled = 'true' and
                (
                    lower(s.skillId) like lower(concat('%',?3,'%'))
                    OR lower(s.name) like lower(concat('%',?3,'%'))
                ) 
              ) and
              u.userId = ?1''')
    Long countByUserIdAndProjectIdAndSkillIdIgnoreCaseContaining(String userId, String projectId, String skillId)

    @Query('''select count(u.id) from UserPerformedSkill u
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?2 and
                s.enabled = 'true'
              ) and
              u.userId = ?1''')
    Long countByUserIdAndProjectId(String userId, String projectId)

    @Query('''select count(u.id) from UserPerformedSkill u
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?2 and
                s.skillId = ?3 and
                s.enabled = 'true'
              ) and
              u.userId = ?1''')
    Long countByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)

    @Query('''select count(u.id) from UserPerformedSkill u
              where 
              u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?2 and
                s.skillId = ?3 and
                s.enabled = 'true'
              ) and
              u.userId = ?1 and 
              u.performedOn > ?4 and
              u.performedOn < ?5''')
    Long countByUserIdAndProjectIdAndSkillIdAndPerformedOnGreaterThanAndPerformedOnLessThan(String userId, String projectId, String skillId, Date startDate, Date endDate)

    Boolean existsByUserId(String userId)

    @Query('''select true from UserPerformedSkill up 
                where up.skillRefId in (
                    select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id 
                    from SkillDef s 
                    where s.projectId=?2 and
                    s.enabled = 'true'
                ) and
                up.userId = ?1''')
    Boolean existsByProjectIdAndUserId(String userId, String projectId)

    static interface PerformedSkillQRes {
        String getId()
        String getProjectId()
        String getSkillName()
        String getSkillId()
        Date getPerformedOn()
    }
    @Query('''select u.projectId as projectId, s.name as skillName, u.skillId as skillId, u.performedOn as performedOn, u.id as id
              from UserPerformedSkill u, SkillDef s
              where u.skillRefId in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s
                where s.projectId = ?2 and
                s.enabled = 'true' and
                (
                    lower(s.skillId) like lower(concat('%',?3,'%')) 
                    OR lower(s.name) like lower(concat('%',?3,'%'))
                )
              ) 
              and u.userId = ?1
              and u.skillRefId = s.id''')
    List<PerformedSkillQRes> findByUserIdAndProjectIdAndSkillIdIgnoreCaseContaining(String userId, String projectId, String skillId, Pageable pageable)

    @Query('''SELECT COUNT(DISTINCT p.skillId) from UserPerformedSkill p where p.userId = ?2 
            and p.skillRefId in (
                select case when copiedFrom is not null then copiedFrom else id end as id 
                from SkillDef 
                where type = 'Skill' and projectId = ?1 and
                enabled = 'true'
            )'''
    )
    Integer countDistinctSkillIdByProjectIdAndUserId(String projectId, String userId)

    @Query('''SELECT COUNT(DISTINCT p.userId) 
                from UserPerformedSkill p
                where p.skillRefId in (
                    select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id
                    from SkillDef s
                    where 
                    s.projectId=?1 and 
                    s.skillId = ?2 and
                    s.enabled = 'true'
                ) AND not exists (select 1 from ArchivedUser au where au.userId = p.userId and au.projectId = ?1)''')
    Integer countDistinctUserIdByProjectIdAndSkillId(String projectId, String skillId)

    @Query(''' select DISTINCT(sdParent)
        from SkillRelDef srd, SkillDef sdChild, SkillDef sdParent
            inner join UserPerformedSkill ups on sdParent.id = ups.skillRefId and ups.userId=?1
        where 
            srd.parent.id = sdParent.id and 
            srd.child.id=sdChild.id and
            sdChild.id in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s 
                where
                s.projectId=?2 and
                type = 'Skill' and
                s.skillId=?3 and
                s.enabled = 'true'
            ) and 
            srd.type='Dependence' ''')
    List<SkillDef> findPerformedParentSkills(String userId, String projectId, String skillId)

    @Query(''' select DISTINCT(sdParent)
        from SkillRelDef srd, SkillDef sdChild, SkillDef sdParent
            inner join UserPerformedSkill ups on sdParent.id = ups.skillRefId and ups.userId=?1
        where 
            srd.parent.id = sdParent.id and 
            srd.child.id=sdChild.id and
            sdChild.id in (
                select case when s.copiedFrom is not null then s.copiedFrom else s.id end as id from SkillDef s 
                where
                s.projectId=?2 and
                type = 'Skill' and
                s.enabled = 'true'
            ) and 
            ups.id in ?3 and
            srd.type='Dependence' ''')
    List<SkillDef> findPerformedParentSkillsById(String userId, String projectId, List<Integer> ids)

    @Nullable
    @Query(value = '''
        WITH skills AS (
            select case when child.copied_from_skill_ref is not null then child.copied_from_skill_ref else child.id end as id,
                   child.point_increment                                                                                as pointIncrement
            from skill_definition parent,
                 skill_relationship_definition rel,
                 skill_definition child
            where parent.project_id = :projectId
              and parent.skill_id = :skillId
              and rel.parent_ref_id = parent.id
              and rel.child_ref_id = child.id
              and rel.type in ('RuleSetDefinition', 'GroupSkillToSubject')
              and child.type = 'Skill'
              and child.version <= :version
              and child.enabled = 'true'
        )
        select CAST(ups.performed_on as date) as day, SUM(skills.pointIncrement) as count
        from user_performed_skill ups,
             skills
        where ups.user_id = :userId
          and ups.skill_ref_id = skills.id
        group by CAST(ups.performed_on as date)''', nativeQuery = true)
    List<DayCountItem> calculatePointHistoryForSubject(@Param('projectId') String projectId,
                                                                           @Param('userId') String userId,
                                                                           @Param('skillId') String skillId,
                                                                           @Param('version') Integer version)

    @Nullable
    @Query(value = '''
        WITH skills AS (
            select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id,
                   point_increment as pointIncrement
            from skill_definition child
            where project_id = :projectId
              and type = 'Skill'
              and version <= :version
              and enabled = 'true'
        )
        select CAST(ups.performed_on as date) as day, SUM(skills.pointIncrement) as count
        from user_performed_skill ups,
             skills
        where ups.user_id = :userId
          and ups.skill_ref_id = skills.id
        group by CAST(ups.performed_on as date)''', nativeQuery = true)
    List<DayCountItem> calculatePointHistoryForProject(@Param('projectId') String projectId,
                                                       @Param('userId') String userId,
                                                       @Param('version') Integer version)

    @Query('''select CAST(ups.performedOn as date) as day, count(ups.id) as count
        from UserPerformedSkill ups
        where
        ups.skillRefId in (
            select case when copiedFrom is not null then copiedFrom else id end as id 
            from SkillDef 
            where type = 'Skill' and 
            skillId = :skillId and 
            projectId = :projectId and
            enabled = 'true'
        ) and
        ups.performedOn > :from and 
        not exists (select 1 from ArchivedUser au where au.userId = ups.userId and au.projectId = :projectId)
        group by CAST(ups.performedOn as date)
    ''')
    List<DayCountItem> countsByDay(@Param('projectId') String projectId, @Param('skillId') String skillId, @Param("from") Date from)

    @Query("SELECT p from UserPerformedSkill p where p.skillRefId=?1 and p.created > ?2 and p.created < ?3" )
    List<UserPerformedSkill> findAllBySkillRefIdWithinTimeRange(Integer skillRefId, Date start, Date end)

    @Nullable
    List<UserPerformedSkill> findAllBySkillRefIdAndUserId(Integer skillRefId, String userId)

    @Nullable
    List<UserPerformedSkill> findAllBySkillRefId(Integer skillRefId)

    @Nullable
    // returns oldest UPS for skill/user
    UserPerformedSkill findTopBySkillRefIdAndUserIdOrderByPerformedOnAsc(Integer skillRefId, String userId)

    @Nullable
    // returns most recent UPS for skill/user
    UserPerformedSkill findTopBySkillRefIdAndUserIdOrderByPerformedOnDesc(Integer skillRefId, String userId)

}
