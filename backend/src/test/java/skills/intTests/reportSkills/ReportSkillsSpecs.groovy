package skills.intTests.reportSkills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.TestUtils
import skills.services.events.CompletionItem
import skills.services.events.SkillEventResult
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

import java.lang.reflect.Type

@Slf4j
class ReportSkillsSpecs extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "complete very simple skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 3
        res.body.completed.find({ it.type == "Skill" }).id == skills[0].skillId
        res.body.completed.find({ it.type == "Skill" }).name == skills[0].name

        res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
            res.body.completed.find({ it.type == "Overall" }).level == 1

        res.body.completed.find({ it.type == "Subject" }).id == subj.subjectId
        res.body.completed.find({ it.type == "Subject" }).name == subj.name
        res.body.completed.find({ it.type == "Subject" }).level == 1
    }

    def "attempt to report skill event for skill definition does not exist"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.addSkill([projectId: projId, skillId: "nope"])
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("Failed to report skill event because skill definition does not exist., errorCode:InternalError, success:false, projectId:TestProject1, skillId:nope")
    }

    def "incrementally achieve a single skill"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            log.info("Adding ${subj1.get(1).skillId} on ${dates.get(it)}")
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.points == 10
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50
        subjSummaryRes.get(0).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0

        !addSkillRes.get(0).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.points == 20
        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(1).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(1).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.points == 30
        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(2).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(2).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.points == 40
        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 0
        subjSummaryRes.get(3).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        !addSkillRes.get(3).body.completed.find({ it.type == "Skill" })

        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.points == 50
        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.todaysPoints == 10
        subjSummaryRes.get(4).skills.find { it.skillId == subj1.get(1).skillId }.totalPoints == 50

        addSkillRes.get(4).body.completed.find({ it.type == "Skill" }).id == subj1.get(1).skillId
    }

    def "achieve subject's level through a single skill"(){
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(1).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        assert !addSkillRes.get(0).body.completed
        assert subjSummaryRes.get(0).skillsLevel == 0
        assert subjSummaryRes.get(0).points == 10
        assert subjSummaryRes.get(0).todaysPoints == 0
        assert subjSummaryRes.get(0).levelPoints == 10

        assert !addSkillRes.get(1).body.completed
        assert subjSummaryRes.get(1).skillsLevel == 0
        assert subjSummaryRes.get(1).points == 20
        assert subjSummaryRes.get(1).todaysPoints == 0
        assert subjSummaryRes.get(1).levelPoints == 20

        assert !addSkillRes.get(2).body.completed
        assert subjSummaryRes.get(2).skillsLevel == 0
        assert subjSummaryRes.get(2).points == 30
        assert subjSummaryRes.get(2).todaysPoints == 0
        assert subjSummaryRes.get(2).levelPoints == 30

        assert !addSkillRes.get(3).body.completed
        assert subjSummaryRes.get(3).skillsLevel == 0
        assert subjSummaryRes.get(3).points == 40
        assert subjSummaryRes.get(3).todaysPoints == 0
        assert subjSummaryRes.get(3).levelPoints == 40

        assert addSkillRes.get(4).body.completed.size() == 1
        assert addSkillRes.get(4).body.completed.get(0).type == "Subject"
        assert addSkillRes.get(4).body.completed.get(0).level == 1
        assert addSkillRes.get(4).body.completed.get(0).id == "subj1"

        assert subjSummaryRes.get(4).skillsLevel == 1
        assert subjSummaryRes.get(4).points == 50
        assert subjSummaryRes.get(4).todaysPoints == 10
        assert subjSummaryRes.get(4).levelPoints == 0
    }

    def "achieve subject's level by progressing through several skill"(){
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "s3${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, totalPoints: 200, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)
        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(it).subjectId)
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        !addSkillRes.get(0).body.completed
        addSkillRes.get(0).body.skillId == "${subj1.get(0).skillId}"
        addSkillRes.get(0).body.name == "${subj1.get(0).name}"
        addSkillRes.get(0).body.pointsEarned == 10
        subjSummaryRes.get(0).skillsLevel == 0
        subjSummaryRes.get(0).points == 10
        subjSummaryRes.get(0).todaysPoints == 0
        subjSummaryRes.get(0).levelPoints == 10

        !addSkillRes.get(1).body.completed
        addSkillRes.get(1).body.skillId == "${subj1.get(1).skillId}"
        addSkillRes.get(1).body.name == "${subj1.get(1).name}"
        addSkillRes.get(1).body.pointsEarned == 10
        subjSummaryRes.get(1).skillsLevel == 0
        subjSummaryRes.get(1).points == 20
        subjSummaryRes.get(1).todaysPoints == 0
        subjSummaryRes.get(1).levelPoints == 20

        !addSkillRes.get(2).body.completed
        addSkillRes.get(2).body.skillId == "${subj1.get(2).skillId}"
        addSkillRes.get(2).body.name == "${subj1.get(2).name}"
        addSkillRes.get(2).body.pointsEarned == 10
        subjSummaryRes.get(2).skillsLevel == 0
        subjSummaryRes.get(2).points == 30
        subjSummaryRes.get(2).todaysPoints == 0
        subjSummaryRes.get(2).levelPoints == 30

        !addSkillRes.get(3).body.completed
        addSkillRes.get(3).body.skillId == "${subj1.get(3).skillId}"
        addSkillRes.get(3).body.name == "${subj1.get(3).name}"
        addSkillRes.get(3).body.pointsEarned == 10
        subjSummaryRes.get(3).skillsLevel == 0
        subjSummaryRes.get(3).points == 40
        subjSummaryRes.get(3).todaysPoints == 0
        subjSummaryRes.get(3).levelPoints == 40

        addSkillRes.get(4).body.completed.size() == 1
        addSkillRes.get(4).body.skillId == "${subj1.get(4).skillId}"
        addSkillRes.get(4).body.name == "${subj1.get(4).name}"
        addSkillRes.get(4).body.pointsEarned == 10
        addSkillRes.get(4).body.completed.get(0).type == "Subject"
        addSkillRes.get(4).body.completed.get(0).level == 1
        addSkillRes.get(4).body.completed.get(0).id == "subj1"

        subjSummaryRes.get(4).skillsLevel == 1
        subjSummaryRes.get(4).points == 50
        subjSummaryRes.get(4).todaysPoints == 10
        subjSummaryRes.get(4).levelPoints == 0
    }

    def "achieve subject's level by progressing through several skill results via websocket connection"(){
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "s3${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, totalPoints: 200, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        List<SkillEventResult> wsResults = []
        WebSocketClient client = new StandardWebSocketClient()
        List<Transport> transports = []
        transports.add(new WebSocketTransport(client))
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
            }

            @Override
            void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe('/user/queue/skill-updates', this)
            }
        }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        // setup websocket connection for sampleUserIds[0]
        String userId = sampleUserIds.get(0)
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        String token = skillsService.wsHelper.getTokenForUser(userId)
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders()
        headers.add('Authorization', "Bearer ${token}")
        stompClient.connect("ws://localhost:${localPort}/skills-websocket", headers, sessionHandler)
        Thread.sleep(2000)

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        List subjSummaryRes = []

        (0..4).each {
            addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(it).skillId], userId, dates.get(it))
            subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(it).subjectId)
        }
        Thread.sleep(2000)

        then:
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

    def "fully achieve a subject"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 15, numPerformToCompletion: 4, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createSchema([subj1, subj2, subj3])

        skillsService.getLevels(projId, "subj1").each{
            log.info("${it.skillId} :: ${it.level} :: ${it.pointsFrom}")
        }
        skillsService.getLevels(projId, "subj2").each{
            log.info("${it.skillId} :: ${it.level} :: ${it.pointsFrom}")
        }
        skillsService.getLevels(projId, "subj3").each{
            log.info("${it.skillId} :: ${it.level} :: ${it.pointsFrom}")
        }

        List<Date> dates = testUtils.getLastNDays(4)
        List addSkillRes = []
        List subjSummaryRes = []
        String userId = sampleUserIds.get(0)

        //achieve level 1
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, dates.get(0))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //15 points
        //achieve level 2
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(0))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //30 points
        //achieve level 3
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, dates.get(1))
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(1))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //60 points
        //achieve level 4
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, dates.get(2))
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(2))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //90 points
        //achieve level 5
        //achieve level 4
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], userId, dates.get(3))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //90 points
        addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(3))
        subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId) //120 points

        //for each skill in subj1
        /*(0..1).each { int skillIndex ->
            //add 3 events for 3 dates, recording results after each date
            (0..2).each {
                addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(skillIndex).skillId], userId, dates.get(it))
                subjSummaryRes << skillsService.getSkillSummary(userId, projId, subj1.get(0).subjectId)
            }
        }*/

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }
        subjSummaryRes.get(0).skillsLevel == 1
        subjSummaryRes.get(0).points == 15
        subjSummaryRes.get(0).todaysPoints == 0
        subjSummaryRes.get(0).levelPoints == 3
        addSkillRes.get(0).body.completed.find { it.type == "Subject" }.level == 1

        subjSummaryRes.get(1).skillsLevel == 2
        subjSummaryRes.get(1).points == 30
        subjSummaryRes.get(1).todaysPoints == 0
        subjSummaryRes.get(1).levelPoints == 0
        addSkillRes.get(1).body.completed.find { it.type == "Subject" }.level == 2

        subjSummaryRes.get(2).skillsLevel == 3
        subjSummaryRes.get(2).points == 60
        subjSummaryRes.get(2).todaysPoints == 0
        subjSummaryRes.get(2).levelPoints == 6
        addSkillRes.get(3).body.completed.find { it.type == "Subject" }.level == 3

        subjSummaryRes.get(3).skillsLevel == 4
        subjSummaryRes.get(3).points == 90
        subjSummaryRes.get(3).todaysPoints == 0
        subjSummaryRes.get(3).levelPoints == 10
        addSkillRes.get(5).body.completed.find { it.type == "Subject" }.level == 4

        subjSummaryRes.get(4).skillsLevel == 4
        subjSummaryRes.get(4).points == 105
        subjSummaryRes.get(4).todaysPoints == 15
        subjSummaryRes.get(4).levelPoints == 25
        subjSummaryRes.get(4).levelTotalPoints == 30
        !addSkillRes.get(6).body.completed.find { it.type == "Subject" }

        subjSummaryRes.get(5).skillsLevel == 5
        subjSummaryRes.get(5).points == 120
        subjSummaryRes.get(5).todaysPoints == 30
        subjSummaryRes.get(5).levelPoints == 10
        subjSummaryRes.get(5).levelTotalPoints == -1
        addSkillRes.get(7).body.completed.find { it.type == "Subject" }.level == 5
    }

    def "fully achieve overall"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 25, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..2).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 50, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        //subj1.skill1 = 50
        //subj1.skill2 = 50

        //subj2 = 100/4

        //subj3 = 100/2

        //30, 20, 20 -- used to be 30, 20, 6
        //60, 80, 12 -- 60, 80, 40

        List<List<Map>> subjects = [subj1, subj2, subj3]
        when:
        skillsService.createSchema(subjects)

        List<Date> dates = testUtils.getLastNDays(10)
        List addSkillRes = []
        List summaries = []
        String userId = sampleUserIds.get(0)

        subjects.each { List<Map> subject ->
            subject.each { Map skill->
                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId, dates.get(it))
                    summaries << skillsService.getSkillSummary(userId, projId)
                }
            }
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        summaries.get(0).skillsLevel == 0
        summaries.get(0).points == 25
        summaries.get(0).todaysPoints == 0
        summaries.get(0).levelPoints == 25
        summaries.get(0).levelTotalPoints == 40
        summaries.get(0).totalPoints == 400
        !addSkillRes.get(0).body.completed.find { it.type == "Overall" }

        summaries.get(1).skillsLevel == 1
        summaries.get(1).points == 50
        summaries.get(1).todaysPoints == 0
        summaries.get(1).levelPoints == 50 - 40 //points minus current level pointsFrom
        addSkillRes.get(1).body.completed.find { it.type == "Overall" }.level == 1

        summaries.get(2).skillsLevel == 1
        summaries.get(2).points == 75
        summaries.get(2).todaysPoints == 0
        summaries.get(2).levelPoints == 75 - 40
        !addSkillRes.get(2).body.completed.find { it.type == "Overall" }

        summaries.get(3).skillsLevel == 2
        summaries.get(3).points == 100
        summaries.get(3).todaysPoints == 0
        summaries.get(3).levelPoints == 100 - 100
        addSkillRes.get(3).body.completed.find { it.type == "Overall" }?.level == 2

        summaries.get(4).skillsLevel == 2
        summaries.get(4).points == 105
        summaries.get(4).todaysPoints == 0
        summaries.get(4).levelPoints == 105 - 100
        !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        summaries.get(5).skillsLevel == 2
        summaries.get(5).points == 110
        summaries.get(5).todaysPoints == 0
        summaries.get(5).levelPoints == 110 - 100
        !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        summaries.get(6).skillsLevel == 2
        summaries.get(6).points == 115
        summaries.get(6).todaysPoints == 0
        summaries.get(6).levelPoints == 115 - 100
        !addSkillRes.get(6).body.completed.find { it.type == "Overall" }

        summaries.get(7).skillsLevel == 2
        summaries.get(7).points == 120
        summaries.get(7).todaysPoints == 0
        summaries.get(7).levelPoints == 120 - 100
        !addSkillRes.get(7).body.completed.find { it.type == "Overall" }

        summaries.get(8).skillsLevel == 2
        summaries.get(8).points == 125
        summaries.get(8).todaysPoints == 0
        summaries.get(8).levelPoints == 125 - 100
        !addSkillRes.get(8).body.completed.find { it.type == "Overall" }

        summaries.get(9).points == 130
        summaries.get(9).skillsLevel == 2
        summaries.get(9).todaysPoints == 0
        summaries.get(9).levelPoints == 130 - 100
        !addSkillRes.get(9).body.completed.find { it.type == "Overall" }

        summaries.get(10).points == 135
        summaries.get(10).skillsLevel == 2
        summaries.get(10).todaysPoints == 0
        summaries.get(10).levelPoints == 135 - 100
        !addSkillRes.get(9).body.completed.find { it.type == "Overall" }
        //addSkillRes.get(10).body.completed.find { it.type == "Overall" }.level == 3

        summaries.get(11).points == 140
        summaries.get(11).skillsLevel == 2
        summaries.get(11).todaysPoints == 0
        summaries.get(11).levelPoints == 140 - 100
        !addSkillRes.get(11).body.completed.find { it.type == "Overall" }

        summaries.get(12).points == 145
        summaries.get(12).skillsLevel == 2
        summaries.get(12).todaysPoints == 0
        summaries.get(12).levelPoints == 145 - 100
        !addSkillRes.get(12).body.completed.find { it.type == "Overall" }

        summaries.get(13).points == 150
        summaries.get(13).skillsLevel == 2
        summaries.get(13).todaysPoints == 0
        summaries.get(13).levelPoints == 150 - 100
        !addSkillRes.get(13).body.completed.find { it.type == "Overall" }

        summaries.get(14).points == 155
        summaries.get(14).skillsLevel == 2
        summaries.get(14).todaysPoints == 0
        summaries.get(14).levelPoints == 155 - 100
        !addSkillRes.get(14).body.completed.find { it.type == "Overall" }

        summaries.get(15).points == 160
        summaries.get(15).skillsLevel == 2
        summaries.get(15).todaysPoints == 0
        summaries.get(15).levelPoints == 160 - 100
        !addSkillRes.get(15).body.completed.find { it.type == "Overall" }

        summaries.get(16).points == 165
        summaries.get(16).skillsLevel == 2
        summaries.get(16).todaysPoints == 0
        summaries.get(16).levelPoints == 165 - 100
        !addSkillRes.get(16).body.completed.find { it.type == "Overall" }

        summaries.get(17).points == 170
        summaries.get(17).skillsLevel == 2
        summaries.get(17).todaysPoints == 0
        summaries.get(17).levelPoints == 170 - 100
        !addSkillRes.get(16).body.completed.find { it.type == "Overall" }
