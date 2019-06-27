package skills.service.icons

import groovy.transform.CompileStatic
import org.springframework.stereotype.Component
import skills.storage.model.CustomIcon
/**
 * Created with IntelliJ IDEA.
 * Date: 11/30/18
 * Time: 11:47 AM
 */
@Component
@CompileStatic
class CssGenerator {
    String cssify(Collection<CustomIcon> customIcons) {
        String css = ""

        customIcons?.each{
            String cssClassName = IconCssNameUtil.getCssClass(it.projectId, it.filename)
            List<String> lines = []
            String cssClass = ".${cssClassName} {"
            lines.add(cssClass)
            lines.add("\tbackground-image: url(\"${it.dataUri}\");".toString())
            lines.add("\tbackground-repeat: no-repeat;")
            lines.add("\twidth: 48px;")
            lines.add("\theight: 48px;")
            lines.add("\tdisplay: inline-block;")
            lines.add("}".toString())
            css += "${lines.join('')}"
        }

        return css
    }
}
