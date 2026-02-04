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
import { defineStore } from 'pinia'
import SettingsService from '@/components/settings/SettingsService.js'
import log from 'loglevel';

export const useAppConfig = defineStore('dashboardAppConfig', () => {
  const loadingConfig = ref(true)
  const config = ref({})

  const loadConfigState = () => {
    loadingConfig.value = true
    return SettingsService.getConfig()
      .then((response) => {
        config.value = response
        if (config.value.logLevel) {
            log.setLevel(config.value.logLevel)
        }
      })
      .finally(() => {
        loadingConfig.value = false
      })
  }

  const refreshConfig = () => {
    return SettingsService.getConfig()
        .then((response) => {
          config.value = response
        })
  }
  const isLoadingConfig = computed(() => loadingConfig.value)

  const toNumOr0 = (strNum) => {
    return strNum ? Number(strNum) : 0
  }

  const rankingAndProgressViewsEnabled = computed(() => {
    return (
      config.value.rankingAndProgressViewsEnabled === true ||
      config.value.rankingAndProgressViewsEnabled === 'true'
    )
  })

  const getConfigsThatStartsWith = (startsWith) => {
    const configs = config.value
    const res = Object.keys(configs)
      .filter(key => key.startsWith(startsWith))
      .reduce((obj, key) => {
        obj[key] = configs[key]
        return obj
      }, {})
    return res
  }

  const listFromCommaSeparatedString = (strProp) => {
    return strProp ? strProp.split(',').map(s => s.trim()) : []
  }

  const isTrue = (val) => !!val && (val === true || val?.toLowerCase() === 'true')

  const maxBadgeBonusInMinutes = computed(() => config.value.maxBadgeBonusInMinutes)
  const minNameLength = computed(() => config.value.minNameLength)
  const maxAdminGroupNameLength = computed(() => config.value.maxAdminGroupNameLength)
  const maxProjectNameLength = computed(() => config.value.maxProjectNameLength)
  const maxQuizNameLength = computed(() => config.value.maxQuizNameLength)
  const maxQuizAnswerHintLength = computed(() => config.value.maxQuizAnswerHintLength)
  const nameValidationRegex = computed(() => config.value.nameValidationRegex)
  const minIdLength = computed(() => config.value.minIdLength)
  const maxIdLength = computed(() => config.value.maxIdLength)
  const descriptionMaxLength = computed(() => config.value.descriptionMaxLength)
  const paragraphValidationRegex = computed(() => config.value.paragraphValidationRegex)
  const addPrefixToInvalidParagraphsOptions = computed(() => config.value.addPrefixToInvalidParagraphsOptions)
  const addPrefixToInvalidParagraphsBtnLabel  = computed(() => config.value.addPrefixToInvalidParagraphsBtnLabel || 'Add Prefix')
  const addPrefixToGeneratedValueBtnLabel =  computed(() => config.value.addPrefixToGeneratedValueBtnLabel || 'Add Prefix Then Use')
  const showMissingPrefixBtnLabel  = computed(() => config.value.showMissingPrefixBtnLabel || 'View Missing Preview')
  const closeMissingPrefixPreview  = computed(() => config.value.closeMissingPrefixPreview || 'Close Preview')
  const formFieldDebounceInMs = computed(() => config.value.formFieldDebounceInMs || 400)
  const maxSubjectNameLength = computed(() => config.value.maxSubjectNameLength)
  const maxBadgeNameLength = computed(() => config.value.maxBadgeNameLength)
  const maxCustomLabelLength = computed(() => config.value.maxCustomLabelLength)
  const maxHostLength = computed(() => config.value.maxHostLength)
  const maxSkillVersion = computed(() => config.value.maxSkillVersion)
  const maxSkillNameLength = computed(() => config.value.maxSkillNameLength)
  const maxPointIncrement = computed(() => toNumOr0(config.value.maxPointIncrement))
  const maxNumPerformToCompletion = computed(() => toNumOr0(config.value.maxNumPerformToCompletion))
  const maxNumPointIncrementMaxOccurrences = computed(() => toNumOr0(config.value.maxNumPointIncrementMaxOccurrences))
  const maxTimeWindowInMinutes = computed(() => toNumOr0(config.value.maxTimeWindowInMinutes))
  const maxAnswersPerQuizQuestion = computed(() => config.value.maxAnswersPerQuizQuestion)
  const maxTimeWindowInHrs = computed(() => maxTimeWindowInMinutes.value / 60)
  const maxSkillsPerSubject = computed(() => config.value.maxSkillsPerSubject)
  const needToBootstrap = computed(() => config.value.needToBootstrap)
  const verifyEmailAddresses = computed(() => config.value.verifyEmailAddresses)
  const oAuthOnly = computed(() => config.value.oAuthOnly)
  const oAuthProviders = computed(() => config.value.oAuthProviders)
  const enablePageVisitReporting = computed(() => config.value.enablePageVisitReporting === true || config.value.enablePageVisitReporting === 'true')
  const allowedAttachmentFileTypes = computed(() => config.value.allowedAttachmentFileTypes)
  const maxAttachmentSize = computed(() => config.value.maxAttachmentSize ? Number(config.value.maxAttachmentSize) : 0)
  const descriptionWarningMessage = computed(() => config.value.descriptionWarningMessage)
  const attachmentWarningMessage = computed(() => config.value.attachmentWarningMessage)
  const allowedAttachmentMimeTypes = computed(() => config.value.allowedAttachmentMimeTypes)
  const docsHost = computed(() => config.value.docsHost)
  const maxBadgesPerProject = computed(() => config.value.maxBadgesPerProject)
  const isPkiAuthenticated = computed(() => config.value.authMode === 'PKI')
  const isSAML2Authenticated = computed(() => config.value.authMode === 'SAML2')
  const saml2RegistrationId = computed(() => config.value.saml2RegistrationId)
  const expirationGracePeriod = computed(() => config.value.expirationGracePeriod)
  const expireUnusedProjectsOlderThan = computed(() => config.value.expireUnusedProjectsOlderThan)
  const minimumProjectPoints = computed(() => config.value.minimumProjectPoints)
  const maxProjectsPerAdmin = computed(() => config.value.maxProjectsPerAdmin)
  const minimumSubjectPoints = computed(() => config.value.minimumSubjectPoints)
  const maxSubjectsPerProject = computed(() => config.value.maxSubjectsPerProject)
  const defaultCommunityDescriptor = computed(() => config.value.defaultCommunityDescriptor || '')
  const userCommunityBeforeLabel = computed(() => config.value.userCommunityBeforeLabel || '')
  const userCommunityAfterLabel = computed(() => config.value.userCommunityAfterLabel || '')
  const userCommunityRestrictedDescriptor = computed(() => config.value.userCommunityRestrictedDescriptor || '')
  const userCommunityDocsLabel = computed(() => config.value.userCommunityDocsLabel || 'Learn More')
  const userCommunityDocsLink = computed(() => config.value.userCommunityDocsLink || '' )
  const usersTableAdditionalUserTagKey = computed(() => config.value.usersTableAdditionalUserTagKey)
  const usersTableAdditionalUserTagLabel = computed(() => config.value.usersTableAdditionalUserTagLabel)
  const maxSkillsInBulkImport = computed(() => config.value.maxSkillsInBulkImport)
  const userSuggestOptions = computed(() => config.value.userSuggestOptions)
  const maxContactOwnersMessageLength = computed(() => config.value.maxContactOwnersMessageLength)
  const defaultLandingPage = computed(() => config.value.defaultLandingPage)
  const maxSkillTagLength = computed(() => config.value.maxSkillTagLength)
  const customHeader = computed(() => config.value.customHeader)
  const customFooter = computed(() => config.value.customFooter)
  const dashboardVersion = computed(() => config.value.dashboardVersion)
  const buildTimestamp = computed(() => config.value.buildTimestamp)
  const currentUsersCommunityDescriptor = computed(() => config.value.currentUsersCommunityDescriptor)
  const activityHistoryStartDate = computed(() => config.value.activityHistoryStartDate)
  const motivationalSkillWarningGracePeriod = computed(() => config.value.motivationalSkillWarningGracePeriod)
  const skillsDisplayProjectDescription = computed(() => config.value.skillsDisplayProjectDescription)
  const maxSelfReportRejectionMessageLength = computed(() => config.value.maxSelfReportRejectionMessageLength)
  const approvalConfUserTagKey = computed(() => config.value.approvalConfUserTagKey)
  const approvalConfUserTagLabel = computed(() => config.value.approvalConfUserTagLabel)
  const projectMetricsTagCharts = computed(() => config.value.projectMetricsTagCharts)
  const maxDailyUserEvents = computed(() => config.value.maxDailyUserEvents)
  const allowedVideoUploadMimeTypes = computed(() => config.value.allowedVideoUploadMimeTypes)
  const allowedSlidesUploadMimeTypes = computed(() => config.value.allowedSlidesUploadMimeTypes)
  const videoUploadWarningMessage = computed(() => config.value.videoUploadWarningMessage)
  const slidesUploadWarningMessage = computed(() => config.value.slidesUploadWarningMessage)
  const maxVideoCaptionsLength = computed(() => config.value.maxVideoCaptionsLength)
  const maxVideoTranscriptLength = computed(() => config.value.maxVideoTranscriptLength)
  const userPageTagsToDisplay = computed(() => config.value.userPageTagsToDisplay)
  const maxProjectInviteEmails = computed(() => config.value.maxProjectInviteEmails)
  const artifactBuildTimestamp = computed(() => config.value.artifactBuildTimestamp)
  const minUsernameLength = computed(() => config.value.minUsernameLength)
  const minPasswordLength = computed(() => config.value.minPasswordLength)
  const maxPasswordLength = computed(() => config.value.maxPasswordLength)
  const maxFirstNameLength = computed(() => config.value.maxFirstNameLength)
  const maxLastNameLength = computed(() => config.value.maxLastNameLength)
  const numProjectsToStartShowingAsCards = computed(() => config.value.numProjectsToStartShowingAsCards)
  const disableScrollToTop = computed(() => config.value.disableScrollToTop)
  const dbUpgradeInProgress = computed(() => isTrue(config.value.dbUpgradeInProgress))
  const exportHeaderAndFooter = computed(() => config.value.exportHeaderAndFooter)
  const limitAdminAccess = computed(() => config.value.limitAdminAccess)
  const maxGraderFeedbackMessageLength = computed(() => config.value.maxGraderFeedbackMessageLength)
  const maxTakeQuizInputTextAnswerLength = computed(() => config.value.maxTakeQuizInputTextAnswerLength)
  const maxTextInputAiGradingCorrectAnswerLength = computed(() => config.value.maxTextInputAiGradingCorrectAnswerLength)
  const disableEncouragementsConfetti = computed(() => isTrue(config.value.disableEncouragementsConfetti))
  const contactSupportEnabled = computed(() => isTrue(config.value.contactSupportEnabled))
  const contactSupportExternalUrl = computed(() => config.value.contactSupportExternalUrl)
  const contactSupportExternalTitle = computed(() => config.value.contactSupportExternalTitle)
  const contactSupportExternalDescription = computed(() => config.value.contactSupportExternalDescription)
  const contactSupportExternalEmail = computed(() => config.value.contactSupportExternalEmail)
  const contactSupportExternalEmailDescription = computed(() => config.value.contactSupportExternalEmailDescription)
  const maxRolePageSize = computed(() => config.value.maxRolePageSize)
  const matomoUrl = computed(() => config.value.matomoUrl)
  const matomoSiteId = computed(() => config.value.matomoSiteId)
  const matomoProcessUserIdRegex = computed(() => config.value.matomoProcessUserIdRegex)
  const enableOpenAIIntegration = computed(() => config.value.enableOpenAIIntegration)
  const openaiTakingLongerThanExpectedMessages = computed(() => config.value.openaiTakingLongerThanExpectedMessages)
  const openaiTakingLongerThanExpectedTimeoutPerMsg = computed(() => config.value.openaiTakingLongerThanExpectedTimeoutPerMsg)
  const openaiModelDefaultTemperature = computed(() => Number(config.value.openaiModelDefaultTemperature))
  const openaiDefaultModel = computed(() => config.value.openaiDefaultModel)
  const openaiFooterMsg = computed(() => config.value.openaiFooterMsg)
  const openaiFooterPoweredByLink = computed(() => config.value.openaiFooterPoweredByLink)
  const openaiFooterPoweredByLinkText = computed(() => config.value.openaiFooterPoweredByLinkText)
  const openaiNotSupportedChatModels = computed(() => listFromCommaSeparatedString(config.value.openaiNotSupportedChatModels))
  const openAiDisableSingleQuestionTypeChange = computed(() => isTrue(config.value.openAiDisableSingleQuestionTypeChange))
  const maxAiPromptLength = computed(() => config.value.maxAiPromptLength)
  const delayBetweenScreenReaderAnnouncements = computed(() => config.value.delayBetweenScreenReaderAnnouncements || 2500)
  const openAiAnnounceGenStatusInterval = computed(() => config.value.openAiAnnounceGenStatusInterval || 6000)

  const sdPointHistoryChartAchievementsCombinePct = computed(() => config.value.sdPointHistoryChartAchievementsCombinePct || 0.05)


  return {
    loadConfigState,
    refreshConfig,
    isLoadingConfig,
    defaultLandingPage,
    getConfigsThatStartsWith,
    rankingAndProgressViewsEnabled,
    docsHost,
    minNameLength,
    maxAdminGroupNameLength,
    maxProjectNameLength,
    maxQuizNameLength,
    maxQuizAnswerHintLength,
    nameValidationRegex,
    minIdLength,
    maxIdLength,
    descriptionMaxLength,
    formFieldDebounceInMs,
    paragraphValidationRegex,
    addPrefixToInvalidParagraphsOptions,
    addPrefixToInvalidParagraphsBtnLabel,
    addPrefixToGeneratedValueBtnLabel,
    showMissingPrefixBtnLabel,
    closeMissingPrefixPreview,
    maxSubjectNameLength,
    maxBadgeNameLength,
    maxCustomLabelLength,
    maxSkillVersion,
    maxSkillNameLength,
    maxPointIncrement,
    maxNumPerformToCompletion,
    maxNumPointIncrementMaxOccurrences,
    maxTimeWindowInHrs,
    maxAnswersPerQuizQuestion,
    maxSkillsPerSubject,
    needToBootstrap,
    verifyEmailAddresses,
    oAuthOnly,
    oAuthProviders,
    enablePageVisitReporting,
    allowedAttachmentFileTypes,
    maxAttachmentSize,
    descriptionWarningMessage,
    attachmentWarningMessage,
    allowedAttachmentMimeTypes,
    maxBadgesPerProject,
    isPkiAuthenticated,
    isSAML2Authenticated,
    saml2RegistrationId,
    expirationGracePeriod,
    expireUnusedProjectsOlderThan,
    minimumProjectPoints,
    maxProjectsPerAdmin,
    minimumSubjectPoints,
    maxSubjectsPerProject,
    userCommunityBeforeLabel,
    userCommunityAfterLabel,
    defaultCommunityDescriptor,
    userCommunityRestrictedDescriptor,
    userCommunityDocsLabel,
    userCommunityDocsLink,
    usersTableAdditionalUserTagKey,
    usersTableAdditionalUserTagLabel,
    maxBadgeBonusInMinutes,
    maxSkillsInBulkImport,
    userSuggestOptions,
    maxContactOwnersMessageLength,
    maxSkillTagLength,
    customHeader,
    customFooter,
    dashboardVersion,
    buildTimestamp,
    currentUsersCommunityDescriptor,
    activityHistoryStartDate,
    motivationalSkillWarningGracePeriod,
    skillsDisplayProjectDescription,
    maxSelfReportRejectionMessageLength,
    approvalConfUserTagKey,
    approvalConfUserTagLabel,
    projectMetricsTagCharts,
    maxDailyUserEvents,
    allowedSlidesUploadMimeTypes,
    allowedVideoUploadMimeTypes,
    videoUploadWarningMessage,
    slidesUploadWarningMessage,
    maxVideoCaptionsLength,
    maxVideoTranscriptLength,
    userPageTagsToDisplay,
    maxProjectInviteEmails,
    artifactBuildTimestamp,
    minUsernameLength,
    minPasswordLength,
    maxPasswordLength,
    maxFirstNameLength,
    maxLastNameLength,
    numProjectsToStartShowingAsCards,
    disableScrollToTop,
    dbUpgradeInProgress,
    exportHeaderAndFooter,
    limitAdminAccess,
    maxHostLength,
    maxGraderFeedbackMessageLength,
    maxTakeQuizInputTextAnswerLength,
    maxTextInputAiGradingCorrectAnswerLength,
    disableEncouragementsConfetti,
    contactSupportEnabled,
    contactSupportExternalUrl,
    contactSupportExternalTitle,
    contactSupportExternalDescription,
    contactSupportExternalEmail,
    contactSupportExternalEmailDescription,
    maxRolePageSize,
    matomoUrl,
    matomoSiteId,
    matomoProcessUserIdRegex,
    enableOpenAIIntegration,
    openaiTakingLongerThanExpectedMessages,
    openaiTakingLongerThanExpectedTimeoutPerMsg,
    openaiModelDefaultTemperature,
    openaiDefaultModel,
    openaiFooterMsg,
    openaiFooterPoweredByLink,
    openaiFooterPoweredByLinkText,
    openaiNotSupportedChatModels,
    openAiDisableSingleQuestionTypeChange,
    maxAiPromptLength,
    delayBetweenScreenReaderAnnouncements,
    openAiAnnounceGenStatusInterval,
    sdPointHistoryChartAchievementsCombinePct
  }
})
