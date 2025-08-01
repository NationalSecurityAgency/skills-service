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
package skills

import groovy.util.logging.Slf4j
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.util.unit.DataSize

import jakarta.annotation.PostConstruct

@Configuration
@ConfigurationProperties("skills.config")
@Slf4j
class UIConfigProperties {
    Map<String,String> ui = [:]
    Map<String,String> client = [:]

    String compactDailyEventsOlderThan

    String dbUpgradeInProgress

    DataSize maxAttachmentSize
    List<String> allowedAttachmentFileTypes
    List<MediaType> allowedAttachmentMimeTypes;
    List<String> allowedVideoUploadMimeTypes;
    List<String> allowedSlidesUploadMimeTypes;

    @PostConstruct
    void copyConfigToUi() {
        ui.put("maxDailyUserEvents", compactDailyEventsOlderThan)
        ui.put("dbUpgradeInProgress", dbUpgradeInProgress)
        ui.put("maxAttachmentSize", maxAttachmentSize.toBytes().toString())
        ui.put("allowedAttachmentFileTypes", allowedAttachmentFileTypes)
        ui.put("allowedAttachmentMimeTypes", allowedAttachmentMimeTypes.collect {it.toString()})
        ui.put("allowedVideoUploadMimeTypes", allowedVideoUploadMimeTypes)
        ui.put("allowedSlidesUploadMimeTypes", allowedSlidesUploadMimeTypes)
    }

    @PostConstruct
    void init() {
        if(client['loggingEnabled']) {
            log.info("Client Logging Enabled: ${client}")
        }
    }
}
