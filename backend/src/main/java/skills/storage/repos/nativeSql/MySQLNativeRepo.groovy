package skills.storage.repos.nativeSql

import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import skills.storage.model.SkillDef

import javax.persistence.Query;

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Conditional(DBConditions.MySQL)
@Service
class MySQLNativeRepo implements NativeQueriesRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    void decrementPointsForDeletedSkill(String projectId, String deletedSkillId, String parentSubjectSkillId) {
        String q = '''UPDATE user_points a join user_points b on a.user_id = b.user_id and (a.day = b.day OR (a.day is null and b.day is null)) 
        set b.points = b.points - a.points
        where a.project_id = :projectId and a.skill_id= :deletedSkillId and (b.skill_id= :parentSubjectSkillId or b.skill_id is null) and b.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("parentSubjectSkillId", parentSubjectSkillId)
        query.setParameter("deletedSkillId", deletedSkillId)
        query.executeUpdate();
    }

    @Override
    void updateOverallScoresBySummingUpAllChildSubjects(String projectId, SkillDef.ContainerType subjectType) {
        String q = '''
         update user_points points,
          (
            select
              user_id                 sumUserId,
              day                     sumDay,
              SUM(pointsInner.points) sumPoints
            from user_points pointsInner
              join skill_definition definition
                on pointsInner.project_id = definition.project_id and pointsInner.skill_id = definition.skill_id and
                   definition.type = :subjectType
            where pointsInner.project_id = :projectId and definition.project_id = :projectId
            group by user_id, day
          ) sum
        set points.points = sum.sumPoints
        where sum.sumUserId = points.user_id and (sum.sumDay = points.day OR (sum.sumDay is null and points.day is null)) and points.skill_id is null and points.project_id = :projectId'''.toString()

        Query query = entityManager.createNativeQuery(q);
        query.setParameter("projectId", projectId);
        query.setParameter("subjectType", subjectType.toString())
        query.executeUpdate();
    }
}
