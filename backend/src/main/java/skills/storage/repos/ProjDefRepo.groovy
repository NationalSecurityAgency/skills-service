package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.ProjDef

interface ProjDefRepo extends CrudRepository<ProjDef, Long> {
    ProjDef findByProjectId(String projectId)

    boolean existsByProjectId(String projectId)
    boolean existsByName(String projectName)

    @Query(value = "select p.* from project_definition p, user_roles u where p.project_id = u.project_id and u.user_id=?1 order by p.display_order", nativeQuery = true)
    List<ProjDef> getProjectsByUser(String userId)

    @Query(value = "SELECT COUNT(DISTINCT s.userId) from UserPoints s where s.projectId=?1")
    int calculateDistinctUsers(String projectId)

    @Query("select p from ProjDef p where lower(p.name) LIKE %?1%" )
    List<ProjDef> queryProjectsByNameQuery(String nameQuery)

    @Query("SELECT DISTINCT s.version from SkillDef s where s.projectId=?1 ORDER BY s.version ASC")
    List<Integer> getUniqueVersionList(String projectId)
}
