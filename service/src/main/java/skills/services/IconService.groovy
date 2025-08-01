/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    Collection<CustomIcon> getIconsForGlobalBadge(String globalBadgeId){
        return iconRepo.findAllByGlobalBadgeId(globalBadgeId)
    }

    @Transactional(readOnly = true)
    CustomIcon getIconByProjectIdAndFilename(String projectId, String filename){
        return iconRepo.findByProjectIdAndFilename(projectId, filename)
    }

    Iterable<CustomIcon> getAllIcons(){
        return iconRepo.findAll()
    }

    void saveIcon(CustomIcon icon){
        iconRepo.save(icon)
    }

    void saveAllIcons(Collection<CustomIcon> icons) {
        iconRepo.saveAll(icons)
    }

    CustomIcon loadIcon(String filename, String projectId){
        return iconRepo.findByProjectIdAndFilename(projectId, filename)
    }
    void deleteProjectIcon(String projectId, String filename){
        iconRepo.deleteByProjectIdAndGlobalBadgeIdAndFilename(projectId, null, filename)
    }
    void deleteGlobalBadgeIcon(String globalBadgeId, String filename){
        iconRepo.deleteByProjectIdAndGlobalBadgeIdAndFilename(null, globalBadgeId, filename)
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getGlobalBadgeCustomIcons(String globalBadgeId) {
        return iconRepo.findAllByGlobalBadgeId(globalBadgeId).collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass('GLOBAL', icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }

}
