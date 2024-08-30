/**
 * Copyright 2024 SkillTree
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
package skills.dbupgrade

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.json.JsonMapper
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import skills.controller.AddSkillHelper
import skills.controller.request.model.SkillEventRequest

import java.nio.charset.StandardCharsets

@Slf4j
@Component
class EventsResourceProcessor {
    static final int STATUS_INTERVAL = 1000
    JsonMapper jsonMapper = new JsonMapper()

    @Autowired
    AddSkillHelper addSkillHelper

    @Autowired
    DBCheckPointer checkPointer

    void processFile(Resource file) {
        String fileName = file.filename
        log.info("processing queued skill event file [${fileName}]")
        ObjectWriter errorSerializer = jsonMapper.writerFor(QueuedSkillEvent)

        InputStream inputStream = file.getInputStream();
        String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        inputStream.close()

        int i = 0
        try (MappingIterator<QueuedSkillEvent> itr = jsonMapper.readerFor(QueuedSkillEvent).readValues(text)) {
            int startAt = checkPointer.getRecordToStartOn(fileName)
            while (itr.hasNext()) {
                QueuedSkillEvent queuedSkillEvent = itr.nextValue()
                if (i >= startAt) {
                    try {
                        SkillEventRequest skr = queuedSkillEvent.skillEventRequest
                        if (!skr) {
                            skr = new SkillEventRequest(userId: queuedSkillEvent.userId)
                            skr.timestamp = queuedSkillEvent.requestTime.getTime()
                        } else if (!skr.userId) {
                            skr.userId = queuedSkillEvent.userId
                        }
                        addSkillHelper.addSkill(queuedSkillEvent.projectId, queuedSkillEvent.skillId, skr)
                    } catch (Exception e) {
                        String asStr = errorSerializer.writeValueAsString(queuedSkillEvent)
                        log.error("unable to add queued event [$asStr]", e)
                    }

                    checkPointer.recordRecord(fileName, i)
                } else {
                    log.debug("skipping record [{}], last record read before shutdown was [{}]", i, startAt)
                }
                i++

                if (i % STATUS_INTERVAL == 0 && i > 0) {
                    log.info("recovered [$i] events from [${file}] so far")
                }
            }
        }
        log.info("finished processing queued skill event file [${file}], recovered [$i] total events")
        checkPointer.cleanup(fileName)
    }
}
