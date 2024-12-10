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
import org.springframework.context.ApplicationContext
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import skills.auth.SecurityMode
import skills.utils.SecretsUtil

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec

@Configuration
@Conditional(SecurityMode.FormOrSAML2Auth)
@Slf4j
class KeyStoreKeyFactory implements ResourceLoaderAware {

    @Value('#{"${security.oauth2.jwt.useKeystore:false}"}')
    private Boolean useKeystore

    @Value('#{"${security.oauth2.jwt.keystore.resource:}"}')
    private String jwtKeystoreResource

    @Value('#{"${security.oauth2.jwt.keystore.password:}"}')
    private String jwtKeystorePassword

    @Value('#{"${security.oauth2.jwt.keystore.alias:1}"}')
    private String jwtKeystoreAlias

    @Autowired
    ApplicationContext applicationContext

    private KeyStore store

    private ResourceLoader resourceLoader

    private Object lock = new Object()

    KeyPair getJwtKeyPair() {
        if (useKeystore) {
            if (!jwtKeystoreResource) {
                jwtKeystoreResource = "file:${applicationContext.getEnvironment().getProperty('server.ssl.keystore')}"
            }
            if (!jwtKeystorePassword) {
                jwtKeystorePassword = applicationContext.getEnvironment().getProperty('server.ssl.keystore-password')
            }
            return getKeyPairFromKeyStore()
        } else {
            return generateRsaKey()
        }
    }

    private KeyPair getKeyPairFromKeyStore() {
        assert jwtKeystoreResource && jwtKeystorePassword, "useKeystore set to true for JWT, but missing keystore resource and/or password"
        InputStream inputStream = null
        Resource resource = resourceLoader.getResource(jwtKeystoreResource)
        char[] password = this.jwtKeystorePassword.toCharArray()
        try {
            synchronized (lock) {
                if (store == null) {
                    store = KeyStore.getInstance("jks")
                    inputStream = resource.getInputStream()
                    store.load(inputStream, password)
                }
            }
            RSAPrivateCrtKey key = (RSAPrivateCrtKey) store.getKey(jwtKeystoreAlias, password)
            RSAPublicKeySpec spec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent())
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec)
            return new KeyPair(publicKey, key)
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot load keys from store: " + resource, e)
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close()
                }
            }
            catch (IOException e) {
                log.warn("Cannot close open stream: ", e)
            }
        }
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPair = keyPairGenerator.generateKeyPair()
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex)
        }
        return keyPair
    }

    @Override
    void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader
    }
}
