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

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
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
                    and (:projectIdFilter is null OR lower(action.projectId) like lower(concat('%', :projectIdFilter, '%')))
                    and (:itemFilter is null OR action.item = :itemFilter)
                    and (:userFilter is null OR lower(userAttrs.userIdForDisplay) like lower(concat('%', :userFilter, '%')))
                    and (:quizFilter is null OR lower(action.quizId) like lower(concat('%', :quizFilter, '%')))
                    and (:itemIdFilter is null OR lower(action.itemId) like lower(concat('%', :itemIdFilter, '%')))
                    and (:actionFilter is null OR action.action = :actionFilter)
    ''')
    Page<UserActionsPreview> getActions(@Nullable @Param("projectIdFilter") String projectIdFilter,
                                        @Nullable @Param("itemFilter") DashboardItem itemFilter,
                                        @Nullable @Param("userFilter") String userFilter,
                                        @Nullable @Param("quizFilter") String quizFilter,
                                        @Nullable @Param("itemIdFilter") String itemIdFilter,
                                        @Nullable @Param("actionFilter") DashboardAction actionFilter,
                                        Pageable pageable)

    @Nullable
    @Query('''select distinct action.action
                from UserActionsHistory action
                where (:projectId is null OR lower(action.projectId) = lower(:projectId))
                    and (:quizId is null OR lower(action.quizId) = lower(:quizId))
    ''')
    List<DashboardAction> findDistinctDashboardActions(@Nullable @Param("projectId") String projectId,
                                                        @Nullable @Param("quizId") String quizId)

    @Nullable
    @Query('''select distinct action.item
                from UserActionsHistory action
                where (:projectId is null OR lower(action.projectId) = lower(:projectId))
                    and (:quizId is null OR lower(action.quizId) = lower(:quizId))
    ''')
    List<DashboardItem> findDistinctDashboardItems(@Nullable @Param("projectId") String projectId,
                                                       @Nullable @Param("quizId") String quizId)
}
