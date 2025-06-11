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
package skills.storage.repos.nativeSql

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import skills.controller.result.model.ProjectUser
import skills.storage.model.QueryUsersCriteria
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefPartial
import skills.storage.model.SkillsDBLock
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillsDBLockRepo
import skills.storage.repos.UserPointsRepo

import jakarta.persistence.EntityManager
import jakarta.persistence.ParameterMode
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import java.util.stream.Stream

@Conditional(DBConditions.PostgresQL)
@Service
class PostgresQlNativeRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId) {
        String q = '''
        UPDATE user_points b set points = b.points - a.points
        FROM user_points a
        WHERE a.user_id = b.user_id
            and a.project_id = :projectId and a.skill_id= :deletedSkillId and (b.skill_id= :parentSubjectSkillId or b.skill_id is null) and b.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("parentSubjectSkillId", parentSubjectSkillId)
        query.setParameter("deletedSkillId", deletedSkillId)
        query.executeUpdate();
    }

    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType) {
        String q = '''
        update user_points points set points = sum.sumPoints
        from (
            select
                user_id as sumUserId,
                sum(pointsInner.points) as sumPoints
            from user_points pointsInner
            join skill_definition definition on pointsInner.project_id = definition.project_id and
                pointsInner.skill_id = definition.skill_id and
                definition.type = :subjectType
            where pointsInner.project_id = :projectId and definition.project_id = :projectId
            group by sumUserId
        ) as sum
        where sum.sumUserId = points.user_id and points.skill_id is null and points.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("subjectType", subjectType.toString())
        query.executeUpdate();
    }

    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId) {
        String q = '''
            WITH RECURSIVE skill_deps_path(parentProjectId, parentSkillId, parentId, parentName, parentType, childProjectId, childSkillId, childId, childName, childType) AS (
              select sd.project_id as parentProjectId, sd.skill_id as parentSkillId, sd.id as parentId, sd.name as parentName, sd.type as parentType,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName, sd1.type as childType
              from skill_definition sd,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where sd.id = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and sd.project_id=:projectId and sd.skill_id=:skillId 
              UNION ALL
              select skill_deps_path.childProjectId as parentProjectId, skill_deps_path.childSkillId as parentSkillId, skill_deps_path.childId as parentId, skill_deps_path.childName as parentName, skill_deps_path.childType as parentType,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName, sd1.type as childType
              from  skill_deps_path,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where skill_deps_path.childId = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and skill_deps_path.childProjectId=:projectId
            )
            select CAST(pd.project_id as TEXT) as parentProjectId, CAST(pd.name as TEXT) as parentProjectName, skill_deps_path.parentId, CAST(skill_deps_path.parentSkillId as TEXT), CAST(skill_deps_path.parentName as TEXT), CAST(skill_deps_path.parentType as TEXT),
                   CAST(skill_deps_path.childProjectId as TEXT), CAST(pd1.name as TEXT) as childProjectName, skill_deps_path.childId, CAST(skill_deps_path.childSkillId as TEXT), CAST(skill_deps_path.childName as TEXT), CAST(skill_deps_path.childType as TEXT),
                   ua.id as achievementId
            from skill_deps_path
              join project_definition pd on skill_deps_path.parentProjectId = pd.project_id
              join project_definition pd1 on skill_deps_path.childProjectId = pd1.project_id
              left join user_achievement ua
                ON ua.skill_ref_id = skill_deps_path.childId AND ua.user_id=:userId
         '''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("userId", userId)

        List resList = query.getResultList()?.collect {
            new GraphRelWithAchievement(
                    parentProjectId: it[0],
                    parentProjectName: it[1],
                    parentId: it[2],
                    parentSkillId: it[3],
                    parentName: it[4],
                    parentType: it[5],
                    childProjectId: it[6],
                    childProjectName: it[7],
                    childId: it[8],
                    childSkillId: it[9],
                    childName: it[10],
                    childType: it[11],
                    achievementId: it[12]
            )
        }
        return resList
    }

    void updatePointTotalsForSkill(String projectId, String subjectId, String skillId, int incrementDelta) {
        String q = '''
        WITH
            eventsRes AS (
                SELECT 
                    user_id, COUNT(id) eventCount
                FROM 
                    user_performed_skill
                WHERE 
                    skill_ref_id in (
                        select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id 
                        from skill_definition 
                        where type = 'Skill' and project_id = :projectId and skill_id = :skillId
                    )
                GROUP BY 
                    user_id
            )
        UPDATE
            user_points points
        SET
            points = points + (eventsRes.eventCount * :incrementDelta)
        FROM
            eventsRes
        WHERE 
            eventsRes.user_id = points.user_id
            AND points.project_id=:projectId 
            AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("subjectId", subjectId)
        query.setParameter("incrementDelta", incrementDelta)
        query.executeUpdate()
    }

    void updatePointTotalWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int newOccurrences, int previousOccurrences) {
        subjectId = subjectId ?: '';
        String q = '''
            WITH
                eventsRes AS (
                    SELECT rank_filter.user_id, count(rank_filter.id) eventCount
                    FROM (
                        SELECT user_performed_skill.id, user_performed_skill.user_id,
                            rank() OVER (
                                PARTITION BY user_id
                                ORDER BY created DESC
                            )
                        FROM user_performed_skill
                        where project_id = :projectId and skill_id = :skillId
                    ) rank_filter
                    WHERE RANK >= :newOccurrences
                    group by user_id
                )
            UPDATE 
                user_points points
            SET 
                points = points - :pointsDelta 
            FROM
                eventsRes
            WHERE
                eventsRes.user_id = points.user_id
                AND points.project_id=:projectId 
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("subjectId", subjectId)
        query.setParameter("newOccurrences", newOccurrences)
        query.setParameter("pointsDelta", (previousOccurrences - newOccurrences) * pointIncrement)
        query.executeUpdate()
    }

    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep) {
        String q = '''
            DELETE from user_performed_skill ups
            USING (SELECT rank_filter.id FROM (
                SELECT user_performed_skill.id, user_performed_skill.performed_on,
                       rank() OVER (
                           PARTITION BY user_id
                           ORDER BY performed_on ASC
                           )
                FROM user_performed_skill where project_id = :projectId and skill_id = :skillId
            ) rank_filter WHERE RANK > :numEventsToKeep) as idsToRemove
            WHERE idsToRemove.id = ups.id;'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("numEventsToKeep", numEventsToKeep)
        query.executeUpdate()
    }

    void removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(String projectId, String skillId, int numOfOccurrences) {
        String q = '''
            DELETE
            FROM user_achievement ua
            USING
                 (
                     SELECT user_id, count(id) eventCount
                     FROM user_performed_skill
                     WHERE
                         skill_id = :skillId and
                         project_id = :projectId
                     GROUP BY user_id
                 ) eventsByUserId
            WHERE ua.project_id = :projectId and 
                ua.skill_id = :skillId and 
                ua.user_id = eventsByUserId.user_id and 
                eventsByUserId.eventCount < :numOfOccurrences'''

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        query.setParameter("numOfOccurrences", numOfOccurrences)
        query.executeUpdate()
    }

    int addBadgeAchievementForEligibleUsers(String projectId, String badgeId, Integer badgeRowId, Boolean notified, Date start, Date end) {

        String q = '''
        WITH badgeSkills AS (
            SELECT sr.child_ref_id childId 
            FROM skill_relationship_definition sr 
            INNER JOIN skill_definition sd ON sr.parent_ref_id = sd.id AND 
            sd.skill_id = :badgeId AND
            sd.project_id = :projectId
        )
        INSERT INTO user_achievement (user_id, project_id, skill_id, skill_ref_id, notified, points_when_achieved)
        SELECT ua.user_id, ''' +"'$projectId', '$badgeId', $badgeRowId, '${notified.toString()}', -1"+
        '''
        FROM user_achievement ua 
        WHERE ua.skill_ref_id IN (SELECT childId FROM badgeSkills) AND
        NOT EXISTS (
            SELECT 1 
            FROM user_achievement 
            WHERE skill_id = :badgeId AND 
            user_id = ua.user_id AND
            project_id = :projectId
        )
        GROUP BY ua.user_id 
        HAVING COUNT(*) = (SELECT COUNT(*) FROM badgeSkills)'''

        final String dateFrag = '''
        AND 
        (
            SELECT MAX(performed_on) 
            FROM user_performed_skill 
            WHERE user_id=ua.user_id AND 
            skill_ref_id IN (SELECT childId FROM badgeSkills) 
        ) BETWEEN :start AND :end
        '''

        boolean dateCheck = start != null && end != null

        if (dateCheck) {
            q += dateFrag
        }

        Query query = entityManager.createNativeQuery(q);
        query.setParameter('projectId', projectId)
        query.setParameter('badgeId', badgeId)

        if (dateCheck) {
            query.setParameter('start', start)
            query.setParameter('end', end)
        }

        return query.executeUpdate()
    }

    int addGlobalBadgeAchievementForEligibleUsers(String badgeId,
                                                  Integer badgeRowId,
                                                  Boolean notified,
                                                  Integer requiredSklls,
                                                  Integer requiredLevels,
                                                  Date start,
                                                  Date end) {

        final String cteFrag = '''
        WITH badgeSkills AS (
            SELECT sr.child_ref_id childId
            FROM skill_relationship_definition sr
            INNER JOIN skill_definition sd ON sr.parent_ref_id = sd.id AND
            sd.skill_id = :badgeId AND
            sd.project_id is null
        )
        '''

        final String insertStatement = '''
        INSERT INTO user_achievement (user_id, skill_id, skill_ref_id, notified, points_when_achieved)
        '''

        final String selectFrag = '''SELECT ua.user_id, ''' +"'$badgeId', $badgeRowId, '${notified.toString()}', -1 "

        final String skillDateFrag = '''
        AND
        (
            SELECT MAX(performed_on)
            FROM user_performed_skill
            WHERE user_id=ua.user_id AND
            skill_ref_id IN (SELECT childId FROM badgeSkills)
        ) BETWEEN :start AND :end
        '''

        boolean includeLevels = requiredLevels != null && requiredLevels > 0
        boolean includeSkills = requiredSklls != null && requiredSklls > 0
        boolean includeDates = start != null && end != null

        String q = insertStatement
        String levels = ''
        String skills = ''
        if (includeLevels) {
            levels = selectFrag +
            '''
            FROM USER_ACHIEVEMENT ua
            INNER JOIN GLOBAL_BADGE_LEVEL_DEFINITION g ON g.level=ua.level 
            AND g.project_id = ua.project_id AND g.skill_id = :badgeId
            WHERE ua.SKILL_ID is null 
            GROUP BY ua.user_id having count(ua.project_id) >= (SELECT count(*) FROM global_badge_level_definition WHERE skill_id = :badgeId)
        '''
        }

        if (includeSkills) {
            skills = selectFrag +
            '''
            FROM USER_ACHIEVEMENT ua
            WHERE ua.skill_ref_id IN (SELECT childId FROM badgeSkills) AND
            NOT EXISTS (
                SELECT 1 
                FROM user_achievement 
                WHERE skill_id = :badgeId AND 
                user_id = ua.user_id AND
                project_id is null
            )
            GROUP BY ua.user_id 
            HAVING COUNT(*) = (SELECT COUNT(*) FROM badgeSkills)
            '''
        }

        if (includeSkills) {
            q = cteFrag + q + skills
            if (includeDates) {
                q += skillDateFrag
            }
        }

        if (includeLevels) {
            if (includeSkills) {
                q += '''
                INTERSECT
                '''
            }

            q += levels
        }

        if(!includeSkills && !includeLevels){
            return 0;
        }

        Query query = entityManager.createNativeQuery(q);
        if (includeSkills || includeLevels) {
            query.setParameter('badgeId', badgeId)
        }
        if (includeDates) {
            query.setParameter('start', start)
            query.setParameter('end', end)
        }

        return query.executeUpdate()
    }

    void createOrUpdateUserEvent(String projectId, Integer skillRefId, String userId, Date start, String type, Integer count, Integer weekNumber) {
        //start and end date should be consistently formatted for updates to work
        String sql = '''
           INSERT INTO user_events (
            project_id,
            skill_ref_id, 
            user_id, 
            event_time, 
            count,
            event_type,
            week_number
           ) 
           VALUES (
            :projectId,
            :skillRefId, 
            :userId, 
            :start, 
            :count,
            :type,
            :weekNumber
          ) ON CONFLICT ON CONSTRAINT user_events_unique_row DO UPDATE SET count = user_events.count+excluded.count;
        '''

        Query query = entityManager.createNativeQuery(sql)
        query.setParameter("projectId", projectId)
        query.setParameter("skillRefId", skillRefId)
        query.setParameter("userId", userId)
        query.setParameter("start", start)
        query.setParameter("type", type)
        query.setParameter("count", count)
        query.setParameter("weekNumber", weekNumber)
        query.executeUpdate()
    }

    long countUsers(QueryUsersCriteria queryUsersCriteria) {
        String sql = QueryUserCriteriaHelper.generateCountSql(queryUsersCriteria)
        if (!sql) {
            return 0
        }

        Query query = entityManager.createNativeQuery(sql)
        QueryUserCriteriaHelper.setCountParams(query, queryUsersCriteria)

        return query.getSingleResult()
    }

    Stream<String> getUserIds(QueryUsersCriteria queryUsersCriteria) {
        String sql = QueryUserCriteriaHelper.generateSelectUserIdsSql(queryUsersCriteria)
        if (!sql) {
            return []
        }

        Query query = entityManager.createNativeQuery(sql)
        QueryUserCriteriaHelper.setSelectUserIdParams(query, queryUsersCriteria)

        return query.getResultStream()
    }

    void updateUserPointsForASkill(String projectId, String skillId) {
        userPointsRepo.updateUserPointsForASkill(projectId, skillId)
    }

    void updateUserPointsForSubject(String projectId, String skillId, Boolean enabledSkillsOnly) {
        userPointsRepo.updateSubjectUserPoints(projectId, skillId, false)
    }

    void updateUserPointsForProject(String projectId) {
        userPointsRepo.updateUserPointsForProject(projectId)
    }

    SkillsDBLock insertLockOrSelectExisting(String lockKey) {
        return skillsDBLockRepo.insertLockOrSelectExisting(lockKey)
    }

    Long countDistinctUsersByProjectIdAndSubjectIdAndUserIdLike(String projectId, String subjectId, String userId, int minimumPoints, int maximumPoints) {
        userPointsRepo.countDistinctUsersByProjectIdAndSubjectIdAndUserIdLike(projectId, subjectId, userId, minimumPoints, maximumPoints)
    }

    Long countDistinctUsersByProjectIdAndSubjectId(String projectId, String subjectId) {
        userPointsRepo.countDistinctUsersByProjectIdAndSubjectId(projectId, subjectId)
    }

    List<SkillDefPartial> getSkillsWithCatalogStatusExplodeSkillGroups(String projectId, String subjectId) {
        skillRelDefRepo.getSkillsWithCatalogStatusExplodeSkillGroups(projectId, subjectId)
    }
}
