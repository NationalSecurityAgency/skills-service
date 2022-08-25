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
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import skills.auth.AuthMode
import skills.auth.GrantedAuthoritiesUpdater
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.InviteTokenValidationResponse
import skills.controller.result.model.InviteUsersResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserRoleRes
import skills.notify.builders.Formatting
import skills.services.AccessSettingsStorageService
import skills.services.EmailSendingService
import skills.services.FeatureService
import skills.services.ProjectInvite
import skills.services.settings.Settings
import skills.services.settings.SettingsDataAccessor
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.ProjDef
import skills.storage.model.ProjectAccessToken
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.ProjectAccessTokenRepo
import skills.storage.repos.UserRoleRepo
import skills.utils.Expiration
import skills.utils.ExpirationUtils

import java.util.regex.Pattern

@Slf4j
@Component
class InviteOnlyProjectService {
    private static final String DEFAULT_DURATION = "PT24H"

    private static final int MAX_GENERATION_ATTEMPTS = 5
    private static final String INVITE_TEMPLATE = "project_invitation.html"

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
    UserRoleRepo userRoleRepoa

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
    EmailSendingService emailService

    @Autowired
    SettingsService settingsService

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    // email validation regex as defined by RFC 5322
    private static final Pattern VALID_EMAIL = ~/^[a-zA-Z0-9_!#$%&'*+\/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$/

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

        ProjectAccessToken accessToken = new ProjectAccessToken()
        accessToken.token = code
        accessToken.project = projDef
        accessToken.expires = expiration.expiresOn
        accessToken.created = created
        accessToken.recipientEmail = email;
        accessToken = projectAccessTokenRepo.save(accessToken)

        ProjectInvite invite = new ProjectInvite()
        invite.projectId = projDef.projectId
        invite.projectName = projDef.name
        invite.validFor = expiration.validFor
        invite.token = accessToken.token
        invite.recipientEmail = email

        log.info("user [{}] has generated invite token [{}] for project [{}] for recipient [{}]", currentUser, code, projectId, email)
        return invite
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

        projectAccessTokenRepo.save(projectAccessToken)
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
    public InviteUsersResult inviteUsers(String projectId, List<String> emailAddresses, String duration=DEFAULT_DURATION) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(emailAddresses, "emailAddresses")
        boolean enabled = featureService.isEmailServiceFeatureEnabled()
        SkillsValidator.isTrue(enabled, "Project Invites can only be used if email has been configured for this instance", projectId)

        final SettingsResult settingsResult = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)
        final List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(EmailSettingsService.settingsGroup);

        final String htmlHeader =  emailSettings.find {it.setting == EmailSettingsService.htmlHeader }?.value ?: null
        final String htmlFooter = emailSettings.find { it.setting == EmailSettingsService.htmlFooter }?.value ?: null

        if (!settingsResult) {
            throw new SkillException("No public URL is configured for the system, unable to send project invite email")
        }

        String publicUrl = settingsResult.value
        if (!publicUrl.endsWith("/")){
            publicUrl += "/"
        }

        final String url = "${publicUrl}"
        final successfullySent = []

        Date created = new Date()
        emailAddresses.each {
            if (VALID_EMAIL.matcher(it).matches()) {
                try {
                    String email = it
                    ProjectInvite invite = generateProjectInviteToken(projectId, email, created, duration)

                    Context templateContext = new Context()
                    templateContext.setVariable("validTime", invite.validFor)
                    templateContext.setVariable("inviteCode", invite.token)
                    templateContext.setVariable("projectId", invite.projectId)
                    templateContext.setVariable("projectName", invite.projectName)
                    templateContext.setVariable("publicUrl", url)
                    templateContext.setVariable("htmlHeader", htmlHeader)
                    templateContext.setVariable("htmlFooter", htmlFooter)

                    emailService.sendEmailWithThymeleafTemplate("SkillTree Project Invitation", email, INVITE_TEMPLATE, templateContext)
                    successfullySent << email
                } catch (Exception e) {
                    log.error("Error sending project invites, [${successfullySent?.size()}] successful, [${emailAddresses.minus(successfullySent)?.size()}] unsuccessful", e)
                }
            }
        }

        return new InviteUsersResult(projectId: projectId, successful: successfullySent, unsuccessful: emailAddresses.minus(successfullySent))
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
    public boolean isInviteOnlyProject(String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
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
    public boolean canUserAccess(String projectId, String userId, String idType) {
        if (authMode == AuthMode.PKI) {
            userId = userInfoService.getUserName(userId, true, idType)
        }
        List<UserRoleRes> roles = accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, userId)
        return roles?.find {it.roleName == RoleName.ROLE_PRIVATE_PROJECT_USER }
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
