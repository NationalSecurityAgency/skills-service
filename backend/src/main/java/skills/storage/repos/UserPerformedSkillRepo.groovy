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

import javax.validation.constraints.Null

@CompileStatic
interface UserPerformedSkillRepo extends JpaRepository<UserPerformedSkill, Integer> {

    // find an exact performed event
    @Nullable
    UserPerformedSkill findByProjectIdAndSkillIdAndUserIdAndPerformedOn(String projectId, String skillId, String userId, Date performedOn)

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    Long countByUserIdAndProjectIdAndSkillIdContaining(String userId, String projectId, String skillId)
    Long countByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)
    Long countByUserIdAndProjectIdAndSkillIdAndPerformedOnGreaterThanAndPerformedOnLessThan(String userId, String projectId, String skillId, Date startDate, Date endDate)

    @Query("SELECT DISTINCT(p.userId) from UserPerformedSkill p where p.projectId=?1 and lower(p.userId) LIKE %?2%" )
    List<String> findDistinctUserIdsForProject(String projectId, String userIdQuery, Pageable pageable)

    @Query("SELECT DISTINCT(p.userId) from UserPerformedSkill p where lower(p.userId) LIKE %?1%" )
    List<String> findDistinctUserIds(String userIdQuery, Pageable pageable)

    Boolean existsByUserId(String userId)
    Boolean existsByProjectIdAndUserId(String userId, String projectId)

    List<UserPerformedSkill> findByUserIdAndProjectIdAndSkillIdContaining(String userId, String projectId, String skillId, Pageable pageable)

    @Query('SELECT COUNT(DISTINCT p.skillId) from UserPerformedSkill p where p.projectId=?1 and p.userId = ?2')
    Integer countDistinctSkillIdByProjectIdAndUserId(String projectId, String userId)

    @Query(''' select DISTINCT(sdParent)
        from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild
            inner join UserPerformedSkill ups on sdParent.id = ups.skillRefId and ups.userId=?1
        where 
            srd.parent=sdParent.id and 
            srd.child=sdChild.id and
            sdChild.projectId=?2 and 
            sdChild.skillId=?3 and 
            srd.type='Dependence' ''')
    List<SkillDef> findPerformedParentSkills(String userId, String projectId, String skillId)

    @Nullable
    @Query('''select SUM(sdChild.pointIncrement)
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserPerformedSkill ups
      where
          srd.parent=sdParent.id and 
          srd.child=sdChild.id and
          sdChild.id = ups.skillRefId and 
          sdChild.projectId=:projectId and
          sdParent.projectId=:projectId and 
          ups.userId=:userId and
          sdParent.skillId=:skillId and
          sdChild.version<=:version and
          srd.type='RuleSetDefinition' and
          (CAST(ups.performedOn as date)=:day OR CAST(:day as date) is null)''')
    Integer calculateUserPointsByProjectIdAndUserIdAndAndDayAndVersion(@Param('projectId') String projectId,
                                                                       @Param('userId') String userId,
                                                                       @Param('skillId') String skillId,
                                                                       @Param('version') Integer version,
                                                                       @Param('day') @Nullable Date day)

    @Nullable
    @Query('''select CAST(ups.performedOn as date) as day, SUM(sdChild.pointIncrement) as count
    from SkillDef sdParent, SkillRelDef srd, SkillDef sdChild, UserPerformedSkill ups
      where
          sdParent.projectId = :projectId and
          sdChild.projectId = :projectId and
          srd.parent=sdParent.id and 
          srd.child=sdChild.id and
          sdChild.id = ups.skillRefId and 
          ups.userId=:userId and
          (sdParent.skillId=:skillId OR :skillId is null) and
          sdChild.version<=:version and
          srd.type='RuleSetDefinition'
       group by CAST(ups.performedOn as date)''')
    List<DayCountItem> calculatePointHistoryByProjectIdAndUserIdAndVersion(@Param('projectId') String projectId,
                                                                           @Param('userId') String userId,
                                                                           @Nullable @Param('skillId') String skillId,
                                                                           @Param('version') Integer version)
}
