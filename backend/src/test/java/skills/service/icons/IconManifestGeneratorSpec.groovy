package skills.service.icons

import spock.lang.Specification
import skills.storage.model.CustomIcon

/**
 * Created with IntelliJ IDEA.
 * Date: 12/10/18
 * Time: 1:28 PM
 */
class IconManifestGeneratorSpec extends Specification {

    def "generates IconManifest"(){
        CustomIcon icon1 = new CustomIcon(projectId: "proj", filename: "file.png", id:  1)
        CustomIcon icon2 = new CustomIcon(projectId: "proj", filename: "file2.jpg", id:  2)
        def icons = [icon1, icon2]

        when:
        IconManifest manifest = IconManifestGenerator.generateIconManifiest(icons)

        then:
        manifest.name == "custom-icons"
        manifest.icons?.size() == 2
        manifest.icons.each{
            assert it.name in ["file.png", "file2.jpg"]
            assert it.cssClass in ["proj-filepng", "proj-file2jpg"]
        }
    }

    def "generates empty IconManifest from empty icons"(){
        when:
        IconManifest manifest = IconManifestGenerator.generateIconManifiest([])

        then:
        manifest.name == "custom-icons"
        !manifest.icons
    }
}