//        addSkillRes.get(17).body.completed.find { it.type == "Overall" }.level == 4

        summaries.get(18).points == 175
        summaries.get(18).skillsLevel == 2
        summaries.get(18).todaysPoints == 0
        summaries.get(18).levelPoints == 175 - 100
        !addSkillRes.get(18).body.completed.find { it.type == "Overall" }

        summaries.get(19).points == 180
        summaries.get(19).skillsLevel == 3
        summaries.get(19).todaysPoints == 0
        summaries.get(19).levelPoints == 0
        addSkillRes.get(19).body.completed.find { it.type == "Overall" }.level == 3

        summaries.get(20).points == 185
        summaries.get(20).skillsLevel == 3
        summaries.get(20).todaysPoints == 0
        summaries.get(20).levelPoints == 185 - 180
        !addSkillRes.get(20).body.completed.find { it.type == "Overall" }

        summaries.get(21).points == 190
        summaries.get(21).skillsLevel == 3
        summaries.get(21).todaysPoints == 0
        summaries.get(21).levelPoints == 190 - 180
        !addSkillRes.get(21).body.completed.find { it.type == "Overall" }

        summaries.get(22).points == 195
        summaries.get(22).skillsLevel == 3
        summaries.get(22).todaysPoints == 0
        summaries.get(22).levelPoints == 195 - 180
        !addSkillRes.get(22).body.completed.find { it.type == "Overall" }

        summaries.get(23).points == 200
        summaries.get(23).skillsLevel == 3
        summaries.get(23).todaysPoints == 0
        summaries.get(23).levelPoints == 200 - 180
        !addSkillRes.get(23).body.completed.find { it.type == "Overall" }

        summaries.get(24).points == 250
        summaries.get(24).skillsLevel == 3
        summaries.get(24).todaysPoints == 0
        summaries.get(24).levelPoints == 250 - 180
        !addSkillRes.get(24).body.completed.find { it.type == "Overall" }

        summaries.get(25).points == 300
        summaries.get(25).skillsLevel == 4
        summaries.get(25).todaysPoints == 0
        summaries.get(25).levelPoints == 300 - 268
        addSkillRes.get(25).body.completed.find { it.type == "Overall" }.level == 4

        summaries.get(26).points == 350
        summaries.get(26).skillsLevel == 4
        summaries.get(26).todaysPoints == 0
        summaries.get(26).levelPoints == 350 - 268
        !addSkillRes.get(26).body.completed.find { it.type == "Overall" }

        summaries.get(27).points == 400
        summaries.get(27).skillsLevel == 5
        summaries.get(27).todaysPoints == 0
        summaries.get(27).levelPoints == 400 - 368
        addSkillRes.get(27).body.completed.find { it.type == "Overall" }.level == 5
        summaries.get(27).levelTotalPoints == -1
    }

    def "two users achieving fully should not step on each other"(){
        List<Map> subj1 = (1..2).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 25, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 5, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..2).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 50, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        List<List<Map>> subjects = [subj1, subj2, subj3]
        when:
        skillsService.createSchema(subjects)

        List<Date> dates = testUtils.getLastNDays(10)
        List addSkillRes = []
        List addSkillRes2 = []
        List summaries = []
        List summaries2 = []
        String userId = "user1"
        String userId2 = "user2"

        subjects.each { List<Map> subject ->
            subject.each { Map skill->
                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId, dates.get(it))
                    summaries << skillsService.getSkillSummary(userId, projId)
                }

                (0..(skill.numPerformToCompletion-1)).each {
                    addSkillRes2 << skillsService.addSkill([projectId: projId, skillId: skill.skillId], userId2, dates.get(it))
                    summaries2 << skillsService.getSkillSummary(userId2, projId)
                }
            }
        }

        then:
        addSkillRes.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        addSkillRes2.each {
            assert it.body.skillApplied
            assert it.body.explanation == "Skill event was applied"
        }

        assertResults(summaries, addSkillRes)

        // --------------------------------------
        // 2nd user
        // --------------------------------------
        assertResults(summaries2, addSkillRes2)
    }

    private boolean assertResults(List summaries, List addSkillRes){
        assert summaries.get(0).skillsLevel == 0
        assert summaries.get(0).points == 25
        assert summaries.get(0).todaysPoints == 0
        assert summaries.get(0).levelPoints == 25
        assert summaries.get(0).levelTotalPoints == 40
        assert summaries.get(0).totalPoints == 400
        assert !addSkillRes.get(0).body.completed.find { it.type == "Overall" }

        assert summaries.get(1).skillsLevel == 1
        assert summaries.get(1).points == 50
        assert summaries.get(1).todaysPoints == 0
        assert summaries.get(1).levelPoints == 50 - 40 //points minus current level pointsFrom
        assert addSkillRes.get(1).body.completed.find { it.type == "Overall" }.level == 1

        assert summaries.get(2).skillsLevel == 1
        assert summaries.get(2).points == 75
        assert summaries.get(2).todaysPoints == 0
        assert summaries.get(2).levelPoints == 75 - 40
        assert !addSkillRes.get(2).body.completed.find { it.type == "Overall" }

        assert summaries.get(3).skillsLevel == 2
        assert summaries.get(3).points == 100
        assert summaries.get(3).todaysPoints == 0
        assert summaries.get(3).levelPoints == 100 - 100
        assert addSkillRes.get(3).body.completed.find { it.type == "Overall" }?.level == 2

        assert summaries.get(4).skillsLevel == 2
        assert summaries.get(4).points == 105
        assert summaries.get(4).todaysPoints == 0
        assert summaries.get(4).levelPoints == 105 - 100
        assert !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        assert summaries.get(5).skillsLevel == 2
        assert summaries.get(5).points == 110
        assert summaries.get(5).todaysPoints == 0
        assert summaries.get(5).levelPoints == 110 - 100
        assert !addSkillRes.get(5).body.completed.find { it.type == "Overall" }

        assert summaries.get(6).skillsLevel == 2
        assert summaries.get(6).points == 115
        assert summaries.get(6).todaysPoints == 0
        assert summaries.get(6).levelPoints == 115 - 100
        assert !addSkillRes.get(6).body.completed.find { it.type == "Overall" }

        assert summaries.get(7).skillsLevel == 2
        assert summaries.get(7).points == 120
        assert summaries.get(7).todaysPoints == 0
        assert summaries.get(7).levelPoints == 120 - 100
        assert !addSkillRes.get(7).body.completed.find { it.type == "Overall" }

        assert summaries.get(8).skillsLevel == 2
        assert summaries.get(8).points == 125
        assert summaries.get(8).todaysPoints == 0
        assert summaries.get(8).levelPoints == 125 - 100
        assert !addSkillRes.get(8).body.completed.find { it.type == "Overall" }

        assert summaries.get(9).points == 130
        assert summaries.get(9).skillsLevel == 2
        assert summaries.get(9).todaysPoints == 0
        assert summaries.get(9).levelPoints == 130 - 100
        assert !addSkillRes.get(9).body.completed.find { it.type == "Overall" }

        assert summaries.get(10).points == 135
        assert summaries.get(10).skillsLevel == 2
        assert summaries.get(10).todaysPoints == 0
        assert summaries.get(10).levelPoints == 135 - 100
        assert !addSkillRes.get(9).body.completed.find { it.type == "Overall" }

        assert summaries.get(11).points == 140
        assert summaries.get(11).skillsLevel == 2
        assert summaries.get(11).todaysPoints == 0
        assert summaries.get(11).levelPoints == 140 - 100
        assert !addSkillRes.get(11).body.completed.find { it.type == "Overall" }

        assert summaries.get(12).points == 145
        assert summaries.get(12).skillsLevel == 2
        assert summaries.get(12).todaysPoints == 0
        assert summaries.get(12).levelPoints == 145 - 100
        assert !addSkillRes.get(12).body.completed.find { it.type == "Overall" }

        assert summaries.get(13).points == 150
        assert summaries.get(13).skillsLevel == 2
        assert summaries.get(13).todaysPoints == 0
        assert summaries.get(13).levelPoints == 150 - 100
        assert !addSkillRes.get(13).body.completed.find { it.type == "Overall" }

        assert summaries.get(14).points == 155
        assert summaries.get(14).skillsLevel == 2
        assert summaries.get(14).todaysPoints == 0
        assert summaries.get(14).levelPoints == 155 - 100
        assert !addSkillRes.get(14).body.completed.find { it.type == "Overall" }

        assert summaries.get(15).points == 160
        assert summaries.get(15).skillsLevel == 2
        assert summaries.get(15).todaysPoints == 0
        assert summaries.get(15).levelPoints == 160 - 100
        assert !addSkillRes.get(15).body.completed.find { it.type == "Overall" }

        assert summaries.get(16).points == 165
        assert summaries.get(16).skillsLevel == 2
        assert summaries.get(16).todaysPoints == 0
        assert summaries.get(16).levelPoints == 165 - 100
        assert !addSkillRes.get(16).body.completed.find { it.type == "Overall" }

        assert summaries.get(17).points == 170
        assert summaries.get(17).skillsLevel == 2
        assert summaries.get(17).todaysPoints == 0
        assert summaries.get(17).levelPoints == 170 - 100
        assert !addSkillRes.get(16).body.completed.find { it.type == "Overall" }

        assert summaries.get(18).points == 175
        assert summaries.get(18).skillsLevel == 2
        assert summaries.get(18).todaysPoints == 0
        assert summaries.get(18).levelPoints == 175 - 100
        assert !addSkillRes.get(18).body.completed.find { it.type == "Overall" }

        assert summaries.get(19).points == 180
        assert summaries.get(19).skillsLevel == 3
        assert summaries.get(19).todaysPoints == 0
        assert summaries.get(19).levelPoints == 0
        assert addSkillRes.get(19).body.completed.find { it.type == "Overall" }.level == 3

        assert summaries.get(20).points == 185
        assert summaries.get(20).skillsLevel == 3
        assert summaries.get(20).todaysPoints == 0
        assert summaries.get(20).levelPoints == 185 - 180
        assert !addSkillRes.get(20).body.completed.find { it.type == "Overall" }

        assert summaries.get(21).points == 190
        assert summaries.get(21).skillsLevel == 3
        assert summaries.get(21).todaysPoints == 0
        assert summaries.get(21).levelPoints == 190 - 180
        assert !addSkillRes.get(21).body.completed.find { it.type == "Overall" }

        assert summaries.get(22).points == 195
        assert summaries.get(22).skillsLevel == 3
        assert summaries.get(22).todaysPoints == 0
        assert summaries.get(22).levelPoints == 195 - 180
        assert !addSkillRes.get(22).body.completed.find { it.type == "Overall" }

        assert summaries.get(23).points == 200
        assert summaries.get(23).skillsLevel == 3
        assert summaries.get(23).todaysPoints == 0
        assert summaries.get(23).levelPoints == 200 - 180
        assert !addSkillRes.get(23).body.completed.find { it.type == "Overall" }

        assert summaries.get(24).points == 250
        assert summaries.get(24).skillsLevel == 3
        assert summaries.get(24).todaysPoints == 0
        assert summaries.get(24).levelPoints == 250 - 180
        assert !addSkillRes.get(24).body.completed.find { it.type == "Overall" }

        assert summaries.get(25).points == 300
        assert summaries.get(25).skillsLevel == 4
        assert summaries.get(25).todaysPoints == 0
        assert summaries.get(25).levelPoints == 300 - 268
        assert addSkillRes.get(25).body.completed.find { it.type == "Overall" }.level == 4

        assert summaries.get(26).points == 350
        assert summaries.get(26).skillsLevel == 4
        assert summaries.get(26).todaysPoints == 0
        assert summaries.get(26).levelPoints == 350 - 268
        assert !addSkillRes.get(26).body.completed.find { it.type == "Overall" }

        assert summaries.get(27).points == 400
        assert summaries.get(27).skillsLevel == 5
        assert summaries.get(27).todaysPoints == 0
        assert summaries.get(27).levelPoints == 400 - 368
        assert addSkillRes.get(27).body.completed.find { it.type == "Overall" }.level == 5
        assert summaries.get(27).levelTotalPoints == -1
        return true
    }



    def "if skill is already completed then simply inform the caller"() {
        String subj = "testSubj"
        String skillId = "skillId"

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId     : projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill",
                                   pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
        ])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId])
        def res1 = skillsService.addSkill([projectId: projId, skillId: skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
        res.body.completed.size() > 0

        !res1.body.skillApplied
        res1.body.explanation == "This skill reached its maximum points"
        !res1.body.completed
    }


    def "skills from different projects with the same subject id do not intermingle"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skills1 = SkillsFactory.createSkills(10, )

        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2)
        def skills2 = SkillsFactory.createSkills(10, 2)

        setup:
        skillsService.deleteProjectIfExist(proj2.projectId)

        when:
        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)
        def res = skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[0].skillId])

        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills2)

        def res2 = skillsService.addSkill([projectId: proj2.projectId, skillId: skills2[0].skillId])

        def skillsResult1 = skillsService.getSkillsForSubject(proj1.projectId, subj1.subjectId)
        def skillsResult2 = skillsService.getSkillsForSubject(proj2.projectId, subj2.subjectId)

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 3
        res.body.completed.find({ it.type == "Skill" }).id == skills1[0].skillId
        res.body.completed.find({ it.type == "Skill" }).name == skills1[0].name

        res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).level == 1

        res.body.completed.find({ it.type == "Subject" }).id == subj1.subjectId
        res.body.completed.find({ it.type == "Subject" }).name == subj1.name
        res.body.completed.find({ it.type == "Subject" }).level == 1

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        skillsResult1.size() == 10
        skillsResult1.find { it.projectId == proj1.projectId && it.skillId == skills1[0].skillId }

        skillsResult2.size() == 10
        skillsResult2.find { it.projectId == proj2.projectId && it.skillId == skills2[0].skillId }
    }

    def "deleting skill events should decrease project level"(){
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skills1 = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        when:

        def skillsLevelAfter3
        Date date = new Date()
        for(int i=0; i<5; i++){
            if(i==3) {
                def summary = skillsService.getSkillSummary("aUser", proj1.projectId, subj1.subjectId)
                skillsLevelAfter3 = summary.skillsLevel
            }
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[i].skillId], "aUser", date)
        }

        def skillsLevelAfter5 = skillsService.getSkillSummary("aUser", proj1.projectId, subj1.subjectId).skillsLevel

        for(int i=3; i<5; i++) {
            skillsService.deleteSkillEvent([projectId: proj1.projectId, skillId: skills1[i].skillId, userId: 'aUser', timestamp: date.time])
        }

        def skillsLevelAfterDelete = skillsService.getSkillSummary("aUser", proj1.projectId, subj1.subjectId).skillsLevel

        then:
        skillsLevelAfter3 != skillsLevelAfter5
        skillsLevelAfterDelete == skillsLevelAfter3
    }

    @Autowired
    UserAchievedLevelRepo userAchievementsRepo

    def "deleting all skill events should decrease project level to zero"(){
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skills1 = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        String userId = "aUser"

        when:

        Date date = new Date()
        for(int i=0; i<10; i++){
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[i].skillId], userId, date)
        }

        def skillsLevelAfterInsert = skillsService.getSkillSummary(userId, proj1.projectId, subj1.subjectId).skillsLevel

        for(int i=0; i<10; i++) {
            skillsService.deleteSkillEvent([projectId: proj1.projectId, skillId: skills1[i].skillId, userId: userId, timestamp: date.time])
        }

        def skillsLevelAfterDelete = skillsService.getSkillSummary(userId, proj1.projectId, subj1.subjectId).skillsLevel

        then:
        skillsLevelAfterInsert > 0
        skillsLevelAfterDelete == 0

        List<UserAchievement> userAchievements = userAchievementsRepo.findAll().findAll({it.userId == userId && it.projectId == proj1.projectId})
        !userAchievements
    }

    def "deleting all skill events should decrease project level to zero - one skill achieves all levels"(){
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skills1 = SkillsFactory.createSkills(1, )
        skills1[0].pointIncrement = 200

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        String userId = "aUser"

        when:

        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[0].skillId], userId, date)
        def skillsLevelAfterInsert = skillsService.getSkillSummary(userId, proj1.projectId, subj1.subjectId).skillsLevel
        skillsService.deleteSkillEvent([projectId: proj1.projectId, skillId: skills1[0].skillId, userId: userId, timestamp: date.time])
        def skillsLevelAfterDelete = skillsService.getSkillSummary(userId, proj1.projectId, subj1.subjectId).skillsLevel

        then:
        skillsLevelAfterInsert > 0
        skillsLevelAfterDelete == 0

        List<UserAchievement> userAchievements = userAchievementsRepo.findAll().findAll({it.userId == userId.toLowerCase() && it.projectId == proj1.projectId})
        !userAchievements
    }

    def "Project level should not change if deleting skill event over threshold"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skills1 = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        when:

        Date date = new Date()
        for(int i=0; i<6; i++){
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[i].skillId], "aUser", date)
        }

        def skillsLevelAfterInsert = skillsService.getSkillSummary("aUser", proj1.projectId, subj1.subjectId).skillsLevel

        skillsService.deleteSkillEvent([projectId: proj1.projectId, skillId: skills1[0].skillId, userId: 'aUser', timestamp: date.time])

        def skillsLevelAfterDelete = skillsService.getSkillSummary("aUser", proj1.projectId, subj1.subjectId).skillsLevel

        then:
        skillsLevelAfterInsert > 0
        skillsLevelAfterDelete == skillsLevelAfterInsert
    }

    def "Skill Events may not be reported for future times"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "usera", new Date().plus(1))

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Skill Events may not be in the future")
    }

    def "Skill Events - user ids cannot have spaces"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "user a", new Date())

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Spaces are not allowed in user id. Provided [user a]")
    }

}
