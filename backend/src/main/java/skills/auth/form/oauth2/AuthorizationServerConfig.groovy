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
package skills.auth.form.oauth2

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.ClientRegistrationException
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.token.*
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import skills.auth.SecurityMode
import skills.storage.model.ProjDef
import skills.storage.accessors.ProjDefAccessor

@Configuration
@Conditional(SecurityMode.FormAuth)
@EnableAuthorizationServer
class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    public static final String SKILLS_PROXY_USER = 'proxy_user'

    @Value('#{"${security.oauth2.jwt.keystore.resource:jwtkeys.jks}"}')
    private String jwtKeystoreResource

    @Value('#{"${security.oauth2.jwt.keystore.password:password}"}')
    private String jwtKeystorePassword

    @Value('#{"${security.oauth2.jwt.keystore.alias:jwtkeys}"}')
    private String jwtKeystoreAlias

    @Autowired
    private AuthenticationManager authenticationManager

    @Override
    void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(this.authenticationManager)
                .tokenStore(tokenStore())
                .tokenServices(tokenServices())
                .tokenEnhancer(tokenEnhancer())
    }

    @Override
    void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer
                // we're allowing access to the token only for clients with 'ROLE_TRUSTED_CLIENT' authority
                .tokenKeyAccess("hasAuthority('ROLE_TRUSTED_CLIENT')")
                .checkTokenAccess("hasAuthority('ROLE_TRUSTED_CLIENT')")
    }

    @Override
    void configure(ClientDetailsServiceConfigurer configurer) throws Exception {
        configurer.withClientDetails(skillsClientDetailsService())
    }

    @Bean
    TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter())
    }

    @Bean
    JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter()
        jwtAccessTokenConverter.setAccessTokenConverter(new DefaultAccessTokenConverter() {
            @Override
            OAuth2Authentication extractAuthentication(Map<String, ?> claims) {
                OAuth2Authentication authentication = super.extractAuthentication(claims)
                authentication.setDetails(claims)
                return authentication
            }
        })

        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource(jwtKeystoreResource), jwtKeystorePassword.toCharArray())
        jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair(jwtKeystoreAlias))

        return jwtAccessTokenConverter
    }

    @Bean
    TokenEnhancer tokenEnhancer() {
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain()
        tokenEnhancerChain.setTokenEnhancers([new SkillsProxyUserTokenEnhancer(), accessTokenConverter()])
        return tokenEnhancerChain
    }

    @Bean
    @Primary
    DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices()
        defaultTokenServices.setTokenStore(tokenStore())
        defaultTokenServices.setTokenEnhancer(tokenEnhancer())
//        defaultTokenServices.setAccessTokenValiditySeconds(10)
        return defaultTokenServices
    }

    @Bean
    SkillsClientDetailsService skillsClientDetailsService() {
        return new SkillsClientDetailsService()
    }

    @Slf4j
    static class SkillsProxyUserTokenEnhancer implements TokenEnhancer {
        @Override
        OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
            OAuth2AccessToken result = new DefaultOAuth2AccessToken(accessToken)
            String proxyUser = authentication.getOAuth2Request().requestParameters.get(SKILLS_PROXY_USER)
            if (proxyUser) {
                Map<String, Object> info = new LinkedHashMap<String, Object>(accessToken.getAdditionalInformation())
                info.put(SKILLS_PROXY_USER, proxyUser)
                result.setAdditionalInformation(info)
            } else {
                log.warn("No $SKILLS_PROXY_USER found on OAuth2 request.")
                throw new InvalidRequestException(("client_credentials grant_type must specify $SKILLS_PROXY_USER field"))
            }
            return result
        }
    }

    static class SkillsClientDetailsService implements ClientDetailsService {
        @Value('#{"${security.oauth2.resource.id:skills-service-oauth}"}')
        private String resourceId

        @Autowired
        private ProjDefAccessor projDefAccessor

        @Autowired
        PasswordEncoder passwordEncoder

        @Override
        ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
            ProjDef projDef = projDefAccessor.getProjDef(clientId)

            BaseClientDetails result = new BaseClientDetails()
            result.setClientId(clientId)
            result.setScope(["read", "write"])
            result.setAutoApproveScopes(result.getScope())
            result.setAuthorizedGrantTypes(["client_credentials"])
            result.setAuthorities([new SimpleGrantedAuthority("ROLE_TRUSTED_CLIENT")])
            result.setResourceIds([resourceId])
            result.setClientSecret(passwordEncoder.encode(projDef.clientSecret))
            return result
        }
    }
}
