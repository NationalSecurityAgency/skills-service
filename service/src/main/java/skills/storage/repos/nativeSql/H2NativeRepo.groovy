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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.ProjectUser
import skills.storage.model.QueryUsersCriteria
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefPartial
import skills.storage.model.SkillRelDef
import skills.storage.model.SkillsDBLock
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillsDBLockRepo
import skills.storage.repos.UserPointsRepo

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import java.util.stream.Stream

@Conditional(DBConditions.H2)
@Service
@Slf4j
class H2NativeRepo implements NativeQueriesRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillsDBLockRepo skillsDBLockRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    TransactionManager transactionManager

    @Override
    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId) {
        String q = '''
        merge into user_points(id, points) key(id)
        SELECT b.id, b.points - a.points as points
        FROM user_points a, user_points b
        WHERE a.user_id = b.user_id
          and a.skill_id = :deletedSkillId
          and (b.skill_id = :parentSubjectSkillId or b.skill_id is null)
          and b.project_id = :projectId
          and a.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter('projectId', projectId);
        query.setParameter('parentSubjectSkillId', parentSubjectSkillId)
        query.setParameter('deletedSkillId', deletedSkillId)
        query.executeUpdate();
    }

    @Override
    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType) {
        String q = '''
        merge into user_points (id, points) key (id)
    select points.id, sum.sumPoints
    from (select user_id sumUserId, SUM(pointsInner.points) sumPoints
          from user_points pointsInner
                   join skill_definition definition
                        on pointsInner.project_id = definition.project_id and
                           pointsInner.skill_id = definition.skill_id and
                           definition.type = :subjectType
          where pointsInner.project_id = :projectId
            and definition.project_id = :projectId
          group by user_id) sum,
         user_points points
    where sum.sumUserId = points.user_id
      and points.skill_id is null
      and points.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("subjectType", subjectType.toString())
        query.executeUpdate();
    }

    @Override
    List<GraphRelWithAchievement> getDependencyGraphWithAchievedIndicator(String projectId, String skillId, String userId) {
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

    void updatePointTotalsForSkill(String projectId, String subjectId, String skillId, int incrementDelta) {
        String eventCountSql = '''
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
           '''

        Query query = entityManager.createNativeQuery(eventCountSql);
        query.setParameter("projectId", projectId);
        query.setParameter("skillId", skillId)
        List<PerformedSkillEventCount> eventCounts = query.getResultList().collect {
            new PerformedSkillEventCount(userId: it[0], eventCount: it[1])
        }

        String updateSql = '''
            UPDATE
                user_points points
            SET
                points = points + (:eventCount * :incrementDelta)
            WHERE
                points.user_id = :userId
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

    private List<PerformedSkillEventCount> getGroupedEventCountsByUserIdAndDate(String projectId, String skillId) {
        String eventCountSql = '''
            SELECT
                user_id, COUNT(id) eventCount, FORMATDATETIME(performed_on,'yyyy-MM-dd') performedOn
            FROM
                user_performed_skill
            WHERE
                skill_ref_id in (
                    select case when copied_from_skill_ref is not null then copied_from_skill_ref else id end as id 
                    from skill_definition 
                    where type = 'Skill' and project_id = :projectId and skill_id = :skillId
                )
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

    private static class PerformedSkillEventCount {
        String userId
        int eventCount
        String performedOn
    }

    @Override
    void updatePointTotalWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int newOccurrences, int numOccurrences) {
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({ it.eventCount > numOccurrences })

        String updateSql = '''
            UPDATE
                user_points points
            SET
                points = points - :decrementDelta
            WHERE
                points.user_id = :userId
                AND points.project_id=:projectId
                AND (points.skill_id = :subjectId OR points.skill_id = :skillId OR points.skill_id IS NULL)
        '''
        eventsCountsToEdit?.each {
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
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep) {
        List<PerformedSkillEventCount> eventCounts = getGroupedEventCountsByUserId(projectId, skillId)
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({ it.eventCount > numEventsToKeep })

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
        List<PerformedSkillEventCount> eventsCountsToEdit = eventCounts.findAll({ it.eventCount < numOfOccurrences })

        String updateSql = '''
            DELETE 
            FROM user_achievement ua
            WHERE
                ua.project_id = :projectId and 
                ua.skill_id = :skillId and 
                ua.user_id = :userId
        '''
        eventsCountsToEdit?.each {
            Query updateQ = entityManager.createNativeQuery(updateSql)
            updateQ.setParameter("userId", it.userId)
            updateQ.setParameter("projectId", projectId)
            updateQ.setParameter("skillId", skillId)
            updateQ.executeUpdate()
        }
    }

    @Override
    int addBadgeAchievementForEligibleUsers(String projectId, String badgeId, Integer badgeRowId, Boolean notified, Date start, Date end) {
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
        if (badgeSkillIds) {
            boolean dateCheck = start != null && end != null
            if (dateCheck) {
                selectUsersQ += dateFrag
            }

            Query getUsers = entityManager.createNativeQuery(selectUsersQ)
            getUsers.setParameter('badgeSkillIds', badgeSkillIds)
            getUsers.setParameter('projectId', projectId)
            getUsers.setParameter('badgeId', badgeId)
            getUsers.setParameter('numBadgeSkills', badgeSkillIds.size())
            if (dateCheck) {
                getUsers.setParameter('start', start)
                getUsers.setParameter('end', end)
            }
            List<String> r = getUsers.getResultList()

            if (r) {
                results.addAll(r)
            }
        }

        int updated = 0
        results?.each {
            String insert = '''
            INSERT INTO user_achievement (user_id, project_id, skill_id, skill_ref_id, notified, points_when_achieved)
            VALUES (:userId, :projectId, :skillId, :skillRefId, :notified, :pointsWhenAchieved )
            '''
            Query insertAchievement = entityManager.createNativeQuery(insert)
            insertAchievement.setParameter("userId", it)
            insertAchievement.setParameter("projectId", projectId)
            insertAchievement.setParameter("skillId", badgeId)
            insertAchievement.setParameter("skillRefId", badgeRowId)
            insertAchievement.setParameter("notified", Boolean.FALSE.toString())
            insertAchievement.setParameter("pointsWhenAchieved", -1)
            insertAchievement.executeUpdate()
            updated++
        }
        return updated
    }

    int addGlobalBadgeAchievementForEligibleUsers(String badgeId,
                                                  Integer badgeRowId,
                                                  Boolean notified,
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
            FROM user_achievement ua
            INNER JOIN global_badge_level_definition g ON g.level=ua.level 
            AND g.project_id = ua.project_id and g.skill_id = :badgeId
            WHERE ua.skill_id is null 
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

        def users = []
        if (!requireLevels) {
            users = usersWithRequiredSkills
        } else if (!requireSkills) {
            users = usersWithRequiredLevel
        } else {
            users = usersWithRequiredSkills.intersect(usersWithRequiredLevel)
        }

        int updated = 0
        users?.each {
            String insert = '''
            INSERT INTO user_achievement (user_id, project_id, skill_id, skill_ref_id, notified, points_when_achieved)
            VALUES (:userId, :projectId, :skillId, :skillRefId, :notified, :pointsWhenAchieved )
            '''
            Query insertAchievement = entityManager.createNativeQuery(insert)
            insertAchievement.setParameter("userId", it)
            insertAchievement.setParameter("projectId", null)
            insertAchievement.setParameter("skillId", badgeId)
            insertAchievement.setParameter("skillRefId", badgeRowId)
            insertAchievement.setParameter("notified", Boolean.FALSE.toString())
            insertAchievement.setParameter("pointsWhenAchieved", -1)
            insertAchievement.executeUpdate()
            updated++
        }
        return updated
    }

    @Override
    void createOrUpdateUserEvent(String projectId, Integer skillRefId, String userId, Date start, String type, Integer count, Integer weekNumber) {
        // find existing event
        String exists = '''
        SELECT id FROM user_events WHERE project_id = :projectId AND skill_ref_id = :skillRefId AND user_id = :userId AND event_time = :start AND week_number = :weekNumber AND event_type = :type
        '''
        Query existsQuery = entityManager.createNativeQuery(exists)
        existsQuery.setParameter("projectId", projectId)
        existsQuery.setParameter("skillRefId", skillRefId)
        existsQuery.setParameter("userId", userId)
        existsQuery.setParameter("start", start)
        existsQuery.setParameter("weekNumber", weekNumber)
        existsQuery.setParameter("type", type)
        List<Integer> existing = existsQuery.getResultList()
        if (existing) {
            String existingId = existing.first()
            String update = '''
                UPDATE user_events SET count = count+:count WHERE id = :id
            '''
            Query updateQuery = entityManager.createNativeQuery(update)
            updateQuery.setParameter("id", existingId)
            updateQuery.setParameter("count", count)
            updateQuery.executeUpdate()
        } else {
            String insertSql = '''
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
            )
            '''
            Query query = entityManager.createNativeQuery(insertSql)
            query.setParameter("projectId", projectId)
            query.setParameter("skillRefId", skillRefId)
            query.setParameter("userId", userId)
            query.setParameter("start", start)
            query.setParameter("count", count)
            query.setParameter("type", type)
            query.setParameter("weekNumber", weekNumber)
            query.executeUpdate()
        }
    }

    @Override
    long countUsers(QueryUsersCriteria queryUsersCriteria) {
        String sql = QueryUserCriteriaHelper.generateCountSql(queryUsersCriteria)
        if (!sql) {
            return 0
        }
        Query query = entityManager.createNativeQuery(sql)
        QueryUserCriteriaHelper.setCountParams(query, queryUsersCriteria)

        return query.getSingleResult()
    }

    @Override
    Stream<String> getUserIds(QueryUsersCriteria queryUsersCriteria) {
        String sql = QueryUserCriteriaHelper.generateSelectUserIdsSql(queryUsersCriteria)
        if (!sql) {
            return []
        }

        Query query = entityManager.createNativeQuery(sql)
        QueryUserCriteriaHelper.setSelectUserIdParams(query, queryUsersCriteria)

        return query.getResultStream()
    }

    @Override
    void updateUserPointsForASkill(String projectId, String skillId) {
        userPointsRepo.updateUserPointsForASkillInH2(projectId, skillId)
    }


    @Override
    void updateUserPointsForSubject(String projectId, String skillId, Boolean enabledSkillsOnly) {
        userPointsRepo.updateSubjectUserPointsInH2(projectId, skillId, enabledSkillsOnly)
    }

    @Override
    void updateUserPointsForProject(String projectId) {
        userPointsRepo.updateUserPointsForProjectInH2(projectId)
    }

    @Override
    SkillsDBLock insertLockOrSelectExisting(String lockKey) {
        SkillsDBLock lock = skillsDBLockRepo.findByLock(lockKey)
        if (!lock) {
            lock = new SkillsDBLock(lock: lockKey, expires: true)
            try {
                // we have to execute the save in a new transaction as another
                // thread might have inserted the lock row after the above and may be holding
                // a pessmisitic_write lock which results in an exception being thrown by the save call
                // if save isn't performed in a separate transaction, the current session becomes invalidated
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
                transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW")
                transactionTemplate.execute({
                    skillsDBLockRepo.save(lock)
                })
            } catch (Throwable t) {
                log.debug("failed to insert key [{}]", t, lockKey)
            }
            //this second find is necessary so that the pessmisitc_write lock blocks access to this row until the lock is released
            lock = skillsDBLockRepo.findByLock(lockKey)
            assert lock
        }

        return lock
    }


    @Override
    Long countDistinctUsersByProjectIdAndSubjectIdAndUserIdLike(String projectId, String subjectId, String userIdQuery) {

        if (!skillDefRepo.existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(projectId, subjectId, [SkillDef.ContainerType.Subject])) {
            ErrorCode code = ErrorCode.SubjectNotFound
            throw new SkillException("Subject [${subjectId}] doesn't exist.", projectId, null, code)
        }

        List<SkillDefPartial> skills = skillRelDefRepo.getSkillsWithCatalogStatus(projectId, subjectId, [SkillRelDef.RelationshipType.RuleSetDefinition])
        List<String> subjectSkillIds = []
        skills?.each {
            subjectSkillIds.add(it.skillId)
            if (it.skillType == SkillDef.ContainerType.SkillsGroup) {
                skillRelDefRepo.getChildrenPartial(projectId, it.skilLId, SkillRelDef.RelationshipType.SkillsGroupRequirement)?.each { SkillDefPartial groupSkill ->
                    subjectSkillIds.add(groupSkill.skillId)
                }
            }
        }

        return userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(projectId, subjectSkillIds, userIdQuery)
    }

    @Override
    Long countDistinctUsersByProjectIdAndSubjectId(String projectId, String subjectId) {
        if (!skillDefRepo.existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(projectId, subjectId, [SkillDef.ContainerType.Subject])) {
            ErrorCode code = ErrorCode.SubjectNotFound
            throw new SkillException("Subject [${subjectId}] doesn't exist.", projectId, null, code)
        }

        List<SkillDefPartial> skills = skillRelDefRepo.getSkillsWithCatalogStatus(projectId, subjectId, [SkillRelDef.RelationshipType.RuleSetDefinition])
        List<String> subjectSkillIds = []
        skills?.each {
            subjectSkillIds.add(it.skillId)
            if (it.skillType == SkillDef.ContainerType.SkillsGroup) {
                skillRelDefRepo.getChildrenPartial(projectId, it.skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)?.each { SkillDefPartial groupSkill ->
                    subjectSkillIds.add(groupSkill.skillId)
                }
            }
        }

        return userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdIn(projectId, subjectSkillIds)
    }

    @Override
    List<SkillDefPartial> getSkillsWithCatalogStatusExplodeSkillGroups(String projectId, String subjectId) {
        if (!skillDefRepo.existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(projectId, subjectId, [SkillDef.ContainerType.Subject])) {
            ErrorCode code = ErrorCode.SubjectNotFound
            throw new SkillException("Subject [${subjectId}] doesn't exist.", projectId, null, code)
        }

        List<SkillDefPartial> skills = skillRelDefRepo.getSkillsWithCatalogStatus(projectId, subjectId, [SkillRelDef.RelationshipType.RuleSetDefinition])
        List<SkillDefPartial> all = []
        skills?.each {
            all.add(it)
            if (it.skillType == SkillDef.ContainerType.SkillsGroup) {
                skillRelDefRepo.getChildrenPartial(projectId, it.skilLId, SkillRelDef.RelationshipType.SkillsGroupRequirement)?.each { SkillDefPartial groupSkill ->
                    all.add(groupSkill)
                }
            }
        }
        return all
    }
}
