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

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserActionsHistory

interface UserActionsHistoryRepo extends CrudRepository<UserActionsHistory, Long> {

    static interface UserActionsPreview {
        Long getId()
        DashboardAction getAction()
        DashboardItem getItem()
        String getItemId()
        Integer getItemRefId()

        String getUserId()
        String getUserIdForDisplay()
        String getProjectId()
        String getQuizId()

        Date getCreated()
    }

    @Query('''select action.id as id, 
                    action.action as action,
                    action.item as item,
                    action.itemId as itemId,
                    action.itemRefId as itemRefId,
                    action.userId as userId,
                    userAttrs.userIdForDisplay as userIdForDisplay,
                    action.projectId as projectId,
                    action.quizId as quizId,
                    action.created as created
                from UserActionsHistory action, UserAttrs userAttrs
                where action.userId = userAttrs.userId
    ''')
    List<UserActionsPreview> getActions(Pageable pageable)

}
