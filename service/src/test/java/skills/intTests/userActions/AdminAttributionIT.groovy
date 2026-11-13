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
package skills.intTests.userActions

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import skills.auth.adminAttribution.AdminAttributionLoggingFilter
import skills.auth.adminAttribution.AdminAttributionProperties
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.storage.model.auth.RoleName

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

@TestPropertySource(properties = [
        'skills.authorization.adminAttribution.enabled=true',
        'skills.authorization.adminAttribution.headerName=X-Test-Admin-User'
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAttributionIT extends DefaultIntSpec {
    static final String HEADER = 'X-Test-Admin-User'

    @Autowired
    AdminAttributionProperties attributionProperties

    List<SkillsService> users
    Logger attributionLogger
    ListAppender<ILoggingEvent> appender
    BlockingQueue<ILoggingEvent> loggedRequests

    def setup() {
        users = getRandomUsers(3).collect { String userId -> createService(userId) }
        // The deployment allowlist uses the randomly selected FORM/PKI-compatible test identities.
        attributionProperties.allowedRequesters = [users[0].userName.toLowerCase(Locale.ROOT)] as Set
        attributionLogger = LoggerFactory.getLogger(AdminAttributionLoggingFilter) as Logger
        loggedRequests = new LinkedBlockingQueue<ILoggingEvent>()
        BlockingQueue<ILoggingEvent> events = loggedRequests
        appender = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                super.append(event)
                events.add(event)
            }
        }
        appender.start()
        attributionLogger.addAppender(appender)
    }

    def cleanup() {
        attributionLogger.detachAppender(appender)
        appender.stop()
        attributionProperties.allowedRequesters = [] as Set
    }

    def 'project edits record the acting user and authenticated requester without leaking to the next request'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        users[0].addProjectAdmin(project.projectId, users[1].userName)

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Attributed project', description: 'Preserved description'], users[1].userName.toUpperCase(Locale.ROOT))
        Map edit = users[1].getUserActionsForProject(project.projectId).data.find { Map row -> row.action == 'Edit' }
        Map attributes = users[1].getProjectUserActionAttributes(project.projectId, edit.id as Long)
        users[0].createSubject(createSubject())
        Map subject = users[1].getUserActionsForProject(project.projectId).data.find { Map row -> row.item == 'Subject' }

        then:
        response.statusCode == HttpStatus.OK
        users[1].getProject(project.projectId).name == 'Attributed project'
        edit.userId == users[1].userName.toLowerCase(Locale.ROOT)
        attributes.requestingSystemUserId == users[0].userName.toLowerCase(Locale.ROOT)
        attributes.description == 'Preserved description'
        attributes.newName == 'Attributed project'
        subject.userId == users[0].userName.toLowerCase(Locale.ROOT)
        !users[1].getProjectUserActionAttributes(project.projectId, subject.id as Long).containsKey('requestingSystemUserId')
        messages().size() == 1
        messages()[0].contains("userId=[${users[1].userName.toLowerCase(Locale.ROOT)}]")
        messages()[0].contains("requestingSystemUserId=[${users[0].userName.toLowerCase(Locale.ROOT)}]")
        messages()[0].contains('method=[POST]')
        messages()[0].contains("path=[/admin/projects/${project.projectId}]")
        messages()[0].contains('attributionStatus=[applied] httpStatus=[200]')
    }

    def 'non-allowlisted administrators including root cannot provide the header'() {
        given:
        Map project = createProject()
        users[1].createProject(project)
        users[1].addProjectAdmin(project.projectId, users[0].userName)
        SkillsService requester = root ? createRootSkillService(users[1].userName, 'password') : users[1]
        long historyCount = users[0].getUserActionsForProject(project.projectId).count

        when:
        ResponseEntity<Map> response = attributedRequest(requester, "/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], users[0].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getProject(project.projectId).name == project.name
        users[0].getUserActionsForProject(project.projectId).count == historyCount
        messages().size() == 1
        messages()[0].contains('attributionStatus=[rejected] httpStatus=[403]')

        where:
        root << [false, true]
    }

    def 'allowlisting does not let a non-admin requester borrow the acting user privileges'() {
        given:
        Map project = createProject()
        users[1].createProject(project)

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], users[1].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[1].getProject(project.projectId).name == project.name
        users[1].getUserActionsForProject(project.projectId).count == 1
    }

    def 'an attributed user must administer this project, not just another project'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        users[1].createProject(createProject(2))
        String actingUser = existing ? users[1].userName : 'unknown-attribution-user'

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], actingUser)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getProject(project.projectId).name == project.name
        users[0].getUserActionsForProject(project.projectId).count == 1
        messages()[0].contains("claimedActingUserId=[${actingUser}]")

        where:
        existing << [false, true]
    }

    def 'malformed scoped headers are rejected before any mutation'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        HttpHeaders headers = new HttpHeaders()
        values.each { String value -> headers.add(HEADER, value) }

        when:
        ResponseEntity<Map> response = users[0].requestWithHeaders("/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], headers)
        awaitAdminLog()

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == 'BadParam'
        users[0].getProject(project.projectId).name == project.name
        users[0].getUserActionsForProject(project.projectId).count == 1
        messages()[0].contains('attributionStatus=[rejected] httpStatus=[400]')

        where:
        values << [[''], ['null'], ['x' * 256], ['first', 'second'], ['first,second']]
    }

    def 'allowlisted root requester and root acting user qualify as resource administrators'() {
        given:
        Map resource = quizResource ? QuizDefFactory.createQuiz() : createProject()
        String id = quizResource ? resource.quizId : resource.projectId
        if (quizResource) {
            users[2].createQuizDef(resource)
            users[2].addQuizUserRole(id, users[rootRequester ? 1 : 0].userName, RoleName.ROLE_QUIZ_ADMIN.name())
        } else {
            users[2].createProject(resource)
            users[2].addProjectAdmin(id, users[rootRequester ? 1 : 0].userName)
        }
        SkillsService rootUser = createRootSkillService(users[rootRequester ? 0 : 1].userName, 'password')
        SkillsService requester = rootRequester ? rootUser : users[0]
        SkillsService actor = rootRequester ? users[1] : rootUser

        when:
        ResponseEntity<Map> response = attributedRequest(requester, "/admin/${quizResource ? 'quiz-definitions' : 'projects'}/${id}",
                HttpMethod.POST, resource + [name: 'Root attributed edit'], actor.userName)
        Map history = quizResource ? users[2].getUserActionsForQuiz(id) : users[2].getUserActionsForProject(id)
        Map edit = history.data.find { Map row -> row.action == 'Edit' }

        then:
        response.statusCode == HttpStatus.OK
        (quizResource ? users[2].getQuizDef(id) : users[2].getProject(id)).name == 'Root attributed edit'
        edit.userId == actor.userName.toLowerCase(Locale.ROOT)
        (quizResource ? users[2].getQuizUserActionAttributes(id, edit.id as Long) :
                users[2].getProjectUserActionAttributes(id, edit.id as Long)).requestingSystemUserId == requester.userName.toLowerCase(Locale.ROOT)

        where:
        rootRequester | quizResource
        false         | false
        true          | false
        false         | true
        true          | true
    }

    def 'quiz edits record both identities and preserve existing attributes'() {
        given:
        Map quiz = QuizDefFactory.createQuiz()
        users[0].createQuizDef(quiz)
        users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.name())

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/quiz-definitions/${quiz.quizId}",
                HttpMethod.POST, quiz + [name: 'Attributed quiz', description: 'Quiz description'], users[1].userName)
        Map edit = users[1].getUserActionsForQuiz(quiz.quizId).data.find { Map row -> row.action == 'Edit' }
        Map attributes = users[1].getQuizUserActionAttributes(quiz.quizId, edit.id as Long)

        then:
        response.statusCode == HttpStatus.OK
        users[1].getQuizDef(quiz.quizId).name == 'Attributed quiz'
        edit.userId == users[1].userName.toLowerCase(Locale.ROOT)
        attributes.requestingSystemUserId == users[0].userName.toLowerCase(Locale.ROOT)
        attributes.description == 'Quiz description'
        messages()[0].contains("resource=[quiz:${quiz.quizId}]")
    }

    def 'a quiz administrator of another quiz cannot be attributed'() {
        given:
        Map quiz = QuizDefFactory.createQuiz()
        users[0].createQuizDef(quiz)
        users[1].createQuizDef(QuizDefFactory.createQuiz(2))

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/quiz-definitions/${quiz.quizId}",
                HttpMethod.POST, quiz + [name: 'Must not change'], users[1].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getQuizDef(quiz.quizId).name == quiz.name
        users[0].getUserActionsForQuiz(quiz.quizId).count == 1
    }

    def 'a project approver cannot qualify as either attribution identity'() {
        given:
        Map project = createProject()
        users[2].createProject(project)
        SkillsService approver = users[requesterApprover ? 0 : 1]
        SkillsService administrator = users[requesterApprover ? 1 : 0]
        users[2].addUserRole(approver.userName, project.projectId, RoleName.ROLE_PROJECT_APPROVER.name())
        users[2].addProjectAdmin(project.projectId, administrator.userName)

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}",
                HttpMethod.GET, null, users[1].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[2].getProject(project.projectId).name == project.name

        where:
        requesterApprover << [false, true]
    }

    def 'quiz read-only access cannot qualify as either attribution identity'() {
        given:
        Map quiz = QuizDefFactory.createQuiz()
        users[2].createQuizDef(quiz)
        users[2].createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions())
        Map project = createProject()
        SkillsService reader = users[requesterReadOnly ? 0 : 1]
        SkillsService administrator = users[requesterReadOnly ? 1 : 0]
        users[2].createProject(project)
        users[2].createSubject(createSubject())
        users[2].createSkill(createSkill() + [selfReportingType: 'Quiz', quizId: quiz.quizId])
        users[2].addProjectAdmin(project.projectId, reader.userName)
        users[2].addQuizUserRole(quiz.quizId, administrator.userName, RoleName.ROLE_QUIZ_ADMIN.name())
        assert reader.getQuizDef(quiz.quizId).name == quiz.name

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/quiz-definitions/${quiz.quizId}",
                HttpMethod.GET, null, users[1].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[2].getQuizDef(quiz.quizId).name == quiz.name

        where:
        requesterReadOnly << [false, true]
    }

    def 'an empty allowlist rejects every header-bearing admin requester'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        attributionProperties.allowedRequesters = [] as Set

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}", HttpMethod.GET, null, users[0].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getProject(project.projectId).name == project.name
    }

    def 'protected resource attribution requires acting-user community membership including for root'() {
        given:
        SkillsService actor = rootActor ? createRootSkillService(users[1].userName, 'password') : users[1]
        SkillsService root = rootActor ? actor : createRootSkillService(users[2].userName, 'password')
        root.saveUserTag(users[0].userName, 'dragons', ['DivineDragon'])
        Map resource = (quizResource ? QuizDefFactory.createQuiz() : createProject()) + [enableProtectedUserCommunity: true]
        String id = quizResource ? resource.quizId : resource.projectId
        String path = "/admin/${quizResource ? 'quiz-definitions' : 'projects'}/${id}"
        if (quizResource) {
            users[0].createQuizDef(resource)
        } else {
            users[0].createProject(resource)
        }
        if (!rootActor) {
            root.saveUserTag(actor.userName, 'dragons', ['DivineDragon'])
            if (quizResource) {
                users[0].addQuizUserRole(id, actor.userName, RoleName.ROLE_QUIZ_ADMIN.name())
            } else {
                users[0].addProjectAdmin(id, actor.userName)
            }
            // There is no user-tag deletion endpoint; retain the role while revoking membership.
            userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(actor.userName.toLowerCase(Locale.ROOT), 'dragons'))
        }
        long historyCount = (quizResource ? users[0].getUserActionsForQuiz(id) : users[0].getUserActionsForProject(id)).count

        expect:
        actor.requestWithHeaders(path, HttpMethod.GET, null, new HttpHeaders()).statusCode == HttpStatus.FORBIDDEN

        when:
        ResponseEntity<Map> rejected = attributedRequest(users[0], path, HttpMethod.POST,
                resource + [name: 'Must not change'], actor.userName)

        then:
        rejected.statusCode == HttpStatus.FORBIDDEN
        (quizResource ? users[0].getQuizDef(id) : users[0].getProject(id)).name == resource.name
        (quizResource ? users[0].getUserActionsForQuiz(id) : users[0].getUserActionsForProject(id)).count == historyCount
        messages().size() == 1
        messages()[0].contains("claimedActingUserId=[${actor.userName}]")
        messages()[0].contains('attributionStatus=[rejected] httpStatus=[403]')

        when:
        root.saveUserTag(actor.userName, 'dragons', ['DivineDragon'])
        ResponseEntity<Map> accepted = attributedRequest(users[0], path, HttpMethod.POST,
                resource + [name: 'Eligible attributed edit'], actor.userName)
        Map edit = (quizResource ? users[0].getUserActionsForQuiz(id) : users[0].getUserActionsForProject(id))
                .data.find { Map row -> row.action == 'Edit' }

        then:
        accepted.statusCode == HttpStatus.OK
        (quizResource ? actor.getQuizDef(id) : actor.getProject(id)).name == 'Eligible attributed edit'
        edit.userId == actor.userName.toLowerCase(Locale.ROOT)
        (quizResource ? users[0].getQuizUserActionAttributes(id, edit.id as Long) :
                users[0].getProjectUserActionAttributes(id, edit.id as Long)).requestingSystemUserId == users[0].userName.toLowerCase(Locale.ROOT)
        !users[0].isRoot()
        messages().size() == 2
        messages()[1].contains('attributionStatus=[applied] httpStatus=[200]')

        where:
        rootActor | quizResource
        false     | false
        true      | false
        false     | true
        true      | true
    }

    def 'batch history with no action attributes records the requester on every row'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        users[0].addProjectAdmin(project.projectId, users[1].userName)
        List<String> archivedUsers = [users[1].userName, users[2].userName]*.toLowerCase(Locale.ROOT)

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/projects/${project.projectId}/users/archive",
                HttpMethod.POST, [userIds: archivedUsers], users[1].userName)
        List<Map> actions = users[1].getUserActionsForProject(project.projectId).data.findAll { Map row -> row.action == DashboardAction.ArchiveUser.name() }

        then:
        response.statusCode == HttpStatus.OK
        users[1].requestWithHeaders("/admin/projects/${project.projectId}/users/archive?page=1&limit=10&orderBy=userId&ascending=true",
                HttpMethod.GET, null, new HttpHeaders()).body.data.userId.sort() == archivedUsers*.toLowerCase(Locale.ROOT).sort()
        actions.size() == 2
        actions.every { Map row -> row.userId == users[1].userName.toLowerCase(Locale.ROOT) }
        actions.every { Map row -> users[1].getProjectUserActionAttributes(project.projectId, row.id as Long) ==
                [requestingSystemUserId: users[0].userName.toLowerCase(Locale.ROOT)] }
    }

    def 'resource renames retain validated attribution'() {
        given:
        Map resource = quizResource ? QuizDefFactory.createQuiz() : createProject()
        if (quizResource) {
            users[0].createQuizDef(resource)
            users[0].addQuizUserRole(resource.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.name())
        } else {
            users[0].createProject(resource)
            users[0].addProjectAdmin(resource.projectId, users[1].userName)
        }
        String id = quizResource ? resource.quizId : resource.projectId
        String newId = "${id}Renamed"
        Map changes = resource + (quizResource ? [quizId: newId] : [projectId: newId])

        when:
        ResponseEntity<Map> response = attributedRequest(users[0], "/admin/${quizResource ? 'quiz-definitions' : 'projects'}/${id}",
                HttpMethod.POST, changes, users[1].userName)
        Map history = quizResource ? users[1].getUserActionsForQuiz(newId) : users[1].getUserActionsForProject(newId)
        Map edit = history.data.find { Map row -> row.action == 'Edit' }

        then:
        response.statusCode == HttpStatus.OK
        edit.userId == users[1].userName.toLowerCase(Locale.ROOT)
        (quizResource ? users[1].getQuizUserActionAttributes(newId, edit.id as Long) :
                users[1].getProjectUserActionAttributes(newId, edit.id as Long)).requestingSystemUserId == users[0].userName.toLowerCase(Locale.ROOT)

        where:
        quizResource << [false, true]
    }

    def 'unscoped admin actions ignore attribution but still require the allowlist and log the claim'() {
        given:
        Map badge = [badgeId: 'globalBadge', name: 'Original badge']
        users[0].createGlobalBadge(badge)
        users[0].grantGlobalBadgeAdminRole(badge.badgeId, users[1].userName)

        when:
        ResponseEntity<Map> denied = attributedRequest(users[1], "/admin/badges/${badge.badgeId}",
                HttpMethod.POST, badge + [name: 'Denied edit'], users[2].userName)
        ResponseEntity<Map> accepted = attributedRequest(users[0], "/admin/badges/${badge.badgeId}",
                HttpMethod.POST, badge + [name: 'Unscoped edit'], 'unknown-attribution-user')

        then:
        denied.statusCode == HttpStatus.FORBIDDEN
        accepted.statusCode == HttpStatus.OK
        users[0].getGlobalBadge(badge.badgeId).name == 'Unscoped edit'
        messages().size() == 2
        messages()[1].contains("userId=[${users[0].userName.toLowerCase(Locale.ROOT)}]")
        messages()[1].contains('claimedActingUserId=[unknown-attribution-user]')
        messages()[1].contains('attributionStatus=[ignored_unscoped] httpStatus=[200]')
    }

    def 'even an unmatched admin route rejects a non-allowlisted requester'() {
        given:
        users[1].createProject(createProject())

        when:
        ResponseEntity<Map> response = attributedRequest(users[1], '/admin/no-such-resource', HttpMethod.GET, null, users[0].userName)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        messages().size() == 1
        messages()[0].contains('httpStatus=[403]')
    }

    def 'header-bearing requests rejected by CORS are also logged'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        HttpHeaders headers = new HttpHeaders()
        headers.set(HEADER, users[1].userName)
        headers.setOrigin('https://untrusted.example')

        when:
        ResponseEntity<String> response = users[0].requestWithHeaders("/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], headers, String)
        awaitAdminLog()

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getProject(project.projectId).name == project.name
        messages().size() == 1
        messages()[0].contains('attributionStatus=[rejected] httpStatus=[403]')
    }

    def 'header-bearing requests rejected by CSRF are also logged'() {
        given:
        Map project = createProject()
        users[0].createProject(project)
        HttpHeaders headers = new HttpHeaders()
        headers.set(HEADER, users[1].userName)
        headers.set('X-XSRF-TOKEN', 'invalid-token')

        when:
        ResponseEntity<Map> response = users[0].requestWithHeaders("/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Must not change'], headers)
        awaitAdminLog()

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        users[0].getProject(project.projectId).name == project.name
        messages().size() == 1
        messages()[0].contains('attributionStatus=[rejected] httpStatus=[403]')
    }

    def 'root endpoints ignore the header even for a non-allowlisted root requester'() {
        given:
        SkillsService root = createRootSkillService(users[1].userName, 'password')
        HttpHeaders headers = new HttpHeaders()
        headers.set(HEADER, 'unknown-attribution-user')

        when:
        ResponseEntity<Boolean> response = root.requestWithHeaders('/root/isRoot', HttpMethod.GET, null, headers, Boolean)

        then:
        response.statusCode == HttpStatus.OK
        response.body
        messages().empty
    }

    def 'other endpoints ignore the header and emit no attribution log'() {
        given:
        Map project = createProject()
        users[1].createProject(project)

        when:
        ResponseEntity<Map> response = attributedRequest(users[1], path.replace('PROJECT', project.projectId as String), HttpMethod.GET, null, 'unknown-attribution-user')

        then:
        response.statusCode == HttpStatus.OK
        messages().empty

        where:
        path << ['/app/userInfo', '/api/projects/PROJECT/summary', '/public/status']
    }

    def 'missing and non-configured headers retain requester attribution without logging'() {
        given:
        Map project = createProject()
        users[1].createProject(project)
        HttpHeaders headers = new HttpHeaders()
        headers.set('X-SkillTree-Acting-User', users[0].userName)

        when:
        ResponseEntity<Map> response = users[1].requestWithHeaders("/admin/projects/${project.projectId}",
                HttpMethod.POST, project + [name: 'Ordinary edit'], headers)
        Map edit = users[1].getUserActionsForProject(project.projectId).data.find { Map row -> row.action == 'Edit' }

        then:
        response.statusCode == HttpStatus.OK
        edit.userId == users[1].userName.toLowerCase(Locale.ROOT)
        !users[1].getProjectUserActionAttributes(project.projectId, edit.id as Long).containsKey('requestingSystemUserId')
        messages().empty
    }

    private ResponseEntity<Map> attributedRequest(SkillsService requester, String path, HttpMethod method, Object body, String actor) {
        HttpHeaders headers = new HttpHeaders()
        headers.set(HEADER, actor)
        ResponseEntity<Map> response = requester.requestWithHeaders(path, method, body, headers)
        if (path == '/admin' || path.startsWith('/admin/')) {
            awaitAdminLog()
        }
        return response
    }

    private void awaitAdminLog() {
        // HTTP response bytes can reach the client just before the filter's completion log.
        assert loggedRequests.poll(10, TimeUnit.SECONDS) != null
    }

    private List<String> messages() {
        return appender.list.collect { ILoggingEvent event -> event.formattedMessage }
    }
}
