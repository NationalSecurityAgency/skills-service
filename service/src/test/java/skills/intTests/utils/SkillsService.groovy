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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.codec.net.URLCodec
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.StreamUtils
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.CopyToAnotherProjectRequestType
import skills.controller.request.model.PageVisitRequest
import skills.services.settings.Settings
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.settings.EmailSettingsService
import skills.storage.model.auth.RoleName
import skills.utils.GroovyToJavaByteUtils

@Slf4j
class SkillsService {

    WSHelper wsHelper

    CertificateRegistry certificateRegistry = null

    WaitForAsyncTasksCompletion waitForAsyncTasksCompletion //optionally configured

    SkillsService() {
        wsHelper = new WSHelper().init()
    }

    SkillsService(String username) {
        wsHelper = new WSHelper(username: username).init()
    }

    SkillsService(String username, String password) {
        wsHelper = new WSHelper(username: username, password: password).init()
    }

    SkillsService(String username, String password, String firstName, String lastName, String service) {
        wsHelper = new WSHelper(username: username, password: password, skillsService: service, firstName: firstName, lastName: lastName).init()
    }

    SkillsService(String username, String password, String firstName, String lastName, String service, CertificateRegistry certificateRegistry) {
        this.certificateRegistry = certificateRegistry
        wsHelper = new WSHelper(username: username, password: password, skillsService: service, firstName: firstName, lastName: lastName, certificateRegistry: certificateRegistry).init(certificateRegistry != null)
    }

    SkillsService(UseParams userParams, String service, CertificateRegistry certificateRegistry) {
        this.certificateRegistry = certificateRegistry
        wsHelper = new WSHelper(username: userParams.username,
                password: userParams.password,
                skillsService: service,
                firstName: userParams.firstName,
                lastName: userParams.lastName,
                email: userParams.email ?: (userParams.username?.contains("@") ? userParams.username : "${userParams.username}@skills.org"),
                certificateRegistry: certificateRegistry).init(certificateRegistry != null)
    }

    String getClientSecret(String projectId){
        wsHelper.get("/projects/${projectId}/clientSecret", "admin", null, false)
    }

    String resetClientSecret(String projectId){
        wsHelper.adminPost("/projects/${projectId}/resetClientSecret".toString(), null)
    }

    void verifyEmail(String email, String verificationCode) {
        wsHelper.rawPost('verifyEmail', [email: email, token: verificationCode])
    }

    String getUserName() {
        wsHelper.username
    }

    void setProxyCredentials(String clientId, String secretCode) {
        wsHelper.setProxyCredentials(clientId, secretCode)
    }

    def deleteAllMyProjects() {
        def projects = getProjects()
        projects.each {
            deleteProject(it.projectId)
            log.debug("Removed {} project", it.projectId)
        }
    }

    def deleteAllUsers() {
        def users = getRootUsers()
        def nonrootUsers = getNonRootUsers()

        deleteUserRole()
    }

    /**
     * high level utility to quickly construct rule-set schema
     * @param subjects list of subjects, where each item in the subject is props for a skill
     */
    def createSchema(List<List<Map>> subjects){
        String projId = subjects.first().first().projectId
        createProject([projectId: projId, name: projId])
        subjects.each {
            createSubject([projectId: projId, subjectId: it.first().subjectId, name: it.first().subjectId])
            it.each { Map params ->
                createSkill(params)
            }
        }
    }

