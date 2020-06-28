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

import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import skills.storage.model.SkillDef

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

@Conditional(DBConditions.H2)
@Service
class H2NativeRepo implements NativeQueriesRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId) {
        String q = '''
        merge into USER_POINTS(id, points) key(id)
        SELECT b.id, b.points - a.points as points
        FROM user_points a, user_points b
        WHERE a.user_id = b.user_id
          and (a.day = b.day OR (a.day is null and b.day is null))
          and a.skill_id = :deletedSkillId
          and (b.skill_id = :parentSubjectSkillId or b.skill_id is null)
          and b.project_id = :projectId
          and a.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("parentSubjectSkillId", parentSubjectSkillId)
        query.setParameter("deletedSkillId", deletedSkillId)
        query.executeUpdate();
    }

    @Override
    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType) {
        String q = '''
        merge into USER_POINTS (id, points) key (id)
    select points.id, sum.sumPoints
    from (select user_id sumUserId, day sumDay, SUM(pointsInner.points) sumPoints
          from user_points pointsInner
                   join skill_definition definition
                        on pointsInner.project_id = definition.project_id and
                           pointsInner.skill_id = definition.skill_id and
                           definition.type = :subjectType
          where pointsInner.project_id = :projectId
            and definition.project_id = :projectId
          group by user_id, day) sum,
         user_points points
    where sum.sumUserId = points.user_id
      and (sum.sumDay = points.day OR (sum.sumDay is null and points.day is null))
      and points.skill_id is null
      and points.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("subjectType", subjectType.toString())
        query.executeUpdate();
    }

    @Override
    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId){
        String q = '''
            WITH RECURSIVE skill_deps_path(parentProjectId, parentSkillId, parentId, parentName, childProjectId, childSkillId, childId, childName) AS (
              select sd.project_id as parentProjectId, sd.skill_id as parentSkillId, sd.id as parentId, sd.name as parentName,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
              from skill_definition sd,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where sd.id = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and sd.project_id=:projectId and sd.skill_id=:skillId 
              UNION ALL
              select skill_deps_path.childProjectId as parentProjectId, skill_deps_path.childSkillId as parentSkillId, skill_deps_path.childId as parentId, skill_deps_path.childName as parentName,
                     sd1.project_id as childProjectId, sd1.skill_id as childSkillId, sd1.id as childId, sd1.name as childName
              from  skill_deps_path,
                   skill_relationship_definition srd,
                   skill_definition sd1
              where skill_deps_path.childId = srd.parent_ref_id
                and sd1.id = srd.child_ref_id
                and srd.type = 'Dependence'
                and skill_deps_path.childProjectId=:projectId
            )
            select pd.project_id as parentProjectId, pd.name as parentProjectName, skill_deps_path.parentId, skill_deps_path.parentSkillId, skill_deps_path.parentName,
                   skill_deps_path.childProjectId, pd1.name as childProjectName, skill_deps_path.childId, skill_deps_path.childSkillId, skill_deps_path.childName,
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

        List resList = query.getResultList().collect {
            new GraphRelWithAchievement(
                    parentProjectId: it[0],
                    parentProjectName: it[1],
                    parentId: it[2],
                    parentSkillId: it[3],
                    parentName: it[4],

                    childProjectId: it[5],
                    childProjectName: it[6],
                    childId: it[7],
                    childSkillId: it[8],
                    childName: it[9],
                    achievementId: it[10]
            )
        }
        return resList
    }

    void updatePointTotalsForSkill(String projectId, String subjectId, String skillId, int incrementDelta){
        String eventCountSql = '''
            SELECT 
                user_id, COUNT(id) eventCount
            FROM 
                user_performed_skill
            WHERE 
                skill_id = :skillId
                AND project_id = :projectId
            GROUP BY 
                user_id
           '''

        Query query = entityManager.createNativeQuery(eventCountSql);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = query.getResultList().collect{
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1])
        }

        String updateSql = '''
            UPDATE
                user_points points
            SET
                points = points + (:eventCount * :incrementDelta)
            WHERE
                points.user_id = :userId
                AND points.day IS NULL
                AND points.project_id=:projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''

        eventCounts?.each {
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("eventCount", it.eventCount)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.setParameter("subjectId", subjectId)
            updateQ.setParameter("incrementDelta", incrementDelta)
            updateQ.executeUpdate()
        }
    }

    void updatePointHistoryForSkill(String projectId, String subjectId, String skillId, int incrementDelta){
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserIdAndDate(projectId, skillId)

        String updateSql = '''
            UPDATE 
                user_points points
            SET 
                points = points + (:eventCount * :incrementDelta) 
            WHERE
                points.user_id = :userId
                AND points.day = :performedOn
                AND points.project_id = :projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''
        eventCounts?.each{
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("eventCount", it.eventCount)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.setParameter("subjectId", subjectId)
            updateQ.setParameter("performedOn", it.performedOn)
            updateQ.setParameter("incrementDelta", incrementDelta)
            updateQ.executeUpdate()
        }

    }

    private List<PerformedSkillEventCount> getGroupedEventCountsByUserIdAndDate(String projectId, String skillId) {
        String eventCountSql = '''
            SELECT
                user_id, COUNT(id) eventCount, FORMATDATETIME(performed_on,'yyyy-MM-dd') performedOn
            FROM
                user_performed_skill
            WHERE
                skill_id = :skillId AND project_id = :projectId
            GROUP BY
                user_id, FORMATDATETIME(performed_on,'yyyy-MM-dd')
           '''

        Query eventCountQuery = entityManager.createNativeQuery(eventCountSql);
        eventCountQuery.setParameter("projectId", projectId);
        eventCountQuery.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = eventCountQuery.getResultList().collect {
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1], performedOn: it[2])
        }
        eventCounts
    }

    private static class PerformedSkillEventCount{
        String userId
        int eventCount
        String performedOn
    }

    @Override
    void updatePointTotalWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int numOccurrences) {
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({it.eventCount > numOccurrences})

        String updateSql = '''
            UPDATE
                user_points points
            SET
                points = points - :decrementDelta
            WHERE
                points.user_id = :userId
                AND points.day IS NULL
                AND points.project_id=:projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''
        eventsCountsToEdit?.each{
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.setParameter("subjectId", subjectId)
            updateQ.setParameter("decrementDelta", (it.eventCount - numOccurrences) * pointIncrement)
            updateQ.executeUpdate()
        }
    }

    private List<PerformedSkillEventCount> getGroupedEventCountsByUserId(String projectId, String skillId) {
        String eventCountSql = '''
            SELECT
                user_id, COUNT(id) eventCount
            FROM
                user_performed_skill
            WHERE
                skill_id = :skillId AND project_id = :projectId
            GROUP BY
                user_id
           '''

        Query eventCountQuery = entityManager.createNativeQuery(eventCountSql);
        eventCountQuery.setParameter("projectId", projectId);
        eventCountQuery.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = eventCountQuery.getResultList().collect {
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1])
        }
        return eventCounts
    }

    @Override
    void updatePointHistoryWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int numOccurrences) {

        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({it.eventCount > numOccurrences})

        String getRowsToRemoveSql = '''SELECT id, FORMATDATETIME(performed_on,'yyyy-MM-dd') FROM user_performed_skill 
                WHERE 
                    project_id = :projectId and 
                    skill_id = :skillId and 
                    user_id = :userId
                ORDER BY performed_on DESC
                limit :numToRemove'''

        String updateSql = '''
            UPDATE 
                user_points points
            SET 
                points = points - :decrementDelta 
            WHERE
                points.user_id = :userId
                AND points.day = :performedOn
                AND points.project_id = :projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''

        eventsCountsToEdit?.each { eventCountItem ->
            Query query = entityManager.createNativeQuery(getRowsToRemoveSql)
            query.setParameter("userId", eventCountItem.userId)
            query.setParameter("projectId", projectId)
            query.setParameter("skillId", skillId)
            query.setParameter("numToRemove", (eventCountItem.eventCount - numOccurrences))
            // group by date
            query.getResultList().groupBy { it[1] }.each {
                Query updateQ = entityManager.createNativeQuery(updateSql)
                updateQ.setParameter("userId", eventCountItem.userId)
                updateQ.setParameter("projectId", projectId)
                updateQ.setParameter("skillId", skillId)
                updateQ.setParameter("subjectId", subjectId)
                updateQ.setParameter("performedOn", it.key)
                updateQ.setParameter("decrementDelta", it.value.size() * pointIncrement)
                updateQ.executeUpdate()
            }
        }
    }

    @Override
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep){
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({it.eventCount > numEventsToKeep})

        String q = '''
            DELETE 
            FROM user_performed_skill 
            WHERE id in (
                SELECT id FROM user_performed_skill 
                WHERE 
                    project_id = :projectId and 
                    skill_id = :skillId and 
                    user_id = :userId
                ORDER BY performed_on DESC
                limit :numToRemove
            )
            '''

        eventsCountsToEdit.each {
            Query query = entityManager.createNativeQuery(q);
            query.setParameter("projectId", projectId);
            query.setParameter("skillId", skillId)
            query.setParameter("userId", it.userId)
            query.setParameter("numToRemove", it.eventCount - numEventsToKeep)
            query.executeUpdate()
        }
    }

    @Override
    void removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(String projectId, String skillId, int numOfOccurrences) {
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({it.eventCount < numOfOccurrences})

        String updateSql = '''
            DELETE 
            FROM user_achievement ua
            WHERE
                ua.project_id = :projectId and 
                ua.skill_id = :skillId and 
                ua.user_id = :userId
        '''
        eventsCountsToEdit?.each{
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.executeUpdate()
        }
    }

    @Override
    List<String> findUsersEligibleForBadge(String projectId, String badgeId, Date start, Date end) {
        String badgeSkillsQ = '''
        SELECT sr.child_ref_id 
        FROM skill_relationship_definition sr
        INNER JOIN skill_definition sd ON sr.parent_ref_id = sd.id AND
        sd.skill_id = :badgeId AND
        sd.project_id = :projectId
        '''

        Query selectBadgeSkills = entityManager.createNativeQuery(badgeSkillsQ)
        selectBadgeSkills.setParameter('badgeId', badgeId)
        selectBadgeSkills.setParameter('projectId', projectId)
        List<String> badgeSkillIds = selectBadgeSkills.getResultList()

        String selectUsersQ = '''
        SELECT ua.user_id
        FROM user_achievement ua
        WHERE ua.skill_ref_id IN (:badgeSkillIds) AND
        NOT EXISTS (
            SELECT 1
            FROM user_achievement
            WHERE skill_id = :badgeId AND
            user_id = ua.user_id AND
            project_id = :projectId
        )
        GROUP BY ua.user_id 
        HAVING COUNT(*) = :numBadgeSkills'''

        String dateFrag = '''
        AND 
        (
            SELECT MAX(performed_on) 
            FROM user_performed_skill 
            WHERE user_id=ua.user_id AND 
            skill_ref_id IN (:badgeSkillIds) 
        ) BETWEEN :start AND :end
        '''

        List<String> results = []
        if(badgeSkillIds) {
            boolean dateCheck = start != null && end != null
            if(dateCheck) {
                selectUsersQ += dateFrag
            }

            Query getUsers = entityManager.createNativeQuery(selectUsersQ)
            getUsers.setParameter('badgeSkillIds', badgeSkillIds)
            getUsers.setParameter('projectId', projectId)
            getUsers.setParameter('badgeId', badgeId)
            getUsers.setParameter('numBadgeSkills', badgeSkillIds.size())
            if(dateCheck) {
                getUsers.setParameter('start', start)
                getUsers.setParameter('end', end)
            }
            List<String> r = getUsers.getResultList()

            if(r) {
                results.addAll(r)
            }
        }

        return results
    }

    List<String> findUsersEligbleForGlobalBadge(String badgeId,
                                                Integer requiredSklls,
                                                Integer requiredLevels,
                                                Date start,
                                                Date end) {

        boolean requireSkills = requiredSklls != null && requiredSklls > 0
        boolean requireLevels = requiredLevels != null && requiredLevels > 0

        String badgeSkillsQ = '''
        SELECT sr.child_ref_id 
        FROM skill_relationship_definition sr
        INNER JOIN skill_definition sd ON sr.parent_ref_id = sd.id AND
        sd.skill_id = :badgeId AND
        sd.project_id is null
        '''

        String levelsQ = '''
            SELECT 
            ua.user_id 
            FROM USER_ACHIEVEMENT ua
            INNER JOIN GLOBAL_BADGE_LEVEL_DEFINITION g ON g.level=ua.level 
            AND g.project_id = ua.project_id
            WHERE ua.SKILL_ID is null 
            GROUP BY ua.user_id having count(ua.project_id) >= (SELECT count(*) FROM global_badge_level_definition WHERE skill_id = :badgeId)
        '''

        List<String> usersWithRequiredLevel = []
        if (requireLevels) {
            Query selectBadgeLevels = entityManager.createNativeQuery(levelsQ)
            selectBadgeLevels.setParameter("badgeId", badgeId)
            usersWithRequiredLevel = selectBadgeLevels.getResultList()
        }

        List<String> usersWithRequiredSkills = []
        if (requireSkills) {
            Query skills = entityManager.createNativeQuery(badgeSkillsQ)
            skills.setParameter("badgeId", badgeId)
            List<String> badgeSkills = skills.getResultList()

            if (badgeSkills) {
                String usersWithSkills = '''
                SELECT ua.user_id
                FROM user_achievement ua
                WHERE ua.skill_ref_id IN (:badgeSkillIds) AND
                NOT EXISTS (
                    SELECT 1
                    FROM user_achievement
                    WHERE skill_id = :badgeId AND
                    user_id = ua.user_id AND
                    project_id is null
                )
               GROUP BY ua.user_id 
               HAVING COUNT(*) = :numBadgeSkills'''

                boolean dateRange = start != null && end != null
                if (dateRange) {
                    usersWithSkills += '''
                     AND 
                    (
                        SELECT MAX(performed_on) 
                        FROM user_performed_skill 
                        WHERE user_id=ua.user_id AND 
                        skill_ref_id IN (:badgeSkillIds) 
                    ) BETWEEN :start AND :end
                '''
                }

                Query usersWithBadgeSkills = entityManager.createNativeQuery(usersWithSkills)
                usersWithBadgeSkills.setParameter("badgeSkillIds", badgeSkills)
                usersWithBadgeSkills.setParameter("badgeId", badgeId)
                usersWithBadgeSkills.setParameter("numBadgeSkills", badgeSkills.size())
                usersWithBadgeSkills.setParameter("badgeSkillIds", badgeSkills)
                usersWithRequiredSkills = usersWithBadgeSkills.getResultList()
            }
        }

        if (!requireLevels) {
            return usersWithRequiredSkills
        } else if (!requireSkills) {
            return usersWithRequiredLevel
        } else {
            return usersWithRequiredSkills.intersect(usersWithRequiredLevel)
        }
    }
}
