package skills.service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.util.DigestUtils
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.service.icons.CustomIconFacade
import skills.service.icons.IconManifest

import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit

@RestController()
@RequestMapping("/icons")
@Slf4j
class CustomIconController {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    CustomIconFacade iconFacade

    ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    //TODO: Will need to be housed under api/?
    /**
     * Generate the icon css for a specific project-id, this would be used by external clients
     * where only one project-id is being interacted with
     * @param projectId
     * @param response
     */
    @RequestMapping(value = "/custom-icons/{projectId}", method = RequestMethod.GET, produces = "text/css")
    public void getCss(@PathVariable("projectId") String projectId, HttpServletResponse response){
        CacheControl cacheControl = CacheControl.maxAge(36, TimeUnit.HOURS).cachePublic()

        setCaching(response, cacheControl)

        String css = iconFacade.generateCss(projectId)
        response.setContentLength(css?.bytes?.length)
        if (css) {
            response.addHeader("Etag", DigestUtils.md5DigestAsHex(css.bytes))
        }
        writeContentToOutput(response, css?.bytes)
    }

    /**
     * This generates the custom-icon-index.js for users of the admin interface, in that case,
     * it is necessary to generate css based on all the project-ids that the user has access to
     * as we won't know which one they will be viewing/working with in the admin interface
     * @param response
     */
    @RequestMapping(value = "/custom-icon-index", method = RequestMethod.GET, produces = "application/json")
    public void getIconManifestForUser(HttpServletResponse response){
        CacheControl cacheControl = CacheControl.maxAge(36, TimeUnit.HOURS).cachePublic()

        setCaching(response, cacheControl)

        IconManifest manifest = iconFacade.generateJsIconIndexForUser(userInfoService.getCurrentUser())
        String json = objectMapper.writer().writeValueAsString(manifest)
        response.setContentLength(json?.bytes?.length)
        response.setContentType("application/json")

        if (json){
            response.addHeader("Etag", DigestUtils.md5DigestAsHex(json.bytes))
        }
        writeContentToOutput(response, json?.bytes)
    }

    /**
     * This generates the custom-icon.css for users of the admin interface, in that case,
     * it is necessary to generate css based on all the project-ids that the user has access to
     * as we won't know which one they will be viewing/working with in the admin interface
     * @param response
     */
    @RequestMapping(value = "/custom-icon", method = RequestMethod.GET, produces = "text/css")
    public void getIconCssForUser(HttpServletResponse response){
        CacheControl cacheControl = CacheControl.maxAge(36, TimeUnit.HOURS).cachePublic()

        setCaching(response, cacheControl)
        response.setContentType("text/css")

        UserInfo currentUser = userInfoService.getCurrentUser()
        String css
        if (currentUser) {
            css = iconFacade.generateCssForUser(currentUser)
            if (css) {
                response.setContentLength(css?.bytes?.length)
                response.addHeader("Etag", DigestUtils.md5DigestAsHex(css.bytes))
            }
        }
        writeContentToOutput(response, css?.bytes)
    }

    private static void setCaching(HttpServletResponse response, CacheControl cacheControl) {
        response.addHeader("Cache-Control", cacheControl.getHeaderValue())
        response.setDateHeader("Last-Modified", System.currentTimeMillis())
    }

    private static void writeContentToOutput(HttpServletResponse response, byte[] content){
        if (content){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content)
            response.setStatus(HttpStatus.OK.value())
            IOUtils.copy(byteArrayInputStream, response.getOutputStream())
            response.getOutputStream().flush()
        }else{
            response.setStatus(HttpStatus.NO_CONTENT.value())
        }
    }

}
