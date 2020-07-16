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
package skills.settings

import groovy.transform.WithWriteLock
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.SettingsService

import javax.annotation.PostConstruct
import javax.mail.MessagingException

@Service
@Slf4j
@ConditionalOnProperty(
        name = "skills.db.startup",
        havingValue = "true",
        matchIfMissing = true)
class EmailSettingsService {

    static final String settingsGroup = 'GLOBAL.EMAIL'
    static final String hostSetting = 'email.host'
    static final String portSetting = 'email.port'
    static final String protocolSetting = 'email.protocol'
    static final String usernameSetting = 'email.username'
    static final String passwordSetting = 'email.password'
    static final String authSetting = 'email.auth'
    static final String tlsEnableSetting = 'email.tls.enable'

    @Autowired
    SettingsService settingsService

    JavaMailSender mailSender

    @PostConstruct
    void init() {
        EmailConnectionInfo emailConnectionInfo = new EmailConnectionInfo(
                host: settingsService.getGlobalSetting(hostSetting, settingsGroup)?.value,
                port: settingsService.getGlobalSetting(portSetting, settingsGroup)?.value?.toInteger() ?: -1,
                protocol: settingsService.getGlobalSetting(protocolSetting, settingsGroup)?.value,
                username: settingsService.getGlobalSetting(usernameSetting, settingsGroup)?.value,
                password: settingsService.getGlobalSetting(passwordSetting, settingsGroup)?.value,
                authEnabled: settingsService.getGlobalSetting(authSetting,settingsGroup)?.value?.toBoolean() ?: false,
                tlsEnabled: settingsService.getGlobalSetting(tlsEnableSetting, settingsGroup)?.value?.toBoolean() ?: false,
        )

        try {
            configureMailSender(emailConnectionInfo)
        } catch (SkillException e) {
            log.warn('Email connection failed. No email can be sent without updating the configuration', e)
        }
    }

    void updateConnectionInfo(EmailConnectionInfo emailConnectionInfo) {
        configureMailSender(emailConnectionInfo)
        storeSettings(emailConnectionInfo)
    }

    void configureMailSender(EmailConnectionInfo emailConnectionInfo) {
        log.info('Configuring the email sender with properties [{}]', emailConnectionInfo)
        JavaMailSenderImpl tmpMailSender = createJavaMailSender(emailConnectionInfo)

        try {
            tmpMailSender.testConnection()

            log.info('Refreshing the email sender')
            updateMailSender(tmpMailSender)
        } catch (MessagingException e) {
            log.warn('Email connection failed!', e)
//            throw new SkillException('Could not connect with the email settings ' + emailConnectionInfo, e)
        }
    }

    private static JavaMailSenderImpl createJavaMailSender(EmailConnectionInfo emailConnectionInfo) {
        JavaMailSenderImpl tmpMailSender = new JavaMailSenderImpl()
        tmpMailSender.setHost(emailConnectionInfo.host)
        tmpMailSender.setPort(emailConnectionInfo.port)
        tmpMailSender.setUsername(emailConnectionInfo.username)
        tmpMailSender.setPassword(emailConnectionInfo.password)

        Properties props = tmpMailSender.getJavaMailProperties()
        if (emailConnectionInfo.protocol) {
            props.put('mail.transport.protocol', emailConnectionInfo.protocol)
        }
        props.put('mail.smtp.auth', emailConnectionInfo.authEnabled)
        props.put('mail.smtp.starttls.enable', emailConnectionInfo.tlsEnabled)
        props.put('mail.debug', 'false')

        return tmpMailSender
    }

    boolean testConnection(EmailConnectionInfo emailConnectionInfo) {
        log.info('Testing email connection with properties [{}]', emailConnectionInfo)
        JavaMailSenderImpl mailSender = createJavaMailSender(emailConnectionInfo)

        try {
            mailSender.testConnection()
            log.info('Test connection successful!')
            return true
        } catch (MessagingException e) {
            log.info('Test connection failed!')
            return false
        }
    }

    void storeSettings(EmailConnectionInfo emailConnectionInfo) {
        saveOrUpdateGlobalGroup(settingsGroup, [
                (hostSetting)     : emailConnectionInfo.host,
                (portSetting)     : emailConnectionInfo.port?.toString(),
                (protocolSetting) : emailConnectionInfo.protocol,
                (usernameSetting) : emailConnectionInfo.username,
                (passwordSetting) : emailConnectionInfo.password,
                (authSetting)     : emailConnectionInfo.authEnabled?.toString(),
                (tlsEnableSetting): emailConnectionInfo.tlsEnabled?.toString(),
        ])
    }

    EmailConnectionInfo fetchEmailSettings(){
        List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(settingsGroup)
        EmailConnectionInfo info = new EmailConnectionInfo()
        if (emailSettings) {
            def mappedSettings = emailSettings.collectEntries() {
                [it.setting, it.value]
            }
            if(mappedSettings[(hostSetting)]) {
                info.host = mappedSettings[(hostSetting)]
            }
            if(mappedSettings[(portSetting)]) {
                info.port = Integer.valueOf(mappedSettings[(portSetting)])
            }
            if(mappedSettings[(protocolSetting)]) {
                info.protocol = mappedSettings[(protocolSetting)]
            }
            if(mappedSettings[(usernameSetting)]) {
                info.username = mappedSettings[(usernameSetting)]
            }
            if(mappedSettings[(passwordSetting)]) {
                info.password = mappedSettings[(passwordSetting)]
            }
            if (mappedSettings[(authSetting)]) {
                info.authEnabled = Boolean.valueOf(mappedSettings[(authSetting)])
            }
            if (mappedSettings[(tlsEnableSetting)]) {
                info.tlsEnabled = Boolean.valueOf(mappedSettings[(tlsEnableSetting)])
            }
        }
        return info
    }

    private void saveOrUpdateGlobalGroup(String settingsGroup, Map<String, String> settingsMap) {
        List<SettingsRequest> settingsRequests = []
        List<SettingsRequest> deleteIfExist = []
        settingsMap.each { String setting, String value ->
            GlobalSettingsRequest gsr = new GlobalSettingsRequest(settingGroup: settingsGroup, setting: setting, value: value)
            if (value) {
                settingsRequests << gsr
            } else {
                //if there's not value specified, we need to check if that setting previously had a value and delete it
                deleteIfExist << gsr
            }
        }
        settingsService.saveSettings(settingsRequests)
        deleteIfExist.each {
            settingsService.deleteGlobalSetting(it.setting)
        }
    }

    @WithWriteLock
    void updateMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender
    }

    JavaMailSender getMailSender() {
        return this.mailSender;
    }
}
