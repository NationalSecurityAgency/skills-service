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
package skills.services.userActions

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillQuizException
import skills.controller.result.model.DashboardUserActionRes
import skills.controller.result.model.DashboardUserActionsFilterOptions
import skills.controller.result.model.TableResult
import skills.services.inception.InceptionProjectService
import skills.storage.model.UserActionsHistory
import skills.storage.repos.UserActionsHistoryRepo

import java.text.SimpleDateFormat

@Service
@Slf4j
class UserActionsHistoryService {

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    @Autowired
    UserInfoService userInfoService

    @JsonFilter("DynamicFilter")
    class DynamicFilterMixIn {
    }

    ObjectMapper mapper
    @PostConstruct
    void init() {
        mapper = new ObjectMapper().addMixIn(Object.class, DynamicFilterMixIn.class)
        SimpleBeanPropertyFilter simpleBeanPropertyFilter = SimpleBeanPropertyFilter.serializeAllExcept("clientSecret", "password")
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("DynamicFilter", (SimpleBeanPropertyFilter)simpleBeanPropertyFilter);
        mapper.setFilterProvider(filterProvider)
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"));
    }

    @Transactional
    void saveUserAction(UserActionInfo userActionInfo){
        if (userActionInfo.projectId == InceptionProjectService.inceptionProjectId) {
            return
        }
        String actionAttributesAsStr = userActionInfo.actionAttributes ? mapper.writeValueAsString(userActionInfo.actionAttributes) : null
        UserActionsHistory userActionsHistory = new UserActionsHistory(
                action: userActionInfo.action,
                item: userActionInfo.item,
                itemId: userActionInfo.itemId,
                itemRefId: userActionInfo.itemRefId,
                userId: userInfoService.currentUserId,
                projectId: userActionInfo.projectId,
                quizId: userActionInfo.quizId,
                actionAttributes: actionAttributesAsStr,
        )
        userActionsHistoryRepo.save(userActionsHistory)
    }

    @Transactional
    void saveUserActions(List<UserActionInfo> userActionInfos) {
        List<UserActionsHistory> userActionsHistoryList = userActionInfos.findAll {
            it.projectId != InceptionProjectService.inceptionProjectId }.collect { UserActionInfo userActionInfo ->
                String actionAttributesAsStr = userActionInfo.actionAttributes ? mapper.writeValueAsString(userActionInfo.actionAttributes) : null
                return new UserActionsHistory(
                        action: userActionInfo.action,
                        item: userActionInfo.item,
                        itemId: userActionInfo.itemId,
                        itemRefId: userActionInfo.itemRefId,
                        userId: userInfoService.currentUserId,
                        projectId: userActionInfo.projectId,
                        quizId: userActionInfo.quizId,
                        actionAttributes: actionAttributesAsStr,
                )
        }
        userActionsHistoryRepo.saveAll(userActionsHistoryList)
    }

    @Transactional
    TableResult getUsersActions(PageRequest pageRequest,
                                String projectId,
                                String quizId,
                                String projectIdFilter,
                                DashboardItem itemFilter,
                                String userFilter,
                                String quizFilter,
                                String itemIdFilter,
                                DashboardAction actionFilter) {
        // xxxNotProvided variables/params are a workaround for an issues introduced in
        // spring-boot:3.4.3 where usage of the same named parameter in query such as
        //    `(:projectIdFilter is null OR lower(action.projectId) like :projectIdFilter)`
        // yields `org.hibernate.QueryParameterException: No argument for named parameter ':projectIdFilter_1'

        String projectIdFilterQuery = projectIdFilter ? '%' + projectIdFilter.toLowerCase() + '%' : null
        String projectIdFilterQueryNotProvided = projectIdFilter ? "false" : "true"
        String userFilterQuery = userFilter ? '%' + userFilter.toLowerCase() + '%' : null
        String userFilterQueryNotProvided = userFilter ? "false" : "true"
        String quizFilterQuery = quizFilter ? '%' + quizFilter.toLowerCase() + '%' : null
        String quizFilterQueryNotProvided = quizFilter ? "false" : "true"
        String itemIdFilterQuery = itemIdFilter ? '%' + itemIdFilter.toLowerCase() + '%' : null
        String itemIdFilterQueryNotProvided = itemIdFilter ? "false" : "true"
        Page<UserActionsHistoryRepo.UserActionsPreview> userActionsPreviewFromDB = userActionsHistoryRepo.getActions(
                projectId?.toLowerCase(), quizId?.toLowerCase(),
                projectIdFilterQuery, projectIdFilterQueryNotProvided,
                itemFilter,
                userFilterQuery, userFilterQueryNotProvided,
                quizFilterQuery, quizFilterQueryNotProvided,
                itemIdFilterQuery, itemIdFilterQueryNotProvided,
                actionFilter, pageRequest)
        Long totalRows = userActionsPreviewFromDB.getTotalElements()
        List<DashboardUserActionRes> actionResList = userActionsPreviewFromDB.getContent().collect {
            new DashboardUserActionRes(
                    id: it.id,
                    action: it.action,
                    item: it.item,
                    itemId: it.itemId,
                    itemRefId: it.itemRefId,
                    userId: it.userId,
                    userIdForDisplay: it.userIdForDisplay,
                    projectId: it.projectId,
                    quizId: it.quizId,
                    created: it.created,
                    firstName: it.firstName,
                    lastName: it.lastName
            )
        }

        return new TableResult(
                count: totalRows,
                totalCount: totalRows,
                data: actionResList
        )
    }

    Map getActionAttributes(Long id, String assertThisProjectId = null, String assertThisQuizId = null) {
        Optional<UserActionsHistory> optional = userActionsHistoryRepo.findById(id)
        if (optional.empty) {
            throw new SkillException("Failed to locate UserActionsHistory by id [${id}]");
        }
        UserActionsHistory userActionsHistory = optional.get()

        if (assertThisProjectId
                && (!userActionsHistory.projectId || !userActionsHistory.projectId.equalsIgnoreCase(assertThisProjectId))) {
           throw new SkillException("UserActionsHistory id [${id}] does not belong to project [${assertThisProjectId}]", assertThisProjectId, null, ErrorCode.AccessDenied);
        }

        if (assertThisQuizId
                && (!userActionsHistory.quizId || !userActionsHistory.quizId.equalsIgnoreCase(assertThisQuizId))) {
            throw new SkillQuizException("UserActionsHistory id [${id}] does not belong to quiz [${assertThisQuizId}]", assertThisQuizId, ErrorCode.AccessDenied);
        }

        if (!userActionsHistory.actionAttributes) {
            return new HashMap<>()
        }

        Map result = mapper.readValue(userActionsHistory.actionAttributes, Map.class)
        return result
    }

    DashboardUserActionsFilterOptions getUserActionsFilterOptions(String projectId = null, String quizId = null) {
        String projectIdFilter = projectId ? projectId.toLowerCase() : null
        String quizIdFilter = quizId ? quizId.toLowerCase() : null
        List<DashboardAction> dashboardActions = userActionsHistoryRepo.findDistinctDashboardActions(projectIdFilter, quizIdFilter)
        List<DashboardItem> dashboardItems = userActionsHistoryRepo.findDistinctDashboardItems(projectIdFilter, quizIdFilter)

        return new DashboardUserActionsFilterOptions(
                actionFilterOptions: dashboardActions.collect { it.toString() },
                itemFilterOptions: dashboardItems.collect { it.toString() }
        )
    }
}
