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
package skills.intTests.utils

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import skills.SpringBootApp
import skills.services.LevelDefinitionStorageService
import skills.services.LockingService
import skills.storage.model.UserAttrs
import skills.storage.repos.*
import skills.utils.RetryUtil
import spock.lang.Specification

@Slf4j
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class DefaultIntSpec extends Specification {

    public static String DEFAULT_ROOT_USER_ID = "rootUser"

    static {
        // must call in the main method and not in @PostConstruct method as H2 jdbc driver will cache timezone prior @PostConstruct method is called
        // alternatively we could pass in -Duser.timezone=UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    };

    SkillsService skillsService

    GreenMail greenMail

    @LocalServerPort
    int localPort

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SettingRepo settingRepo

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    PlatformTransactionManager transactionManager

    @Autowired(required=false)
    MockUserInfoService mockUserInfoService

    @Autowired(required=false)
    CertificateRegistry certificateRegistry

    @Autowired
    WaitForAsyncTasksCompletion waitForAsyncTasksCompletion

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserEventsRepo userEventsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    ClientPrefRepo clientPrefRepo

    @Autowired
    LockingService lockingService

    @Autowired
    QuizDefRepo quizDefRepo

    @PersistenceContext
    EntityManager entityManager

    @Autowired
    TransactionHelper transactionHelper

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    private UserUtil userUtil

    @PostConstruct
    def init(){
        userUtil = new UserUtil(certificateRegistry: certificateRegistry)
    }

    def setup() {
        // allows for over-ridding the setup method
        doSetup()
    }

    def doSetup() {
        String msg = "\n-------------------------------------------------------------\n" +
                "START: [${specificationContext.currentIteration.name}]\n" +
                "-------------------------------------------------------------"
        log.info(msg)
        /**
         * deleting projects and users will wipe the entire db clean due to cascading
         */
        projDefRepo.deleteAll()
        quizDefRepo.deleteAll()
        adminGroupDefRepo.deleteAll()
        userAttrsRepo.deleteAll()
        // global badges don't have references to a project so must delete those manually
        skillDefRepo.deleteAll()
        // notificationsRepo no longer has a fk to users so must delete explicitly
        RetryUtil.withRetry(3, {
            notificationsRepo.deleteAll()
        })

        settingRepo.findAll().each {
            if (!it.settingGroup?.startsWith("public_")) {
                settingRepo.delete(it)
            }
        }

        deleteAllAttachments()

        userActionsHistoryRepo.deleteAll()

        waitForAsyncTasksCompletion.clearScheduledTaskTable()
        skillsService = createService()
    }

    def cleanup() {
        if (greenMail) {
            log.info('Stopping email service')
            greenMail.stop()
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        String msg = "\n-------------------------------------------------------------\n" +
                "END: [${specificationContext.currentIteration.name}]\n" +
                "-------------------------------------------------------------"
        log.info(msg)
    }

    SkillsService createRootSkillService(String username = DEFAULT_ROOT_USER_ID, String password = 'aaaaaaaa') {
        SkillsService rootSkillsService = createService(username, password)
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        return rootSkillsService
    }

    static String defaultFromEmail = "resetspec@skilltreetests"
    def startEmailServer() {
        greenMail = new GreenMail(ServerSetupTest.SMTP)
        greenMail.start()

        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : ServerSetupTest.SMTP.port,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false,
                "publicUrl"  : "http://localhost:${localPort}/".toString(),
                "fromEmail"  : defaultFromEmail
        ])
    }


    SkillsService createService(
            String username = "skills@skills.org",
            String password = "password",
            String firstName = "Skills",
            String lastName = "Test",
            String url = "http://localhost:${localPort}".toString()){

        boolean pkiEnabled = mockUserInfoService != null
        if (pkiEnabled) {
            url = url.replace("http://", "https://")
        }

        SkillsService.UseParams userParams = new SkillsService.UseParams(
                username: username,
                password: password,
                firstName: firstName,
                lastName: lastName
        )
        SkillsService res = new SkillsService(userParams, url, pkiEnabled != null ? certificateRegistry : null)
        res.waitForAsyncTasksCompletion = waitForAsyncTasksCompletion
        return res
    }

    SkillsService createService(
            SkillsService.UseParams userParams,
            String url = "http://localhost:${localPort}".toString()){

        boolean pkiEnabled = mockUserInfoService != null
        if (pkiEnabled) {
            url = url.replace("http://", "https://")
        }

        SkillsService res = new SkillsService(userParams, url, pkiEnabled != null ? certificateRegistry : null)
        res.waitForAsyncTasksCompletion = waitForAsyncTasksCompletion
        return res
    }

    SkillsService createSupervisor(){
        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()
        String supervisorUserId = 'foo@bar.com'
        SkillsService supervisorService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)
        return supervisorService
    }

    /*
     * Returns N number of random users. NOTE - if tests are run in pki mode,
     * N must be less than or equal to the number of test p12 certificates available,
     * otherwise an exception will occur.
     *
     * @param numUsers number of random users - must be less than or equal to the number
     * of test p12 certificates available if in pki mode
     * @return
     */
    List<String>  getRandomUsers(int numUsers, boolean createEmail = true, List<String> exclude=[DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME]) {
        //create email addresses for the users automatically?
        List<String> userIds =  userUtil.getUsers(numUsers+exclude.size())
        exclude.each { userToExclude ->
            String idToRemove = userIds.find { it.equalsIgnoreCase(userToExclude) }
            userIds.remove(idToRemove)
        }

        while (userIds.size() > numUsers) {
            userIds.pop()
        }
        if (createEmail) {
            userIds?.each {
                if (!(userAttrsRepo.findByUserIdIgnoreCase(it))) {
                    try {
                        UserAttrs userAttrs = new UserAttrs()
                        userAttrs.userId = it.toLowerCase()
                        userAttrs.userIdForDisplay = it
                        userAttrs.email = it.contains('@') ? it : EmailUtils.generateEmaillAddressFor(it)
                        userAttrs.firstName = "${it.toUpperCase()}_first"
                        userAttrs.lastName = "${it.toUpperCase()}_last"
                        userAttrs.userTagsLastUpdated = new Date()
                        userAttrsRepo.save(userAttrs)
                    } catch (Exception e) {
                        throw new RuntimeException("error initializing UserAttrs for [${it}]", e)
                    }
                }
            }

        }

        return userIds
    }

    int deleteAllAttachments() {
        return transactionHelper.doInTransaction {
            entityManager.createQuery("DELETE from Attachment").executeUpdate()
        }
    }

    protected <T> T runInTransaction(Closure<T> inTransactionLogic) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW")
        return transactionTemplate.execute(new TransactionCallback<T>() {
            @Override
            T doInTransaction(TransactionStatus status) {
                return inTransactionLogic.call()
            }
        })
    }

}
