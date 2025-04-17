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
package skills.auth

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.UserAttrsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.utils.RetryUtil
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal

@Component
@Slf4j
class UserInfoService {

    static final String ID_IDTYPE = "ID"
    static final String DN_IDTYPE = "DN"

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    UserAttrsService userAttrsService

    @Autowired(required = false)
    UserAuthService userAuthService

    @Profile
    UserInfo getCurrentUser() {
        UserInfo currentUser
        def principal = SecurityContextHolder.getContext()?.authentication?.principal
        if (principal) {
            if (principal instanceof UserInfo) {
                log.trace("User principal {}", principal)
                currentUser = principal
            } else if(principal instanceof Saml2AuthenticatedPrincipal){
                log.trace("User principal {}", principal);
                currentUser = userAuthService.loadByUserId(principal.getName())
            }else {
                log.info("Unexpected/Unauthenticated princial [${principal}]")
            }
        }
        return currentUser
    }


    @Profile
    boolean isCurrentUserASuperDuperUser() {
        boolean isRootUser = this.currentUser?.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }
        return isRootUser
    }

    String getCurrentUserId() {
        return getCurrentUser()?.username?.toLowerCase()
    }

    /**
     * Abstracts dealing with PKI vs Password/Form modes when user id param is provided
     */
    String getUserName(String userIdParam, boolean retry = true, String idType = DN_IDTYPE) {
        return RetryUtil.withRetry(3) {
            return doGetUserName(userIdParam, retry, idType)
        }
    }

    @Profile
    protected String doGetUserName(String userIdParam, boolean retry, String idType) {
        log.debug("doGetUserName: userIdParam=[{}], retry=[{}], idType=[{}]", userIdParam, retry, idType)
        String userNameRes = userIdParam
        if (!userIdParam) {
            UserInfo userInfo = getCurrentUser()
            userNameRes =  userInfo.username
            log.debug("called getCurrentUser: userNameRes=[{}]", userNameRes)
        } else if (authMode == AuthMode.PKI) {
            if (ID_IDTYPE != idType?.toUpperCase()) {
                UserInfo userInfo
                try {
                    log.debug("called lookupUserDn: userIdParam=[{}]", userIdParam)
                    userInfo = pkiUserLookup.lookupUserDn(userIdParam)
                } catch (Throwable e) {
                    String msg = ""
                    if (e instanceof HttpClientErrorException) {
                        msg = ((HttpClientErrorException) e).getResponseBodyAsString()
                    } else if (e.getCause() instanceof HttpClientErrorException) {
                        msg = ((HttpClientErrorException) e.getCause()).getResponseBodyAsString()
                    } else {
                        msg = e.getMessage()
                    }
                    log.error("user-info-service lookup failed: ${msg}")
                    SkillException ske = new SkillException(msg)
                    ske.errorCode = ErrorCode.UserNotFound
                    ske.doNotRetry = !retry
                    ske.printStackTrace = false
                    throw ske
                }

                if (!userInfo) {
                    log.error("received empty user information from lookup")
                    SkillException ske = new SkillException("User Info Service does not know about user with provided lookup id of [${userIdParam}]")
                    ske.doNotRetry = !retry
                    throw ske
                }

                userNameRes = userInfo.username
            }
        }

        /**
         * Save UserAttrs as loading of user relies on those records to be present;
         * also it was decided to use db fk constraints to map to this table to ensure valid userId and to provide simple way to
         * remove users
         */
        if (!(authMode == AuthMode.PKI)) {
            userAttrsService.saveUserAttrs(userNameRes, new UserInfo(username: userNameRes))
        }

        String res = userNameRes?.toLowerCase()
        log.debug("doGetUserName result =[{}]", res)
        return res
    }

    /**
     * @param userKey - this will be the user's DN in PKI authMode, or the actual userId in FORM authMode
     * @return return the correct userKey based on authMode,
     * i.e. - do a DN lookup if in PKI mode, otherwise just return the passed in userKey
     */
    String lookupUserId(String userKey) {
        assert userKey
        String userId
        if (authMode == AuthMode.FORM) {
            userId = userKey  // we are using FORM authMode, so the userKey is the userId
        } else {
            // we using PKI auth mode so we need to lookup the user to convert from DN to username
            userId = pkiUserLookup.lookupUserDn(userKey)?.username
        }
        return userId?.toLowerCase()
    }
}
