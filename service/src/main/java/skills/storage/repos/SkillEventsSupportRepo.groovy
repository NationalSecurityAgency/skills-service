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

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.LevelDef
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillRelDef

/**
 * Custom repo to support latency aware actions of reporting skill events code
 */
interface SkillEventsSupportRepo extends CrudRepository<SkillDef, Long> {

    static interface TinyProjectDef {
        Integer getId()
        String getProjectId()
        Integer getTotalPoints()
    }

    static interface TinySkillDef {
        Integer getId()
        String getSkillId()
        String getName()
        SkillDef.ContainerType getType()
        Integer getTotalPoints()
        Integer getPointIncrement()
        Integer getNumSkillsRequired()
        String getEnabled()
    }

    static interface TinyUserPoints {
        Integer getId()
        Integer getSkillRefId()
        Integer getPoints()
        Date getDay()
    }

    static interface TinyUserAchievement {
        Integer getSkillRefId()
        Integer getLevel()
    }

    @Query('''SELECT
        s.id as id,
        s.skillId as skillId,
        s.name as name,
        s.totalPoints as totalPoints,
        s.type as type,
        s.pointIncrement as pointIncrement,
        s.numSkillsRequired as numSkillsRequired,
        s.enabled as enabled
        from SkillDef s, SkillRelDef srd 
        where
            s.id = srd.parent and  
            srd.child.id=?1 and 
            srd.type in ?2''')
    List<TinySkillDef> findTinySkillDefsParentsByChildIdAndTypeIn(Integer childId, List<SkillRelDef.RelationshipType> types)

    @Query('''SELECT
        up.id as id,
        up.skillRefId as skillRefId,
        up.points as points,
        up.day as day
        from UserPoints up
        where
            up.projectId=?1 and  
            up.userId=?2 and
            (up.skillRefId in (?3) or up.skillRefId is null) and 
            (up.day=?4 or up.day is null)''')
    List<TinyUserPoints> findTinyUserPointsProjectIdAndUserIdAndSkillsAndDay(String projectId, String usedId, List<Integer> skillRefIds, Date day)

