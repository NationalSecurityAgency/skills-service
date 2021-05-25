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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

class PublicConfigSpecs extends DefaultIntSpec {

    def "retrieve public configs"() {
        when:
        def config = skillsService.getPublicConfigs()
        then:
        config
        config.descriptionMaxLength == "2000"
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "public configs should return install mode"() {
        when:
        def config = skillsService.getPublicConfigs()
        then:
        config
        config.authMode == "FORM"
    }

    @Requires({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "public configs should return install mode - pki"() {
        when:
        def config = skillsService.getPublicConfigs()
        then:
        config
        config.authMode == "PKI"
    }

    def "needToBootstrap should be true if root user doesn't exist"() {
        when:
        def noRootConfig = skillsService.getPublicConfigs()
        skillsService.grantRoot()
        def withRootConfig = skillsService.getPublicConfigs()
        then:
        noRootConfig
        noRootConfig.needToBootstrap == true

        withRootConfig
        withRootConfig.needToBootstrap == false
    }

    def "public configs should include custom header/footer"() {
        skillsService.grantRoot()
        skillsService.saveSystemSettings("http://not-real.fakefakefake", "PT1H", "noreply@fakefakefake", "<div>header</div>", "<div>footer</div>")
        when:
        def configs = skillsService.getPublicConfigs()

        then:
        configs.customHeader == "<div>header</div>"
        configs.customFooter == "<div>footer</div>"
    }

    def "retrieve public Client Display configs"() {
        when:
        def config = skillsService.getPublicClientDisplayConfigs()
        then:
        config
        config.docsHost == "https://code.nsa.gov/skills-docs"
    }
}
