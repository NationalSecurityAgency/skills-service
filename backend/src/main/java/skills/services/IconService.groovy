package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.CustomIconResult
import skills.icons.IconCssNameUtil
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

    @Transactional(readOnly = true)
    Collection<CustomIcon> getIconsForProject(String projectId){
        return iconRepo.findAllByProjectId(projectId)
    }

    @Transactional(readOnly = true)
    Collection<CustomIcon> getGlobalIcons(){
        return iconRepo.findAllByProjectIdIsNull()
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
    void deleteGlobalIcon(String filename){
        iconRepo.deleteByProjectIdAndFilename(null, filename)
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getGlobalCustomIcons(){
        return iconRepo.findAllByProjectIdIsNull().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass('GLOBAL', icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }

}
