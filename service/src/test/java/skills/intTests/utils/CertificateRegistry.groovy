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
package skills.intTests.utils


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.security.KeyStore
import java.security.Principal
import java.security.cert.X509Certificate

@Component
class CertificateRegistry {

    @Autowired
    ResourceLoader resourceLoader

    Map<String, Resource> certs = [:]

    @PostConstruct
    public void init() {
        Resource[] intTestCerts = getCertificates()
        intTestCerts?.each{
            String username = loadCNFromCert(it)
            certs.put(username.trim(), it)
        }
    }

    public Resource getCertificate(String username) {
        return certs.get(username.trim())
    }

    private Resource[] getCertificates() {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("/certs/test.skilltree.*");
    }

    String loadCNFromCert(Resource certificate) {
        KeyStore p12 = KeyStore.getInstance("pkcs12");
        try(InputStream inputStream = certificate.getInputStream()) {
            p12.load(inputStream, "skillspass".toCharArray());
            Enumeration<String> e = p12.aliases();
            String cn = ""

            if (e.hasMoreElements()) {
                String alias = e.nextElement();
                X509Certificate c = (X509Certificate) p12.getCertificate(alias);
                Principal subject = c.getSubjectDN();
                String[] subjectArray = subject.toString().split(",");
                cn = subjectArray.split(",")[0].split("=")[1]
            }

            return cn
        }
    }
}