    @Profile
    def createProject(Map props, String originalProjectId = null) {
        wsHelper.appPost(getProjectUrl(originalProjectId ?: props.projectId), props)
    }
    @Profile
    def validateProjectForEnablingCommunity(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId) + "/validateEnablingCommunity")
    }
    @Profile
    def validateQuizForEnablingCommunity(String quizId) {
        wsHelper.adminGet("/quiz-definitions/${quizId}/validateEnablingCommunity")
    }
    @Profile
    def validateAdminGroupForEnablingCommunity(String adminGroupId) {
        wsHelper.adminGet("/admin-group-definitions/${adminGroupId}/validateEnablingCommunity")
    }
    @Profile
    def validateGlobalBadgeForEnablingCommunity(String badgeId) {
        wsHelper.adminGet("/badges/${badgeId}/validateEnablingCommunity")
    }

    @Profile
    def copyProject(String fromProjId, Map toProjProps) {
        wsHelper.adminPost("/projects/${fromProjId}/copy".toString(), toProjProps)
    }

    @Profile
    def copySkillDefsIntoAnotherProjectSubject(String fromProjId, List<String> skillIds, String toProjectId, String toSubjId) {
        def request = [copyType: CopyToAnotherProjectRequestType.SelectSkills, skillIds: skillIds, toSubjectId: toSubjId]
        wsHelper.adminPost("/projects/${fromProjId}/copy/projects/${toProjectId}".toString(), request)
    }

    @Profile
    def copySkillDefsIntoAnotherProjectSkillGroup(String fromProjId, List<String> skillIds, String toProjectId, String toSubjId, String otherGroupId) {
        def request = [copyType: CopyToAnotherProjectRequestType.SelectSkills, skillIds: skillIds, toSubjectId: toSubjId, toGroupId: otherGroupId]
        wsHelper.adminPost("/projects/${fromProjId}/copy/projects/${toProjectId}".toString(), request)
    }

    @Profile
    def validateCopySkillDefsIntoAnotherProject(String fromProjId, List<String> skillIds, String toProjectId, String toSubjId, String otherGroupId = null) {
        def request = [copyType: CopyToAnotherProjectRequestType.SelectSkills, skillIds: skillIds, toSubjectId: toSubjId, toGroupId: otherGroupId]
        wsHelper.adminPost("/projects/${fromProjId}/copy/projects/${toProjectId}/validateCopy".toString(), request)?.body
    }

    @Profile
    def copySubjectDefIntoAnotherProject(String fromProjId, String fromSubjectId, String toProjectId) {
        def request = [copyType: CopyToAnotherProjectRequestType.EntireSubject, fromSubjectId: fromSubjectId]
        wsHelper.adminPost("/projects/${fromProjId}/copy/projects/${toProjectId}".toString(), request)
    }
    @Profile
    def validateCopySubjectDefIntoAnotherProject(String fromProjId, String fromSubjectId, String toProjectId) {
        def request = [copyType: CopyToAnotherProjectRequestType.EntireSubject, fromSubjectId: fromSubjectId]
        wsHelper.adminPost("/projects/${fromProjId}/copy/projects/${toProjectId}/validateCopy".toString(), request).body
    }

    static String PROD_MODE = Settings.PRODUCTION_MODE.settingName

    def enableProdMode(proj) {
        setProdMode(proj, true)
    }

    def disableProdMode(proj) {
        setProdMode(proj, false)
    }

    def setProdMode(proj, boolean isProd) {
        this.changeSetting(proj.projectId, PROD_MODE, [projectId: proj.projectId, setting: PROD_MODE, value: isProd.toString()])
    }


    @Profile
    def createProjectAndSubjectAndSkills(Map projProps, Map subjProps, List skills) {
        if (projProps) {
            createProject(projProps)
        }
        if (subjProps) {
            createSubject(subjProps)
        }
        if (skills) {
            createSkills(skills)
        }
    }

    @Profile
    def createSubjectAndSkills(Map subjProps, List skills) {
        createSubject(subjProps)
        createSkills(skills)
    }

    def createUser(Map props){
        //props: firstName, lastName, email, password
        if (this.certificateRegistry == null) {
            wsHelper.put("createAccount", "", props)
        } else {
            WSHelper temp = new WSHelper(username: props.email,
                    password: props.password,
                    skillsService: wsHelper.skillsService,
                    firstName: props.firstName,
                    lastName: props.lastName,
                    certificateRegistry: certificateRegistry).init(certificateRegistry != null)
            temp.get("", "", [:], false)
        }
    }

    def searchOtherProjectsByName(String projectId, String query) {
        wsHelper.adminGet("/projects/${projectId}/projectSearch?nameQuery=${query}")
    }


    @Profile
    def changeProjectDisplayOrder(Map props, Integer newDisplayOrderIndex){
        wsHelper.adminPatch(getProjectUrl(props.projectId), [
                action: "NewDisplayOrderIndex",
                newDisplayOrderIndex: newDisplayOrderIndex,
        ]);
    }

    @Profile
    def changeSubjectDisplayOrder(Map props, Integer newDisplayOrderIndex){
        wsHelper.adminPatch(getSubjectUrl(props.projectId, props.subjectId), [
                action: "NewDisplayOrderIndex",
                newDisplayOrderIndex: newDisplayOrderIndex,
        ]);
    }

    @Profile
    def moveSkillUp(Map props){
        wsHelper.adminPatch(getSkillUrl(props.projectId, props.subjectId, props.skillId), '{"action": "DisplayOrderUp"}')
    }

    @Profile
    def moveSkillDown(Map props){
        wsHelper.adminPatch(getSkillUrl(props.projectId, props.subjectId, props.skillId), '{"action": "DisplayOrderDown"}')
    }


    @Profile
    def changeBadgeDisplayOrder(Map props, Integer newDisplayOrderIndex){
        wsHelper.adminPatch(getBadgeUrl(props.projectId, props.badgeId), [
                action: "NewDisplayOrderIndex",
                newDisplayOrderIndex: newDisplayOrderIndex,
        ]);
    }

    @Profile
    def changeGlobalBadgeDisplayOrder(Map props, Integer newDisplayOrderIndex){
        wsHelper.globalBadgePatch(getGlobalBadgeUrl(props.badgeId), [
                action: "NewDisplayOrderIndex",
                newDisplayOrderIndex: newDisplayOrderIndex,
        ]);
    }

    @Profile
    def updateProject(Map props, String oldProjectId = null) {
        wsHelper.adminPost(getProjectUrl( oldProjectId ?: props.projectId), props)
    }

    def projectIdExists(Map props){
        String id = props.projectId
        wsHelper.appPost("/projectExist", props)
    }

    def projectNameExists(Map props){
        String name = props.projectName
        wsHelper.appPost("/projectExist", [name: name])?.body
    }

    def getProjects() {
        //note that search is only used if the requesting user is a root user
        //otherwise whatever value is supplied for the parameter is ignored by the service
        wsHelper.appGet("/projects")
    }

    def searchProjects(String search) {
        wsHelper.rootGet("/searchProjects?name=${search}")
    }

    def getAllProjects() {
        //note that search is only used if the requesting user is a root user
        //otherwise whatever value is supplied for the parameter is ignored by the service
        wsHelper.rootGet("/projects")
    }

    def getProject(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId))
    }

    def getProjectUsersCount(String projectId) {
        wsHelper.adminGet("${getProjectUrl(projectId)}/users/count")
    }

    def deleteProjectIfExist(String projectId) {
        def res = wsHelper.appPost("/projectExist", [projectId: projectId])
        Boolean exists = res.body
        if(exists) {
            deleteProject(projectId)
        }
    }

    def deleteProject(String projectId) {
        wsHelper.adminDelete("/projects/${projectId}")
    }

    def getProjectErrors(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        wsHelper.adminGet("/projects/${projectId}/errors?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def deleteAllProjectErrors(String projectId) {
        wsHelper.adminDelete("/projects/${projectId}/errors")
    }

    def deleteSpecificProjectError(String projectId, Integer errorId) {
        wsHelper.adminDelete("/projects/${projectId}/errors/${errorId}")
    }

    def deleteUserRole(String userId, String projectId, String role) {
//        userId = getUserId(userId)
        wsHelper.adminDelete("/projects/${projectId}/users/${userId}/roles/${role}")
    }

    def getUserRolesForProject(String projectId, List<RoleName> roles, int limit=10, int page=1, String orderBy="userId", boolean ascending=true) {
        String rolesStr = roles.collect { "roles=${it.toString()}" }.join("&")
        wsHelper.adminGet("/projects/${projectId}/userRoles?${rolesStr}&limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def getUserRolesForProjectAndUser(String projectId, String userId) {
        userId = getUserId(userId)
        wsHelper.adminGet("/projects/${projectId}/users/${userId}/roles")
    }

    def addUserRole(String userId, String projectId, String role) {
        userId = getUserId(userId)
        wsHelper.adminPost("/projects/${projectId}/users/${userId}/roles/${role}", [:])
    }

    @Profile
    def createSubject(Map props, boolean throwExceptionOnFailure = true) {
        wsHelper.adminPost(getSubjectUrl(props.projectId, props.subjectId), props, throwExceptionOnFailure)
    }

    @Profile
    def updateSubject(Map props, String originalSubjectId = null) {
        wsHelper.adminPost(getSubjectUrl(props.projectId, originalSubjectId ?: props.subjectId), props)
    }

    def getSubjects(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId) + "/subjects")
    }

    def getSubjectsAndSkillGroups(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId) + "/subjectsAndSkillsGroups")
    }

    def subjectNameExists(Map props){
        wsHelper.adminPost("/projects/${props.projectId}/subjectNameExists", [name:props.subjectName])?.body
    }

    def getSubjectDescriptions(String projectId, String subjectId, String userId = null) {
        userId = getUserId(userId)
        String url = "/projects/${projectId}/subjects/${subjectId}/descriptions".toString()
        Map params = userId ? [userId: userId] : null
        wsHelper.apiGet(url, params)
    }

    def getBadgeDescriptions(String projectId, String badgeId, boolean isGlobal = false, String userId = null) {
        userId = getUserId(userId)
        String url = "/projects/${projectId}/badges/${badgeId}/descriptions"
        Map params = [:]
        if (isGlobal){
            params["global"] = true
        }
        if (userId) {
            params["userId"] = userId
        }
        wsHelper.apiGet(url.toString(), params)
    }

    def badgeNameExists(Map props){
        wsHelper.adminPost("/projects/${props.projectId}/badgeNameExists", [name:props.badgeName])?.body
    }

    def skillNameExists(Map props){
        wsHelper.adminPost("/projects/${props.projectId}/skillNameExists", [name: props.skillName])?.body
    }

    def deleteSubject(Map props) {
        wsHelper.adminDelete(getSubjectUrl(props.projectId, props.subjectId))
    }

    def createSkills(List<Map> props) {
        props.each {
            def res = createSkill(it)
            assert res.statusCode.value() == 200
        }
    }

    def shareSkill(String projectId, String skillId, String shareToProjectId){
        String url = "/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}".toString()
        wsHelper.adminPost(url, null)
    }

    def deleteShared(String projectId, String skillId, String shareToProjectId) {
        String url = "/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}"
        wsHelper.adminDelete(url)
    }

    def getSharedSkills(String projectId) {
        String url = "/projects/${projectId}/shared".toString()
        wsHelper.adminGet(url)
    }

    def getSharedWithMeSkills(String projectId) {
        String url = "/projects/${projectId}/sharedWithMe".toString()
        wsHelper.adminGet(url)
    }

    def getSharedSkillSummary(String projectIdTo, String projectIdFrom, String skillId){
        ///projects/{projectId}/projects/{crossProjectId}/skills/{skillId}/summary
        String url = "/projects/${projectIdTo}/projects/${projectIdFrom}/skills/${skillId}/summary"
        wsHelper.apiGet(url)
    }

    @Profile
    def createSkill(Map props, boolean throwExceptionOnFailure = true) {
        wsHelper.adminPost(getSkillUrl(props.projectId, props.subjectId, props.skillId), props, throwExceptionOnFailure)
    }

    def updateSkill(Map props, String originalSkillId = null) {
        wsHelper.adminPost(getSkillUrl(props.projectId, props.subjectId, originalSkillId ?: props.skillId), props)
    }

    def createBadge(Map props, String originalBadgeId = null) {
        wsHelper.adminPost(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def createGlobalBadge(Map props, String originalBadgeId = null) {
        wsHelper.appPut(getGlobalBadgeUrl(originalBadgeId ?: props.badgeId), props)
    }

    def updateGlobalBadge(Map props, String originalBadgeId = null) {
        wsHelper.globalBadgePut(getGlobalBadgeUrl(originalBadgeId ?: props.badgeId), props)
    }

    def updateBadge(Map props, String originalBadgeId = null) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def addLearningPathPrerequisite(String projectId, String id, String prereqId) {
        this.addLearningPathPrerequisite(projectId, id, projectId, prereqId)
    }

    def addLearningPathPrerequisite(String projectId, String id, String prereqProjectId, String prereqId) {
        String url = "/projects/${projectId}/${id}/prerequisite/${prereqProjectId}/${prereqId}".toString()
        wsHelper.adminPost(url, [:])
    }

    def deleteLearningPathPrerequisite(String projectId, String id, String prereqId) {
        this.deleteLearningPathPrerequisite(projectId, id, projectId, prereqId)
    }

    def deleteLearningPathPrerequisite(String projectId, String id, String prereqProjectId, String prereqId) {
        String url = "/projects/${projectId}/${id}/prerequisite/${prereqProjectId}/${prereqId}".toString()
        wsHelper.adminDelete(url, [:])
    }

    def vadlidateLearningPathPrerequisite(String projectId, String id, String prereqProjectId, String prereqId, boolean throwExceptionOnFailure = true) {
        String url = "/projects/${projectId}/${id}/prerequisiteValidate/${prereqProjectId}/${prereqId}".toString()
        wsHelper.adminGet(url, [:])
    }

    def checkIfSkillsHaveDependencies(String projectId, List<String> skillIds) {
        String url = "/projects/${projectId}/hasDependency"
        return wsHelper.adminPost(url, [skillIds: skillIds]).body
    }

    def deleteSkill(Map props) {
        wsHelper.adminDelete(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }
    def deleteSkillEvent(Map props) {
        String url = "/projects/${props.projectId}/skills/${props.skillId}/users/${props.userId}/events/${props.timestamp}"
        wsHelper.adminDelete(url, props)
    }

    def deleteAllSkillEvents(Map props) {
        String url = "/projects/${props.projectId}/users/${props.userId}/events"
        wsHelper.adminDelete(url, props)
    }

    def getSkill(Map props) {
        wsHelper.adminGet(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }

    def getSkillsForProject(String projectId, String optionalSkillNameQuery = "", boolean excludeImportedSkills = false, boolean includeDisabled = false, boolean excludeReusedSkills = false) {
        String query = optionalSkillNameQuery ? "?skillNameQuery=${optionalSkillNameQuery}" : ''
        if (excludeImportedSkills) {
            String append = "excludeImportedSkills=true"
            query = query.contains("?") ? "${query}&${append}" : "${query}?${append}"
        }
        if (includeDisabled) {
            String append = "includeDisabled=true"
            query = query.contains("?") ? "${query}&${append}" : "${query}?${append}"
        }
        if (excludeReusedSkills) {
            String append = "excludeReusedSkills=true"
            query = query.contains("?") ? "${query}&${append}" : "${query}?${append}"
        }

        wsHelper.adminGet("/projects/${projectId}/skills${query}")
    }

    def getSkillsForSubject(String projectId, String subjectId, boolean includeGroupSkills = false) {
        wsHelper.adminGet("/projects/${projectId}/subjects/${subjectId}/skills?includeGroupSkills=${includeGroupSkills}")
    }

    def getSkillsForGroup(String projectId, String groupId) {
        wsHelper.adminGet("/projects/${projectId}/groups/${groupId}/skills")
    }

    def getSubject(Map props) {
        wsHelper.adminGet("/projects/${props.projectId}/subjects/${props.subjectId}")
    }

    def getSubjectForGroup(String projectId, String groupId) {
        wsHelper.adminGet("/projects/${projectId}/groups/${groupId}/subject")
    }

    def getBadge(String projectId, String badgeId) {
        this.getBadge([projectId: projectId, badgeId: badgeId])
    }

    def getBadges(String projectId) {
        wsHelper.adminGet("/projects/${projectId}/badges")
    }

    def getBadge(Map props) {
        wsHelper.adminGet("/projects/${props.projectId}/badges/${props.badgeId}")
    }

    def getBadgeSkills(String projectId, String badgeId) {
        wsHelper.adminGet("/projects/${projectId}/badge/${badgeId}/skills")
    }
    def removeBadge(Map props) {
        wsHelper.adminDelete("/projects/${props.projectId}/badges/${props.badgeId}")
    }

    def getAvailableProjectsForGlobalBadge(String badgeId, String query="") {
        wsHelper.globalBadgeGet("${getGlobalBadgeUrl(badgeId)}/projects/available?query=${query}")
    }

    def getAvailableSkillsForGlobalBadge(String badgeId, String query) {
        wsHelper.globalBadgeGet("${getGlobalBadgeUrl(badgeId)}/skills/available?query=${query}")
    }

    def getGlobalBadge(String badgeId) {
        wsHelper.globalBadgeGet(getGlobalBadgeUrl(badgeId))
    }

    def getGlobalBadgeSkills(String badgeId) {
        wsHelper.globalBadgeGet("/badges/${badgeId}/skills")
    }

    def getLevelsForProject(String projectId) {
        wsHelper.globalBadgeGet("/projects/${projectId}/levels")
    }

    def getServiceStatus() {
        wsHelper.get("/status", "public", null)
    }

    def getServiceIsAlive() {
        wsHelper.get("/isAlive", "public", null)
    }

    def getAllGlobalBadges() {
        wsHelper.appGet("/badges")
    }

    def doesGlobalBadgeNameExists(String name) {
        wsHelper.appPost("/badges/name/exists", [name:name])?.body
    }
    def doesGlobalBadgeIdExists(String id) {
        wsHelper.appGet("/badges/id/${id}/exists")
    }

    def deleteGlobalBadge(String badgeId) {
        wsHelper.globalBadgeDelete(getGlobalBadgeUrl(badgeId))
    }

    @Profile
    def addSkill(Map props, String userId = null, Date date = new Date(), String approvalRequestedMsg = null, Boolean doNotRequireApproval = null) {
        Map params = [:]

        if (userId) {
            userId = getUserId(userId)
            assert date

            params.userId = userId
            params.timestamp = date.time
            params.approvalRequestedMsg = approvalRequestedMsg
        }
        if (doNotRequireApproval) {
            params.doNotRequireApproval = doNotRequireApproval
        }

        return wsHelper.apiPut("/projects/${props.projectId}/skills/${props.skillId}", params ?: null)
    }

    def reportCrossProjectSkill(String projId, String crossProjectId, String otherProjId, String optionalUserId = null, Date optionalDate = null, String optionalApprovalMsg = null) {
        String url = "/projects/${projId}/crossProject/${crossProjectId}/skills/${otherProjId}"
        Map params = [:]

        if (optionalUserId) {
            optionalUserId = getUserId(optionalUserId)
            params.userId = optionalUserId
        }
        if (optionalDate) {
            params.timestamp = optionalDate.time
        }
        if (optionalApprovalMsg) {
            params.approvalRequestedMsg = optionalApprovalMsg
        }

        return wsHelper.apiPost(url, params ?: null)
    }

    @Profile
    def bulkAddSkill(Map props, List<String> userIds, Date date) {
        userIds = userIds.collect { getUserId(it, false) }
        assert date
        return wsHelper.adminPost("/projects/${props.projectId}/skills/${props.skillId}", [ userIds : userIds, timestamp:date.time])
    }

    @Profile
    def bulkAddSkill(Map props, List<String> userIds, Long timestamp) {
        userIds = userIds.collect { getUserId(it, false) }
        return wsHelper.adminPost("/projects/${props.projectId}/skills/${props.skillId}", [ userIds : userIds, timestamp: timestamp])
    }

    def getApprovals(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        return wsHelper.adminGet("/projects/${projectId}/approvals?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def getApprovalsHistory(String projectId, int limit, int page, String orderBy, Boolean ascending,  String skillNameFilter = '', String userIdFilter ='', String approverUserIdFilter = '') {
        return wsHelper.adminGet("/projects/${projectId}/approvals/history?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}&" +
                "skillNameFilter=${skillNameFilter}&userIdFilter=${userIdFilter}&approverUserIdFilter=${approverUserIdFilter}")
    }


    def approve(String projectId, List<Integer> approvalId, String msg = null) {
        return wsHelper.adminPost("/projects/${projectId}/approvals/approve", [skillApprovalIds: approvalId, approvalMessage: msg])
    }

    def configureApproverForUser(String projectId, String approverUserId, String userId) {
        return wsHelper.adminPost("/projects/${projectId}/approverConf/${approverUserId}", [userId: userId])
    }

    def getApproverConf(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/approverConf")
    }

    def countApproverConf(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/approverConf/count")
    }

    def deleteApproverConf(String projectId, Integer approverRefId) {
        return wsHelper.adminDelete("/projects/${projectId}/approverConf/${approverRefId}")
    }

    def configureApproverForSkillId(String projectId, String approverUserId, String skillId) {
        return wsHelper.adminPost("/projects/${projectId}/approverConf/${approverUserId}", [skillId: skillId])
    }

    def configureApproverForUserTag(String projectId, String approverUserId, String userTagKey, String userTagValue) {
        return wsHelper.adminPost("/projects/${projectId}/approverConf/${approverUserId}", [userTagKey: userTagKey, userTagValue: userTagValue])
    }

    def configureFallbackApprover(String projectId, String approverUserId) {
        return wsHelper.adminPost("/projects/${projectId}/approverConf/${approverUserId}/fallback", [])
    }

    def rejectSkillApprovals(String projectId, List<Integer> approvalId, String msg = null) {
        return wsHelper.adminPost("/projects/${projectId}/approvals/reject", [skillApprovalIds: approvalId, rejectionMessage: msg])
    }

    def getSkillApprovalsStats(String projectId, String skillId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/${skillId}/approvals/stats")
    }

    def removeRejectionFromView(String projectId, Integer approvalId, String userId = null) {
        wsHelper.apiDelete("/projects/${projectId}/rejections/${approvalId}?${userId ? "userId=${userId}" : ""}")
    }

    def getSelfReportStats(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/selfReport/stats")
    }

    def addSkillAndOptionallyThrowExceptionAtTheEnd(Map props, String userId, Date date, boolean throwException) {
        userId = getUserId(userId)
        return wsHelper.apiPost("/projects/${props.projectId}/skills/${props.skillId}/throwException/${throwException}".toString(),
                [ userId : userId, timestamp:date.time])
    }

    def addSkillAsAdminProxy(Map props, String adminUserId, String userId, Date date = new Date(), String approvalRequestedMsg = null) {
        userId = getUserId(userId)
        assert date
        wsHelper.proxyApiPut(wsHelper.getTokenForUser(adminUserId), "/projects/${props.projectId}/skills/${props.skillId}", [ userId : userId, timestamp:date.time, approvalRequestedMsg: approvalRequestedMsg])
    }

    def addSkillAsProxy(Map props, String userId, boolean includeGrantType=true, boolean includeProxyUser=true) {
//        userId = getUserId(userId)
        wsHelper.proxyApiPut(wsHelper.getTokenForUser(userId, includeGrantType, includeProxyUser), "/projects/${props.projectId}/skills/${props.skillId}", null)
    }

    def getSkillSummaryAsProxy(String userId, String projId, String subjId=null, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/${subjId ? "subjects/${subjId}/" : ''}summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.proxyApiGet(wsHelper.getTokenForUser(userId), url)
    }

    def addBadge(Map props) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, props.badgeId), props)
    }

    def assignSkillToBadge(String projectId, String badgeId, String skillId) {
        this.assignSkillToBadge([projectId: projectId, badgeId: badgeId, skillId: skillId])
    }

    def assignSkillToBadge(Map props) {
        wsHelper.adminPost(getAddSkillToBadgeUrl(props.projectId, props.badgeId, props.skillId), props)
    }

    def assignSkillsToBadge(String projectId, String badgeId, List<String> skillIds) {
        wsHelper.adminPost(getAddSkillsToBadgeUrl(projectId, badgeId), [skillIds: skillIds])
    }

    def assignSkillToSkillsGroup(String groupId, Map props) {
        wsHelper.adminPost(getAddSkillToSkillsGroupUrl(props.projectId, props.subjectId, groupId, props.skillId), props)
    }

    def removeSkillFromBadge(Map props) {
        wsHelper.adminDelete(getAddSkillToBadgeUrl(props.projectId, props.badgeId, props.skillId), props)
    }
    def assignSkillToGlobalBadge(String projectId, String badgeId, String skillId) {
        this.assignSkillToGlobalBadge(['badgeId': badgeId, 'projectId': projectId, 'skillId': skillId])
    }
    def assignSkillToGlobalBadge(Map props) {
        wsHelper.globalBadgePost(getAddSkillToGlobalBadgeUrl(props.badgeId, props.projectId, props.skillId), props)
    }

    def removeSkillFromGlobalBadge(String projectId, String badgeId, String skillId) {
        this.removeSkillFromGlobalBadge([badgeId: badgeId, projectId: projectId, skillId: skillId])
    }

    def removeSkillFromGlobalBadge(Map props) {
        wsHelper.globalBadgeDelete(getAddSkillToGlobalBadgeUrl(props.badgeId, props.projectId, props.skillId), props)
    }

    def assignProjectLevelToGlobalBadge(Map props) {
        wsHelper.globalBadgePost(getAddProjectLevelToGlobalBadgeUrl(props.badgeId, props.projectId, props.level), props)
    }

    def changeProjectLevelOnGlobalBadge(Map props) {
        wsHelper.globalBadgePost(getchangeProjectLevelOnGlobalBadgeUrl(props.badgeId, props.projectId, props.currentLevel, props.newLevel), [:])
    }

    def removeProjectLevelFromGlobalBadge(Map props) {
        wsHelper.globalBadgeDelete(getAddProjectLevelToGlobalBadgeUrl(props.badgeId, props.projectId, props.level), props)
    }

    def suggestDashboardUsers(String query) {
        String url = "/users/suggestDashboardUsers"
        wsHelper.appPost(url, [suggestQuery: query])?.body
    }

    def suggestClientUsersForProject(String projectId, String query){
        String url = "/users/projects/${projectId}/suggestClientUsers".toString()
        wsHelper.appPost(url, [suggestQuery: query])?.body
    }

    def suggestClientUsers(String query){
        String url = "/users/suggestClientUsers".toString()
        wsHelper.appPost(url, [suggestQuery: query])?.body
    }

    def getUserLevel(String projectId, String userId = null) {
        userId = getUserId(userId)
        String url = "/projects/${projectId}/level"
        if (userId) {
            url = "${url}?userId=${userId}"
        }

        wsHelper.apiGet(url)
    }

    def getUserTags(String userId) {
        String url = "/userInfo/userTags/${userId}"
        wsHelper.appGet(url)
    }

    def getCustomIconCssForProject(String projectId){
        String url = "/projects/${projectId}/customIconCss"
        wsHelper.get(url.toString(), "api", null, false)
    }

    def getSkillSummary(String userId, String projId, String subjId=null, int version = -1, boolean includeSkills=true) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/${subjId ? "subjects/${subjId}/" : ''}summary?includeSkills=${includeSkills}"
        if (userId) {
            url += "&userId=${userId}"
        }
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getApiSkills(String projectId, String query = null, String userId = null, Integer limit = null, Integer page = null, String orderBy = null, Boolean ascending = null) {
        String url = "/projects/${projectId}/skills?query=${query ?: ''}"
        if (userId != null) {
            url = "${url}&userId=${userId}"
        }
        if (limit != null) {
            url = "${url}&limit=${limit}"
        }
        if (ascending != null) {
            url = "${url}&ascending=${ascending}"
        }
        if (orderBy != null) {
            url = "${url}&orderBy=${orderBy}"
        }
        if (page != null) {
            url = "${url}&page=${page}"
        }
        wsHelper.apiGet(url)
    }

    def getApiAllSubjectsBadgesAndSkills(String projectId) {
        String url = "/projects/${projectId}/skillsSubjectsAndBadges"
        wsHelper.apiGet(url)
    }

    def documentVisitedSkillId(String projectId, String skillId) {
        String url = "/projects/${projectId}/skills/visited/${skillId}"
        wsHelper.apiPost(url, [])
    }

    def getSkillsSummaryForCurrentUser(String projId, int version = -1) {
        String url = "/projects/${projId}/summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSkillsSummaryForUser(String projId, String userId, int version = -1) {
        String url = "/projects/${projId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getMyProgressSummary() {
        String url = "/myProgressSummary"
        wsHelper.apiGet(url)
    }

    def getMyProgressBadges() {
        String url = "/mybadges"
        wsHelper.apiGet(url)
    }

    def getAvailableMyProjects() {
        String url = "/availableForMyProjects"
        wsHelper.apiGet(url)
    }

    def addMyProject(String projectId) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiPost(url, null)
    }

    def addMyHiddenProject(String projectId) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiPost(url, [isHiddenProject: true])
    }

    def moveMyProject(String projectId, Integer newSortIndex) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiPost(url, [newSortIndex: newSortIndex])
    }

    def removeMyProject(String projectId) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiDelete(url)
    }

    def getDependencyGraph(String projId) {
        String url = "/projects/${projId}/dependency/graph"
        wsHelper.adminGet(url)
    }

    def getSingleSkillSummary(String userId, String projId, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSingleSkillSummaryForCurrentUser(String projId, String skillId, int version = -1) {
        String url = "/projects/${projId}/skills/${skillId}/summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSingleSkillSummaryWithSubject(String userId, String projectId, String subjectId, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projectId}/subjects/${subjectId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSubjectSummaryForUser(String userId, String projId, String subjectId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/subjects/${subjectId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSubjectSummaryForCurrentUser(String projId, String subjectId, int version = -1) {
        String url = "/projects/${projId}/subjects/${subjectId}/summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getCrossProjectSkillSummary(String userId, String projId, String otherProjectId, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/projects/${otherProjectId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getCrossProjectSkillSummaryWithSubject(String userId, String projId, String otherProjectId, String subject, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/projects/${otherProjectId}/subjects/${subject}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getBadgesSummary(String userId, String projId){
        userId = getUserId(userId)
        String url = "/projects/${projId}/badges/summary"
        if (userId) {
            url += "?userId=${userId}"
        }
        wsHelper.apiGet(url)
    }

    def getBadgeSummary(String userId, String projId, String badgeId, int version = -1, boolean global = false){
        userId = getUserId(userId)
        String url = "/projects/${projId}/badges/${badgeId}/summary?global=${global}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        if (userId) {
            url += "&userId=${userId}"
        }
        wsHelper.apiGet(url)
    }

    def listVersions(String projectId) {
        String url = "/projects/${projectId}/versions"
        wsHelper.appGet(url)
    }

    def getSkillDependencyInfo(String userId, String projId, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/skills/${skillId}/dependencies?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getIconUsages(String projId, String cssClass) {
        return wsHelper.adminGet("/projects/${projId}/icons/${cssClass}/usage")
    }

    def uploadIcon(Map props, File icon, boolean throwException = false){
        Map body = [:]
        body.put("customIcon", icon)
        wsHelper.adminUpload("/projects/${props.projectId}/icons/upload", body, throwException)
    }
    def uploadGlobalIcon(Map props, File icon, boolean throwException = false){
        Map body = [:]
        body.put("customIcon", icon)
        wsHelper.adminUpload("/badges/${props.badgeId}/icons/upload", body, throwException)
    }
    def uploadAttachment(String fileName, String fileContents, String projectId=null, String skillId=null, String quizId=null){
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(fileContents, fileName)
        return uploadAttachment(resource, projectId, skillId, quizId)
    }
    def uploadAttachment(Resource attachment, String projectId=null, String skillId=null, String quizId=null){
        Map body = [:]
        body.put("file", attachment)
        if (projectId) { body.put("projectId", projectId)}
        if (skillId) { body.put("skillId", skillId)}
        if (quizId) { body.put("quizId", quizId)}

        assert !(projectId && quizId)
        assert projectId || skillId || quizId

        String url
        if (projectId) {
            url = "/projects/${projectId}/upload"
        } else if (quizId) {
            url = "/quiz-definitions/${quizId}/upload"
        } else {
            // assume global badge if proj and skill ids are null
            url = "/badges/${skillId}/upload"
        }
        wsHelper.adminUpload(url, body, true)
    }
    // note - not supported, used for testing purposes only
    def uploadAttachments(List<Resource> attachments, String projectId='TestProject1'){
        Map body = [:]
        attachments.each { attachment ->
            body.put("file", attachment)
            body.put("projectId", projectId)
        }
        wsHelper.adminUpload("/projects/${projectId}/upload", body)
    }

    static class FileAndHeaders {
        File file
        HttpHeaders headers
    }

    String downloadAttachmentAsText(String downloadUrl, Map params=null) {
        FileAndHeaders fileAndHeaders = downloadAttachment(downloadUrl, params)
        File file = fileAndHeaders.file
        return file.text
    }

    FileAndHeaders downloadAttachment(String downloadUrl, Map params=null) {
        ResponseEntity<Resource> responseEntity = wsHelper.getResource(downloadUrl, params)
        File file = File.createTempFile('download', 'tmp')
        StreamUtils.copy(responseEntity.getBody().getInputStream(), new FileOutputStream(file))
        return new FileAndHeaders(file: file, headers: responseEntity.headers)
    }

    def deleteIcon(Map props){
        wsHelper.adminDelete("/projects/${props.projectId}/icons/${props.filename}")
    }

    def deleteGlobalIcon(Map props){
        wsHelper.globalBadgeDelete("/badges/${props.badgeId}/icons/${props.filename}")
    }

    def getIconCssForProject(Map props){
        wsHelper.appGet("/projects/${props.projectId}/customIcons")
    }

    def getCustomIconsForBadge(Map props){
        wsHelper.globalBadgeGet("/badges/${props.badgeId}/icons/customIcons")
    }

    def getCustomIconCssForGlobalBadge(String globalBadgeId) {
        String url = "/badges/${globalBadgeId}/customIconCss"
        wsHelper.get(url.toString(), "api", null, false)
    }

    def getPerformedSkills(String userId, String project, String query = '', String orderBy = "performedOn") {
        userId = getUsername(userId)
        return wsHelper.adminGet("${getProjectUrl(project)}/performedSkills/${userId}?query=${query}&limit=10&ascending=0&page=1&byColumn=0&orderBy=${orderBy}".toString())
    }

    def getUserInfoForProject(String projectId, String userId){
        String endpoint = "/projects/${projectId}/users/${userId}".toString()
        return wsHelper.adminGet(endpoint)
    }

    def getUsersPerLevel(String projectId, String subjectId = null){
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rankDistribution/usersPerLevel" : "/projects/${projectId}/rankDistribution/usersPerLevel"
        return wsHelper.apiGet(endpoint)
    }

    def getRank(String userId, String projectId, String subjectId = null){
        userId = getUserId(userId)
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rank" : "/projects/${projectId}/rank"
        endpoint = "${endpoint}"
        if (userId) {
            endpoint += "?userId=${userId}"
        }
        return wsHelper.apiGet(endpoint)
    }

    def getLeaderboard(String userId, String projectId, String subjectId = null, String type="topTen"){
        userId = getUserId(userId)
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/leaderboard" : "/projects/${projectId}/leaderboard"
        endpoint = "${endpoint}?type=${type}"
        if (userId) {
            endpoint += "&userId=${userId}"
        }
        return wsHelper.apiGet(endpoint)
    }

    def getRankDistribution(String userId, String projectId, String subjectId = null){
        userId = getUserId(userId)
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rankDistribution" : "/projects/${projectId}/rankDistribution"
        endpoint = "${endpoint}"
        if (userId) {
            endpoint += "?userId=${userId}"
        }
        return wsHelper.apiGet(endpoint)
    }

    def getPointHistory(String userId, String projectId, String subjectId=null, Integer version = -1, Integer minNumOfDaysBeforeReturningHistory = null){
        userId = getUserId(userId)
        String endpointStart = subjectId ? getSubjectUrl(projectId, subjectId) : getProjectUrl(projectId)
        String url = "${endpointStart}/pointHistory"
        Boolean paramAdded = false;
        if (userId) {
            url += "?userId=${userId}"
            paramAdded = true
        }
        if (version >= 0) {
            url += "${paramAdded ? "&" : "?"}version=${version}"
        }
        if (minNumOfDaysBeforeReturningHistory != null) {
            url += "${paramAdded ? "&" : "?"}minNumOfDaysBeforeReturningHistory=${minNumOfDaysBeforeReturningHistory}"
        }
        return wsHelper.apiGet(url.toString())
    }

    def getProjectUsers(String projectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = "", int minimumPoints = 0, int maximumPoints = 100) {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getUserProgressExcelExport(String projectId, String orderBy = 'totalPoints', boolean ascending = true, String query = "", int minimumPoints = 0, int maximumPoints = 100) {
        return downloadAttachment("/admin${getProjectUrl(projectId)}/users/export/excel?&ascending=${ascending ? 1 : 0}&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getUserAchievementsExcelExport(String projectId, Map params=null) {
        return downloadAttachment("/admin${getProjectUrl(projectId)}/achievements/export/excel".toString(), params)
    }
    def getSkillMetricsExcelExport(String projectId) {
        return downloadAttachment("/admin${getProjectUrl(projectId)}/skills/export/excel".toString())
    }
    def getSkillsForSubjectExport(String projectId, String subjectId) {
        return downloadAttachment("/admin${getSubjectUrl(projectId, subjectId)}/skills/export/excel".toString())
    }

    def getSubjectUsers(String projectId, String subjectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '', int minimumPoints = 0, int maximumPoints = 100) {
        return wsHelper.adminGet("${getSubjectUrl(projectId, subjectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getUserStats(String projectId, String userId) {
//        userId = getUserIdEncoded(userId)
        return wsHelper.adminGet("/projects/${projectId}/users/${userId}/stats".toString())
    }

    def getExpiredSkills(String projectId, String userId, String skillName, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true) {
        return wsHelper.adminGet("/projects/${projectId}/expirations?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&orderBy=${orderBy}&userIdForDisplay=${userId}&skillName=${skillName}".toString())
    }

    def getLastSkillEventForProject(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/lastSkillEvent".toString())
    }

    def getSkillUsers(String projectId, String skillId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '', int minimumPoints = 0, int maximumPoints = 100) {
        return wsHelper.adminGet("${getSkillUrl(projectId, null, skillId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getBadgeUsers(String projectId, String badgeId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '', int minimumPoints = 0, int maximumPoints = 100) {
        return wsHelper.adminGet("${getBadgeUrl(projectId, badgeId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getUserTagUsers(String projectId, String tagKey, String tagValue, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '', int minimumPoints = 0, int maximumPoints = 100) {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/userTags/${tagKey}/${tagValue}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&minimumPoints=${minimumPoints}&maximumPoints=${maximumPoints}".toString())
    }

    def getLevels(String projectId, String subjectId = null) {
        return wsHelper.adminGet(getLevelsUrl(projectId, subjectId))
    }

    def adminGetUserLevelForProject(String projectId, String userId){
        userId = getUserId(userId)
        return wsHelper.apiGet(getUserLevelForProjectUrl(projectId, userId))
    }

    def apiGetUserLevelForProject(String projectId, String userId){
        userId = getUserId(userId)
        return wsHelper.apiGet(getUserLevelForProjectUrl(projectId, userId))
    }

    def editLevel(String projectId, String subjectId, String level, Map props){
        return wsHelper.adminPost(getEditLevelUrl(projectId, subjectId, level), props)
    }

    def addLevel(String projectId, String subjectId, Map props) {
        return wsHelper.adminPost(getAddLevelUrl(projectId, subjectId), props)
    }

    def deleteLevel(String projectId){
        return wsHelper.adminDelete(getDeleteLevelUrl(projectId))
    }

    def deleteLevel(String projectId, String subjectId){
        return wsHelper.adminDelete(getDeleteLevelUrl(projectId, subjectId))
    }

    @Deprecated
    def getMetricsChart(String projectId, String chartBuilderId, String section, String sectionId, Map props=null) {
        String endpoint = "/projects/${projectId}/${section}/${sectionId}/metrics/${chartBuilderId}"
        wsHelper.adminGet(endpoint, props)
    }

    def getMetricsData(String projectId, String metricsId, Map props=null) {
        String endpoint = "/projects/${projectId}/metrics/${metricsId}"
        wsHelper.adminGet(endpoint, props)
    }

    def getGlobalMetricsData(String metricsId, Map props=null) {
        String endpoint = "/metrics/${metricsId}"
        wsHelper.globalBadgeGet(endpoint, props)
    }

    def getApiGlobalMetricsData(String metricsId, Map props = null) {
        String endpoint = "/metrics/${metricsId}"
        wsHelper.apiGet(endpoint, props)
    }

    def getAllMetricsChartsForSection(String projectId, String section, String sectionId, Map props=null) {
        String endpoint = "/projects/${projectId}/${section}/${sectionId}/metrics"
        wsHelper.adminGet(endpoint, props)
    }

    def getAllGlobalMetricsChartsForSection(String section, Map props=null) {
        wsHelper.get("/${section}", "metrics", props)
    }

    def getGlobalMetricsChart(String chartBuilderId, String section, String sectionId, Map props = null) {
        wsHelper.get("/${section}/${sectionId}/metric/${chartBuilderId}", "metrics", props)
    }

    def getSetting(String projectId, String setting){
        return wsHelper.adminGet(getSettingUrl(projectId, setting))
    }

    def getSettings(String project){
        return wsHelper.adminGet(getSettingsUrl(project))
    }

    def getUserSettings(){
        return wsHelper.appGet(getUserSettingsUrl())
    }

    def getPublicConfigs() {
        wsHelper.get("/public/config", "", [:])
    }

    def getPublicClientDisplayConfigs() {
        wsHelper.get("/public/clientDisplay/config", "", [:])
    }

    def getPublicSetting(String setting, String settingGroup){
        return wsHelper.appGet("/public/settings/${setting}/group/${settingGroup}")
    }

    def getPublicSettings(String settingGroup){
        return wsHelper.appGet("/public/settings/group/${settingGroup}")
    }

    def findLatestSkillVersion(String projectId) {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/latestVersion")
    }

    def getRootUsers() {
        return wsHelper.rootGet('/rootUsers')
    }

    def getNonRootUsers(String query) {
        return wsHelper.rootPost("/users", [suggestQuery: query])?.body
    }

    def runReplayEventsAfterUpgrade() {
        return wsHelper.rootPost("/runReplayEventsAfterUpgrade")?.body
    }

    def saveUserTag(String userId, String tagKey, List<String> tags) {
        return wsHelper.rootPost("/users/${userId}/tags/${tagKey}", [tags: tags])?.body
    }

    def getCurrentUser() {
        return wsHelper.appGet("/userInfo")
    }

    def updateUserInfo(Map userInfo) {
        return wsHelper.appPost('/userInfo', userInfo)
    }

    def isRoot() {
        return wsHelper.rootGet('/isRoot')
    }

    def addRootRole(String userId) {
        userId = getUserId(userId, false)
        return wsHelper.rootPut("/addRoot/${userId}")
    }

    def expireSkills() {
        wsHelper.rootPost('/runSkillExpiration');
    }

    def pinProject(String projectId) {
        return wsHelper.rootPost("/pin/${projectId}")
    }

    def unpinProject(String projectId) {
        return wsHelper.rootDelete("/pin/${projectId}")
    }

    def getUsersWithoutRole(String role, String usernameQuery) {
        String url = "/users/without/role/${role}"
        return wsHelper.rootPost(url, [suggestQuery: usernameQuery])?.body
    }

    def getUsersWithRole(String role, int limit=10, int page=1, String orderBy="userId", boolean ascending=true) {
        return wsHelper.rootGet("/users/roles/${role}?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}").data
    }

    def removeRootRole(String userId) {
//        userId = getUserId(userId)
        return wsHelper.delete("/deleteRoot/${userId}", 'root', null)
    }

    def createRootAccount(Map<String, String> userInfo) {
        return wsHelper.createRootAccount(userInfo)
    }

    def grantRoot() {
        return wsHelper.grantRoot()
    }

    def grantGlobalBadgeAdminRole(String badgeId, String userId) {
        userId = getUserId(userId)
        return wsHelper.globalBadgePut("/badges/${badgeId}/users/${userId}/roles/${RoleName.ROLE_GLOBAL_BADGE_ADMIN.toString()}")
    }

    def grantRootRole(String userId) {
        userId = getUserId(userId)
        return wsHelper.rootPut("/users/${userId}/roles/${RoleName.ROLE_SUPER_DUPER_USER.toString()}")
    }

    def grantDashboardAdminRole(String userId) {
        userId = getUserId(userId)
        return wsHelper.rootPut("/users/${userId}/roles/${RoleName.ROLE_DASHBOARD_ADMIN_ACCESS.toString()}")
    }

    def removeGlobalBadgeAdminRole(String badgeId, String userId) {
        return wsHelper.globalBadgeDelete("/badges/${badgeId}/users/${userId}/roles/${RoleName.ROLE_GLOBAL_BADGE_ADMIN.toString()}")
    }

    def getUserRolesForGlobalBadge(String badgeId){
        wsHelper.globalBadgeGet("${getGlobalBadgeUrl(badgeId)}/userRoles")
    }

    def revokeInviteOnlyProjectAccess(String projectId, String userId) {
        return wsHelper.adminDelete("/projects/${projectId}/users/${userId}/roles/${RoleName.ROLE_PRIVATE_PROJECT_USER.toString()}")
    }

    def canAccessProject(String projectId, String userId) {
        return wsHelper.adminGet("/projects/${projectId}/${userId}/canAccess")
    }

    def addOrUpdateGlobalSetting(String setting, Map value) {
        return wsHelper.rootPut("/global/settings/${setting}", value)
    }

    def configuredProjectAsInviteOnly(String projectId){
        return changeSetting(projectId, "invite_only", [
                projectId: projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName,
                value: Boolean.TRUE.toString().toLowerCase()
        ])
    }
    def requestNewProjectInvite(String projectId) {
        return wsHelper.apiPost("/projects/${projectId}/newInviteRequest")
    }

    def changeSetting(String project, String setting, Map value){
        return wsHelper.adminPost(getSettingUrl(project, setting), value)
    }

    def changeSettings(String project, List<Map> settings){
        return wsHelper.adminPost(getSettingsUrl(project), settings)
    }

    def changeUserSettings(List<Map> settings){
        return wsHelper.appPost(getUserSettingsUrl(), settings)
    }

    def checkSettingsValidity(String project, List<Map> settings){
        return wsHelper.adminPost("${getProjectUrl(project)}/settings/checkValidity".toString(), settings)
    }

    def checkCustomDescriptionValidationWithQuizId(String description, String quizId,  Boolean useProtectedCommunityValidator = null){
        return checkCustomDescriptionValidation(description, null, useProtectedCommunityValidator, quizId)
    }

    def checkCustomDescriptionValidation(String description, String projectId = null, Boolean useProtectedCommunityValidator = null, String quizId = null){
        return wsHelper.apiPost("/validation/description", [value: description, projectId: projectId, useProtectedCommunityValidator: useProtectedCommunityValidator, quizId: quizId])
    }

    def checkCustomNameValidation(String description){
        return wsHelper.apiPost("/validation/name", [value: description])
    }

    def checkCustomUrlValidation(String url){
        return wsHelper.apiPost("/validation/url", [value: url])
    }

    def addProjectAdmin(String projectId, String userId) {
        userId = getUserId(userId)
        return wsHelper.adminPut(getAddProjectAdminUrl(projectId, userId))
    }

    boolean doesUserExist(String username, boolean validateCert=true) {
        username = getUserId(username, validateCert)
        return wsHelper.rootContextGet("/userExists/${username}")
    }

    boolean doesSubjectNameExist(String projectId, String subjectName) {
//        String encoded = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
        return wsHelper.adminPost("/projects/${projectId}/subjectNameExists", [name:subjectName])?.body
    }

    boolean doesBadgeNameExist(String projectId, String badgeName) {
        return wsHelper.adminPost("/projects/${projectId}/badgeNameExists",[name:badgeName])?.body
    }

    boolean doesSkillNameExist(String projectId, String skillName) {
        return wsHelper.adminPost("/projects/${projectId}/skillNameExists", [name:skillName])?.body
    }

    boolean doesEntityExist(String projectId, String id) {
        return wsHelper.adminGet("/projects/${projectId}/entityIdExists?id=${id}")
    }

    boolean isSkillReferencedByGlobalBadge(String projectId, String skillId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/${skillId}/globalBadge/exists".toString())
    }

    boolean isSubjectReferencedByGlobalBadge(String projectId, String subjectId) {
        return wsHelper.adminGet("/projects/${projectId}/subjects/${subjectId}/globalBadge/exists".toString())
    }

    boolean isProjectReferencedByGlobalBadge(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/globalBadge/exists")
    }

    boolean isProjectLevelReferencedByGlobalBadge(String projectId, Integer level) {
        return wsHelper.adminGet("/projects/${projectId}/levels/${level}/globalBadge/exists")
    }

    def reportClientVersion(String projectId, String version) {
        return wsHelper.apiPost("/projects/${projectId}/skillsClientVersion", [ skillsClientVersion: version ])
    }

    def requestPasswordReset(String userId) {
        return wsHelper.post("/resetPassword", "", ["userId", userId])
    }

    def saveAiPromptSettings(List<Map> settings) {
        return wsHelper.rootPost("/saveAiPromptSettings", settings)
    }

    def getDefaultAiPromptSetting(String setting) {
        return wsHelper.rootGet("/getDefaultAiPromptSettings/default/${setting}")
    }

    def saveEmailSettings(String host, String protocol, Integer port, boolean tlsEnabled, boolean authEnabled, String username, String password) {
//        username = getUserId(username)
        return wsHelper.rootPost("/saveEmailSettings", [
                host: host,
                protocol: protocol,
                port: port,
                tlsEnabled: tlsEnabled,
                authEnabled: authEnabled,
                username: username,
                password: password
        ])
    }

    def saveEmailHeaderAndFooterSettings(String htmlHeader, String htmlFooter, String plaintextHeader, String plaintextFooter) {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : htmlHeader
        ]
        addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        settingRequest.setting = EmailSettingsService.plaintextHeader
        settingRequest.value = plaintextHeader
        addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        settingRequest.setting = EmailSettingsService.htmlFooter
        settingRequest.value = htmlFooter
        addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        settingRequest.setting = EmailSettingsService.plaintextFooter
        settingRequest.value = plaintextFooter
        addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
    }

    def addOrUpdateUserSetting(String setting, String value) {
        Map params = [
                settingGroup: "user.prefs",
                setting: setting,
                value: value,
        ]
        return wsHelper.appPost("/userInfo/settings", [ params ])
    }

    def addOrUpdateProjectSetting(String projectId, String setting, String value) {
        Map params = [
                setting  : setting,
                value    : value,
                projectId: projectId,
        ]
        return wsHelper.adminPost("/projects/${projectId}/settings", [params])
    }

    def getProjectSettings(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/settings")
    }

    def getEmailSettings() {
        return wsHelper.rootGet('/getEmailSettings')
    }

    def saveSystemSettings(String resetTokenExpiration, String customHeader, String customFooter) {
        return wsHelper.rootPost('/saveSystemSettings',
                [resetTokenExpiration: resetTokenExpiration,
                 customHeader: customHeader,
                 customFooter: customFooter])
    }

    def getSystemSettings() {
        return wsHelper.rootGet('/getSystemSettings')
    }

    def isFeatureEnabled(String featureName) {
        return wsHelper.isFeatureEnabled(featureName)
    }

    def countProjectUsers(String projectId, Boolean allProjectUsers = false, List<String> achievedSkillIds = null, List<String> notAchievedSkillIds = null, List<Map<String,String>> subjectLevels = null, Integer projectLevel = null) {

        def params = [
                projectId: projectId,
                projectLevel: projectLevel,
                subjectLevels: subjectLevels,
                achievedSkillIds: achievedSkillIds,
                notAchievedSkillIds: notAchievedSkillIds,
                allProjectUsers: allProjectUsers
            ]

        return wsHelper.adminPost("/projects/${projectId}/contactUsersCount", params)?.body
    }

    def contactProjectUsers(String projectId, String emailSubject, String emailBody, Boolean allProjectUsers = false, List<String> achievedSkillIds = null, List<String> notAchievedSkillIds = null, List<Map<String,String>> subjectLevels = null, Integer projectLevel = null) {
        def params = [
                queryCriteria: [
                    projectId: projectId,
                    projectLevel: projectLevel,
                    subjectLevels: subjectLevels,
                    achievedSkillIds: achievedSkillIds,
                    notAchievedSkillIds: notAchievedSkillIds,
                    allProjectUsers: allProjectUsers
                ],
                emailBody: emailBody,
                emailSubject: emailSubject
            ]

        return wsHelper.adminPost("/projects/${projectId}/contactUsers", params)
    }

    def countAllProjectAdminsWithEmail() {
        return wsHelper.rootGet("/users/countAllProjectAdmins")
    }

    def contactAllProjectAdmins(String emailSubject, String emailBody) {
        return wsHelper.rootPost("/users/contactAllProjectAdmins", ["emailSubject":emailSubject,"emailBody":emailBody])
    }

    def previewEmail(String projectId, String emailSubject, String emailBody) {
        return wsHelper.adminPost("/projects/${projectId}/previewEmail", ["emailSubject": emailSubject, "emailBody": emailBody])
    }

    def previewEmail(String emailSubject, String emailBody) {
        return wsHelper.rootPost("/users/previewEmail", ["emailSubject": emailSubject, "emailBody": emailBody])
    }

    def lookupMyProjectName(String projectId) {
        return wsHelper.apiGet("/myprojects/${projectId}/name")
    }

    def getExportedSkillsForProjectStats(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/exported/stats")
    }

    def getImportedSkillsStats(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/imported/stats")
    }

    def getSkillsImportedFromCatalog(String projectId, int limit, int page, String orderBy, boolean ascending) {
        return wsHelper.adminGet("/projects/${projectId}/skills/imported?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def getExportedSkillStats(String projectId, String skillId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/${skillId}/exported/stats")
    }

    def getExportedSkills(String projectId, int limit, int page, String orderBy, boolean ascending){
        return wsHelper.adminGet("/projects/${projectId}/skills/exported?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def getCatalogSkills(String projectId, int limit, int page, String orderBy="exportedOn", boolean ascending=true, String projectNameSearch="", String subjectNameSearch="", String skillNameSearch=""){
        URLCodec codec = new URLCodec("UTF-8")
        return wsHelper.adminGet("/projects/${projectId}/skills/catalog?" +
                "limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}" +
                "&projectNameSearch=${codec.encode(projectNameSearch)}" +
                "&subjectNameSearch=${codec.encode(subjectNameSearch)}" +
                "&skillNameSearch=${codec.encode(skillNameSearch)}")
    }

    /**
     *
     * @param importIntoProjectId
     * @param importIntoSubjectId
     * @param catalogSkills - List of Maps with projectId and skillId attributes representing skills shared to the catalog
     */
    def bulkImportSkillsFromCatalog(String importIntoProjectId, String importIntoSubjectId, List<Map> catalogSkills) {
        return wsHelper.adminPost("/projects/${importIntoProjectId}/subjects/${importIntoSubjectId}/import", catalogSkills)
    }

    def bulkImportSkillsIntoGroupFromCatalog(String importIntoProjectId, String importIntoSubjectId, String importGroupId, List<Map> catalogSkills) {
        return wsHelper.adminPost("/projects/${importIntoProjectId}/subjects/${importIntoSubjectId}/groups/${importGroupId}/import", catalogSkills)
    }

    def bulkImportSkillsIntoGroupFromCatalogAndFinalize(String importIntoProjectId, String importIntoSubjectId, String importGroupId, List<Map> catalogSkills) {
        def res = bulkImportSkillsIntoGroupFromCatalog(importIntoProjectId, importIntoSubjectId, importGroupId, catalogSkills)
        finalizeSkillsImportFromCatalog(importIntoProjectId)
        return res
    }

    def bulkImportSkillsFromCatalogAndFinalize(String importIntoProjectId, String importIntoSubjectId, List<Map> catalogSkills) {
        def res = wsHelper.adminPost("/projects/${importIntoProjectId}/subjects/${importIntoSubjectId}/import", catalogSkills)
        finalizeSkillsImportFromCatalog(importIntoProjectId)
        return res
    }

    def updateImportedSkill(String projectId, String skillId, Integer pointIncrement) {
        return wsHelper.adminPatch("/projects/${projectId}/import/skills/${skillId}", [pointIncrement: pointIncrement])
    }

    def finalizeSkillsImportFromCatalog(String projectId, boolean waitForFinalizationToComplete = true) {
        def res = wsHelper.adminPost("/projects/${projectId}/catalog/finalize", [])
        if (waitForFinalizationToComplete) {
            waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        }
        return res
    }

    def getCatalogFinalizeInfo(String projectId) {
        def res = wsHelper.adminGet("/projects/${projectId}/catalog/finalize/info")
        return res
    }

    def importSkillFromCatalog(String importIntoProjectId, String importIntoSubjectId, String catalogSkillProjectId, String catalogSkillSkillId) {
        return this.bulkImportSkillsFromCatalog(importIntoProjectId, importIntoSubjectId, [[projectId: catalogSkillProjectId, skillId: catalogSkillSkillId]])
    }

    def importSkillFromCatalogAndFinalize(String importIntoProjectId, String importIntoSubjectId, String catalogSkillProjectId, String catalogSkillSkillId) {
        def res = this.bulkImportSkillsFromCatalog(importIntoProjectId, importIntoSubjectId, [[projectId: catalogSkillProjectId, skillId: catalogSkillSkillId]])
        finalizeSkillsImportFromCatalog(importIntoProjectId)
        return res
    }

    def reuseSkillInAnotherSubject(String projectId, String skillId, String otherSubjectId) {
        return reuseSkills(projectId, [skillId], otherSubjectId)
    }

    def reuseSkills(String projectId, List<String> skillIds, String otherSubjectId, String otherGroupId = null) {
        return wsHelper.adminPost("/projects/${projectId}/skills/reuse", [subjectId: otherSubjectId, skillIds: skillIds, groupId: otherGroupId])
    }

    def getReusedSkills(String projectId, String parentSkillId) {
        String url = "/projects/${projectId}/reused/${parentSkillId}/skills"
        return wsHelper.adminGet(url)
    }

    def getReuseDestinationsForASkill(String projectId, String skillId) {
        String url = "/projects/${projectId}/skills/${skillId}/reuse/destinations"
        return wsHelper.adminGet(url)
    }

    def moveSkills(String projectId, List<String> skillIds, String otherSubjectId, String otherGroupId = null) {
        return wsHelper.adminPost("/projects/${projectId}/skills/move", [subjectId: otherSubjectId, skillIds: skillIds, groupId: otherGroupId])
    }

    def bulkExportSkillsToCatalog(String projectId, List<String> skillIds) {
        return wsHelper.adminPost("/projects/${projectId}/skills/export", skillIds)
    }

    def exportSkillToCatalog(String projectId, String skillId) {
        return wsHelper.adminPost("/projects/${projectId}/skills/${skillId}/export", [:])
    }

    def removeSkillFromCatalog(String projectId, String skillId) {
        return wsHelper.adminDelete("/projects/${projectId}/skills/${skillId}/export")
    }

    def doesSkillExistInCatalog(String projectId, String skillId) {
        return wsHelper.adminPost("/projects/${projectId}/skills/catalog/exists/${skillId}", [:]).body
    }

    def doSkillsExistInCatalog(String projectId, List<String> skillIds) {
        return wsHelper.adminPost("/projects/${projectId}/skills/catalog/exist", skillIds).body
    }

    def areSkillIdsExportable(String projectId, List<String> skillIds) {
        return wsHelper.adminPost("/projects/${projectId}/skills/catalog/exportable", skillIds).body
    }

    def rebuildUserAndProjectPoints(String projectId) {
        return wsHelper.rootPost("/rebuildUserAndProjectPoints/${projectId}")
    }

    def getUserActionsForEverything(int limit = 10, int page = 1, String orderBy = "created", Boolean ascending = false,
                                    String projectIdFilter = '',
                                    DashboardItem itemFilter = null,
                                    String userFilter = '',
                                    String quizFilter = '',
                                    String itemIdFilter = '',
                                    DashboardAction actionFilter = null) {
        return wsHelper.rootGet("/dashboardActions?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}"
                + "&projectIdFilter=${projectIdFilter}&itemFilter=${itemFilter ?: ''}&userFilter=${userFilter}"
                + "&quizFilter=${quizFilter}&itemIdFilter=${itemIdFilter}&actionFilter=${actionFilter ?: ''}".toString())
    }

    def getUserActionsForProject(String projectId, int limit = 10, int page = 1, String orderBy = "created", Boolean ascending = false,
                                    DashboardItem itemFilter = null,
                                    String userFilter = '',
                                    String itemIdFilter = '',
                                    DashboardAction actionFilter = null) {
        return wsHelper.adminGet("/projects/${projectId}/dashboardActions?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}"
                + "&itemFilter=${itemFilter ?: ''}&userFilter=${userFilter}"
                + "&itemIdFilter=${itemIdFilter}&actionFilter=${actionFilter ?: ''}".toString())
    }

    def getUserActionsForQuiz(String quizId, int limit = 10, int page = 1, String orderBy = "created", Boolean ascending = false,
                              DashboardItem itemFilter = null,
                              String userFilter = '',
                              String itemIdFilter = '',
                              DashboardAction actionFilter = null) {
        return wsHelper.adminGet("/quiz-definitions/${quizId}/dashboardActions?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}"
                + "&itemFilter=${itemFilter ?: ''}&userFilter=${userFilter}"
                + "&itemIdFilter=${itemIdFilter}&actionFilter=${actionFilter ?: ''}".toString())
    }

    def getUserActionAttributes(Long actionId) {
        return wsHelper.rootGet("/dashboardActions/${actionId}/attributes".toString())
    }

    def getQuizUserActionAttributes(String quizId, Long actionId) {
        return wsHelper.adminGet("/quiz-definitions/${quizId}/dashboardActions/${actionId}/attributes".toString())
    }

    def getProjectUserActionAttributes(String projectId, Long actionId) {
        return wsHelper.adminGet("/projects/${projectId}/dashboardActions/${actionId}/attributes".toString())
    }

    def getUserActionFilterOptions() {
        return wsHelper.rootGet("/dashboardActions/filterOptions".toString())
    }

    def getUserActionFilterOptionsForQuiz(String quizId) {
        return wsHelper.adminGet("/quiz-definitions/${quizId}/dashboardActions/filterOptions".toString())
    }

    def getUserActionFilterOptionsForProject(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/dashboardActions/filterOptions".toString())
    }

    def unsubscribeFromSelfApprovalRequestEmails(String projectId) {
        return wsHelper.adminPost("/projects/${projectId}/approvalEmails/unsubscribe", [:])
    }

    def subscribeToSelfApprovalRequestEmails(String projectId) {
        return wsHelper.adminPost("/projects/${projectId}/approvalEmails/subscribe", [:])
    }

    def isSubscribedToSelfApprovalRequestEmails(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/approvalEmails/isSubscribed")
    }

    def addTagToSkills(String projectId, List<String> skillIds, String tagValue, String tagId=null) {
        return wsHelper.adminPost("/projects/${projectId}/skills/tag", [skillIds: skillIds, tagId: tagId ?: tagValue?.toLowerCase()?.replaceAll('[\\W_]', ''), tagValue: tagValue])
    }

    def getTagsForSkills(String projectId, List<String> skillIds) {
        return wsHelper.adminGet("/projects/${projectId}/skills/tags", [skillIds: skillIds])
    }

    def getTagsForProject(String projectId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/tags")
    }

    def deleteTagForSkills(String projectId, List<String> skillIds, String tagId) {
        return wsHelper.adminDelete("/projects/${projectId}/skills/tag", [skillIds: skillIds, tagId: tagId])
    }

    /**
     * Invites users to join invite only project by sending an email with a one time use
     * invitation code to each user in the specified request
     * @param projectId
     * @param invites - [ validityDuration: "", recipients: [] ]
     */
    def inviteUsersToProject(String projectId, Map invites) {
        def result = wsHelper.adminPost("/projects/${projectId}/invite", invites)
        def body = result.body
        return body
    }

    /**
     * Adds the currently authenticated user to the specified invite-only project
     * if the supplied invite token hasn't expired, hasn't already been used,
     * and is for the specified projectId
     *
     * @param projectId
     * @param inviteToken
     */
    def joinProject(String projectId, String inviteToken) {
       return wsHelper.appPost("/projects/${projectId}/join/${inviteToken}", null)
    }

    /**
     * Validates the specified project invite token, returns a response with valid: false if the invite token
     * does not exist, exists for a different project, is expired, or is for a different user
     * @param projectId
     * @param inviteToken
     */
    def validateInvite(String projectId, String inviteToken) {
        def resp = wsHelper.appGet("/projects/${projectId}/validateInvite/${inviteToken}")
        return resp
    }

    def contactProjectOwner(String projectId, String message) {
        def resp = wsHelper.apiPost("/projects/${projectId}/contact", ["message": message])
    }

    def getProjectDescription(String projectId) {
        def resp = wsHelper.appGet("/projects/${projectId}/description")
        return resp
    }

    def getSkillDescription(String projectId, String skillId) {
        def resp = wsHelper.apiGet("/projects/${projectId}/skills/${skillId}/description")
        return resp
    }

    def getPendingProjectInvites(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        def resp = wsHelper.adminGet("/projects/${projectId}/invites/status?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
        return resp
    }

    def deletePendingInvite(String projectId, String recipientEmail) {
        return wsHelper.adminDelete("/projects/${projectId}/invites/${recipientEmail}")
    }

    def remindUserOfPendingInvite(String projectId, String recipientEmail) {
       return wsHelper.adminPost("/projects/${projectId}/invites/${recipientEmail}/remind", [:])
    }

    def extendProjectInviteExpiration(String projectId, String recipientEmail, String isoDuration) {
        def body = [:]
        body.extensionDuration = isoDuration
        body.recipientEmail = recipientEmail
        return wsHelper.adminPost("/projects/${projectId}/invites/extend", body)
    }

    def getQuizDefs() {
        wsHelper.appGet("/quiz-definitions")
    }
    def getQuizDef(String quizId) {
        wsHelper.adminGet(getQuizDefUrl(quizId))
    }
    def countSkillsForQuiz(String quizId) {
        wsHelper.adminGet("${getQuizDefUrl(quizId)}/skills-count")
    }
    def getSkillsForQuiz(String quizId) {
        wsHelper.adminGet("${getQuizDefUrl(quizId)}/skills")
    }
    def getQuizDefSummary(String quizId) {
        wsHelper.adminGet("${getQuizDefUrl(quizId)}/summary")
    }
    def createQuizDef(Map props, String originalQuizId = null) {
        if (originalQuizId) {
            return wsHelper.adminPost(getQuizDefUrl(originalQuizId), props)
        }
        return wsHelper.appPost(getQuizDefUrl(props.quizId), props)
    }
    def removeQuizDef(String quizId) {
        wsHelper.adminDelete(getQuizDefUrl(quizId))
    }

    def reportQuizAttemptForUser(String userId, String quizId, def attemptInfo) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt"
        wsHelper.adminPost(url, attemptInfo)
    }

    def quizIdExist(String quizId) {
        wsHelper.appPost("/quizDefExist", [quizId: quizId])
    }
    def quizNameExist(String quizName) {
        wsHelper.appPost("/quizDefExist", [name: quizName])
    }
    def createQuizQuestionDef(Map props) {
        String url = "${getQuizDefUrl(props.quizId)}/create-question"
        return wsHelper.adminPost(url, props)
    }
    def updateQuizQuestionDef(Map props) {
        String url = "${getQuizDefUrl(props.quizId)}/questions/${props.id}"
        return wsHelper.adminPost(url, props)
    }
    def createQuizQuestionDefs(List<Map> questions) {
        questions.each {
            createQuizQuestionDef(it)
        }
    }
    def deleteQuizQuestionDef(String quizId, Integer questionRefId) {
        String url = "${getQuizDefUrl(quizId)}/questions/${questionRefId}"
        return wsHelper.adminDelete(url)
    }
    def copyQuiz(String quizId, Map props) {
         String url = "${getQuizDefUrl(quizId)}/copy"
         wsHelper.adminPost(url, props)
    }

    def changeQuizQuestionDisplayOrder(String quizId, Integer questionId, Integer newDisplayOrderIndex){
        assert quizId
        assert questionId
        String url = "${getQuizDefUrl(quizId)}/questions/${questionId}"
        wsHelper.adminPatch(url, [
                action: ActionPatchRequest.ActionType.NewDisplayOrderIndex.toString(),
                newDisplayOrderIndex: newDisplayOrderIndex,
        ]);
    }

    def addQuizUserRole(String quizId, String userId, String role) {
        String url = "${getQuizDefUrl(quizId)}/users/${userId}/roles/${role}"
        return wsHelper.adminPost(url, [])
    }

    def deleteQuizUserRole(String quizId, String userId, String role) {
        String url = "${getQuizDefUrl(quizId)}/users/${userId}/roles/${role}"
        return wsHelper.adminDelete(url)
    }

    def getQuizUserRoles(String quizId) {
        String url = "${getQuizDefUrl(quizId)}/userRoles"
        return wsHelper.adminGet(url)
    }

    def getQuizQuestionDefs(String quizId) {
        String url = "${getQuizDefUrl(quizId)}/questions"
        return wsHelper.adminGet(url)
    }

    def getQuizQuestionDef(String quizId, Integer questionId) {
        String url = "${getQuizDefUrl(quizId)}/questions/${questionId}"
        return wsHelper.adminGet(url)
    }

    def getQuizMetrics(String quizId, String startDate = defaultStart, String endDate = defaultEnd) {
        String url = "${getQuizDefUrl(quizId)}/metrics?startDate=${startDate}&endDate=${endDate}"
        return wsHelper.adminGet(url)
    }

    def saveQuizSettings(String quizId, List settings) {
        String url = "${getQuizDefUrl(quizId)}/settings"
        return wsHelper.adminPost(url, settings)
    }

    def getQuizSettings(String quizId) {
        String url = "${getQuizDefUrl(quizId)}/settings"
        return wsHelper.adminGet(url)
    }

    def saveQuizUserPreference(String quizId, String preferenceKey, Object value) {
        String url = "${getQuizDefUrl(quizId)}/preferences/${preferenceKey}"
        return wsHelper.adminPost(url, [value: value])
    }
    def getCurrentUserQuizPreferences(String quizId) {
        String url = "${getQuizDefUrl(quizId)}/preferences"
        return wsHelper.adminGet(url)
    }

    def getQuizInfo(String quizId, String userId = null) {
        String url = "/quizzes/${quizId}"
        return wsHelper.apiGet(url, userId ? [userId: userId] : null)
    }

    def getCurrentUserQuizAttempts(String quizNameQuery = '', int limit=10,
                                   int page=1,
                                   String orderBy='started',
                                   Boolean ascending=true) {
        return wsHelper.apiGet("/quizAttempts", [quizNameQuery: quizNameQuery, limit: limit, page: page, orderBy: orderBy, ascending: ascending])
    }

    def getCurrentUserSingleQuizAttempt(Integer quizAttempt) {
        return wsHelper.apiGet("/quizAttempts/${quizAttempt}")
    }


    def getQuizAttemptResult(String quizId, Integer attemptId) {
        String url = "${getQuizDefUrl(quizId)}/runs/${attemptId}"
        return wsHelper.adminGet(url)
    }

    String defaultStart = '1900-01-01 00:00:00'
    String defaultEnd = '2100-12-31 23:59:59'

    def getQuizRuns(String quizId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = "", String startDate = defaultStart, String endDate = defaultEnd) {
        String url = "${getQuizDefUrl(quizId)}/runs"
        return wsHelper.adminGet("${url}?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}&startDate=${startDate}&endDate=${endDate}".toString())
    }

    def getQuizUserTagCounts(String quizId, String userTagKey, String startDate = defaultStart, String endDate = defaultEnd) {
        String url = "${getQuizDefUrl(quizId)}/userTagCounts?userTagKey=${userTagKey}&startDate=${startDate}&endDate=${endDate}"
        return wsHelper.adminGet(url.toString())
    }

    def getQuizUsageOverTime(String quizId, String startDate = defaultStart, String endDate = defaultEnd) {
        String url = "${getQuizDefUrl(quizId)}/usageOverTime?startDate=${startDate}&endDate=${endDate}"
        return wsHelper.adminGet(url.toString())
    }

    def getUserQuizAnswers(String quizId, Integer answerDefId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true) {
        String url = "${getQuizDefUrl(quizId)}/answers/${answerDefId}/attempts"
        return wsHelper.adminGet("${url}?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}".toString())
    }

    def deleteQuizRun(String quizId, Integer quizAttemptId) {
        String url = "${getQuizDefUrl(quizId)}/runs/${quizAttemptId}"
        wsHelper.adminDelete(url)
    }

    @Deprecated
    def reportQuizAttempt(String quizId, def quizAttemptReq) {
        String url = "/quizzes/${quizId}/attempt"
        return wsHelper.apiPost(url, quizAttemptReq)
    }

    def startQuizAttempt(String quizId, String userId = null, Map additionalParams = null) {
        assert quizId
        String url = "/quizzes/${quizId}/attempt"
        def combinedParams = [:]
        if (userId) {
            combinedParams.userId = userId
        }
        if (additionalParams) {
            combinedParams.putAll(additionalParams)
        }

        return wsHelper.apiPost(url, combinedParams)
    }
    def reportQuizAnswer(String quizId, Integer attemptId, Integer answerId, Map params = [isSelected:true]) {
        String url = "/quizzes/${quizId}/attempt/${attemptId}/answers/${answerId}"
        return wsHelper.apiPost(url, params)
    }
    def completeQuizAttempt(String quizId, Integer attemptId, String userId = null) {
        String url = "/quizzes/${quizId}/attempt/${attemptId}/complete"
        return wsHelper.apiPost(url, userId ? [userId : userId] : null)
    }

    def gradeAnswer( String userId, String quizId,Integer attemptId, Integer answerDefId, Boolean isCorrect, String feedback = null) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/gradeAnswer/${answerDefId}"
        return wsHelper.adminPost(url, [isCorrect: isCorrect, feedback: feedback])
    }

    def failQuizAttempt(String quizId, Integer attemptId, String userId = null) {
        String url = "/quizzes/${quizId}/attempt/${attemptId}/fail"
        return wsHelper.apiPost(url, userId ? [userId : userId] : null)
    }

    def startQuizAttemptForUserId(String quizId, String userId, String skillId = null, String projectId = null) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt"
        return wsHelper.adminPost(url, [skillId: skillId, projectId: projectId])
    }
    def reportQuizAnswerForUserId(String quizId, Integer attemptId, Integer answerId, String userId, Map params = [isSelected:true]) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/answers/${answerId}"
        return wsHelper.adminPost(url, params)
    }
    def completeQuizAttemptForUserId(String quizId, Integer attemptId, String userId) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/complete"
        return wsHelper.adminPost(url, [])
    }
    def failQuizAttemptForUserId(String quizId, Integer attemptId, String userId) {
        String url = "/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/fail"
        return wsHelper.adminPost(url, [])
    }

    def saveSkillVideoAttributes(String quizOrProjectId, String questionOrSkillId, Map videoAttrs, Boolean isQuiz = false) {
        Map body = [:]
        if (videoAttrs.file) { body.put("file", videoAttrs.file) }
        if (videoAttrs.videoUrl) { body.put("videoUrl", videoAttrs.videoUrl)}
        if (videoAttrs.isAlreadyHosted != null) { body.put("isAlreadyHosted", videoAttrs.isAlreadyHosted)}
        if (videoAttrs.captions) { body.put("captions", videoAttrs.captions)}
        if (videoAttrs.transcript) { body.put("transcript", videoAttrs.transcript)}
        if (videoAttrs.height) { body.put("height", videoAttrs.height)}
        if (videoAttrs.width) { body.put("width", videoAttrs.width)}

        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/video"
        return wsHelper.adminUpload(url, body, true)
    }

    def saveQuizTextInputAiGraderConfigs(String quizId, Integer questionId, String correctAnswer, Integer minimumConfidenceLevel, Boolean enabled = true) {
        String url = "/quiz-definitions/${quizId}/questions/${questionId}/textInputAiGradingConf"
        return wsHelper.adminPost(url, [
                enabled: enabled,
                correctAnswer: correctAnswer,
                minimumConfidenceLevel: minimumConfidenceLevel,
        ])
    }

    def getQuizTextInputAiGraderConfigs(String quizId, Integer questionId) {
        String url = "/quiz-definitions/${quizId}/questions/${questionId}/textInputAiGradingConf"
        return wsHelper.adminGet(url)
    }

    def getSkillVideoAttributes(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/video"
        return wsHelper.adminGet(url)
    }
    def deleteSkillVideoAttributes(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/video"
        return wsHelper.adminDelete(url)
    }
    def getVideoCaptions(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/videoCaptions"
        return wsHelper.get(url, "api", null, false)
    }
    def getVideoTranscript(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/videoTranscript"
        return wsHelper.get(url, "api", null, false)
    }

    def saveSlidesAttributes(String quizOrProjectId, String questionOrSkillId, Map skillAttrs, Boolean isQuiz = false) {
        Map body = [:]
        if (skillAttrs.file) { body.put("file", skillAttrs.file) }
        if (skillAttrs.url) { body.put("url", skillAttrs.url)}
        if (skillAttrs.isAlreadyHosted != null) { body.put("isAlreadyHosted", skillAttrs.isAlreadyHosted)}
        if (skillAttrs.width != null) { body.put("width", skillAttrs.width)}

        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/slides"
        return wsHelper.adminUpload(url, body, true)
    }
    def getSlidesAttributes(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/slides"
        return wsHelper.adminGet(url)
    }
    def deleteSlidesAttributes(String quizOrProjectId, String questionOrSkillId, Boolean isQuiz = false) {
        String url = (isQuiz ? "/quiz-definitions" : "/projects") + "/${quizOrProjectId}/" + (isQuiz ? "questions" : "skills") + "/${questionOrSkillId}/slides"
        return wsHelper.adminDelete(url)
    }

    def saveQuizSlidesAttributes(String quizId, Map skillAttrs) {
        Map body = [:]
        if (skillAttrs.file) { body.put("file", skillAttrs.file) }
        if (skillAttrs.url) { body.put("url", skillAttrs.url)}
        if (skillAttrs.isAlreadyHosted != null) { body.put("isAlreadyHosted", skillAttrs.isAlreadyHosted)}
        if (skillAttrs.width != null) { body.put("width", skillAttrs.width)}

        String url = "/quiz-definitions/${quizId}/slides"
        return wsHelper.adminUpload(url, body, true)
    }
    def getQuizSlidesAttributes(String quizId) {
        String url = "/quiz-definitions/${quizId}/slides"
        return wsHelper.adminGet(url)
    }
    def deleteQuizSlidesAttributes(String quizId) {
        String url = "/quiz-definitions/${quizId}/slides"
        return wsHelper.adminDelete(url)
    }

    def saveSkillExpirationAttributes(String projectId, String skillId, Map expirationAttrs) {
        String url = "/projects/${projectId}/skills/${skillId}/expiration"
        return wsHelper.adminPost(url, expirationAttrs)
    }
    def getSkillExpirationAttributes(String projectId, String skillId) {
        String url = "/projects/${projectId}/skills/${skillId}/expiration"
        return wsHelper.adminGet(url)
    }
    def deleteSkillExpirationAttributes(String projectId, String skillId) {
        String url = "/projects/${projectId}/skills/${skillId}/expiration"
        return wsHelper.adminDelete(url)
    }
    def getAdminGroupDefs() {
        wsHelper.appGet("/admin-group-definitions")
    }
    def createAdminGroupDef(Map props) {
        return wsHelper.appPost(getAdminGroupDefUrl(props.adminGroupId), props)
    }
    def updateAdminGroupDef(Map props) {
        return wsHelper.adminPost(getAdminGroupDefUrl(props.adminGroupId), props)
    }
    def getAdminGroupDef(String adminGroupId) {
        wsHelper.adminGet(getAdminGroupDefUrl(adminGroupId))
    }
    def removeAdminGroupDef(String adminGroupId) {
        wsHelper.adminDelete(getAdminGroupDefUrl(adminGroupId))
    }
    def getAdminGroupMembers(String adminGroupId) {
        wsHelper.adminGet("${getAdminGroupDefUrl(adminGroupId)}/members")
    }
    def addAdminGroupOwner(String adminGroupId, String userId) {
        return wsHelper.adminPut("${getAdminGroupDefUrl(adminGroupId)}/users/${userId}/roles/${RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}")
    }
    def addAdminGroupMember(String adminGroupId, String userId) {
        return wsHelper.adminPut("${getAdminGroupDefUrl(adminGroupId)}/users/${userId}/roles/${RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}")
    }
    def deleteAdminGroupOwner(String adminGroupId, String userId) {
        return wsHelper.adminDelete("${getAdminGroupDefUrl(adminGroupId)}/users/${userId}/roles/${RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}")
    }
    def deleteAdminGroupMember(String adminGroupId, String userId) {
        return wsHelper.adminDelete("${getAdminGroupDefUrl(adminGroupId)}/users/${userId}/roles/${RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}")
    }
    def getAdminGroupQuizzesAndSurveys(String adminGroupId) {
        return wsHelper.adminGet("${getAdminGroupDefUrl(adminGroupId)}/quizzes")
    }
    def addQuizToAdminGroup(String adminGroupId, String quizId) {
        return wsHelper.adminPut("${getAdminGroupDefUrl(adminGroupId)}/quizzes/${quizId}")
    }
    def deleteQuizFromAdminGroup(String adminGroupId, String quizId) {
        return wsHelper.adminDelete("${getAdminGroupDefUrl(adminGroupId)}/quizzes/${quizId}")
    }
    def getAdminGroupGlobalBadges(String adminGroupId) {
        return wsHelper.adminGet("${getAdminGroupDefUrl(adminGroupId)}/badges")
    }
    def addGlobalBadgeToAdminGroup(String adminGroupId, String badgeId) {
        return wsHelper.adminPut("${getAdminGroupDefUrl(adminGroupId)}/badges/${badgeId}")
    }
    def deleteGlobalBadgeFromAdminGroup(String adminGroupId, String badgeId) {
        return wsHelper.adminDelete("${getAdminGroupDefUrl(adminGroupId)}/badges/${badgeId}")
    }
    def getAdminGroupProjects(String adminGroupId) {
        return wsHelper.adminGet("${getAdminGroupDefUrl(adminGroupId)}/projects")
    }
    def addProjectToAdminGroup(String adminGroupId, String projectId) {
        return wsHelper.adminPut("${getAdminGroupDefUrl(adminGroupId)}/projects/${projectId}")
    }
    def deleteProjectFromAdminGroup(String adminGroupId, String projectId) {
        return wsHelper.adminDelete("${getAdminGroupDefUrl(adminGroupId)}/projects/${projectId}")
    }
    def getAdminGroupsForProject(String projectId) {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/adminGroups".toString())
    }
    def getAdminGroupsForQuiz(String quizId) {
        return wsHelper.adminGet("${getQuizDefUrl(quizId)}/adminGroups".toString())
    }
    def getAdminGroupsForGlobalBadge(String globalBadgeId) {
        return wsHelper.adminGet("${getGlobalBadgeUrl(globalBadgeId)}/adminGroups".toString())
    }
    def archiveUsers(List<String> userIds, String projectId) {
        return wsHelper.adminPost("/projects/${projectId}/users/archive", [userIds: userIds])
    }
    def restoreArchivedUser(String userId, String projectId) {
        return wsHelper.adminPost("/projects/${projectId}/users/${userId}/restore", [:])
    }

    def reportPageVisit(PageVisitRequest pageVisitRequest) {
        return wsHelper.apiPost("/pageVisit", pageVisitRequest)
    }

    def getWebNotifications() {
        return wsHelper.apiGet("/webNotifications")
    }
    def dismissWebNotification(Integer notificationId) {
        return wsHelper.apiPost("/webNotifications/${notificationId}/dismiss".toString(), [])
    }
    def dismissAllWebNotifications() {
        return wsHelper.apiPost("/webNotifications/dismissAll", [])
    }
    def createWebNotificationAsRoot(Map props) {
        return wsHelper.rootPost("/webNotifications/create", props)
    }

    def getAiModels() {
        return wsHelper.openaiGet("/models")
    }

    def getAiPromptSettings() {
        return wsHelper.openaiGet("/getAiPromptSettings")
    }

    static private String getAdminGroupDefUrl(String adminGroupId) {
        return "/admin-group-definitions/${adminGroupId}".toString()
    }

    private static String getQuizDefUrl(String quizId) {
        return "/quiz-definitions/${quizId}".toString()
    }

    private static String getProjectUrl(String project) {
        return "/projects/${project}".toString()
    }

    private static String getSubjectUrl(String project, String subject) {
        return "${getProjectUrl(project)}/subjects/${subject}".toString()
    }

    private static String getSkillUrl(String project, String subject, String skill) {
        if (subject) {
            return "${getSubjectUrl(project, subject)}/skills/${skill}".toString()
        } else {
            return "${getProjectUrl(project)}/skills/${skill}".toString()
        }
    }

    private static String getSyncSkillPointsUrl(String project, String subject, String groupId) {
        return "${getSubjectUrl(project, subject)}/groups/${groupId}/skills".toString()
    }

    private static String getSkillEventUrl(String project, String skill) {
        // /projects/{projectId}/skills/{skillEventId}
        return "${getProjectUrl(project)}/skills/${skill}".toString()
    }

    private static String getBadgeUrl(String project, String badge) {
        return "${getProjectUrl(project)}/badges/${badge}".toString()
    }

    private static String getGlobalBadgeUrl(String badge) {
        return "/badges/${badge}".toString()
    }

    private static String getAddSkillToSkillsGroupUrl(String project, String subjectId, String groupId, String skillId) {
        return "${getProjectUrl(project)}/subjects/${subjectId}/groups/${groupId}/skills/${skillId}".toString()
    }

    private static String getAddSkillToBadgeUrl(String project, String badge, String skillId) {
        return "${getProjectUrl(project)}/badge/${badge}/skills/${skillId}".toString()
    }

    private static String getAddSkillsToBadgeUrl(String project, String badge) {
        return "${getProjectUrl(project)}/badge/${badge}/skills/add".toString()
    }

    private static String getAddSkillToGlobalBadgeUrl(String badge, String project, String skillId) {
        return "/badges/${badge}/projects/${project}/skills/${skillId}".toString()
    }

    private static String getAddProjectLevelToGlobalBadgeUrl(String badge, String project, String level) {
        return "/badges/${badge}/projects/${project}/level/${level}".toString()
    }

    private static String getchangeProjectLevelOnGlobalBadgeUrl(String badge, String project, String currentLevel, String newLevel) {
        return "/badges/${badge}/projects/${project}/level/${currentLevel}/${newLevel}"
    }

    private static String getSaveIconUrl(String project){
        return "/icons/upload/${project}"
    }

    private static String getLevelsUrl(String project, String subject){
        if(subject){
            return "${getSubjectUrl(project, subject)}/levels".toString()
        }else {
            return "${getProjectUrl(project)}/levels".toString()
        }
    }


    private static String getUserLevelForProjectUrl(String project, String userId){
        if (userId) {
            return "${getProjectUrl(project)}/level?userId=${userId}".toString()
        } else {
            return "${getProjectUrl(project)}/level".toString()
        }
    }

    private static String getDeleteLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/last".toString()
    }

    private static String getDeleteLevelUrl(String project){
        return "${getLevelsUrl(project ,'')}/last".toString()
    }

    private static String getAddLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/next".toString()
    }

    private static String getEditLevelUrl(String project, String subject, String level){
        return "${getLevelsUrl(project, subject)}/edit/${level}".toString()
    }

    private static String getSettingsUrl(String project){
        return "${getProjectUrl(project)}/settings"
    }

    private static String getSettingUrl(String project, String setting){
        return "${getProjectUrl(project)}/settings/${setting}"
    }

    private static String getUserSettingsUrl(){
        return "/userInfo/settings"
    }

    private static String getAddProjectAdminUrl(String project, String userId) {
        return "/projects/${project}/users/${userId}/roles/ROLE_PROJECT_ADMIN"
    }

    String getUserId(String userId, boolean validateCert=true) {
        if (!userId ) {
            return userId
        }

        if (!certificateRegistry) {
            return userId
        } else {
            Resource cert = certificateRegistry.getCertificate(userId)
            if (validateCert) {
                assert cert, "no certificate found for ${userId}"
            }
            String dn = certificateRegistry.loadDNFromCert(cert)
            return dn ?: userId
        }
    }

    String getUsername(String userId) {
        // any specs that interact directly with the database as opposed to going through this test service layer
        // or interact with endpoints that internally don't lookup the provided username against the user-info-service when in pki mode
        // need to ensure that the userId is in the same format and has the same value as the username returned by the mock user info service
        if (!certificateRegistry) {
            return userId
        } else {
            return DnUsernameHelper.getUsername(getUserId(userId))
        }
    }

    static class UseParams {
        static String DEFAULT_USER_NAME = "skills@skills.org";

        String username = DEFAULT_USER_NAME
        String password = "p@ssw0rd"

        String firstName = 'Skills'
        String lastName = 'Test'
        String email = null
    }

}

