package skills.storage.repos

import org.springframework.data.domain.Pageable
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

    @Query(value = "select p from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    List<ProjDef> getProjectsByUser(String userId)

    @Query(value = "select count(p.id) from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    Integer getProjectsByUserCount(String userId)

    @Query("select p from ProjDef p where lower(p.name) LIKE %?1% and p.projectId<>?2" )
    List<ProjDef> queryProjectsByNameQueryAndNotProjectId(String nameQuery, String notProjectId, Pageable pageable)
}
