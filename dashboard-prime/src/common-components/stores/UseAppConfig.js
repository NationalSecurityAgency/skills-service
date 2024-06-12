import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import SettingsService from '@/components/settings/SettingsService.js'

export const useAppConfig = defineStore('dashboardAppConfig', () => {
  const loadingConfig = ref(true)
  const config = ref({})

  const loadConfigState = () => {
    loadingConfig.value = true
    return SettingsService.getConfig()
      .then((response) => {
        config.value = response
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

  const maxBadgeBonusInMinutes = computed(() => config.value.maxBadgeBonusInMinutes)
  const minNameLength = computed(() => config.value.minNameLength)
  const maxProjectNameLength = computed(() => config.value.maxProjectNameLength)
  const maxQuizNameLength = computed(() => config.value.maxQuizNameLength)
  const nameValidationRegex = computed(() => config.value.nameValidationRegex)
  const minIdLength = computed(() => config.value.minIdLength)
  const maxIdLength = computed(() => config.value.maxIdLength)
  const descriptionMaxLength = computed(() => config.value.descriptionMaxLength)
  const paragraphValidationRegex = computed(() => config.value.paragraphValidationRegex)
  const formFieldDebounceInMs = computed(() => config.value.formFieldDebounceInMs || 400)
  const maxSubjectNameLength = computed(() => config.value.maxSubjectNameLength)
  const maxBadgeNameLength = computed(() => config.value.maxBadgeNameLength)
  const maxCustomLabelLength = computed(() => config.value.maxCustomLabelLength)
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
  const attachmentWarningMessage = computed(() => config.value.attachmentWarningMessage)
  const allowedAttachmentMimeTypes = computed(() => config.value.allowedAttachmentMimeTypes)
  const docsHost = computed(() => config.value.docsHost)
  const maxBadgesPerProject = computed(() => config.value.maxBadgesPerProject)
  const isPkiAuthenticated = computed(() => config.value.authMode === 'PKI')
  const expirationGracePeriod = computed(() => config.value.expirationGracePeriod)
  const expireUnusedProjectsOlderThan = computed(() => config.value.expireUnusedProjectsOlderThan)
  const minimumProjectPoints = computed(() => config.value.minimumProjectPoints)
  const maxProjectsPerAdmin = computed(() => config.value.maxProjectsPerAdmin)
  const minimumSubjectPoints = computed(() => config.value.minimumSubjectPoints)
  const maxSubjectsPerProject = computed(() => config.value.maxSubjectsPerProject)
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
  const videoUploadWarningMessage = computed(() => config.value.videoUploadWarningMessage)
  const maxVideoCaptionsLength = computed(() => config.value.maxVideoCaptionsLength)
  const maxVideoTranscriptLength = computed(() => config.value.maxVideoTranscriptLength)
  const userPageTagsToDisplay = computed(() => config.value.userPageTagsToDisplay)
  const maxProjectInviteEmails = computed(() => config.value.maxProjectInviteEmails)
  const artifactBuildTimestamp = computed(() => config.value.artifactBuildTimestamp)
  const minUsernameLength = computed(() => config.value.minUsernameLength)
  const minPasswordLength = computed(() => config.value.minPasswordLength)
  const maxPasswordLength = computed(() => config.value.maxPasswordLength)
  const numProjectsToStartShowingAsCards = computed(() => config.value.numProjectsToStartShowingAsCards)
  return {
    loadConfigState,
    refreshConfig,
    isLoadingConfig,
    defaultLandingPage,
    getConfigsThatStartsWith,
    rankingAndProgressViewsEnabled,
    docsHost,
    minNameLength,
    maxProjectNameLength,
    maxQuizNameLength,
    nameValidationRegex,
    minIdLength,
    maxIdLength,
    descriptionMaxLength,
    formFieldDebounceInMs,
    paragraphValidationRegex,
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
    attachmentWarningMessage,
    allowedAttachmentMimeTypes,
    maxBadgesPerProject,
    isPkiAuthenticated,
    expirationGracePeriod,
    expireUnusedProjectsOlderThan,
    minimumProjectPoints,
    maxProjectsPerAdmin,
    minimumSubjectPoints,
    maxSubjectsPerProject,
    userCommunityBeforeLabel,
    userCommunityAfterLabel,
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
    allowedVideoUploadMimeTypes,
    videoUploadWarningMessage,
    maxVideoCaptionsLength,
    maxVideoTranscriptLength,
    userPageTagsToDisplay,
    maxProjectInviteEmails,
    artifactBuildTimestamp,
    minUsernameLength,
    minPasswordLength,
    maxPasswordLength,
    numProjectsToStartShowingAsCards,
  }
})