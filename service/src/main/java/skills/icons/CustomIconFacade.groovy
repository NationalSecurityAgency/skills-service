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
package skills.icons

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.CustomIconResult
import skills.services.IconService
import skills.storage.model.CustomIcon
import skills.storage.model.ProjDef
import skills.storage.accessors.ProjDefAccessor
import skills.storage.repos.SkillDefRepo

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Service
@CompileStatic
@Slf4j
class CustomIconFacade {

    @Autowired
    IconService iconService

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    CssGenerator cssGenerator

    /**
     * Generates a css style sheet containing all the custom icons fore the specified project id
     *
     * @param projectId
     * @return css or an empty string if no custom icons are found
     */
    @Transactional(readOnly = true)
    String generateCss(String projectId){
        Validate.notNull(projectId, "projectId is required")
        Collection<CustomIcon> icons = iconService.getIconsForProject(projectId)
        return cssGenerator.cssify(icons)
    }

    /**
     * Generates a css style sheet containing all the custom icons fore the specified project id
     *
     * @param projectId
     * @return css or an empty string if no custom icons are found
     */
    @Transactional(readOnly = true)
    String generateGlobalCss(){
        Collection<CustomIcon> icons = iconService.getGlobalIcons()
        return cssGenerator.cssify(icons)
    }

    @Transactional
    def copyIcons(String fromProjectId, String toProjectId) {
        def fromIcons = iconService.getIconsForProject(fromProjectId)
        ProjDef project = projDefAccessor.getProjDef(toProjectId)
        def iconsToUpdate = new HashMap<String, String>()
        Collection<CustomIcon> newIcons = new ArrayList<CustomIcon>()

        fromIcons.each{
            CustomIcon newIcon = new CustomIcon(
                    projectId: toProjectId,
                    filename: it.filename,
                    contentType: it.contentType,
                    width: it.width,
                    height: it.height,
                    dataUri: it.dataUri,
                    projRefId: project.id
            )
            newIcons.push(newIcon)
            String oldClassName = IconCssNameUtil.getCssClass(fromProjectId, it.filename)
            String newClassName = IconCssNameUtil.getCssClass(toProjectId, it.filename)
            iconsToUpdate[oldClassName] = newClassName
        }

        iconService.saveAllIcons(newIcons)

        return iconsToUpdate

    }

    /**
     * Stores an icon and returns the css class to be used for that icon
     * @param projectId
     * @param iconFilename
     * @param contentType
     * @param file
     * @return
     */
    @Transactional
    UploadedIcon saveIcon(String projectId, String iconFilename, String contentType, byte[] file){
        Validate.notNull(iconFilename, "iconFilename is required")
        Validate.notNull(file, "file is required")

        try {
            String dataUri = "data:${contentType};base64,${Base64.getEncoder().encodeToString(file)}"


            def imageDimensions = getImageDimensions(file);

            CustomIcon customIcon = new CustomIcon(
                projectId: projectId,
                filename: iconFilename,
                contentType: contentType,
                width: imageDimensions['width'] as Integer,
                height: imageDimensions['height'] as Integer,
                dataUri: dataUri)

            if (projectId) {
                ProjDef project = projDefAccessor.getProjDef(projectId)
                customIcon.projRefId = project.id
            }

            iconService.saveIcon(customIcon)

            String uploadedCss = cssGenerator.cssify([customIcon])
            String cssClassName = IconCssNameUtil.getCssClass(customIcon.projectId, customIcon.filename)

            return new UploadedIcon(cssClassName: cssClassName, cssDefinition: uploadedCss, name: iconFilename)
        }catch(Exception cve){
            log.error("unable to save icon $iconFilename for project $projectId")
            throw cve;
        }
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getGlobalCustomIcons() {
        return iconService.getGlobalCustomIcons()
    }

    private def getImageDimensions(byte[] imageBytes) {
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);
            return [
                width: image.getWidth(),
                height: image.getHeight(),
            ];
        } finally {
            bis.close();
        }
    }

    void deleteIcon(String projectId, String filename){
        Validate.notNull(projectId, "projectId is required")
        Validate.notNull(filename, "filename is required")
        iconService.deleteIcon(projectId, filename)
    }

    void deleteGlobalIcon(String filename){
        Validate.notNull(filename, "filename is required")
        iconService.deleteGlobalIcon(filename)
    }

    List<String> findUsages(String projectId, String iconClass) {
        skillDefRepo.findNameByProjectIdAndIconClass(projectId, iconClass)
    }
}
