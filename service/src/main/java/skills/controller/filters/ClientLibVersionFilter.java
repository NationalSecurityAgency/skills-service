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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import skills.storage.ReadOnlyDataSourceContext;

import java.io.IOException;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 2)
@Slf4j
public class ClientLibVersionFilter extends OncePerRequestFilter {

    @Value("${skills.clientLibVersion}")
    String clientLibVersion;

    @Value("${skills.config.db-upgrade-in-progress:false}")
    String upgradeInProgress;

    private static String HEADER_SKILLS_CLIENT_LIB_VERSION = "Skills-Client-Lib-Version".toLowerCase();
    private static String HEADER_UPGRADE_IN_PROGRESS = "upgrade-in-progress".toLowerCase();
    private static final String ALLOWED_HEADERS = HEADER_SKILLS_CLIENT_LIB_VERSION+", "+HEADER_UPGRADE_IN_PROGRESS;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        response.addHeader(HEADER_SKILLS_CLIENT_LIB_VERSION, clientLibVersion);
        response.addHeader("Access-Control-Expose-Headers", ALLOWED_HEADERS);
        response.addHeader(HEADER_UPGRADE_IN_PROGRESS, Boolean.valueOf(upgradeInProgress).toString());

        boolean isGet = request.getMethod().equalsIgnoreCase("GET");
        if (isGet) {
            ReadOnlyDataSourceContext.start();
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isGet) {
                ReadOnlyDataSourceContext.end();
            }
        }
    }

}
