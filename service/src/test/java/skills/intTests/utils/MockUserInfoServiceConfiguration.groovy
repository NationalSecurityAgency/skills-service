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
package skills.intTests.utils

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertySource
import skills.auth.SecurityMode

@Configuration(proxyBeanMethods = false)
@Conditional(SecurityMode.PkiAuth)
class MockUserInfoServiceConfiguration {

    @Bean
    static BeanFactoryPostProcessor mockUserInfoProperties(ConfigurableEnvironment environment) {
        return { ConfigurableListableBeanFactory beanFactory ->
            // Servlet filters can resolve URLs during onRefresh(), before dynamic registrars run.
            // Register early, but initialize the mock lazily with normal bean post-processing.
            environment.propertySources.addFirst(new MockEndpointPropertySource(beanFactory))
        } as BeanFactoryPostProcessor
    }

    private static class MockEndpointPropertySource extends PropertySource<ConfigurableListableBeanFactory> {
        private static final Map<String, String> ENDPOINT_PATHS = [
                'skills.authorization.userInfoUri': '/userInfo?dn={dn}',
                'skills.authorization.userQueryUri': '/userQuery?query={query}',
                'skills.authorization.userInfoHealthCheckUri': '/status',
        ]

        MockEndpointPropertySource(ConfigurableListableBeanFactory beanFactory) {
            super('mockUserInfoEndpoints', beanFactory)
        }

        @Override
        Object getProperty(String name) {
            String path = ENDPOINT_PATHS[name]
            if (path == null) {
                return null
            }
            MockUserInfoService mock = getSource().getBean(MockUserInfoService)
            return "https://localhost:${mock.mockServer.httpsPort()}${path}".toString()
        }
    }
}
