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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import jakarta.persistence.*

@Entity
@Table(name="notifications")
@ToString(includeNames = true)
@EntityListeners(AuditingEntityListener)
class Notification {

    static enum Type {
        SkillApprovalRequested, SkillApprovalResponse, ProjectExpiration, ContactUsers, ContactOwner, InviteOnly, InviteOnlyReminder, PasswordReset, VerifyEmail,
        QuizGradingRequested, QuizGradedResponse
    }

    static class KeyValParam {
        String key
        Object val

        KeyValParam(String key, Object val) {
            this.key = key
            this.val = val
        }
        KeyValParam() {
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String userId

    Date requestedOn

    // notification type
    String type
    // Encoded list of List<KeyValParam>
    String encodedParams

    // this will be incremented if notification fails to run
    int failedCount

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
