package skills.icons

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import org.springframework.stereotype.Component
import skills.storage.model.CustomIcon

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

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
            lines.add("\twidth: ${it.width}px;".toString())
            lines.add("\theight: ${it.height}px;".toString())
            lines.add("\tdisplay: inline-block;")
            lines.add("}".toString())
            css += "${lines.join('')}"
        }

        return css
    }
}
