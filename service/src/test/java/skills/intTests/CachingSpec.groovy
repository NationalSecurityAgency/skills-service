/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec

@Slf4j
class CachingSpec extends DefaultIntSpec {

    @Autowired
    ResourceLoader resourceLoader

    String defaultCacheAge = "1209600"

    def "favicon should be cached"() {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/skilltree.ico", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "max-age=${defaultCacheAge}, must-revalidate, private"
    }

    def "index.html must never be cached - access with /"() {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "index.html must never be cached - access with /request-root-account"() {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/request-root-account", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "index.html must never be cached - access with /skills-login"() {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/skills-login", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.getCacheControl() == "no-store"
    }

    def "theme resources should be cached"() {
        when:
        int count = 0
        getFileNamesFromClasspath("/public/themes/*").each {
            println it
            String endpoint = "/themes/${it}/theme.css".toString()
            ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet(endpoint, [:])
            assert responseEntity.statusCode.is2xxSuccessful()
            assert responseEntity.headers.getCacheControl() == "max-age=${defaultCacheAge}, must-revalidate, private"
            count++
        }

        then:
        count > 0
    }

    def "assets should be cached"() {
        when:
        int count = 0
        List<String> ignore = ["dashboard-prime"]
        getFileNamesFromClasspath("/public/assets/**")
                .findAll { !ignore.contains(it) }
                .each {
                    String endpoint = "/assets/${it}".toString()
                    log.info("Checking endpoint: [${endpoint}]")
                    ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet(endpoint, [:])
                    assert responseEntity.statusCode.is2xxSuccessful(), "Failed for [${endpoint}] endpoint"
                    assert responseEntity.headers.getCacheControl() == "max-age=${defaultCacheAge}, must-revalidate, private", "Failed for [${endpoint}] endpoint"
                    count++
                }

        then:
        count > 0
    }

    private getFileNamesFromClasspath(String classpathPath) {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath:${classpathPath}").collect {
            it.filename
        }
    }


}
