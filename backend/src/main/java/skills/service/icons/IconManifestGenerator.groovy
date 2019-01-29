package skills.service.icons

import skills.storage.model.CustomIcon

/**
 * Created with IntelliJ IDEA.
 * Date: 11/30/18
 * Time: 1:52 PM
 */
class IconManifestGenerator {

    public static IconManifest generateIconManifiest(Collection<CustomIcon> customIcons){
        List<IconCss> icons = []

        IconManifest manifest = new IconManifest()
        manifest.name = "custom-icons"
        manifest.generated = new Date()
        customIcons?.each{
            IconCss iconCss = new IconCss(name:  it.filename, cssClass: IconCssNameUtil.getCssClass(it.projectId, it.filename))
            icons.add(iconCss)
        }
        manifest.icons = icons

        return manifest

    }
}
