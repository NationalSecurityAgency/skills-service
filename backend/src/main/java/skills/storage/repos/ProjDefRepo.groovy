package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef

interface ProjDefRepo extends CrudRepository<ProjDef, Long> {

    @Nullable
    ProjDef findByProjectIdIgnoreCase(String projectId)

    void deleteByProjectIdIgnoreCase(String projectId)

    @Nullable
    ProjDef findByProjectId(String projectId)

    @Nullable
    ProjDef findByNameIgnoreCase(String projectId)

    boolean existsByProjectIdIgnoreCase(String projectId)
    boolean existsByNameIgnoreCase(String projectName)

    @Query(value = "select p.* from project_definition p, user_roles u where p.project_id = u.project_id and u.user_id=?1 order by p.display_order", nativeQuery = true)
    List<ProjDef> getProjectsByUser(String userId)

    @Query(value = "SELECT COUNT(DISTINCT s.userId) from UserPoints s where s.projectId=?1 and s.day is null")
    int calculateDistinctUsers(String projectId)

    @Query("select p from ProjDef p where lower(p.name) LIKE %?1%" )
    List<ProjDef> queryProjectsByNameQuery(String nameQuery)
}
