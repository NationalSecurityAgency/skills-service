package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.CustomIcon

import org.springframework.transaction.annotation.Transactional

interface CustomIconRepo extends CrudRepository<CustomIcon, Integer> {

    //TODO: add method that loads custom icons without loading the binary column
    //generating css doesn't need to load that and it could make performance a bit better
    @Transactional(readOnly = true)
    List<CustomIcon> findAllByProjectId(String projectId)

    CustomIcon findByProjectIdAndFilename(String projectId, String filename)

    void delete(CustomIcon toDelete)

    @Transactional
    void deleteByProjectIdAndFilename(String projectId, String filename)
}
