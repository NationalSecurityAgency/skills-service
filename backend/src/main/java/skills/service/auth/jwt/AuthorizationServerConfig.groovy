package skills.service.auth.jwt

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.TokenEnhancer
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import skills.service.auth.SecurityConfiguration

@Configuration
@Conditional(SecurityConfiguration.JwtCondition)
@EnableAuthorizationServer
class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
    private int accessTokenValiditySeconds = 10000
    private int refreshTokenValiditySeconds = 30000

    @Value('#{"${security.oauth2.resource.id:skills-service-oauth}"}')
    private String resourceId

    @Autowired
    private AuthenticationManager authenticationManager

    @Autowired
    PasswordEncoder passwordEncoder

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
    void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient("trusted-app")
                .autoApprove(true)
                .authorizedGrantTypes("authorization_code", "client_credentials", "password", "refresh_token")
                .authorities("ROLE_TRUSTED_CLIENT")
                .scopes("read", "write")
                .resourceIds(resourceId)
                .accessTokenValiditySeconds(accessTokenValiditySeconds)
                .refreshTokenValiditySeconds(refreshTokenValiditySeconds)
                .redirectUris("/home", "/", "http://localhost:8080/login/oauth2/code/skills")//oauth/authorize")
                .secret(passwordEncoder.encode("secret"))
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
//        converter.setSigningKey("abcd")
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource("jwtkeys.jks"), "password".toCharArray())
        jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair("jwtkeys"))

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
        defaultTokenServices.setSupportRefreshToken(true)
        defaultTokenServices.setTokenEnhancer(tokenEnhancer())
        return defaultTokenServices
    }

    @Slf4j
    static class SkillsProxyUserTokenEnhancer implements TokenEnhancer {

        static final String SKILLS_PROXY_USER = 'proxy-user'

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
            }
            return result
        }
    }
}
