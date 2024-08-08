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
package skills.services.admin

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.ocpsoft.prettytime.PrettyTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.auth.*
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.*
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.AccessSettingsStorageService
import skills.services.FeatureService
import skills.services.ProjectInvite
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Notification
import skills.storage.model.ProjDef
import skills.storage.model.ProjectAccessToken
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.ProjectAccessTokenRepo
import skills.storage.repos.UserAttrsRepo
import skills.utils.Expiration
import skills.utils.ExpirationUtils
import skills.utils.PatternsUtil

import java.util.regex.Pattern

@Slf4j
@Component
class InviteOnlyProjectService {
    private static final String DEFAULT_DURATION = "PT24H"

    private static final int MAX_GENERATION_ATTEMPTS = 5

    @Value('#{"${skills.authorization.invite.validateEmail:false}"}')
    Boolean validateInviteEmail

    @Value('#{"${skills.config.ui.rankingAndProgressViewsEnabled}"}')
    Boolean progressAndRankingEnabled

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    ProjectAccessTokenRepo projectAccessTokenRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    InviteCodeGenerator codeGenerator

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    FeatureService featureService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    EmailNotifier notifier

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    /**
     * Generates an invite token for a specific project
     *
     * @param projectId - the projectId for which this invite token is valid
     * @param email - the email recipient for this specific invite token
     * @param created - defaults to current date, allows the same create date to be use for bulk invites
     * @param validDuration duration iso 8601 duration for how long the invite token will be valid, defaults to PT24H if not provided
     * @return
     * @throws skills.controller.exceptions.SkillException if the specified projectId does not exist or the project is not configured for invite only
     */
    @Transactional
    ProjectInvite generateProjectInviteToken(String projectId, String email, Date created=new Date(), String validDuration=DEFAULT_DURATION) {
        boolean enabled = featureService.isEmailServiceFeatureEnabled()
        SkillsValidator.isTrue(enabled, "Project Invites can only be used if email has been configured for this instance", projectId)

        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        SkillsValidator.isTrue(projDef != null, "Project does not exist", projectId)
        SkillsValidator.isTrue(isInviteOnlyProject(projDef.projectId), 'Project must be configured as Invite Only to generate an invite token', projectId)

        String code = generateCode()
        if (!code) {
            throw new SkillException("Unable to generate unique project invite token", projectId)
        }

        Expiration expiration = ExpirationUtils.getExpiration(validDuration)
        String currentUser = userInfoService.getCurrentUserId()

        try {
            ProjectAccessToken accessToken = new ProjectAccessToken()
            accessToken.token = code
            accessToken.project = projDef
            accessToken.expires = expiration.expiresOn
            accessToken.created = created
            accessToken.recipientEmail = email?.toLowerCase()
            accessToken = projectAccessTokenRepo.save(accessToken)

            ProjectInvite invite = new ProjectInvite()
            invite.projectId = projDef.projectId
            invite.projectName = projDef.name
            invite.validFor = expiration.validFor
            invite.token = accessToken.token
            invite.recipientEmail = email?.toLowerCase()

            log.info("user [{}] has generated invite token [{}] for project [{}] for recipient [{}]", currentUser, code, projectId, email)
            return invite
        } catch (DataIntegrityViolationException ex) {
            throw new SkillException("Project Invite already exists for [${email}]", projectId, null, ErrorCode.ProjectInviteAlreadyExists)
        }
    }

    /**
     * Validates if the supplied token is valid
     * @param token
     * @param projectId
     */
    @Transactional(readOnly = true)
    InviteTokenValidationResponse validateInvite(String token, String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(token, "inviteToken")

        ProjectAccessToken projectAccessToken = projectAccessTokenRepo.findByTokenAndProjectId(token, projectId)
        InviteTokenValidationResponse response = new InviteTokenValidationResponse()
        response.projectId = projectId
        if (!projectAccessToken) {
            log.debug("requested token [{}] for projectId [{}] does not exist", token, projectId)
            response.valid = false
            response.message = "Invalid Project Invite"
        } else if(!projectAccessToken.isValid()) {
            log.debug("token [{}] for projectId [{}] with expiration date [{}] is not valid", token, projectId, projectAccessToken.expires)
            response.valid = false
            response.message = "Project Invite has expired"
        } else if(validateInviteEmail && !StringUtils.equalsIgnoreCase(userInfoService.getCurrentUser()?.email, projectAccessToken.recipientEmail)) {
            log.debug("currentUser with email [{}] does not match email [{}] that token was generated with", userInfoService.getCurrentUser()?.email, projectAccessToken.recipientEmail)
            response.valid = false
            response.message = "Project Invite is for a different user"
        } else {
            if (log.isDebugEnabled()) {
                log.debug("token [{}] for projectId [{}] with recipient email [{}] and expiration [{}] is valid for user [{}]", token, projectId, projectAccessToken.recipientEmail, projectAccessToken.expires, userInfoService.getCurrentUser()?.email)
            }
            response.valid = true
        }

        return response
    }

