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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import skills.controller.request.model.AiChatRequest
import skills.services.openai.OpenAIService
import skills.controller.exceptions.SkillsValidator
import skills.settings.AiPromptSettings
import skills.settings.AiPromptSettingsService

@RestController
@RequestMapping("/openai")
@Slf4j
class OpenAiController {

    @Autowired
    OpenAIService openAIService

    @Autowired
    AiPromptSettingsService aiPromptSettingsService

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> chat(@RequestBody AiChatRequest genDescRequest) {
        SkillsValidator.isNotNull(genDescRequest.messages, "genDescRequest.messages")
        SkillsValidator.isTrue(genDescRequest.messages.size() > 0, "genDescRequest.messages must have at least one message")
        SkillsValidator.isNotBlank(genDescRequest.model, "genDescRequest.model")
        SkillsValidator.isNotNull(genDescRequest.modelTemperature, "genDescRequest.modelTemperature")
        SkillsValidator.isTrue(genDescRequest.modelTemperature >= 0 && genDescRequest.modelTemperature <= 2, "genDescRequest.modelTemperature must be >= 0 and <= 2")
        return openAIService.streamChat(genDescRequest)
    }

    @GetMapping("/models")
    OpenAIService.AvailableModels getModels() {
        return openAIService.getAvailableModels()
    }

    @GetMapping('/getAiPromptSettings')
    AiPromptSettings getAiPromptSettings(){
        return aiPromptSettingsService.fetchAiPromptSettings()
    }

}

