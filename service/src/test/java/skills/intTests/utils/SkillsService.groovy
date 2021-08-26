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
import com.github.jknack.handlebars.Options
import groovy.util.logging.Slf4j
@Slf4j
class SkillsService {

    WSHelper wsHelper

    CertificateRegistry certificateRegistry = null
    Options handlebarOptions = null

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
        handlebarOptions = new Options.Builder(null, null, null, null, null).build()
        wsHelper = new WSHelper(username: username, password: password, skillsService: service, firstName: firstName, lastName: lastName, certificateRegistry: certificateRegistry).init(certificateRegistry != null)
    }

    String getClientSecret(String projectId){
        wsHelper.get("/projects/${projectId}/clientSecret", "admin", null, false)
    }

    String resetClientSecret(String projectId){
        wsHelper.adminPost("/projects/${projectId}/resetClientSecret".toString(), null)
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
        createProject(projProps)
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
        wsHelper.supervisorPatch(getGlobalBadgeUrl(props.badgeId), [
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

    def deleteSpecificProjectError(String projectId, String errorType, String error) {
        wsHelper.adminDelete("/projects/${projectId}/errors/${errorType}/${error}")
    }

    def deleteUserRole(String userId, String projectId, String role) {
//        userId = getUserId(userId)
        wsHelper.adminDelete("/projects/${projectId}/users/${userId}/roles/${role}")
    }

    def getUserRolesForProject(String projectId) {
        wsHelper.adminGet("/projects/${projectId}/userRoles")
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
    def updateSubject(Map props, String oritinalSubjectId) {
        wsHelper.adminPost(getSubjectUrl(props.projectId, oritinalSubjectId), props)
    }

    def getSubjects(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId) + "/subjects")
    }

    def subjectNameExists(Map props){
        wsHelper.adminPost("/projects/${props.projectId}/subjectNameExists", [name:props.subjectName])
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

    def updateSkill(Map props, String originalSkillId) {
        wsHelper.adminPost(getSkillUrl(props.projectId, props.subjectId, originalSkillId ?: props.skillId), props)
    }

    def createBadge(Map props, String originalBadgeId = null) {
        wsHelper.adminPost(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def createGlobalBadge(Map props, String originalBadgeId = null) {
        wsHelper.supervisorPut(getGlobalBadgeUrl(originalBadgeId ?: props.badgeId), props)
    }
    def updateBadge(Map props, String originalBadgeId) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def assignDependency(Map props) {
        String url = getSkillDependencyUrl(props)
        wsHelper.adminPost(url, props, false)
    }

    def removeDependency(Map props) {
        String url = getSkillDependencyUrl(props)
        wsHelper.adminDelete(url, props)
    }

    String getSkillDependencyUrl(Map props) {
        String url
        if(props.dependentProjectId){
            url = "/projects/${props.projectId}/skills/${props.skillId}/dependency/projects/${props.dependentProjectId}/skills/${props.dependentSkillId}"
        } else {
            url = "/projects/${props.projectId}/skills/${props.skillId}/dependency/${props.dependentSkillId}"
        }
        return url
    }

    def deleteSkill(Map props) {
        wsHelper.adminDelete(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }
    def deleteSkillEvent(Map props) {
        String url = "/projects/${props.projectId}/skills/${props.skillId}/users/${props.userId}/events/${props.timestamp}"
        wsHelper.adminDelete(url, props)
    }

    def getSkill(Map props) {
        wsHelper.adminGet(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }

    def getSkillsForProject(String projectId) {
        wsHelper.adminGet("/projects/${projectId}/skills")
    }

    def getSkillsForSubject(String projectId, String subjectId) {
        wsHelper.adminGet("/projects/${projectId}/subjects/${subjectId}/skills")
    }

    def getSubject(Map props) {
        wsHelper.adminGet("/projects/${props.projectId}/subjects/${props.subjectId}")
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

    def removeBadge(Map props) {
        wsHelper.adminDelete("/projects/${props.projectId}/badges/${props.badgeId}")
    }

    def getAvailableProjectsForGlobalBadge(String badgeId) {
        wsHelper.supervisorGet("${getGlobalBadgeUrl(badgeId)}/projects/available")
    }

    def getAvailableSkillsForGlobalBadge(String badgeId, String query) {
        wsHelper.supervisorGet("${getGlobalBadgeUrl(badgeId)}/skills/available?query=${query}")
    }

    def getGlobalBadge(String badgeId) {
        wsHelper.supervisorGet(getGlobalBadgeUrl(badgeId))
    }

    def getGlobalBadgeSkills(String badgeId) {
        wsHelper.supervisorGet("/badges/${badgeId}/skills")
    }

    def getLevelsForProject(String projectId) {
        wsHelper.supervisorGet("/projects/${projectId}/levels")
    }

    def getServiceStatus() {
        wsHelper.get("/status", "public", null)
    }

    def getServiceIsAlive() {
        wsHelper.get("/isAlive", "public", null)
    }

    def getAllGlobalBadges() {
        wsHelper.supervisorGet("/badges")
    }

    def doesGlobalBadgeNameExists(String name) {
        wsHelper.supervisorPost("/badges/name/exists", [name:name])?.body
    }
    def doesGlobalBadgeIdExists(String id) {
        wsHelper.supervisorGet("/badges/id/${id}/exists")
    }

    def deleteGlobalBadge(String badgeId) {
        wsHelper.supervisorDelete(getGlobalBadgeUrl(badgeId))
    }

    @Profile
    def addSkill(Map props, String userId = null, Date date = new Date(), String approvalRequestedMsg = null) {
        if (userId) {
            userId = getUserId(userId)
            assert date
            return wsHelper.apiPost("/projects/${props.projectId}/skills/${props.skillId}", [ userId : userId, timestamp:date.time, approvalRequestedMsg: approvalRequestedMsg])
        } else {
            return wsHelper.apiPut("/projects/${props.projectId}/skills/${props.skillId}", null)
        }
    }

    def getApprovals(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        return wsHelper.adminGet("/projects/${projectId}/approvals?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}")
    }

    def getApprovalsHistory(String projectId, int limit, int page, String orderBy, Boolean ascending,  String skillNameFilter = '', String userIdFilter ='', String approverUserIdFilter = '') {
        return wsHelper.adminGet("/projects/${projectId}/approvals/history?limit=${limit}&page=${page}&orderBy=${orderBy}&ascending=${ascending}&" +
                "skillNameFilter=${skillNameFilter}&userIdFilter=${userIdFilter}&approverUserIdFilter=${approverUserIdFilter}")
    }


    def approve(String projectId, List<Integer> approvalId) {
        return wsHelper.adminPost("/projects/${projectId}/approvals/approve", [skillApprovalIds: approvalId])
    }

    def rejectSkillApprovals(String projectId, List<Integer> approvalId, String msg = null) {
        return wsHelper.adminPost("/projects/${projectId}/approvals/reject", [skillApprovalIds: approvalId, rejectionMessage: msg])
    }

    def getSkillApprovalsStats(String projectId, String skillId) {
        return wsHelper.adminGet("/projects/${projectId}/skills/${skillId}/approvals/stats")
    }

    def removeApproval(String projectId, Integer approvalId) {
        wsHelper.apiDelete("/projects/${projectId}/rejections/${approvalId}")
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

    def removeSkillFromBadge(Map props) {
        wsHelper.adminDelete(getAddSkillToBadgeUrl(props.projectId, props.badgeId, props.skillId), props)
    }

    def assignSkillToGlobalBadge(Map props) {
        wsHelper.supervisorPost(getAddSkillToGlobalBadgeUrl(props.badgeId, props.projectId, props.skillId), props)
    }

    def removeSkillFromGlobalBadge(Map props) {
        wsHelper.supervisorDelete(getAddSkillToGlobalBadgeUrl(props.badgeId, props.projectId, props.skillId), props)
    }

    def assignProjectLevelToGlobalBadge(Map props) {
        wsHelper.supervisorPost(getAddProjectLevelToGlobalBadgeUrl(props.badgeId, props.projectId, props.level), props)
    }

    def removeProjectLevelFromGlobalBadge(Map props) {
        wsHelper.supervisorDelete(getAddProjectLevelToGlobalBadgeUrl(props.badgeId, props.projectId, props.level), props)
    }

    def suggestDashboardUsers(String query) {
        String url = "/users/suggestDashboardUsers/"
        wsHelper.appPost(url, [suggestQuery: query])?.body
    }

    def suggestClientUsersForProject(String projectId, String query){
        String url = "/users/projects/${projectId}/suggestClientUsers/".toString()
        wsHelper.appPost(url, [suggestQuery: query])?.body
    }

    def suggestClientUsers(String query){
        String url = "/users/suggestClientUsers/".toString()
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


    def getCustomClientDisplayCss(String projectId = null){
        String url = projectId ? "/projects/${projectId}/customIconCss" : "/icons/customIconCss"
        wsHelper.get(url.toString(), "api", null, false)
    }

    def getSkillSummary(String userId, String projId, String subjId=null, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/${subjId ? "subjects/${subjId}/" : ''}summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSkillsSummaryForCurrentUser(String projId, int version = -1) {
        String url = "/projects/${projId}/summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getMyProgressSummary() {
        String url = "/myProgressSummary"
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

    def moveMyProject(String projectId, Integer newSortIndex) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiPost(url, [newSortIndex: newSortIndex])
    }

    def removeMyProject(String projectId) {
        String url = "/myprojects/${projectId}"
        wsHelper.apiDelete(url)
    }

    def getDependencyGraph(String projId, String skillId=null) {
        String url = skillId ? "/projects/${projId}/skills/${skillId}/dependency/graph" : "/projects/${projId}/dependency/graph"
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

    def getCrossProjectSkillSummary(String userId, String projId, String otherProjectId, String skillId, int version = -1) {
        userId = getUserId(userId)
        String url = "/projects/${projId}/projects/${otherProjectId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getBadgesSummary(String userId, String projId){
        userId = getUserId(userId)
        String url = "/projects/${projId}/badges/summary?userId=${userId}"
        wsHelper.apiGet(url)
    }

    def getBadgeSummary(String userId, String projId, String badgeId, int version = -1, boolean global = false){
        userId = getUserId(userId)
        String url = "/projects/${projId}/badges/${badgeId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        if (global) {
            url += "&global=${global}"
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

    def getSkillsAvailableForDependency(String projId) {
        String url = "/projects/${projId}/dependency/availableSkills"
        wsHelper.adminGet(url)
    }

    def uploadIcon(Map props, File icon){
        Map body = [:]
        body.put("customIcon", icon)
        wsHelper.adminUpload("/projects/${props.projectId}/icons/upload", body)
    }
    def uploadGlobalIcon(File icon){
        Map body = [:]
        body.put("customIcon", icon)
        wsHelper.supervisorUpload("/icons/upload", body)
    }

    def deleteIcon(Map props){
        wsHelper.adminDelete("/projects/${props.projectId}/icons/${props.filename}")
    }

    def deleteGlobalIcon(Map props){
        wsHelper.supervisorDelete("/icons/${props.filename}")
    }

    def getIconCssForProject(Map props){
        wsHelper.appGet("/projects/${props.projectId}/customIcons")
    }

    def getIconCssForGlobalIcons(){
        wsHelper.supervisorGet("/icons/customIcons")
    }

    def getPerformedSkills(String userId, String project, String query = '') {
        userId = getUsername(userId)
        return wsHelper.adminGet("${getProjectUrl(project)}/performedSkills/${userId}?query=${query}&limit=10&ascending=0&page=1&byColumn=0&orderBy=performedOn".toString())
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
        endpoint = "${endpoint}?userId=${userId}"
        return wsHelper.apiGet(endpoint)
    }

    def getLeaderboard(String userId, String projectId, String subjectId = null, String type="topTen"){
        userId = getUserId(userId)
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/leaderboard" : "/projects/${projectId}/leaderboard"
        endpoint = "${endpoint}?type=${type}&userId=${userId}"
        return wsHelper.apiGet(endpoint)
    }

    def getRankDistribution(String userId, String projectId, String subjectId = null){
        userId = getUserId(userId)
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rankDistribution" : "/projects/${projectId}/rankDistribution"
        endpoint = "${endpoint}?userId=${userId}"
        return wsHelper.apiGet(endpoint)
    }

    def getPointHistory(String userId, String projectId, String subjectId=null, Integer version = -1){
        userId = getUserId(userId)
        String endpointStart = subjectId ? getSubjectUrl(projectId, subjectId) : getProjectUrl(projectId)
        String url = "${endpointStart}/pointHistory?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        return wsHelper.apiGet(url.toString())
    }

    def getProjectUsers(String projectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = "") {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getSubjectUsers(String projectId, String subjectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getSubjectUrl(projectId, subjectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getUserStats(String projectId, String userId) {
//        userId = getUserIdEncoded(userId)
        return wsHelper.adminGet("/projects/${projectId}/users/${userId}/stats".toString())
    }

    def getSkillUsers(String projectId, String skillId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getSkillUrl(projectId, null, skillId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getBadgeUsers(String projectId, String badgeId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getBadgeUrl(projectId, badgeId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
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
        wsHelper.supervisorGet(endpoint, props)
    }

    def getApiGlobalMetricsData(String metricsId, Map props = null) {
        String endpoint = "/metrics/${metricsId}"
        wsHelper.apiGet(endpoint, props)
    }

    def getAllMetricsChartsForSection(String projectId, String section, String sectionId, Map props=null) {
        String endpoint = "/projects/${projectId}/${section}/${sectionId}/metrics/"
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
        return wsHelper.rootPost("/users/", [suggestQuery: query])?.body
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

    def pinProject(String projectId) {
        return wsHelper.rootPost("/pin/${projectId}")
    }

    def unpinProject(String projecctId) {
        return wsHelper.rootDelete("/pin/${projectId}")
    }

    def getUsersWithoutRole(String role, String usernameQuery) {
        String url = "/users/without/role/${role}/"
        return wsHelper.rootPost(url, [suggestQuery: usernameQuery])?.body
    }

    def getUsersWithRole(String role) {
        return wsHelper.rootGet("/users/roles/${role}")
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

    def grantSupervisorRole(String userId) {
        userId = getUserId(userId)
        return wsHelper.rootPut("/users/${userId}/roles/ROLE_SUPERVISOR")
    }

    def grantRootRole(String userId) {
        userId = getUserId(userId)
        return wsHelper.rootPut("/users/${userId}/roles/ROLE_SUPER_DUPER_USER")
    }

    def removeSupervisorRole(String userId) {
        userId = getUserId(userId)
        return wsHelper.rootDelete("/users/${userId}/roles/ROLE_SUPERVISOR")
    }

    def addOrUpdateGlobalSetting(String setting, Map value) {
        return wsHelper.rootPut("/global/settings/${setting}", value)
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

    def checkCustomDescriptionValidation(String description){
        return wsHelper.apiPost("/validation/description", [value: description])
    }

    def checkCustomNameValidation(String description){
        return wsHelper.apiPost("/validation/name", [value: description])
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
                setting: setting,
                value: value,
                projectId: projectId,
        ]
        return wsHelper.adminPost("/projects/${projectId}/settings", [ params ])
    }

    def getEmailSettings() {
        return wsHelper.rootGet('/getEmailSettings')
    }

    def saveSystemSettings(String publicUrl, String resetTokenExpiration, String fromEmail, String customHeader, String customFooter) {
        return wsHelper.rootPost('/saveSystemSettings',
                [publicUrl: publicUrl,
                 resetTokenExpiration: resetTokenExpiration,
                 fromEmail: fromEmail,
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

    private String getProjectUrl(String project) {
        return "/projects/${project}".toString()
    }

    private String getSubjectUrl(String project, String subject) {
        return "${getProjectUrl(project)}/subjects/${subject}".toString()
    }

    private String getSkillUrl(String project, String subject, String skill) {
        if (subject) {
            return "${getSubjectUrl(project, subject)}/skills/${skill}".toString()
        } else {
            return "${getProjectUrl(project)}/skills/${skill}".toString()
        }
    }

    private String getSkillEventUrl(String project, String skill) {
        // /projects/{projectId}/skills/{skillEventId}
        return "${getProjectUrl(project)}/skills/${skill}".toString()
    }

    private String getBadgeUrl(String project, String badge) {
        return "${getProjectUrl(project)}/badges/${badge}".toString()
    }

    private String getGlobalBadgeUrl(String badge) {
        return "/badges/${badge}".toString()
    }

    private String getAddSkillToBadgeUrl(String project, String badge, String skillId) {
        return "${getProjectUrl(project)}/badge/${badge}/skills/${skillId}".toString()
    }

    private String getAddSkillToGlobalBadgeUrl(String badge, String project, String skillId) {
        return "/badges/${badge}/projects/${project}/skills/${skillId}".toString()
    }

    private String getAddProjectLevelToGlobalBadgeUrl(String badge, String project, String level) {
        return "/badges/${badge}/projects/${project}/level/${level}".toString()
    }

    private String getSaveIconUrl(String project){
        return "/icons/upload/${project}"
    }

    private String getLevelsUrl(String project, String subject){
        if(subject){
            return "${getSubjectUrl(project, subject)}/levels".toString()
        }else {
            return "${getProjectUrl(project)}/levels".toString()
        }
    }


    private String getUserLevelForProjectUrl(String project, String userId){
        return "${getProjectUrl(project)}/level?userId=${userId}".toString()
    }

    private String getDeleteLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/last".toString()
    }

    private String getDeleteLevelUrl(String project){
        return "${getLevelsUrl(project ,'')}/last".toString()
    }

    private String getAddLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/next".toString()
    }

    private String getEditLevelUrl(String project, String subject, String level){
        return "${getLevelsUrl(project, subject)}/edit/${level}".toString()
    }

    private String getSettingsUrl(String project){
        return "${getProjectUrl(project)}/settings"
    }

    private String getSettingUrl(String project, String setting){
        return "${getProjectUrl(project)}/settings/${setting}"
    }

    private String getUserSettingsUrl(){
        return "/userInfo/settings"
    }

    private String getAddProjectAdminUrl(String project, String userId) {
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
//            dn = StringHelpers.slugify.apply(dn, handlebarOptions)
            return dn
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

}

import org.springframework.core.io.Resource
import skills.services.settings.Settings
