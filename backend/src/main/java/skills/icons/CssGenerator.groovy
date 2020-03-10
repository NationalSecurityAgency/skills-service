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
