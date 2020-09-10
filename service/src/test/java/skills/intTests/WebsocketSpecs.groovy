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
package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.TestUtils
import skills.services.events.CompletionItem
import skills.services.events.SkillEventResult

import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Slf4j
class WebsocketSpecs extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()
    String projId = SkillsFactory.defaultProjId
    List<String> sampleUserIds // loaded from system props
    StompSession stompSession
    List<Map> subj1, subj2, subj3

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
        subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "s3${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, totalPoints: 200, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        skillsService.createSchema([subj1, subj2, subj3])
        if (!skillsService.isRoot()) {
            skillsService.grantRoot()
        }
    }

    def cleanup() {
        stompSession?.disconnect()
    }

    def "achieve subject's level - validate via websocket"(){
        given:
        List subjSummaryRes = []
        List<SkillEventResult> wsResults = []
        boolean skillsAdded = false
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults)

        when:
        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []

        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], sampleUserIds.get(0), dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(sampleUserIds.get(0), projId, subj1.get(it).subjectId)
        }
        skillsAdded = true
        messagesReceived.await(30, TimeUnit.SECONDS)
        then:
        interaction {
            if (skillsAdded) { // interaction closure seemed to be getting called before the "when:" block
                validateResults(subjSummaryRes, wsResults)
            }
        }
    }

    def "Non-notified badge achievements are notified when user connects to websocket" () {
        given:

        def badge = SkillsFactory.createBadge()
        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badge.badgeId, skillId: subj1.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badge.badgeId, skillId: subj2.get(0).skillId])

        (0..9).each {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], 'skills@skills.org', new Date()-it)
            skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], 'skills@skills.org', new Date()-it)
        }

        skillsService.updateBadge([projectId: projId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)

        List<SkillEventResult> wsResults = []

        when:
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults, false, false, 1, 'skills@skills.org')
        messagesReceived.await(30, TimeUnit.SECONDS)

        then:
        wsResults.find{it.skillId=='badge1'}.success
        wsResults.find{it.skillId=='badge1'}.completed
        wsResults.find{it.skillId=='badge1'}.completed.size() == 1
        wsResults.find{it.skillId=='badge1'}.completed[0].type == CompletionItem.CompletionItemType.Badge
        wsResults.find{it.skillId=='badge1'}.completed[0].name == badge.name
    }

    def "Non-notified global badge achievements are notified when user connects to websocket" () {
        given:

        def badge = SkillsFactory.createBadge()
        badge.enabled = false
        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge([projectId: projId, badgeId: badge.badgeId, skillId: subj1.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projId, badgeId: badge.badgeId, skillId: subj2.get(0).skillId])

        def badge2 = SkillsFactory.createBadge(1, 2)
        badge2.enabled = false
        skillsService.createGlobalBadge(badge2)
        skillsService.assignSkillToGlobalBadge([projectId: projId, badgeId: badge2.badgeId, skillId: subj1.get(1).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projId, badgeId: badge2.badgeId, skillId: subj2.get(1).skillId])

        (0..9).each {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], 'skills@skills.org', new Date()-it)
            skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], 'skills@skills.org', new Date()-it)
        }

        (0..9).each {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], 'skills2@skills.org', new Date()-it)
            skillsService.addSkill([projectId: projId, skillId: subj2.get(1).skillId], 'skills2@skills.org', new Date()-it)
        }

        skillsService.createGlobalBadge([projectId: projId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)
        skillsService.createGlobalBadge([projectId: projId, badgeId: badge2.badgeId, enabled: true, name: badge2.name], badge2.badgeId)

        List<SkillEventResult> wsResults = []
        List<SkillEventResult> wsResults2 = []

        when:
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults, false, false, 1, 'skills@skills.org')
        CountDownLatch messagesReceived2 = setupWebsocketConnection(wsResults2, false, false, 1, 'skills2@skills.org')
        messagesReceived.await(30, TimeUnit.SECONDS)
        messagesReceived2.await(30, TimeUnit.SECONDS)

        then:
        wsResults
        wsResults2
        wsResults.find{it.skillId=='badge1'}.success
        wsResults.find{it.skillId=='badge1'}.completed
        wsResults.find{it.skillId=='badge1'}.completed.size() == 1
        wsResults.find{it.skillId=='badge1'}.completed[0].type == CompletionItem.CompletionItemType.GlobalBadge
        wsResults.find{it.skillId=='badge1'}.completed[0].name == badge.name
        !wsResults.find{it.skillId=='badge2'}
        wsResults2.find{it.skillId=='badge2'}.success
        wsResults2.find{it.skillId=='badge2'}.completed
        wsResults2.find{it.skillId=='badge2'}.completed.size() == 1
        wsResults2.find{it.skillId=='badge2'}.completed[0].type == CompletionItem.CompletionItemType.GlobalBadge
        wsResults2.find{it.skillId=='badge2'}.completed[0].name == badge2.name
        !wsResults2.find{it.skillId=='badge1'}
    }

    def "non-notified skill achievements are notified when user connects to websocket" () {
        def subj = SkillsFactory.createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 4, 0, 150)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        skillsService.addSkill([projectId: projId, skillId: skill.skillId], 'skills@skills.org', new Date())


        skill.numPerformToCompletion = 1
        skillsService.updateSkill(skill, skill.skillId)

        List<SkillEventResult> wsResults = []

        when:
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults, false, false, 1, 'skills@skills.org')
        messagesReceived.await(30, TimeUnit.SECONDS)

        then:
        wsResults[0].success
        wsResults[0].completed
        wsResults[0].explanation == 'Achieved due to a modification in the training profile (such as: skill deleted, occurrences modified, badge published, etc..)'
        wsResults[0].completed.size() == 4
        wsResults[0].completed?.find{it.id=='skill1'}.type == CompletionItem.CompletionItemType.Skill
        wsResults[0].completed?.find{it.id=='skill1'}.name == skill.name
        wsResults[0].completed?.findAll { it.type == CompletionItem.CompletionItemType.Subject }.size() == 3
    }

    def "achieve subject's level - validate via xhr streaming"(){
        given:
        List subjSummaryRes = []
        List<SkillEventResult> wsResults = []
        boolean skillsAdded = false
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults, true)

        when:
        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], sampleUserIds.get(0), dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(sampleUserIds.get(0), projId, subj1.get(it).subjectId)
        }
        skillsAdded = true
        messagesReceived.await(30, TimeUnit.SECONDS)

        then:
        interaction {
            if (skillsAdded) { // interaction closure seemed to be getting called before the "when:" block
                validateResults(subjSummaryRes, wsResults)
            }
        }
    }

    def "achieve subject's level - validate via xhr polling"(){
        given:
        List subjSummaryRes = []
        List<SkillEventResult> wsResults = []
        boolean skillsAdded = false
        CountDownLatch messagesReceived = setupWebsocketConnection(wsResults, true, true)

        when:
        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], sampleUserIds.get(0), dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(sampleUserIds.get(0), projId, subj1.get(it).subjectId)
        }
        skillsAdded = true
        messagesReceived.await(30, TimeUnit.SECONDS)

        then:
        interaction {
            if (skillsAdded) { // interaction closure seemed to be getting called before the "when:" block
                validateResults(subjSummaryRes, wsResults)
            }
        }
    }



    private CountDownLatch setupWebsocketConnection(List<SkillEventResult> wsResults, boolean xhr=false, boolean xhrPolling=false, int count=5, String userId=null) {
        CountDownLatch messagesReceived = new CountDownLatch(count)
        List<Transport> transports = []
        if (xhr) {
            RestTemplateXhrTransport xhrTransport = new RestTemplateXhrTransport()
            xhrTransport.xhrStreamingDisabled = xhrPolling
            transports.add(xhrTransport)
        } else {
            transports.add(new WebSocketTransport(new StandardWebSocketClient()))
        }
        SockJsClient sockJsClient = new SockJsClient(transports)
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient)
        stompClient.setMessageConverter(new MappingJackson2MessageConverter())
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            Type getPayloadType(StompHeaders headers) {
                return SkillEventResult
            }

            @Override
            void handleFrame(StompHeaders headers, @Nullable Object payload) {
                SkillEventResult result = (SkillEventResult) payload
                wsResults.add(result)
                messagesReceived.countDown()
            }

            @Override
            void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/user/queue/${projId}-skill-updates", this)
            }
        }

        // setup websocket connection for sampleUserIds[0]
        if(userId == null) {
            userId = sampleUserIds.get(0)
        }
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        String token = skillsService.wsHelper.getTokenForUser(userId)
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders()
        headers.add('Authorization', "Bearer ${token}")
        String protocol = xhr ? 'http' : 'ws'
        stompSession = stompClient.connect("${protocol}://localhost:${localPort}/skills-websocket", headers, sessionHandler).get()
        return messagesReceived
    }

    private validateResults(List subjSummaryRes, List<SkillEventResult> wsResults) {
        wsResults.sort {it.skillId}
        wsResults.each {
            assert it.skillApplied
            assert it.explanation == "Skill event was applied"
        }
        !wsResults.get(0).completed
        wsResults.get(0).skillId == "${subj1.get(0).skillId}"
        wsResults.get(0).name == "${subj1.get(0).name}"
        wsResults.get(0).pointsEarned == 10
        subjSummaryRes.get(0).skillsLevel == 0
        subjSummaryRes.get(0).points == 10
        subjSummaryRes.get(0).todaysPoints == 0
        subjSummaryRes.get(0).levelPoints == 10

        !wsResults.get(1).completed
        wsResults.get(1).skillId == "${subj1.get(1).skillId}"
        wsResults.get(1).name == "${subj1.get(1).name}"
        wsResults.get(1).pointsEarned == 10
        subjSummaryRes.get(1).skillsLevel == 0
        subjSummaryRes.get(1).points == 20
        subjSummaryRes.get(1).todaysPoints == 0
        subjSummaryRes.get(1).levelPoints == 20

        !wsResults.get(2).completed
        wsResults.get(2).skillId == "${subj1.get(2).skillId}"
        wsResults.get(2).name == "${subj1.get(2).name}"
        wsResults.get(2).pointsEarned == 10
        subjSummaryRes.get(2).skillsLevel == 0
        subjSummaryRes.get(2).points == 30
        subjSummaryRes.get(2).todaysPoints == 0
        subjSummaryRes.get(2).levelPoints == 30

        !wsResults.get(3).completed
        wsResults.get(3).skillId == "${subj1.get(3).skillId}"
        wsResults.get(3).name == "${subj1.get(3).name}"
        wsResults.get(3).pointsEarned == 10
        subjSummaryRes.get(3).skillsLevel == 0
        subjSummaryRes.get(3).points == 40
        subjSummaryRes.get(3).todaysPoints == 0
        subjSummaryRes.get(3).levelPoints == 40

        wsResults.get(4).completed.size() == 1
        wsResults.get(4).skillId == "${subj1.get(4).skillId}"
        wsResults.get(4).name == "${subj1.get(4).name}"
        wsResults.get(4).pointsEarned == 10
        wsResults.get(4).completed.get(0).type == CompletionItem.CompletionItemType.Subject
        wsResults.get(4).completed.get(0).level == 1
        wsResults.get(4).completed.get(0).id == "subj1"

        subjSummaryRes.get(4).skillsLevel == 1
        subjSummaryRes.get(4).points == 50
        subjSummaryRes.get(4).todaysPoints == 10
        subjSummaryRes.get(4).levelPoints == 0
    }
}
