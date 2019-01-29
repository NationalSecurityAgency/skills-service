package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.auth.AllowedOrigin

interface AllowedOriginRepo extends CrudRepository<AllowedOrigin, Integer> {
    List<AllowedOrigin> findAllByProjectId(String projectId)
}