    /**
     * If the supplied token is valid, creates an access role for the specified project for
     * the currently authenticated user. Adds the project to the user's My Projects view if
     * progress and ranking views are enabled.
     *
     * Access token is deleted once user role has been added
     *
     * @param token
     * @param projectId
     * @throws SkillException if the supplied token does not exist or has expired
     */
    @Transactional
    void joinProject(String token, String projectId) {
        UserInfo userInfo = userInfoService.getCurrentUser()
        String userId = userInfo?.getUsername()

        ProjectAccessToken projectAccessToken = projectAccessTokenRepo.findByTokenAndProjectId(token, projectId)
        if (!projectAccessToken) {
            log.warn("user [{}] has attempted to use invite code [{}] for project [{}], this code does not exist", userId, token, projectId)
            throw new SkillException("Invitation Code does not exist for Project", projectId, null, ErrorCode.InvalidInvitationCode)
        }

        if (projectAccessToken.expires.before(new Date())) {
            log.warn("user [{}] has attempted to use expired invite code [{}] for project [{}]", userId, token, projectId)
            throw new SkillException("Invitation Code has expired", projectId, null, ErrorCode.ExpiredInvitationCode)
        }

        if (projectAccessToken.claimed != null) {
            log.warn("user [{}] has attempted to use already claimed invite code [{}] for project [{}]", userId, token, projectId)
            throw new SkillException("Invitation Code has already been used", projectId, null, ErrorCode.ClaimedInvitationCode)
        }

        if (validateInviteEmail && !StringUtils.equalsIgnoreCase(userInfo?.email, projectAccessToken.recipientEmail)) {
            log.warn("User [{}] with email [{}] is trying to use an invite code that was sent to [{}]", userId, userInfo.email, projectAccessToken.recipientEmail)
            throw new SkillException("Invitation Code is for a different user", projectId, null, ErrorCode.NotYourInvitationCode)
        }

        if (userInfo?.authorities?.find {it instanceof UserSkillsGrantedAuthority
                && it.getRole().roleName == RoleName.ROLE_PRIVATE_PROJECT_USER && it.getRole().projectId == projectId }) {
            log.info("user [{}] has previously joined project [{}], ignoring new join request", userId, projectId)
            return
        }

        UserRole newRole = accessSettingsStorageService.addUserRoleReturnRaw(userId, projectId, RoleName.ROLE_PRIVATE_PROJECT_USER)

        if (progressAndRankingEnabled) {
            projAdminService.addMyProject(projectId, null)
        }

        projectAccessToken.claimed = new Date()

        log.info("user [{}] has claimed project invite code [{}] for project [{}]", userId, token, projectId)

        projectAccessTokenRepo.deleteByToken(projectAccessToken.token)
        GrantedAuthoritiesUpdater.addUserRoleToCurrentUser(newRole)
    }

    /**
     * Generates invite tokens and sends invite emails to the specified list of recipients
     * @param projectId the project id for which the invite tokens are to be generated
     * @param emailAddresses list of email recipients who are to be invited to join the specified project
     * @param duration iso 8601 duration for how long the invite codes will be valid, defaults to PT24H if not provided
     * @return The list of emails that were successfully sent as well as those that could not be sent
     */
    @Transactional
    InviteUsersResult inviteUsers(String projectId, List<String> emailAddresses, String duration=DEFAULT_DURATION) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(emailAddresses, "emailAddresses")
        boolean enabled = featureService.isEmailServiceFeatureEnabled()
        SkillsValidator.isTrue(enabled, "Project Invites can only be used if email has been configured for this instance", projectId)

        String publicUrl = featureService.getPublicUrl()
        if (!publicUrl) {
            throw new SkillException("No public URL is configured for the system, unable to send project invite email")
        }

        final successfullySent = []
        final List<String> couldNotBeSent = []
        final List<String> couldNotBeSentErrors = []

