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

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import spock.lang.Timeout

import java.util.regex.Pattern

class SettingsSpecs extends DefaultIntSpec {

    def "save and get a single project setting"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        String name = "set1"
        when:
        skillsService.changeSetting(proj1.projectId, name, [projectId: proj1.projectId, setting: name, value: "true"])
        def res = skillsService.getSetting(proj1.projectId, name)
        then:
        res.projectId == proj1.projectId
        res.setting == name
        res.value == "true"
    }

    def "fail to save setting for a project that doesn't exist"() {
        String proj = "dontexist"
        String name = "set1"
        when:
        skillsService.changeSetting(proj, name, [projectId: proj, setting: name, value: "true"])
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "get setting that doesn't exist"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        String name = "set1"
        when:
        skillsService.getSetting(proj1.projectId, name)
        then:
        // skillsService throws an exception if result is null/empty, but this endpoint returns
        // null if setting is not found
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.OK
    }

    def "get settings for a project - no settings defined yet"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        def res = skillsService.getSettings(proj1.projectId)
        then:
        res.size() == 0
    }

    def "get settings for a project - one setting was defined"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        String name = "set1"
        when:
        skillsService.changeSetting(proj1.projectId, name, [projectId: proj1.projectId, setting: name, value: "true"])
        def res = skillsService.getSettings(proj1.projectId)
        then:
        res.size() == 1
        res.get(0).projectId == proj1.projectId
        res.get(0).setting == name
        res.get(0).value == "true"
    }

    def "get settings for a project - several settings"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        skillsService.changeSettings(proj1.projectId, [
                [projectId: proj1.projectId, setting: "set1", value: "true"],
                [projectId: proj1.projectId, setting: "set2", value: "val2"],
                [projectId: proj1.projectId, setting: "set3", value: "val3"],
        ])
        def res = skillsService.getSettings(proj1.projectId)
        res = res.sort { it.setting}
        then:
        res.size() == 3
        res.get(0).projectId == proj1.projectId
        res.get(0).setting == "set1"
        res.get(0).value == "true"

        res.get(1).projectId == proj1.projectId
        res.get(1).setting == "set2"
        res.get(1).value == "val2"

        res.get(2).projectId == proj1.projectId
        res.get(2).setting == "set3"
        res.get(2).value == "val3"
    }

    def "check validity of settings requests"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        def res = skillsService.checkSettingsValidity(proj1.projectId, [
                [projectId: proj1.projectId, setting: "set1", value: "true"],
                [projectId: proj1.projectId, setting: "set2", value: "val2"],
                [projectId: proj1.projectId, setting: "set3", value: "val3"],
        ])
        then:
        res.body.success
        res.body.valid
    }

    def "check validity of settings requests - invalid because there isn't enough points"(){
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        when:
        def res = skillsService.checkSettingsValidity(proj1.projectId, [
                [projectId: proj1.projectId, setting: "set1", value: "true"],
                [projectId: proj1.projectId, setting: "level.points.enabled", value: "true"],
                [projectId: proj1.projectId, setting: "set3", value: "val3"],
        ])
        then:
        res.body.success
        !res.body.valid
        res.body.explanation == "Use Points For Levels: Project has [0] total points. [100] total points required to switch to points based levels"
    }

    def "validate that configured props under skills.store.settings.* are stored as settings"() {
        when:
        def res = skillsService.getPublicSettings("public_groupName1")
        def res1 = skillsService.getPublicSettings("public_groupName2")
        def res2 = skillsService.getPublicSettings("public_groupName3")
        then:
        res.size() == 2
        res.find { it.setting == "settingId1" }.settingGroup == "public_groupName1"
        res.find { it.setting == "settingId1" }.value == "valuea"
        !res.find { it.setting == "settingId1" }.projectId
        !res.find { it.setting == "settingId1" }.userId

        res.find { it.setting == "settingId2" }.settingGroup == "public_groupName1"
        res.find { it.setting == "settingId2" }.value == "valueb"
        !res.find { it.setting == "settingId2" }.projectId
        !res.find { it.setting == "settingId2" }.userId

        res1.size() == 1
        res1.find { it.setting == "settingId3" }.settingGroup == "public_groupName2"
        res1.find { it.setting == "settingId3" }.value == "valuec"
        !res1.find { it.setting == "settingId3" }.projectId
        !res1.find { it.setting == "settingId3" }.userId

        !res2
    }

    def "get public settings"() {
        when:
        def res = skillsService.getPublicSettings("public_groupName1")
        def res1 = skillsService.getPublicSetting( "settingId1", "public_groupName1")
        then:
        res.size() == 2
        res.find { it.setting == "settingId1" }.settingGroup == "public_groupName1"
        res.find { it.setting == "settingId1" }.value == "valuea"
        !res.find { it.setting == "settingId1" }.projectId
        !res.find { it.setting == "settingId1" }.userId

        res.find { it.setting == "settingId2" }.settingGroup == "public_groupName1"
        res.find { it.setting == "settingId2" }.value == "valueb"
        !res.find { it.setting == "settingId2" }.projectId
        !res.find { it.setting == "settingId2" }.userId

        res1
        res1.value == "valuea"
        res1.setting ==  "settingId1"
        res1.settingGroup == "public_groupName1"
    }

    def "save and retrieve email settings"() {
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveEmailSettings("somehost", "smtp", 1026, false, true, "fakeuser", "fakepassword")
        def emailSettings = skillsService.getEmailSettings()

        then:
        emailSettings.host == "somehost"
        emailSettings.protocol == "smtp"
        emailSettings.port == 1026
        emailSettings.tlsEnabled == false
        emailSettings.authEnabled == true
        emailSettings.username == "fakeuser"
        emailSettings.password == "fakepassword"
    }

    @Timeout(12)
    def "save and retrieve email settings with invalid smtp server"() {
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveEmailSettings("somehost", "smtp", 1026, false, true, "fakeuser", "fakepassword")
        def emailSettings = skillsService.getEmailSettings()

        then:
        !emailSettings.success
    }

    def "save and retrieve system settings"() {
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveSystemSettings("http://public",
                "PT1H30M20S",
                "foo@skilltree", "<div>header</div>", "<div>footer</div>")
        def systemSettings = skillsService.getSystemSettings()

        then:
        systemSettings.publicUrl == "http://public"
        systemSettings.resetTokenExpiration == "PT1H30M20S"
        systemSettings.fromEmail == "foo@skilltree"
        systemSettings.customHeader == "<div>header</div>"
        systemSettings.customFooter == "<div>footer</div>"
    }

    def "save system settings with invalid token expiration duration"() {
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveSystemSettings("http://public", "1H30M20S", "foo@skilltree", "<div/>", "<div/>")
        def systemSettings = skillsService.getSystemSettings()

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("1H30M20S is not a valid duration")
    }

    def "save custom header setting with script tag"(){
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveSystemSettings("http://public",
                "PT1H30M20S",
                "foo@skilltree",
                '<div><script type="text/javascript">alert("foo");</script></div>',
                "<div/>")
        def systemSettings = skillsService.getSystemSettings()

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Script tags are not allowed in custom header")
    }

    def "save custom footer setting with script tag"(){
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }

        when:
        skillsService.saveSystemSettings("http://public",
                "PT1H30M20S",
                "foo@skilltree",
                '<div/>',
                "<div><script type=\"text/javascript\">alert(\"foo\");</script></div>")
        def systemSettings = skillsService.getSystemSettings()

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Script tags are not allowed in custom footer")
    }

}
