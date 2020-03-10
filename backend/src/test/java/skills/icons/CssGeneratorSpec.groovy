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

import skills.icons.CssGenerator
import spock.lang.Specification
import skills.storage.model.CustomIcon

class CssGeneratorSpec extends Specification {

    def "generates css"(){

        String mockDataUri1 = "data:image/png;base64:rarararararararararachacha"
        String mockDataUri2 = "data:image/png;base64:rararararachacha"

        CustomIcon icon1 = new CustomIcon(projectId: "proj", width: 48, height: 48, dataUri: mockDataUri1, filename: "file.png", id:  1)
        CustomIcon icon2 = new CustomIcon(projectId: "proj", width: 48, height: 48, dataUri: mockDataUri2, filename: "file2.jpg", id:  2)

        def icons = [icon1, icon2]

        when:
        String css = new CssGenerator().cssify(icons)

        then:
        css == '''.proj-filepng {\tbackground-image: url("''' + mockDataUri1 + '''");\tbackground-repeat: no-repeat;\twidth: 48px;\theight: 48px;\tdisplay: inline-block;}.proj-file2jpg {\tbackground-image: url("''' + mockDataUri2 + '''");\tbackground-repeat: no-repeat;\twidth: 48px;\theight: 48px;\tdisplay: inline-block;}'''
    }

    def "generates css no host"(){

        String mockDataUri1 = "data:image/png;base64:rarararararararararachacha"
        String mockDataUri2 = "data:image/png;base64:rararararachacha"

        CustomIcon icon1 = new CustomIcon(projectId: "proj", width: 48, height: 48, dataUri: mockDataUri1, filename: "file.png", id:  1)
        CustomIcon icon2 = new CustomIcon(projectId: "proj", width: 48, height: 48, dataUri: mockDataUri2, filename: "file2.jpg", id:  2)

        def icons = [icon1, icon2]

        when:
        String css = new CssGenerator().cssify(icons)

        then:
        css == '''.proj-filepng {\tbackground-image: url("''' + mockDataUri1 + '''");\tbackground-repeat: no-repeat;\twidth: 48px;\theight: 48px;\tdisplay: inline-block;}.proj-file2jpg {\tbackground-image: url("''' + mockDataUri2 + '''");\tbackground-repeat: no-repeat;\twidth: 48px;\theight: 48px;\tdisplay: inline-block;}'''
    }

    def "generates empty css when empty icons"(){

        def icons = []

        when:
        String css = new CssGenerator().cssify(icons)

        then:
        css == ""
    }


}
