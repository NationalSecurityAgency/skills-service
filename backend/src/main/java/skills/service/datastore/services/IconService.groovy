package skills.service.datastore.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.CustomIcon
import skills.storage.repos.CustomIconRepo

/**
 * Created with IntelliJ IDEA.
 * Date: 11/30/18
 * Time: 11:12 AM
 */
@Slf4j
@Service
class IconService {

    @Autowired
    CustomIconRepo iconRepo

    Collection<CustomIcon> getIconsForProject(String projectId){
        return iconRepo.findAllByProjectId(projectId)
    }

    Iterable<CustomIcon> getAllIcons(){
        return iconRepo.findAll()
    }

    void saveIcon(CustomIcon icon){
        iconRepo.save(icon)
    }

    CustomIcon loadIcon(String filename, String projectId){
        return iconRepo.findByProjectIdAndFilename(projectId, filename)
    }
    void deleteIcon(String projectId, String filename){
        iconRepo.deleteByProjectIdAndFilename(projectId, filename)
    }

}
