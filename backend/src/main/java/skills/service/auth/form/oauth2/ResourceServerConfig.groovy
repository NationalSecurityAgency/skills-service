package skills.service.auth.form.oauth2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.TokenStore
import skills.service.auth.form.SkillsHttpSessionSecurityContextRepository

import static skills.service.auth.SecurityConfiguration.FormAuth
import static skills.service.auth.SecurityConfiguration.PortalWebSecurityHelper

@Conditional(FormAuth)
@Configuration
@EnableResourceServer
class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Value('#{"${security.oauth2.resource.id:skills-service-oauth}"}')
    private String resourceId

    // The DefaultTokenServices bean provided at the AuthorizationConfig
    @Autowired
    private DefaultTokenServices tokenServices

    // The TokenStore bean provided at the AuthorizationConfig
    @Autowired
    private TokenStore tokenStore

    @Autowired
    PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    SkillsHttpSessionSecurityContextRepository securityContextRepository

    @Autowired
    OAuthUtils oAuthUtils

    @Autowired
    @Qualifier('skillsOAuth2AuthManager')
    AuthenticationManager authenticationManager


    // To allow the ResourceServerConfigurerAdapter to understand the token,
    // it must share the same characteristics with AuthorizationServerConfigurerAdapter.
    // So, we must wire it up the beans in the ResourceServerSecurityConfigurer.
    @Override
    void configure(ResourceServerSecurityConfigurer resources) {
        resources
                .resourceId(resourceId)
                .tokenServices(tokenServices)
                .tokenStore(tokenStore)
                .authenticationManager(authenticationManager)
    }

    @Override
    void configure(HttpSecurity http) throws Exception {
        portalWebSecurityHelper.configureHttpSecurity(
                http.securityContext().securityContextRepository(securityContextRepository)
        .and()
                .requestMatcher(oAuthUtils.oAuthRequestedMatcher)
        )
        http.securityContext().securityContextRepository(securityContextRepository)
    }
}
