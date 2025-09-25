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
package skills.controller


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import skills.auth.openai.GenDescRequest
import skills.auth.openai.LearningContentGenerator
import skills.controller.exceptions.SkillsValidator

@RestController
@RequestMapping("/openai")
@Slf4j
class OpenAiController {

    @Autowired
    LearningContentGenerator learningContentGenerator

    @PostMapping(value = "/stream/description", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> generateDescriptionAndStream(@RequestBody GenDescRequest genDescRequest) {
        SkillsValidator.isNotBlank(genDescRequest.instructions, "genDescRequest.instructions")

        return learningContentGenerator.streamGenerateDescription(genDescRequest)
    }
}

