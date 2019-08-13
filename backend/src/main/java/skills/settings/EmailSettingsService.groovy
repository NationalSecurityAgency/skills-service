package skills.settings

import groovy.transform.WithWriteLock
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.services.settings.SettingsService

import javax.annotation.PostConstruct
import javax.mail.MessagingException

@Service
@Slf4j
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
                host: settingsService.getGlobalSetting(hostSetting)?.value,
                port: settingsService.getGlobalSetting(portSetting)?.value?.toInteger() ?: -1,
                protocol: settingsService.getGlobalSetting(protocolSetting)?.value,
                username: settingsService.getGlobalSetting(usernameSetting)?.value,
                password: settingsService.getGlobalSetting(passwordSetting)?.value,
                authEnabled: settingsService.getGlobalSetting(authSetting)?.value?.toBoolean() ?: false,
                tlsEnabled: settingsService.getGlobalSetting(tlsEnableSetting)?.value?.toBoolean() ?: false,
        )

        try {
            configureMailSender(emailConnectionInfo)
        } catch (SkillException e) {
            log.error('Email connection failed. No email can be sent without updating the configuration', e)
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
            throw new SkillException('Could not connect with the email settings ' + emailConnectionInfo, e)
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

    private void saveOrUpdateGlobalGroup(String settingsGroup, Map<String, String> settingsMap) {
        List<SettingsRequest> settingsRequests = []
        settingsMap.each { String setting, String value ->
            settingsRequests << new GlobalSettingsRequest(settingGroup: settingsGroup, setting: setting, value: value)
        }
        settingsService.saveSettings(settingsRequests)
    }

    @WithWriteLock
    void updateMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender
    }
}
