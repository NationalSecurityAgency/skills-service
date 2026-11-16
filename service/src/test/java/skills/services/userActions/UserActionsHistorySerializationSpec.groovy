/**
 * Copyright 2026 SkillTree
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

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.auth.UserInfoService
import skills.auth.adminAttribution.AdminAttribution
import skills.storage.model.UserActionsHistory
import skills.storage.repos.UserActionsHistoryRepo
import spock.lang.Specification

class UserActionsHistorySerializationSpec extends Specification {
    UserActionsHistoryRepo repository = Mock()
    UserInfoService userInfoService = Stub() {
        getCurrentUserId() >> 'requester'
    }
    UserActionsHistoryService service = new UserActionsHistoryService(
            userActionsHistoryRepo: repository, userInfoService: userInfoService)

    def setup() {
        service.init()
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setAttribute(AdminAttribution.REQUEST_ATTRIBUTE, new AdminAttribution(
                actingUserId: 'actor', requestingSystemUserId: 'requester', projectId: 'project'))
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    def 'attributed single and batch history preserve bean values and filter secrets without mutating the bean'() {
        given:
        HistoryAttributes attributes = new HistoryAttributes(
                name: 'Preserved name', count: 7, enabled: false, action: DashboardAction.Edit,
                password: 'secret-password', clientSecret: 'secret-client', requestingSystemUserId: 'forged',
                nested: new HistoryAttributes(name: 'Nested name', count: 3, password: 'nested-password', clientSecret: 'nested-client'))
        UserActionInfo info = new UserActionInfo(action: DashboardAction.Edit, item: DashboardItem.Project,
                itemId: 'project', projectId: 'PROJECT', actionAttributes: attributes)
        List<UserActionsHistory> saved = []

        when:
        if (batch) {
            service.saveUserActions([info, info])
        } else {
            service.saveUserAction(info)
        }

        then:
        (batch ? 0 : 1) * repository.save(_ as UserActionsHistory) >> { UserActionsHistory row -> saved.add(row); row }
        (batch ? 1 : 0) * repository.saveAll(_) >> { arguments ->
            Iterable<UserActionsHistory> rows = arguments[0] as Iterable<UserActionsHistory>
            rows.each { UserActionsHistory row -> saved.add(row) }
            rows
        }
        saved.size() == (batch ? 2 : 1)
        saved.every { UserActionsHistory row ->
            row.userId == 'actor' && service.mapper.readValue(row.actionAttributes, Map) == [
                    name: 'Preserved name', count: 7, enabled: false, action: 'Edit', requestingSystemUserId: 'requester',
                    nested: [name: 'Nested name', count: 3, enabled: false, action: null, nested: null, requestingSystemUserId: null]]
        }
        saved.every { UserActionsHistory row -> !row.actionAttributes.contains('password') && !row.actionAttributes.contains('clientSecret') }
        attributes.requestingSystemUserId == 'forged'
        attributes.password == 'secret-password'
        attributes.clientSecret == 'secret-client'
        attributes.nested.password == 'nested-password'
        attributes.nested.clientSecret == 'nested-client'

        where:
        batch << [false, true]
    }

    def 'attributed map attributes filter secrets and replace a forged requester without mutating the map'() {
        given:
        Map attributes = [name: 'Preserved map', password: 'secret-password', clientSecret: 'secret-client',
                          requestingSystemUserId: 'forged', nested: [count: 3, password: 'nested-password', clientSecret: 'nested-client']]

        when:
        service.saveUserAction(new UserActionInfo(action: DashboardAction.Edit, item: DashboardItem.Project,
                projectId: 'project', actionAttributes: attributes))

        then:
        1 * repository.save({ UserActionsHistory row ->
            row.userId == 'actor' && service.mapper.readValue(row.actionAttributes, Map) ==
                    [name: 'Preserved map', requestingSystemUserId: 'requester', nested: [count: 3]]
        })
        attributes.requestingSystemUserId == 'forged'
        attributes.password == 'secret-password'
        attributes.clientSecret == 'secret-client'
        attributes.nested == [count: 3, password: 'nested-password', clientSecret: 'nested-client']
    }

    def 'the resource guard keeps unvalidated history attributed to the requester'() {
        when:
        service.saveUserAction(new UserActionInfo(action: DashboardAction.Create, item: DashboardItem.Project,
                projectId: 'destination', actionAttributes: [name: 'Destination', password: 'secret']))

        then:
        1 * repository.save({ UserActionsHistory row ->
            Map attributes = service.mapper.readValue(row.actionAttributes, Map)
            row.userId == 'requester' && attributes.name == 'Destination' &&
                    !attributes.containsKey('requestingSystemUserId') && !attributes.containsKey('password')
        })
    }

    static class HistoryAttributes {
        String name
        int count
        boolean enabled
        DashboardAction action
        String password
        String clientSecret
        String requestingSystemUserId
        HistoryAttributes nested
    }
}
