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
package skills.intTests

import org.springframework.boot.test.context.SpringBootTest
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.core.io.ClassPathResource
import org.springframework.messaging.simp.stomp.*
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.EmailUtils
import skills.utils.WaitFor
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

import java.lang.reflect.Type
import java.security.KeyStore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ['skills.authorization.corsAllowedOriginPatterns=https://trusted.example'],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class WebSocketSecurityIT extends DefaultIntSpec {
    WebSocketStompClient client
    StandardWebSocketClient transport
    StompSession session
    String token
    String projectId
    CompletableFuture<String> errorFrame

    def setup() {
        def project = SkillsFactory.createProject()
        projectId = project.projectId
        skillsService.createProject(project)
        transport = new StandardWebSocketClient()
        client = new WebSocketStompClient(transport)
        authenticateAs(skillsService.userName)
        errorFrame = new CompletableFuture<>()
    }

    def cleanup() {
        if (session?.connected) {
            try {
                session.disconnect()
            } catch (RuntimeException ex) {
                // A rejected frame closes the transport asynchronously, racing disconnect.
                if (!errorFrame.isDone()) {
                    throw ex
                }
            }
        }
        client?.stop()
    }

    def 'trusted origin and valid authentication can connect'() {
        expect:
        connect().get(10, TimeUnit.SECONDS).connected
    }

    def 'permitted subscription receives real skill notifications'() {
        given:
        def subject = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 1000
        skill.numPerformToCompletion = 1
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        CompletableFuture<String> update = new CompletableFuture<>()
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        session.subscribe("/user/queue/${projectId}-skill-updates", new StompFrameHandler() {
            @Override
            Type getPayloadType(StompHeaders headers) { byte[] }

            @Override
            void handleFrame(StompHeaders headers, Object payload) {
                update.complete(new String((byte[]) payload))
            }
        })
        skillsService.addSkill(skill, skillsService.userName)

        then:
        update.get(10, TimeUnit.SECONDS).contains(skill.skillId)
        !errorFrame.isDone()
    }

    def 'untrusted origin cannot establish a websocket connection'() {
        when:
        connect('https://untrusted.example').get(10, TimeUnit.SECONDS)

        then:
        thrown(ExecutionException)
    }

    def 'User Community subscription requires membership - member=#member, superuser=#superuser'() {
        given:
        def root = createRootSkillService()
        skillsService.getCurrentUser()
        root.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        def project = SkillsFactory.createProject(2)
        project.enableProtectedUserCommunity = true
        skillsService.createProject(project)
        projectId = project.projectId
        def user = superuser ? root : createService(getRandomUsers(1, true)[0])
        user.getCurrentUser()
        if (member) {
            root.saveUserTag(user.userName, 'dragons', ['DivineDragon'])
        }
        authenticateAs(user.userName)
        def skill = createNotificationSkill(2)
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        CompletableFuture<String> update = subscribeToSkillUpdates()
        if (member) {
            skillsService.addSkill(skill, user.userName)
        }

        then:
        if (member) {
            assert update.get(10, TimeUnit.SECONDS).contains(skill.skillId)
            assert !errorFrame.isDone()
        } else {
            assert errorFrame.get(10, TimeUnit.SECONDS)
            assert !update.isDone()
        }

        where:
        member | superuser
        false  | false
        true   | false
        false  | true
        true   | true
    }

    def 'invite-only subscription requires project access - invited=#invited'() {
        given:
        skillsService.changeSetting(projectId, 'invite_only', [projectId: projectId, setting: 'invite_only', value: 'true'])
        def user = createService(getRandomUsers(1, true)[0])
        if (invited) {
            startEmailServer()
            String emailAddress = user.userName.contains('@') ? user.userName : EmailUtils.generateEmaillAddressFor(user.userName)
            skillsService.inviteUsersToProject(projectId, [validityDuration: 'PT5M', recipients: [emailAddress]])
            assert WaitFor.wait { greenMail.receivedMessages.length > 0 }
            def email = EmailUtils.getEmail(greenMail, 0)
            def invite = (email.html =~ /join-project\/([^\/]+)\/([^?]+)/)[0][2]
            user.joinProject(projectId, invite)
        }
        authenticateAs(user.userName)
        def skill = createNotificationSkill(1)
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        CompletableFuture<String> update = subscribeToSkillUpdates()
        if (invited) {
            skillsService.addSkill(skill, user.userName)
        }

        then:
        if (invited) {
            assert update.get(10, TimeUnit.SECONDS).contains(skill.skillId)
            assert !errorFrame.isDone()
        } else {
            assert errorFrame.get(10, TimeUnit.SECONDS)
            assert !update.isDone()
        }

        where:
        invited << [false, true]
    }

    def 'invite-only access for #role with role on requested project=#sameProject'() {
        given:
        skillsService.changeSetting(projectId, 'invite_only', [projectId: projectId, setting: 'invite_only', value: 'true'])
        def user = role == RoleName.ROLE_SUPER_DUPER_USER ? createRootSkillService() : createService(getRandomUsers(1, true)[0])
        if (role != RoleName.ROLE_SUPER_DUPER_USER) {
            String roleProjectId = projectId
            if (!sameProject) {
                def otherProject = SkillsFactory.createProject(2)
                skillsService.createProject(otherProject)
                roleProjectId = otherProject.projectId
            }
            skillsService.addUserRole(user.userName, roleProjectId, role.toString())
        }
        authenticateAs(user.userName)
        def skill = createNotificationSkill(1)
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        CompletableFuture<String> update = subscribeToSkillUpdates()
        if (sameProject) {
            skillsService.addSkill(skill, user.userName)
        }

        then:
        if (sameProject) {
            assert update.get(10, TimeUnit.SECONDS).contains(skill.skillId)
            assert !errorFrame.isDone()
        } else {
            assert errorFrame.get(10, TimeUnit.SECONDS)
            assert !update.isDone()
        }

        where:
        role                               | sameProject
        RoleName.ROLE_PROJECT_ADMIN         | true
        RoleName.ROLE_PROJECT_APPROVER      | true
        RoleName.ROLE_SUPER_DUPER_USER       | true
        RoleName.ROLE_PROJECT_ADMIN         | false
        RoleName.ROLE_PROJECT_APPROVER      | false
    }

    @IgnoreIf({env['SPRING_PROFILES_ACTIVE'] == 'pki'})
    def 'superuser cannot subscribe outside the bearer token project'() {
        given:
        def root = createRootSkillService()
        def otherProject = SkillsFactory.createProject(2)
        skillsService.createProject(otherProject)
        skillsService.changeSetting(otherProject.projectId, 'invite_only', [projectId: otherProject.projectId, setting: 'invite_only', value: 'true'])
        authenticateAs(root.userName)
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        session.subscribe("/user/queue/${otherProject.projectId}-skill-updates", new StompSessionHandlerAdapter() {})

        then:
        errorFrame.get(10, TimeUnit.SECONDS)
    }

    private Map createNotificationSkill(int projectNumber) {
        def subject = SkillsFactory.createSubject(projectNumber, 1)
        def skill = SkillsFactory.createSkill(projectNumber, 1, 1)
        skill.pointIncrement = 1000
        skill.numPerformToCompletion = 1
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        return skill
    }

    private CompletableFuture<String> subscribeToSkillUpdates() {
        CompletableFuture<String> update = new CompletableFuture<>()
        session.subscribe("/user/queue/${projectId}-skill-updates", new StompFrameHandler() {
            @Override
            Type getPayloadType(StompHeaders headers) { byte[] }

            @Override
            void handleFrame(StompHeaders headers, Object payload) {
                update.complete(new String((byte[]) payload))
            }
        })
        return update
    }

    @IgnoreIf({env['SPRING_PROFILES_ACTIVE'] == 'pki'})
    def 'CONNECT requires valid authentication'() {
        when:
        connect('https://trusted.example', authorization).get(10, TimeUnit.SECONDS)

        then:
        thrown(ExecutionException)

        where:
        authorization << [null, 'Bearer invalid-token']
    }

    def 'subscriptions outside the permitted user project destination are rejected'() {
        given:
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        session.subscribe(destination, new StompSessionHandlerAdapter() {})

        then:
        errorFrame.get(10, TimeUnit.SECONDS)

        where:
        destination << ['/topic/registry', '/queue/TestProject1-skill-updates',
                        '/user/another-user/queue/TestProject1-skill-updates', '/user/queue/*-skill-updates',
                        '/user/queue/**', '/user/queue/TestProject1-skill-updates/extra',
                        '/user/queue/../TestProject1-skill-updates', '/user/queue/TestProject1-skill-updates?projectId=OtherProject']
    }

    def 'clients cannot publish to #destination'() {
        given:
        session = connect().get(10, TimeUnit.SECONDS)

        when:
        session.send(destination, 'forged update'.bytes)

        then:
        errorFrame.get(10, TimeUnit.SECONDS)

        where:
        destination << ['/topic/registry', '/queue/TestProject1-skill-updates', '/app/report', '/user/queue/TestProject1-skill-updates']
    }

    private void authenticateAs(String username) {
        if (isPkiMode) {
            KeyStore keys = KeyStore.getInstance('PKCS12')
            skillsService.certificateRegistry.getCertificate(username).inputStream.withCloseable {
                keys.load(it, 'skillspass'.toCharArray())
            }
            KeyStore trust = KeyStore.getInstance('JKS')
            new ClassPathResource('/certs/truststore.jks').inputStream.withCloseable {
                trust.load(it, 'skillspass'.toCharArray())
            }
            transport.setSslContext(SSLContexts.custom()
                    .loadKeyMaterial(keys, 'skillspass'.toCharArray())
                    .loadTrustMaterial(trust, null).build())
        } else {
            def tokenClient = createService(skillsService.userName)
            tokenClient.setProxyCredentials(projectId, skillsService.getClientSecret(projectId))
            token = tokenClient.wsHelper.getTokenForUser(username)
        }
    }

    private CompletableFuture<StompSession> connect(String origin = 'https://trusted.example', String authorization = isPkiMode ? null : "Bearer ${token}") {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders()
        headers.setOrigin(origin)
        StompHeaders connectHeaders = new StompHeaders()
        if (authorization) {
            connectHeaders.set('Authorization', authorization)
        }
        client.connectAsync("${isPkiMode ? 'wss' : 'ws'}://localhost:${localPort}/skills-websocket/websocket", headers, connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    Type getPayloadType(StompHeaders frameHeaders) { byte[] }

                    @Override
                    void handleFrame(StompHeaders frameHeaders, Object payload) {
                        errorFrame.complete(frameHeaders.getFirst('message') ?: new String((byte[]) payload))
                    }

                    @Override
                    void afterConnected(StompSession connectedSession, StompHeaders frameHeaders) {
                        session = connectedSession
                    }
                })
    }
}
