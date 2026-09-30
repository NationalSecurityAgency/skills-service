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
package skills.auth

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import spock.lang.Specification
import spock.lang.Unroll

class SecurityConfigurationSpec extends Specification {

    def "CORS allows all origins by default"() {
        given:
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                authMode: AuthMode.DEFAULT_AUTH_MODE,
                corsAllowedOriginPatterns: ['*'],
                corsConfAllowCredentials: false)

        when:
        CorsConfiguration configuration = getConfiguration(securityConfiguration, '/api/projects')

        then:
        configuration.allowedOriginPatterns == ['*']
        !configuration.allowCredentials
        configuration.checkOrigin('https://third-party.example') == 'https://third-party.example'
    }

    @Unroll
    def "CORS allowCredentials uses the configured default for #authMode auth mode"() {
        given:
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                authMode: authMode,
                corsAllowedOriginPatterns: ['*'],
                corsConfAllowCredentials: false)

        expect:
        getConfiguration(securityConfiguration, '/api/projects').allowCredentials == false
        getConfiguration(securityConfiguration, '/app/userInfo').allowCredentials == false

        where:
        authMode << [AuthMode.FORM, AuthMode.SAML2, AuthMode.PKI]
    }

    @Unroll
    def "CORS allowCredentials can be explicitly set to #configuredAllowCredentials for #authMode auth mode"() {
        given:
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                authMode: authMode,
                corsAllowedOriginPatterns: ['*'],
                corsConfAllowCredentials: configuredAllowCredentials)

        expect:
        getConfiguration(securityConfiguration, '/api/projects').allowCredentials == configuredAllowCredentials
        getConfiguration(securityConfiguration, '/app/userInfo').allowCredentials == configuredAllowCredentials

        where:
        authMode      | configuredAllowCredentials
        AuthMode.FORM | true
        AuthMode.PKI  | false
    }

    def "CORS honors configured origin patterns"() {
        given:
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                corsAllowedOriginPatterns: [' https://app.example.com ', 'https://*.customer.example'],
                corsConfAllowCredentials: false)

        when:
        CorsConfiguration configuration = getConfiguration(securityConfiguration, '/api/projects')

        then:
        configuration.allowedOriginPatterns == ['https://app.example.com', 'https://*.customer.example']
        configuration.checkOrigin('https://app.example.com') == 'https://app.example.com'
        configuration.checkOrigin('https://training.customer.example') == 'https://training.customer.example'
        configuration.checkOrigin('https://untrusted.example') == null
    }

    def "CORS configuration is limited to supported cross-origin endpoints"() {
        given:
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                corsAllowedOriginPatterns: ['*'],
                corsConfAllowCredentials: false)
        CorsConfigurationSource source = securityConfiguration.corsConfigurationSource()

        expect:
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/api/projects')).allowCredentials == false
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/public/status')).allowCredentials == false
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/public/clientDisplay/config')).allowCredentials == false
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/public/log')).allowCredentials == false
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/public/isAlive')) == null
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/app/userInfo')).allowCredentials == false
        source.getCorsConfiguration(new MockHttpServletRequest('GET', '/app/projects')) == null
    }

    private static CorsConfiguration getConfiguration(SecurityConfiguration securityConfiguration, String path) {
        return securityConfiguration.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest('GET', path))
    }
}
