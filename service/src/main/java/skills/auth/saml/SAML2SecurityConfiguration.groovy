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
package skills.auth.saml

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.*
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Component
import skills.auth.PortalWebSecurityHelper
import skills.auth.SecurityMode
import skills.auth.UserInfo
import skills.auth.form.LocalUserDetailsService

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec


@Slf4j
@Conditional(SecurityMode.SAML2Auth)
@Component
@Configuration
@EnableWebSecurity
/** Supports Single Identity Provider Only **/
class SAML2SecurityConfiguration{

    public static final String SKILLS_REDIRECT_URI = 'skillsRedirectUri'

    @Value('${spring.security.saml2.metadata-location}')
    String assertingPartyMetadataLocation;

    @Value('${spring.security.saml2.registrationId}')
    String registrationId;

    // Custom properties using for configuring signing credentials

    @Value('${saml2.rp.signing.key-location:#{null}}')
    private String privateKeyLocation;

    @Value('${saml2.rp.signing.cert-location:#{null}}')
    private String certificateLocation;

    @Autowired
    PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    SAML2Utils saml2Utils;

    private final ResourceLoader resourceLoader;

    @Autowired
    public SAML2SecurityConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


     @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception   {

         OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
         authenticationProvider.setResponseAuthenticationConverter(responseToken -> {
             Saml2Authentication authentication = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter()
                                                                                 .convert(responseToken);
             Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            // Assertion assertion = responseToken.getResponse().getAssertions().get(0);
             UserInfo userInfo = saml2Utils.convertToUserInfo(authentication,true);

             return new Saml2Authentication(principal,authentication.getSaml2Response(),userInfo.getAuthorities());
         });

         portalWebSecurityHelper.configureHttpSecurity(http);
         http.saml2Login(saml2 -> saml2
                 .authenticationManager(new ProviderManager(authenticationProvider))
                 .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository())
         );
         http.logout((logout) -> logout.logoutSuccessUrl("/local/logout"));
         return http.build();
    }
   @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        RelyingPartyRegistration registration = null;

        if(privateKeyLocation!=null && certificateLocation!=null){
            PrivateKey privateKey = loadPrivateKey(privateKeyLocation);
            X509Certificate certificate = loadCertificate(certificateLocation);

            // Create the signing credential
            Saml2X509Credential signingCredential = new Saml2X509Credential(privateKey, certificate, Saml2X509Credential.Saml2X509CredentialType.SIGNING);
            registration = RelyingPartyRegistrations.fromMetadataLocation(assertingPartyMetadataLocation)
                    .registrationId(registrationId)
                    .signingX509Credentials(c -> c.add(signingCredential))
                    .build()
        }else{
            registration = RelyingPartyRegistrations.fromMetadataLocation(assertingPartyMetadataLocation)
                    .registrationId(registrationId)
                    .build()
        }
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    Saml2HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new Saml2HttpSessionSecurityContextRepository()
    }

    @Bean
    UserDetailsService saml2UserDetailsService(){
        return new LocalUserDetailsService();
    }

    private PrivateKey loadPrivateKey(String location) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:"+location);
        if (!resource.exists()) {
            resource = resourceLoader.getResource("file:" + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            String key = new String(inputStream.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    private X509Certificate loadCertificate(String location) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:"+location);
        if (!resource.exists()) {
            resource = resourceLoader.getResource("file:" + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(inputStream);
        }
    }

}