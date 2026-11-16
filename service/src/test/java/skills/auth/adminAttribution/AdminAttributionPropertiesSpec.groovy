/**
 * Copyright 2026 SkillTree
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
package skills.auth.adminAttribution

import spock.lang.Specification

class AdminAttributionPropertiesSpec extends Specification {
    def 'attribution is disabled by default with no allowed requesters'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties()

        when:
        properties.validate()

        then:
        !properties.enabled
        properties.headerName == 'X-SkillTree-Acting-User'
        properties.allowedRequesters.empty
    }

    def 'allowlist IDs use exact canonical matching without wildcard expansion'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties(
                enabled: true, allowedRequesters: [' Proxy@Example.org ', 'proxy@example.org', '*'] as Set)

        when:
        properties.validate()

        then:
        properties.allowedRequesters == ['proxy@example.org', '*'] as Set
        !properties.allowedRequesters.contains('other@example.org')
    }

    def 'enabled attribution rejects invalid configured header name #header'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties(enabled: true, headerName: header)

        when:
        properties.validate()

        then:
        IllegalArgumentException exception = thrown()
        exception.message.contains('headerName must be a valid HTTP header name')

        where:
        header << [null, '', 'Contains Space', 'Contains:Colon', "Contains\nNewline"]
    }

    def 'blank allowlist entries fail configuration validation'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties(enabled: true, allowedRequesters: [' '] as Set)

        when:
        properties.validate()

        then:
        IllegalArgumentException exception = thrown()
        exception.message.contains('allowedRequesters must not contain blank user IDs')
    }

    def 'sensitive header name #header is rejected case-insensitively even when disabled'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties(enabled: enabled, headerName: header)

        when:
        properties.validate()

        then:
        IllegalArgumentException exception = thrown()
        exception.message.contains('must not be a sensitive or reserved header name')

        where:
        [header, enabled] << [
                ['Authorization', 'aUtHoRiZaTiOn', 'Proxy-Authorization', 'Cookie', 'cOoKiE', 'Set-Cookie',
                 'WWW-Authenticate', 'Proxy-Authenticate', 'Authentication-Info', 'Proxy-Authentication-Info',
                 'X-API-Key', 'Api-Key', 'X-Goog-Api-Key', 'X-Amz-Security-Token',
                 'X-Auth-Token', 'X-Access-Token', 'X-Session-Id', 'X-Session-Token', 'X-XSRF-Token', 'X-CSRF-Token'],
                [false, true]
        ].combinations()
    }

    def 'custom attribution header names remain configurable'() {
        given:
        AdminAttributionProperties properties = new AdminAttributionProperties(enabled: true, headerName: 'X-Proxy-Acting-User')

        when:
        properties.validate()

        then:
        properties.headerName == 'X-Proxy-Acting-User'
    }
}
