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
package skills.intTests.ai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.openai.OpenAIRestTemplateConfig
import skills.services.openai.OpenAIService

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CopyOnWriteArrayList

@TestPropertySource(properties = ['skills.openai.key=hostname-verification-test-key'])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenAIHostnameVerificationIT extends DefaultIntSpec {

    private static final String API_KEY = 'hostname-verification-test-key'

    @Autowired
    OpenAIService provider

    @Autowired
    OpenAIRestTemplateConfig.OpenAIClientConfig clientConfig

    private RestTemplate originalRestTemplate
    private String originalHost
    private String originalBaseUrl
    private String originalModelsEndpoint
    private HttpComponentsClientHttpRequestFactory requestFactory
    private HttpsServer httpsServer
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>()
    private SkillsService learner

    def setup() {
        originalRestTemplate = provider.restTemplate
        originalHost = provider.openAiHost
        originalBaseUrl = provider.openAiBaseUrl
        originalModelsEndpoint = provider.modelsEndpoint
        learner = createService(getRandomUsers(1)[0])
    }

    def cleanup() {
        provider.restTemplate = originalRestTemplate
        provider.openAiHost = originalHost
        provider.openAiBaseUrl = originalBaseUrl
        provider.modelsEndpoint = originalModelsEndpoint
        requestFactory?.destroy()
        httpsServer?.stop(0)
    }

    def 'trusted matching certificate returns models and receives the configured bearer token'() {
        given:
        Map tls = certificate('localhost')
        startProvider(tls)
        configureClient(tls.certificate as X509Certificate)

        when:
        Map models = learner.getAiModels()

        then:
        models.models.model == ['tls-test-model']
        requests == [[method: 'GET', path: '/v1/models', authorization: "Bearer ${API_KEY}".toString()]]
    }

    def 'trusted mismatched certificate is rejected before the bearer token is sent'() {
        given:
        // The same trusted certificate matches localhost but does not match 127.0.0.1.
        Map tls = certificate('localhost', false)
        startProvider(tls)
        configureClient(tls.certificate as X509Certificate)

        when:
        learner.getAiModels()

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_GATEWAY
        requests.empty

        when:
        // A successful request with the same certificate proves that trust validation
        // and the provider are working; only the requested hostname has changed.
        setProviderHost('localhost')
        Map models = learner.getAiModels()

        then:
        models.models.model == ['tls-test-model']
        requests == [[method: 'GET', path: '/v1/models', authorization: "Bearer ${API_KEY}".toString()]]
    }

    def 'untrusted matching certificate is rejected before the bearer token is sent'() {
        given:
        Map tls = certificate('localhost')
        startProvider(tls)
        Map unrelated = certificate('unrelated.example')
        configureClient(unrelated.certificate as X509Certificate)

        when:
        learner.getAiModels()

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_GATEWAY
        requests.empty

        when:
        // Trusting the provider's certificate changes only the trust decision.
        configureClient(tls.certificate as X509Certificate)
        Map models = learner.getAiModels()

        then:
        models.models.model == ['tls-test-model']
        requests == [[method: 'GET', path: '/v1/models', authorization: "Bearer ${API_KEY}".toString()]]
    }

    private void startProvider(Map tls) {
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.defaultAlgorithm)
        keys.init(tls.keyStore as KeyStore, 'test-password'.toCharArray())
        SSLContext serverContext = SSLContext.getInstance('TLS')
        serverContext.init(keys.keyManagers, null, null)
        httpsServer = HttpsServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        httpsServer.httpsConfigurator = new HttpsConfigurator(serverContext)
        httpsServer.createContext('/v1/models') { HttpExchange exchange ->
            try {
                requests.add([method: exchange.requestMethod, path: exchange.requestURI.path,
                              authorization: exchange.requestHeaders.getFirst('Authorization')])
                byte[] body = '{"data":[{"id":"tls-test-model","created":1766147425720}]}'.getBytes(StandardCharsets.UTF_8)
                exchange.responseHeaders.set('Content-Type', 'application/json')
                exchange.sendResponseHeaders(200, body.length)
                exchange.responseBody.write(body)
            } finally {
                exchange.close()
            }
        }
        httpsServer.start()
        // A literal address avoids DNS fallback masking TLS failures with a later
        // connection-refused error for an alternate localhost address (for example ::1).
        setProviderHost('127.0.0.1')
        provider.modelsEndpoint = 'v1/models'
    }

    private void setProviderHost(String hostname) {
        String url = "https://${hostname}:${httpsServer.address.port}".toString()
        provider.openAiHost = url
        provider.openAiBaseUrl = url
    }

    private void configureClient(X509Certificate trustedCertificate) {
        KeyStore trust = KeyStore.getInstance('PKCS12')
        trust.load(null, null)
        trust.setCertificateEntry('trusted-provider', trustedCertificate)
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.defaultAlgorithm)
        trustManagers.init(trust)
        SSLContext clientContext = SSLContext.getInstance('TLS')
        clientContext.init(null, trustManagers.trustManagers, null)

        requestFactory?.destroy()
        // Exercise the production factory, not a test HTTP client or trust-all verifier.
        // SSLContexts.createSystemDefault() captures this context at client construction.
        // Restore the JVM default before any endpoint requests, including PKI login.
        synchronized (SSLContext) {
            SSLContext originalContext = SSLContext.default
            try {
                SSLContext.setDefault(clientContext)
                requestFactory = new HttpComponentsClientHttpRequestFactory(
                        new OpenAIRestTemplateConfig().openAIClientHttpClient(clientConfig))
            } finally {
                SSLContext.setDefault(originalContext)
            }
        }
        provider.restTemplate = new RestTemplate(requestFactory)
    }

    private static Map certificate(String hostname, boolean includeLoopbackAddress = true) {
        KeyPairGenerator generator = KeyPairGenerator.getInstance('RSA')
        generator.initialize(2048)
        KeyPair pair = generator.generateKeyPair()
        X500Name subject = new X500Name("CN=${hostname}")
        Instant now = Instant.now()
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject, BigInteger.ONE,
                Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(1, ChronoUnit.DAYS)),
                subject, pair.public)
        List<GeneralName> names = [new GeneralName(GeneralName.dNSName, hostname)]
        if (includeLoopbackAddress) {
            names.add(new GeneralName(GeneralName.iPAddress, '127.0.0.1'))
        }
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names as GeneralName[]))
        BouncyCastleProvider cryptoProvider = new BouncyCastleProvider()
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(cryptoProvider).getCertificate(
                builder.build(new JcaContentSignerBuilder('SHA256withRSA').setProvider(cryptoProvider).build(pair.private)))
        KeyStore keyStore = KeyStore.getInstance('PKCS12')
        keyStore.load(null, null)
        keyStore.setKeyEntry('provider', pair.private, 'test-password'.toCharArray(), [cert] as Certificate[])
        return [keyStore: keyStore, certificate: cert]
    }
}
