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
                customIcon.setProjDef(project)
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
}
