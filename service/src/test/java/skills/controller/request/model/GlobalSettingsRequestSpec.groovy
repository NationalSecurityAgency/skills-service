package skills.controller.request.model

import spock.lang.Specification

class GlobalSettingsRequestSpec extends Specification {

    def "toString() must not include isEnabled"() {
        GlobalSettingsRequest input = new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: "value")

        when:
        String res = input.toString()
        println res
        then:
        !res.contains("false")
        !res.contains("enabled")
    }

    def "no exceptions when isEnabled() is called with value=null"() {
        when:
        GlobalSettingsRequest input = new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: null)
        then:
        !input.isEnabled()
    }

    def "isEnabled logic"() {
        expect:
        new GlobalSettingsRequest(settingGroup: "group", setting: "setting", value: input).isEnabled() == res
        where:
        input      | res
        null       | false
        false      | false
        true       | true
        "enabled"  | true
        "enabled"  | true
        "true"     | true
        "on"       | true
        "disabled" | false
        "off"      | false
        "disable"  | false
    }
}
