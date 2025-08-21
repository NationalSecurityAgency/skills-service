/*
 * Copyright 2024 SkillTree
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defineStore } from 'pinia'
import axios from 'axios'
import { SkillsConfiguration } from '@skilltree/skills-client-js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'


export const useAuthState = defineStore('authState', () => {
    const userInfoState = ref(null)
    const userInfo = computed(() => userInfoState.value)
    const restoringSessionState = ref(true)
    const restoringSession = computed(() => restoringSessionState.value)
    const localAuth = ref(false)
    const oAuthAuth = ref(false)

    const router = useRouter()
    const route = useRoute()
    const appConfig = useAppConfig()
    const appInfoState = useAppInfoState()

    const setAuthUser = (authData) => {
        localAuth.value = true
        localStorage.setItem('localAuth', 'true')
    }
    const storeUser = (newInfo) => {
        if (newInfo) {
            userInfoState.value = newInfo
        }
    }
    const setOauth2AuthUser = () => {
        oAuthAuth.value = true
        localStorage.setItem('oAuthAuth', 'true')
    }
    const setRestoringSession = (value) => {
        restoringSessionState.value = value
    }
    const clearAuthData = () => {
        userInfoState.value = null
        localAuth.value = false
        oAuthAuth.value = false
        SkillsConfiguration.logout()
        localStorage.removeItem('localAuth')
        localStorage.removeItem('oAuthAuth')
        localStorage.removeItem('expirationDate')
        localStorage.removeItem('userInfo')
        delete axios.defaults.headers.common.Authorization
    }

    const handleLogin = (result) => {
        // special handling for oAuth
        if (result.headers.tokenexpirationtimestamp) {
            const expirationDate = new Date(Number(result.headers.tokenexpirationtimestamp))
            setLogoutTimer(expirationDate)
        }
        setAuthUser()
    }

    const signup = (authData) => {
        const url = authData.isRootAccount ? '/createRootAccount' : '/createAccount'
        return axios
          .put(url, authData)
          .then((result) => {
              if (result) {
                  handleLogin(result)
                  return fetchUser().then(() => {
                      if (authData.isRootAccount) {
                          // when creating root account for the first time, reload the config state
                          // at a minimum it will update the flag indicating whether root user needs to be created
                          return appConfig.loadConfigState()
                      }
                  })
              }
          })
    }
    const login = (authData) => {
        return axios
          .post('/performLogin', authData, { handleError: false })
          .then((result) => {
              handleLogin(result)
              return fetchUser()
          })
    }
    const oAuth2Login = (oAuthId) => {
        setOauth2AuthUser()
        const redirect = route.query.redirect
        const newLocation = `/oauth2/authorization/${encodeURIComponent(oAuthId)}${redirect ? `?skillsRedirectUri=${redirect}` : ''}`
        window.location = newLocation
    }
    
    const saml2Login = (registrationid) => {
        const newLocation = `/saml2/authenticate/${registrationid}`
        window.location = newLocation
    }
    
    const restoreSessionIfAvailable = () => {
        setRestoringSession(true)
        return new Promise((resolve, reject) => {
            let reAuthenticated = false
            // attempt to retrieve userInfo using PKI (2-way ssl)
            // (or username/password if being redirected after successful login)
            fetchUser(false)
              .then(() => {
                  if (userInfoState.value) {
                      reAuthenticated = true
                      localAuth.value = !appConfig.isPkiAuthenticated.value
                  } else {
                      // cannot obtain userInfo, so clear any other lingering auth data
                      clearAuthData()
                  }
                  resolve(reAuthenticated)
              })
              .catch((error) => reject(error))
              .finally(() => {
                  setRestoringSession(false)
              })
        })
    }
    const logout =() => {
        clearAuthData()
        appInfoState.setShowUa(false)
        return axios.post('/logout').then(() => {
            router.replace('/skills-login')
        })
    }
    const setLogoutTimer = (expirationDate) => {
        const expiresInMillis = expirationDate.getTime() - new Date().getTime()
        setTimeout(() => {
            logout()
        }, expiresInMillis)
    }
    const fetchUser = (storeInLocalStorage = true) => {
        return axios.get('/app/userInfo')
          .then((response) => {
              storeUser(response.data)
              if (storeInLocalStorage) {
                  localStorage.setItem('userInfo', JSON.stringify(response.data))
              }
          })
    }
    const configureSkillsClientForInception = () => {
        return new Promise((resolve, reject) => {
            if (userInfoState.value) {
                const projectId = 'Inception'
                const serviceUrl = window.location.origin
                let authenticator
                if (appConfig.isPkiAuthenticated) {
                    authenticator = 'pki'
                } else {
                    authenticator = `/app/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(userInfoState.value.userId)}/token`
                }

                SkillsConfiguration.configure({
                    serviceUrl,
                    projectId,
                    authenticator
                })

                SkillsConfiguration.afterConfigure()
                  .then(() => {
                      resolve()
                  })
                  .catch((error) => reject(error))
            } else {
                resolve()
            }
        })
    }

    const isAuthenticated = computed(() => {
        return (
          (appConfig.isPkiAuthenticated ||
            appConfig.isSAML2Authenticated ||
            localAuth.value ||
            oAuthAuth.value) &&
          userInfoState.value !== null
        )
    })

    const currentUserCommunity = computed(() => {
        return userInfo.value?.userCommunity
    })
    const showUserCommunityInfo = computed(() => {
        return Boolean(currentUserCommunity.value)
    })

    return {
        signup,
        login,
        oAuth2Login,
        saml2Login,
        restoreSessionIfAvailable,
        setRestoringSession,
        logout,
        configureSkillsClientForInception,
        isAuthenticated,
        userInfo,
        restoringSession,
        clearAuthData,
        currentUserCommunity,
        showUserCommunityInfo,
        fetchUser
    }
})
