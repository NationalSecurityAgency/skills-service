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
package skills.tasks

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kagkarlsson.scheduler.Serializer
import groovy.util.logging.Slf4j

@Slf4j
class JsonSerializer implements Serializer {

    static final ObjectMapper mapper = new ObjectMapper()

    @Override
    byte[] serialize(Object o) {
        Optional<String> objectAsString = Optional.empty()

        try {
            objectAsString = Optional.ofNullable(mapper.writeValueAsString(o));
        } catch (JsonProcessingException squash){
            log.error("error serializing db-scheduled task data", squash)
        }

        return objectAsString.isEmpty() ? null :objectAsString.get().getBytes()
    }

    @Override
    <T> T deserialize(Class<T> aClass, byte[] bytes) {
        T o;
        def map = null
        try {
            map = mapper.readValue(new String(bytes), Map.class)
        } catch (IOException ioe) {
            log.error("error deserializing db-scheduled task data", ioe)
        }
        o = mapper.convertValue(map, aClass)
        return o
    }
}
