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

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import skills.auth.SecurityMode
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.ProjDef

import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration

@Configuration
@Conditional(SecurityMode.FormAuth)
@Slf4j
class AuthorizationServerConfig {

    public static final String SKILLS_PROXY_USER = 'proxy_user'

    @Autowired
    KeyStoreKeyFactory keyStoreKeyFactory

    @Bean('authServerSecurityFilterChain')
    @Order(100)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults())	// Enable OpenID Connect 1.0

        return http.build()
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        def log = this.log
        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer = context -> {
            JwtClaimsSet.Builder claims = context.getClaims()
            // Customize claims
            JwtEncodingContext ctx = context;
            if (ctx.getAuthorizationGrantType() == AuthorizationGrantType.CLIENT_CREDENTIALS) {
                OAuth2ClientCredentialsAuthenticationToken authToken = ctx.getAuthorizationGrant()
                String proxyUser = authToken.getAdditionalParameters().get(SKILLS_PROXY_USER)
                if (proxyUser) {
                    claims.claim(SKILLS_PROXY_USER, proxyUser);
                } else {
                    log.warn("No $SKILLS_PROXY_USER found on OAuth2 request.")
                    OAuth2Error error = new OAuth2Error(
                            OAuth2ErrorCodes.INVALID_REQUEST,
                            "client_credentials grant_type must specify $SKILLS_PROXY_USER field",
                            null)
                    throw new OAuth2AuthenticationException(error)
                }
            }

        }
        return jwtCustomizer
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = keyStoreKeyFactory.getJwtKeyPair()
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic()
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate()
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
//                .keyID(UUID.randomUUID().toString())
                .build()
        JWKSet jwkSet = new JWKSet(rsaKey)
        return new ImmutableJWKSet<>(jwkSet)
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        // customize endpoint for backwards compatibility (new default endpoint is /oauth2/token)
        return AuthorizationServerSettings.builder()
                .tokenEndpoint("/oauth/token").build()
    }

    @Bean
    RegisteredClientRepository registeredClientRepository() {
        return new SkillsRegisteredClientRepository()
    }

    static class SkillsRegisteredClientRepository implements RegisteredClientRepository {

        @Value('#{"${security.oauth2.resource.id:skills-service-oauth}"}')
        private String resourceId

        @Value('#{"${security.oauth2.jwt.accessTokenValiditySeconds:43200}"}') // default 12 hours.
        private int accessTokenValiditySeconds

        @Autowired
        private ProjDefAccessor projDefAccessor

        @Autowired
        PasswordEncoder passwordEncoder

        @Override
        RegisteredClient findById(String id) {
            return loadRegisteredClient(id)
        }

        @Override
        RegisteredClient findByClientId(String clientId) {
            return loadRegisteredClient(clientId)
        }

        private RegisteredClient loadRegisteredClient(String clientId) {
            ProjDef projDef = projDefAccessor.getProjDef(clientId)

            RegisteredClient registeredClient = RegisteredClient.withId(clientId)
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(projDef.clientSecret))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                    .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
//                    .redirectUri("http://127.0.0.1:8080/authorized")
//                    .scope(OidcScopes.OPENID)
//                    .scope(OidcScopes.PROFILE)
                    .scope("read")
                    .scope("write")
//                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofSeconds(accessTokenValiditySeconds)).build())
                    .build()

            return registeredClient
        }

        @Override
        void save(RegisteredClient registeredClient) {
            throw new UnsupportedOperationException()
        }
    }
}
