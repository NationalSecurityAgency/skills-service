package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import skills.storage.model.SkillDef
import skills.storage.model.UserPerformedSkill

interface UserPerformedSkillRepo extends JpaRepository<UserPerformedSkill, Integer> {

    void deleteByProjectIdAndSkillId(String projectId, String skillId)

    Long countByUserIdAndProjectIdAndSkillIdContaining(String userId, String projectId, String skillId)
    Long countByUserIdAndProjectIdAndSkillId(String userId, String projectId, String skillId)
    Long countByUserIdAndProjectIdAndSkillIdAndPerformedOnGreaterThanAndPerformedOnLessThan(String userId, String projectId, String skillId, Date startDate, Date endDate)

    @Query("SELECT DISTINCT(p.userId) from UserPerformedSkill p where p.projectId=?1" )
    List<String> findDistinctUserIdsForProject(String projectId)

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
    inner join UserPerformedSkill ups on sdParent.skillId = ups.skillId and ups.userId=?1
      where srd.parent=sdParent.id and srd.child=sdChild.id and
      sdChild.projectId=?2 and sdChild.skillId=?3 and srd.type='Dependence' ''')
    List<SkillDef> findPerformedParentSkills(String userId, String projectId, String skillId)
}
