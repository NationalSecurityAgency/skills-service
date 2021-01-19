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
import org.apache.commons.lang3.ObjectUtils
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
    @Query(value="""
        select start as day, sum(count) as count from user_events
        where start > :start AND skill_ref_id = :skillRefId AND event_type = :type 
        group by start
        order by start desc
    """, nativeQuery = true)
    Stream<DayCountItem> getCountForSkill(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") UserEvent.EventType type)

    @Nullable
    @Query(value="""
        select start as day, sum(count) as count from user_events
        where start > :start AND event_type = :type AND 
        skill_ref_id in (SELECT child_ref_id FROM skill_relationship_definition where parent_ref_id = :skillRefId)
        group by start
        order by start desc
    """, nativeQuery = true)
    Stream<DayCountItem> getCountForSubject(@Param("skillRefId") Integer skillRefId, @Param("start") Date start, @Param("type") UserEvent.EventType type)

    @Query(value="""
        select start as day, sum(count) as count from user_events
        where start > :start AND event_type = :type AND 
        skill_ref_id in (SELECT id FROM skill_definition WHERE project_id = :projectId AND type = 'Skill')   
        group by start 
        order by start desc
    """, nativeQuery = true)
    Stream<DayCountItem> getCountForProject(@Param("projectId") String projectId, @Param("start") Date start, @Param("type") UserEvent.EventType type)

    @Nullable
    Stream<UserEvent> findAllBySkillRefIdAndEventType(Integer skillRefId, UserEvent.EventType type)

    @QueryHints(value = [
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")
    ])
    @Nullable
    Stream<UserEvent> findAllByEventTypeAndStartLessThan(UserEvent.EventType type, Date start)

    void deleteByEventTypeAndStartLessThan(UserEvent.EventType type, Date start)


}
