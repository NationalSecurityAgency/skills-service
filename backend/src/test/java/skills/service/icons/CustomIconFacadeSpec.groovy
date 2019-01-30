//package skills.service.icons
//
//import org.springframework.security.core.GrantedAuthority
//import spock.lang.Specification
//import skills.service.auth.UserInfo
//import skills.service.auth.UserSkillsGrantedAuthority
//import skills.service.datastore.services.IconService
//import skills.storage.model.CustomIcon
//import skills.storage.model.auth.RoleName
//import skills.storage.model.auth.UserRole
//
///**
// * Created with IntelliJ IDEA.
// * Date: 12/10/18
// * Time: 1:55 PM
// */
//class CustomIconFacadeSpec extends Specification{
//
//    CssGenerator cssGenerator = new CssGenerator(iconHeight: "48px", iconWidth: "48px", iconPort: 8443, iconProtocol: "https", iconHost: "localhost")
//
//    def "generate icon index for user"(){
//        UserInfo userInfo = Mock(UserInfo)
//        UserRole mockRole = new UserRole(projectId: "proj", roleName:  RoleName.ROLE_PROJECT_ADMIN)
//        GrantedAuthority mockAuth = new UserSkillsGrantedAuthority(role: mockRole)
//
//        UserRole mockRole2 = new UserRole(projectId: "proj2", roleName:  RoleName.ROLE_PROJECT_ADMIN)
//        GrantedAuthority mockAuth2 = new UserSkillsGrantedAuthority(role: mockRole2)
//
//        userInfo.authorities >> [mockAuth, mockAuth2]
//
//        IconService iconService = Mock(IconService){
//            1 * getIconsForProject("proj") >> [new CustomIcon(projectId: "proj", filename: "file.png", id:  1)]
//            1 * getIconsForProject("proj2") >> []
//        }
//
//        when:
//        IconManifest manifest = new CustomIconFacade(iconService: iconService).generateJsIconIndexForUser(userInfo)
//
//        then:
//        manifest.icons
//        manifest.icons.size() == 1
//    }
//
//    def "generate icon index, handles SUPER_DUPER_USER correctly"(){
//        UserInfo userInfo = Mock(UserInfo)
//        UserRole mockRole = new UserRole(projectId: null, roleName:  RoleName.ROLE_SUPER_DUPER_USER)
//        GrantedAuthority mockAuth = new UserSkillsGrantedAuthority(role: mockRole)
//
//
//        userInfo.authorities >> [mockAuth]
//
//        IconService iconService = Mock(IconService){
//            1 * getAllIcons() >> [new CustomIcon(projectId: "proj", filename: "file.png", id:  1)]
//        }
//
//        when:
//        IconManifest manifest = new CustomIconFacade(iconService: iconService).generateJsIconIndexForUser(userInfo)
//
//        then:
//        manifest.icons
//        manifest.icons.size() == 1
//    }
//
//    def "generate css for project"(){
//        IconService iconService = Mock(IconService){
//            1 * getIconsForProject("proj") >> [new CustomIcon(projectId: "proj", filename: "file.png", id:  1)]
//        }
//
//        when:
//        String css = new CustomIconFacade(cssGenerator: cssGenerator, iconService: iconService).generateCss("proj")
//
//        then:
//        css
//        //we already test the css format generation in another spec
//        css.contains("file.png")
//    }
//
//    def "generate css for user"(){
//        UserInfo userInfo = Mock(UserInfo)
//        UserRole mockRole = new UserRole(projectId: "proj", roleName:  RoleName.ROLE_PROJECT_ADMIN)
//        GrantedAuthority mockAuth = new UserSkillsGrantedAuthority(role: mockRole)
//
//        UserRole mockRole2 = new UserRole(projectId: "proj2", roleName:  RoleName.ROLE_PROJECT_ADMIN)
//        GrantedAuthority mockAuth2 = new UserSkillsGrantedAuthority(role: mockRole2)
//
//        userInfo.authorities >> [mockAuth, mockAuth2]
//
//        IconService iconService = Mock(IconService){
//            1 * getIconsForProject("proj") >> [new CustomIcon(projectId: "proj", filename: "file.png", id:  1)]
//            1 * getIconsForProject("proj2") >> []
//        }
//
//        when:
//        String css = new CustomIconFacade(cssGenerator: cssGenerator, iconService: iconService).generateCssForUser(userInfo)
//
//        then:
//        css
//        //we already test the css format generation in another spec
//        css.contains("file.png")
//    }
//
//    def "generate css for user, handles SUPER_DUPER_USER correctly"(){
//        UserInfo userInfo = Mock(UserInfo)
//        UserRole mockRole = new UserRole(projectId: null, roleName:  RoleName.ROLE_SUPER_DUPER_USER)
//        GrantedAuthority mockAuth = new UserSkillsGrantedAuthority(role: mockRole)
//
//
//        userInfo.authorities >> [mockAuth]
//
//        IconService iconService = Mock(IconService){
//            1 * getAllIcons() >> [new CustomIcon(projectId: "proj", filename: "file.png", id:  1)]
//        }
//
//        when:
//        String css = new CustomIconFacade(cssGenerator: cssGenerator, iconService: iconService).generateCssForUser(userInfo)
//
//        then:
//        css
//        //we already test the css format generation in another spec
//        css.contains("file.png")
//    }
//
//    def "save icon"(){
//        IconService iconService = Mock(IconService){
//            1 * saveIcon(_)
//        }
//
//        when:
//        UploadedIcon icon = new CustomIconFacade(cssGenerator: cssGenerator, iconService: iconService).saveIcon("proj", "file.png", "image/png", "content".bytes)
//
//        then:
//        icon
//        icon.cssClassName == "proj-filepng"
//    }
//
//    def "get icon"(){
//        IconService iconService = Mock(IconService){
//            1 * loadIcon("file.png", "proj") >> new CustomIcon(imageContent: "content".bytes)
//        }
//        CustomIconFacade facade = new CustomIconFacade(iconService: iconService)
//
//        when:
//        byte[] result = facade.getIcon("proj", "file.png")
//
//        then:
//        result
//        new String(result) == "content"
//    }
//
//    def "get non-existing icon"(){
//        IconService iconService = Mock(IconService){
//            1 * loadIcon("none.png", "foo") >> null
//        }
//        CustomIconFacade facade = new CustomIconFacade(iconService: iconService)
//
//        when:
//        byte[] result = facade.getIcon("foo", "none.png")
//
//        then:
//        !result
//    }
//
//}