    @Query('''SELECT
        up.id as id,
        up.skillRefId as skillRefId,
        up.points as points,
        up.day as day
        from UserPoints up, SkillRelDef srd
        where
            up.userId=?1 and  
            up.skillRefId = srd.child and  
            srd.parent.id=?2 and 
            up.day is null''')
    List<TinyUserPoints> findTotalTinyUserPointsByUserIdAndParentId(String usedId, Integer parentId)

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.enabled as enabled,
        s.selfReportingType as selfReportingType,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly,
        s.groupId as groupId,
        s.numSkillsRequired as numSkillsRequired
        from SkillDef s where s.projectId = ?1 and s.skillId=?2 and s.type = ?3''')
    @Nullable
    SkillDefMin findByProjectIdAndSkillIdAndType(String projectId, String skillId, SkillDef.ContainerType type)

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.enabled as enabled,
        s.selfReportingType as selfReportingType,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly
        from SkillDef s where ( :projectId is null OR s.projectId = :projectId ) and s.skillId=:skillId''')
    @Nullable
    SkillDefMin findByProjectIdAndSkillId(@Param("projectId") String projectId, @Param("skillId") String skillId)

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.enabled as enabled,
        s.selfReportingType as selfReportingType,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly
        from SkillDef s where s.projectId is null and s.skillId=:skillId''')
    @Nullable
    SkillDefMin findBySkillIdWhereProjectIdIsNull(@Param("skillId") String skillId)

    @Query('''SELECT
        badge.id as id,
        badge.projectId as projectId,
        badge.skillId as skillId,
        badge.name as name,
        badge.pointIncrement as pointIncrement,
        badge.pointIncrementInterval as pointIncrementInterval,
        badge.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        badge.totalPoints as totalPoints,
        badge.type as type,
        badge.enabled as enabled
        from SkillDef badge 
        where 
            badge.projectId is null and 
            badge.type = 'GlobalBadge'
            AND (
                EXISTS  (SELECT true
                from SkillDef s1, SkillRelDef sr1
                where
                    badge.id = sr1.parent and
                    s1.id = sr1.child and 
                    sr1.type = 'BadgeRequirement' and 
                    s1.projectId = ?1 and 
                    s1.skillId = ?2)
            OR EXISTS (SELECT true
                from GlobalBadgeLevelDef gbld
                where
                    badge.skillId = gbld.badgeId and
                    gbld.projectId = ?1)
            )''')
    @Nullable
    List<SkillDefMin> findGlobalBadgesForProjectIdAndSkillId(String projectId, String skillId)

    @Modifying
    @Query('''update UserPoints up set up.points = up.points + ?2 where up.id = ?1''')
    void addUserPoints(Integer id, int pointsToAdd)

    @Modifying
    @Query('''update UserPoints up set up.contributesToSkillsGroup = ?2 where up.id = ?1''')
    void updateContributingFlag(Integer id, String contributesToSkillsGroup)

    @Query('''SELECT l from LevelDef l where l.skillRefId = ?1''')
    List<LevelDef> findLevelsBySkillId(Integer skillId)

    @Query('''SELECT 
        l.projectRefId as projectRefId,
        l.skillRefId as skillRefId,
        l.level as level,
        l.percent as percent,
        l.pointsFrom as pointsFrom,
        l.pointsTo as pointsTo 
        from LevelDef l where l.skillRefId in (?1) or l.projectRefId = ?2''')
    List<LevelDefInterface> findLevelsBySkillIdsOrByProjectId(List<Integer> skillIds, Integer projectId)

    @Query('''SELECT 
        ua.skillRefId as skillRefId,
        ua.level as level
        from UserAchievement ua 
        where 
            ua.userId = ?1 and
            (ua.skillRefId in (?3) or ua.skillRefId is null) and
            ua.projectId = ?2 ''')
    List<TinyUserAchievement> findTinyUserAchievementsByUserIdAndProjectIdAndSkillIds(String userId, String projectId, List<Integer> skillIds)

    @Query('''SELECT 
        ua.skillRefId as skillRefId,
        ua.level as level
        from UserAchievement ua 
        where 
            ua.userId = ?1 and
            ua.skillRefId is null and
            ua.projectId = ?2 ''')
    List<TinyUserAchievement> findTinyUserAchievementsByUserIdAndProjectId(String userId, String projectId)

    @Query('''SELECT l from LevelDef l, ProjDef p 
        where
            l.projectRefId = p.id and 
            p.projectId = ?1''')
    List<LevelDef> findLevelsByProjectId(String projectId)

    @Query('''SELECT
        s.id as id,
        s.projectId as projectId,
        s.skillId as skillId,
        s.name as name,
        s.pointIncrement as pointIncrement,
        s.pointIncrementInterval as pointIncrementInterval,
        s.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        s.totalPoints as totalPoints,
        s.type as type,
        s.startDate as startDate,
        s.endDate as endDate,
        s.enabled as enabled,
        s.copiedFrom as copiedFrom,
        s.copiedFromProjectId as copiedFromProjectId,
        s.readOnly as readOnly
        from SkillDef s, SkillRelDef srd 
        where
            s.id = srd.parent and  
            srd.child.id=?1 and 
            srd.type=?2''')
    List<SkillDefMin> findParentSkillsByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)


    @Query('''SELECT 
        p.totalPoints as totalPoints,
        p.id as id,
        p.projectId as projectId
        from ProjDef p where p.projectId = ?1''')
    TinyProjectDef getTinyProjectDef(String projectId)

    @Query(value = '''SELECT 
        p.totalPoints as totalPoints,
        p.id as id,
        p.projectId as projectId
        from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1''')
    List<TinyProjectDef> getTinyProjectDefForUserId(String userId)


    @Query('''select max(ups.performedOn)
    from SkillRelDef srd, SkillDef sdChild, UserPerformedSkill ups
      where
      srd.child=sdChild and sdChild.skillId = ups.skillId and
      ups.userId=?1 and 
      ups.projectId=?2 and 
      sdChild.projectId=?2 and
      srd.parent.id=?3 and 
      srd.type=?4''')
    Date getUserPerformedSkillLatestDate(String userId, String projectId, Integer parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''select max(ups.performedOn)
    from UserPerformedSkill ups
      where
      ups.userId=?1 and 
      ups.projectId=?2''')
    Date getUserPerformedSkillLatestDate(String userId, String projectId)


    @Query('''select max(ups.performedOn)
    from UserPerformedSkill ups
      where
      ups.userId=?1 and 
      ups.projectId=?2 and 
      ups.skillId=?3''')
    Date getUserPerformedSkillLatestDate(String userId, String projectId, String skillId)
}
