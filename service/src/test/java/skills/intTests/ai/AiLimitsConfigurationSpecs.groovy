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

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import skills.services.openai.OpenAIUsageLimitsProperties
import spock.lang.Specification
import spock.lang.Unroll

/** Tests binding/startup only; endpoint behavior uses the standard DefaultAiIntSpec context. */
class AiLimitsConfigurationSpecs extends Specification {
    @Unroll
    def 'reject invalid configured limit #name=#value at startup'() {
        expect:
        contextRunner()
                .withPropertyValues("skills.openai.limits.${name}=${value}")
                .run { context -> assert context.startupFailure != null }

        where:
        [name, value] << [numericLimits(), ['0', '-1', 'invalid']].combinations()
    }

    @Unroll
    def 'each limit binds independently: #name'() {
        expect:
        contextRunner()
                .withPropertyValues("skills.openai.limits.${name}=1")
                .run { context ->
                    assert context.startupFailure == null
                    def limits = context.getBean(OpenAIUsageLimitsProperties)
                    assert limits[name] == 1
                    assert numericLimits().findAll { it != name }.every { limits[it] == defaults()[it] }
                }

        where:
        name << numericLimits()
    }

    def 'omitted limits enable protective defaults and allowlist remains deployment specific'() {
        expect:
        contextRunner()
                .withPropertyValues('skills.openai.limits.allowedModels=')
                .run { context ->
                    assert context.startupFailure == null
                    def limits = context.getBean(OpenAIUsageLimitsProperties)
                    assert !limits.allowedModels
                    assert numericLimits().every { limits[it] == defaults()[it] }
                }
    }

    private static List<String> numericLimits() {
        defaults().keySet().toList()
    }

    private static Map defaults() {
        [maxRequestBytes: 10485760, maxMessages: 1000, maxMessageCharacters: 1000000,
         maxTotalMessageCharacters: 5000000, maxOutputTokens: 32768,
         requestsPerMinutePerUser: 600, requestsPerMinuteGlobal: 6000, maxConcurrentRequestsGlobal: 200]
    }

    private static ApplicationContextRunner contextRunner() {
        new ApplicationContextRunner().withUserConfiguration(LimitsConfiguration).withInitializer { context ->
            new YamlPropertySourceLoader().load('application', new ClassPathResource('application.yml')).each {
                context.environment.propertySources.addLast(it)
            }
        }
    }

    @TestConfiguration
    @EnableConfigurationProperties(OpenAIUsageLimitsProperties)
    static class LimitsConfiguration { }
}
