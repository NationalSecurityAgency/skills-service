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
package skills.controller.filters;


import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 2)
@Slf4j
public class ClientLibVersionFilter extends OncePerRequestFilter {

    @Value("${skills.clientLibVersion}")
    String clientLibVersion;

    private static String HEADER_SKILLS_CLIENT_LIB_VERSION = "Skills-Client-Lib-Version".toLowerCase();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        response.addHeader(HEADER_SKILLS_CLIENT_LIB_VERSION, clientLibVersion);
        response.addHeader("Access-Control-Expose-Headers", HEADER_SKILLS_CLIENT_LIB_VERSION);

        filterChain.doFilter(request, response);
    }

}
