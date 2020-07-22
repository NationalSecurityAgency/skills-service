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
package skills.auth.form

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.auth.util.AccessDeniedExplanation
import skills.auth.util.AccessDeniedExplanationGenerator

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Conditional(SecurityMode.FormAuth)
@Slf4j
@Component
class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Autowired
    ObjectMapper om

    @Override
    void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException ex) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Access Denied User [${authentication}], reqested resource [${request.getServletPath()}]")
        AccessDeniedExplanation explanation = new AccessDeniedExplanationGenerator().generateExplanation(request.getServletPath())
        response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        if(explanation) {
            String asJson = om.writeValueAsString(explanation)
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            response.setContentLength(asJson.bytes.length)
            response.getWriter().print(asJson)
            response.getWriter().flush()
        }
    }
}
