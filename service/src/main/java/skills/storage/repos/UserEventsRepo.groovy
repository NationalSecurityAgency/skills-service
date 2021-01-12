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
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef
import skills.storage.model.UserEvent
import skills.storage.model.UserPerformedSkill

import javax.persistence.QueryHint
import java.util.stream.Stream

@CompileStatic
interface UserEventsRepo extends CrudRepository<UserEvent, Integer> {

    @Nullable
    List<UserEvent> findAllBySkillRefIdAndUserIdAndEventType(Integer skillRefId, String userId, UserEvent.EventType type)

    @Nullable
    UserEvent findBySkillRefIdAndUserIdAndStartAndStopAndEventType(Integer skillRefId, String userId, Date start, Date stop, UserEvent.EventType type)

    @Nullable
    List<UserEvent> findAllBySkillRefId(Integer skillRefId)

    @Nullable
    Stream<UserEvent> findAllBySkillRefIdAndEventType(Integer skillRefId, UserEvent.EventType type)

    @QueryHints(value = [
        @QueryHint(name = "HINT_CACHEABLE", value = "false"),
        @QueryHint(name = "READ_ONLY", value = "true")
    ])
    @Nullable
    Stream<UserEvent> findAllByEventTypeAndStartLessThan(UserEvent.EventType type, Date start)

    void deleteByEventTypeAndStartLessThan(UserEvent.EventType type, Date start)


}
