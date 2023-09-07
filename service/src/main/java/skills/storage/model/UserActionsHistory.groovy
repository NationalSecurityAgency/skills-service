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
package skills.storage.model

import groovy.transform.ToString
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem

@ToString(includeNames = true)
@Entity()
@Table(name='user_actions_history')
@EntityListeners(AuditingEntityListener)
class UserActionsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    @Enumerated(EnumType.STRING)
    DashboardAction action

    @Enumerated(EnumType.STRING)
    DashboardItem item
    String itemId
    Integer itemRefId

    String userId
    String projectId
    String quizId

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    String actionAttributes

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created
}
