package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec

@Slf4j
@SpringBootTest(properties = ['skills.config.ui.rankingAndProgressViewsEnabled=false', 'skills.config.ui.defaultLandingPage=progress'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class RankingAndProgressViewsDisabledIT extends DefaultIntSpec {

    def "landingPage page is always admin when rankingAndProgressViewsDisabled=true"() {

        def currentUser1 = skillsService.getCurrentUser()
        def res1 = skillsService.getUserSettings()
        when:

        skillsService.changeUserSettings([
                [userId: 'skills@skills.org', settingGroup: 'user.prefs', setting: "home_page", value: "progress"],
        ])
        def currentUser2 = skillsService.getCurrentUser()
        def res2 = skillsService.getUserSettings()

        then:
        currentUser1.landingPage == 'admin'  // default
        !res1

        currentUser2.landingPage == 'admin'  // must NOT be overridden
        res2.size() == 1
        res2.get(0).userId == 'skills@skills.org'
        res2.get(0).setting == 'home_page'
        res2.get(0).value == "progress"
        res2.get(0).settingGroup == "user.prefs"
        res2.get(0).projectId == null
    }
}