        Date created = new Date()
        emailAddresses.each {String email ->
            if (PatternsUtil.isValidEmail(email)) {
                ProjectAccessToken alreadyInvited = projectAccessTokenRepo.findByProjectIdAndRecipientEmail(projectId, email.toLowerCase())
                if (!alreadyInvited) {
                    try {
                        ProjectInvite invite = generateProjectInviteToken(projectId, email, created, duration)

                        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                                userIds: [email],
                                type: Notification.Type.InviteOnly.toString(),
                                keyValParams: [
                                        projectName     : invite.projectName,
                                        projectId       : invite.projectId,
                                        publicUrl       : publicUrl,
                                        validTime       : invite.validFor,
                                        inviteCode      : invite.token,
                                        communityHeaderDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                                ],
                        )
                        notifier.sendNotification(request)
                        successfullySent << email
                    } catch (Exception e) {
                        log.error("Error sending project invites, [${successfullySent?.size()}] successful, [${emailAddresses.minus(successfullySent)?.size()}] unsuccessful", e)
                        couldNotBeSent.add(email)
                        couldNotBeSentErrors.add("${email}: ${e.message}".toString())
                    }
                } else {
                    couldNotBeSent.add(email)
                    couldNotBeSentErrors.add("${email} already has a pending invite".toString())
                }
            } else {
                couldNotBeSent.add(email)
                couldNotBeSentErrors.add("${email} is not a valid email".toString())
            }
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.ProjectInvite,
                actionAttributes: [
                        emailAddresses: emailAddresses,
                        duration      : duration
                ],
                itemId: projectId,
                projectId: projectId,
        ))

        return new InviteUsersResult(projectId: projectId, successful: successfullySent, unsuccessful: couldNotBeSent, unsuccessfulErrors: couldNotBeSentErrors)
    }

    @Transactional
    void removeUserFromProject(String projectId, String userId) {
        String currentUserId = userInfoService.getCurrentUserId()
        log.info("user [{}] has removed access to project [{}] for user [{}]", currentUserId, projectId, userId)
        accessSettingsStorageService.deleteUserRole(userId, projectId, RoleName.ROLE_PRIVATE_PROJECT_USER)
    }

    @Transactional
    void removeExpiredInviteTokens(Date expiresBefore) {
        log.info("deleting project invite tokens expired before [{}]", expiresBefore)
        projectAccessTokenRepo.deleteByExpiresBefore(expiresBefore)
    }

    @Transactional
    void removeClaimedInviteTokens(Date claimedBefore) {
        log.info("deleting project invite tokens claimed before [{}]", claimedBefore)
        projectAccessTokenRepo.deleteClaimedTokensOlderThen(claimedBefore)
    }

    /**
     * Checks if the specified projectId is configured as an invite only project
     * @param projectId - not null
     * @return true if the project exists and has been configured as an invite only project
     */
    @Transactional(readOnly = true)
    boolean isInviteOnlyProject(String projectId) {
        SkillsValidator.isNotNull(projectId, "projectId")
        if (StringUtils.EMPTY == projectId) {
            return false
        }
        return settingsDataAccessor.getProjectSetting(projectId, Settings.INVITE_ONLY_PROJECT.settingName)?.isEnabled()
    }

    /**
     * Checks if the specified user id has the necessary user role to access the specified invite only project id
     * @param projectId a projectId configured for invite only
     * @param userId a non-null userid
     * @param idType not required, only used in certain authentication configurations
     * @return
     */
    @Transactional(readOnly = true)
    boolean canUserAccess(String projectId, String userId) {
        List<UserRoleRes> roles = accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, userId)
        return roles?.find {it.roleName == RoleName.ROLE_PRIVATE_PROJECT_USER || it.roleName == RoleName.ROLE_PROJECT_ADMIN}
    }

    @Transactional(readOnly = true)
    boolean isPrivateProjRoleOrAdminRole(String projectId, String userId) {
        if (authMode == AuthMode.PKI) {
            userId = userInfoService.getUserName(userId, true, UserInfoService.ID_IDTYPE)
        }
        List<UserRoleRes> roles = accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, userId)
        return roles?.find {it.roleName == RoleName.ROLE_PRIVATE_PROJECT_USER || it.roleName == RoleName.ROLE_PROJECT_ADMIN}
    }

    @Transactional(readOnly = true)
    TableResult getPendingInvites(String projectId, String userEmailQuery, PageRequest pagingRequest) {
        if (!isInviteOnlyProject(projectId)) {
            throw new SkillsAuthorizationException("Project is not configured as Invite Only")
        }
        int total = projectAccessTokenRepo.countAllUnclaimedByProjectId(projectId, userEmailQuery)
        List<ProjectInviteStatus> inviteStatuses = []
        if (total > 0) {
            List<ProjectAccessToken> invites = projectAccessTokenRepo.findAllUnclaimedByProjectId(projectId, userEmailQuery, pagingRequest)
            invites.each {
                inviteStatuses << convert(it)
            }
        }

        return new TableResult(data: inviteStatuses, count: inviteStatuses.size(), totalCount: total)
    }

    @Transactional(readOnly = false)
    void extendValidity(String projectId, String recipientEmail, String duration) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(recipientEmail, "recipientEmail")
        SkillsValidator.isNotBlank(duration, "extensionDuration")
        log.info("User [{}] is extending the expiration for invite token to project [{}] for user [{}] by [{}]", userInfoService.getCurrentUserId(), projectId, recipientEmail, duration)
        ProjectAccessToken accessToken = projectAccessTokenRepo.findByProjectIdAndRecipientEmail(projectId, recipientEmail.toLowerCase())
        Date now = new Date()
        if (accessToken.expires.before(now)) {
            log.debug("invite token for user [{}] is already expired, resetting expiration to [{}] before adding extension duration of [{}]", accessToken.recipientEmail, now, duration)
            accessToken.expires = now
        }
        Expiration expiration = ExpirationUtils.extendExpiration(accessToken.expires, duration)
        accessToken.expires = expiration.expiresOn
        projectAccessTokenRepo.save(accessToken)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Extend,
                item: DashboardItem.ProjectInvite,
                actionAttributes: [
                        duration      : duration
                ],
                itemId: recipientEmail,
                projectId: projectId,
        ))
    }

    @Transactional(readOnly = false)
    void deleteInvite(String projectId, String recipientEmail) {
        projectAccessTokenRepo.deleteByProjectIdAndRecipientEmail(projectId, recipientEmail.toLowerCase())
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.ProjectInvite,
                itemId: recipientEmail,
                projectId: projectId,
        ))
    }

    void remindUser(String projectId, String recipientEmail) {
        boolean enabled = featureService.isEmailServiceFeatureEnabled()
        SkillsValidator.isTrue(enabled, "Project Invites can only be used if email has been configured for this instance", projectId)

        String publicUrl = featureService.getPublicUrl()
        if (!publicUrl) {
            throw new SkillException("No public URL is configured for the system, unable to send project invite email")
        }

        ProjectAccessToken existingToken = projectAccessTokenRepo.findByProjectIdAndRecipientEmail(projectId, recipientEmail.toLowerCase())
        if (!existingToken) {
            throw new SkillException("No project invite exists for [${recipientEmail}]", projectId)
        }

        if (existingToken.expires.before(new Date())) {
            throw new SkillException("Project Invite for [${existingToken.recipientEmail}] is expired", projectId, null, ErrorCode.ExpiredProjectInvite)
        }

        PrettyTime prettyTime = new PrettyTime()
        String relativeTime = prettyTime.format(existingToken.expires)
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [recipientEmail],
                type: Notification.Type.InviteOnlyReminder.toString(),
                keyValParams: [
                        projectName     : existingToken.project.name,
                        projectId       : existingToken.project.projectId,
                        publicUrl       : publicUrl,
                        relativeTime    : relativeTime,
                        inviteCode      : existingToken.token,
                        communityHeaderDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                ],
        )
        notifier.sendNotification(request)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Remind,
                item: DashboardItem.ProjectInvite,
                itemId: recipientEmail,
                projectId: projectId,
        ))
    }

    private ProjectInviteStatus convert(ProjectAccessToken token) {
        return new ProjectInviteStatus(
                recipientEmail: token.recipientEmail,
                created: token.created,
                expires: token.expires
        )
    }

    // generate invite code, attempt to resolve the unlikely scenario where a conflict occurs with an already
    // generated code
    private String generateCode() {
        String code
        int retries = 0
        while (retries < MAX_GENERATION_ATTEMPTS) {
            code = codeGenerator.generateCode()
            if (!projectAccessTokenRepo.findByToken(code)) {
                break
            }
            log.info("generated code conflicts with existing codes, trying again")
            code = null
        }

        return code
    }

}
