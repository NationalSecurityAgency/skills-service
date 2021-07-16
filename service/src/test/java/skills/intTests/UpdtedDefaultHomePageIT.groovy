package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec

@Slf4j
@SpringBootTest(properties = ['skills.config.ui.rankingAndProgressViewsEnabled=true', 'skills.config.ui.defaultLandingPage=progress'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UpdtedDefaultHomePageIT extends DefaultIntSpec {

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
        currentUser1.landingPage == 'progress'  // default is updated to progress
    }
}
