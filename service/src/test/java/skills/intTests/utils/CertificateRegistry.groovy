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
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.stereotype.Component
import skills.auth.SecurityMode

import javax.annotation.PostConstruct
import java.security.KeyStore
import java.security.Principal
import java.security.cert.X509Certificate

@Conditional(SecurityMode.PkiAuth)
@Component
class CertificateRegistry {

    @Autowired
    ResourceLoader resourceLoader

    Map<String, Resource> certs = [:]

    private List<String> idsOnly = []

    private Random random = new Random()

    @PostConstruct
    public void init() {
        Resource[] intTestCerts = getCertificates()
        intTestCerts?.each{
            String username = loadCNFromCert(it)
            username = username.trim().toLowerCase()
            certs.put(username, it)
            idsOnly.add(username)
        }
    }

    public Resource getCertificate(String username) {
        return certs.get(username.trim().toLowerCase())
    }

    private Resource[] getCertificates() {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath:/certs/test.skilltree.*p12");
    }

    public String getRandomUser(){
        return idsOnly.get(random.nextInt(idsOnly.size()))
    }

    public List<String> getRandomUsers(int num) {
        List<String> copy = new ArrayList<>(idsOnly)
        List<String> res = []
        (0..num).each {
            if (copy.size() == 0) {
                throw new IllegalStateException("no more certificate users left to satisfy getRandomUsers request")
            }
            String user = copy.remove(random.nextInt(copy.size()))
            res.add(user)
        }

        return res
    }

    public String loadCNFromCert(Resource certificate) {
        KeyStore p12 = KeyStore.getInstance("pkcs12");


        certificate.getInputStream().withCloseable { InputStream inputStream ->
            p12.load(inputStream, "skillspass".toCharArray());
            Enumeration<String> e = p12.aliases();
            String cn = ""

            if (e.hasMoreElements()) {
                String alias = e.nextElement();
                X509Certificate c = (X509Certificate) p12.getCertificate(alias);
                Principal subject = c.getSubjectDN();
                String[] subjectArray = subject.toString().split(",");
                cn = subjectArray[0].split("=")[1]
            }

            return cn
        }
    }

    public String loadDNFromCert(Resource certificate){
        KeyStore p12 = KeyStore.getInstance("pkcs12");


        certificate?.getInputStream()?.withCloseable { InputStream inputStream ->
            p12.load(inputStream, "skillspass".toCharArray());
            Enumeration<String> e = p12.aliases();
            String dn = ""

            if (e.hasMoreElements()) {
                String alias = e.nextElement();
                X509Certificate c = (X509Certificate) p12.getCertificate(alias);
                Principal subject = c.getSubjectDN();
                dn = subject.toString()
            }

            return dn
        }
    }
}
