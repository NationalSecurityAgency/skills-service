/**
 * Copyright 2025 SkillTree
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
package skills.auth.pki

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.cert.X509Certificate

class ExtractCertUtil {

    static Logger log = LoggerFactory.getLogger(ExtractCertUtil.class)

    static X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request?.getAttribute("jakarta.servlet.request.X509Certificate")
        if (certs != null && certs.length > 0) {
            log.debug("X.509 client authentication certificate: [{}]", certs[0])
            return certs[0]
        }
        log.error("No client certificate found in request [{}].", request?.getRequestURI())
        return null
    }

    static String getIssuerDn(HttpServletRequest request) {
        return extractClientCertificate(request)?.getIssuerX500Principal()?.getName()
    }

    static String getDn(HttpServletRequest request) {
        return extractClientCertificate(request)?.getSubjectX500Principal()?.getName()
    }
}
