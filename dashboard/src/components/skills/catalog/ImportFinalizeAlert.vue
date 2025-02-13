/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import FinalizePreviewModal from '@/components/skills/catalog/FinalizePreviewModal.vue'
import LengthyOperationProgressBar from '@/components/utils/LengthyOperationProgressBar.vue'
import { useRoute } from 'vue-router'
import SettingsService from '@/components/settings/SettingsService.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useProjDetailsState } from '@/stores/UseProjDetailsState.js'

const finalizeState = useFinalizeInfoState()
const pluralSupport = useLanguagePluralSupport()
const route = useRoute()
const subjectsState = useSubjectsState()
const subjectSkillsState = useSubjectSkillsState()
const projDetailsState = useProjDetailsState()
const appConfig = useAppConfig()
onMounted(() => {
  finalizeState.loadInfo()
})
const shouldShow = computed(() => !finalizeState.isLoading && finalizeState.info.numSkillsToFinalize > 0)
const finalizeInfo = computed(() => finalizeState.info)
const dashboardSkillsCatalogGuide = computed(() => {
  return `${appConfig.docsHost}/dashboard/user-guide/skills-catalog.html#finalization`
})

const showFinalizeModal = ref(false)
watch(
  () => finalizeInfo.value.finalizeIsRunning,
  (isRunning) => {
    if (isRunning) {
      checkFinalizationState()
    }
  })

const getFinalizationState = () => {
  return SettingsService.getProjectSetting(route.params.projectId, 'catalog.finalize.state', false);
}
const checkFinalizationState = () => {
  setTimeout(() => {
    getFinalizationState().then((res) => {
      if (res) {
        if (res.value === 'RUNNING') {
          checkFinalizationState()
        } else if (res.value === 'COMPLETED') {
          finalizeState.info.finalizeIsRunning = false
          finalizeState.info.finalizeSuccessfullyCompleted = true
          if (route.params.subjectId) {
            subjectsState.loadSubjectDetailsState()
            subjectSkillsState.subjectSkills.forEach((sk) => {
              sk.enabled = true
            })
            // iterate over loaded group ids and update enabled attribute
            subjectSkillsState.groupSkills.forEach((values) => {
              values.forEach((sk) => {
                sk.enabled = true
              })
            })
          } else if (route.params.projectId) {
            subjectsState.loadSubjects({ projectId: route.params.projectId })
            projDetailsState.loadProjectDetailsState()
          }
        } else {
          finalizeState.info.finalizeIsRunning = false
          finalizeState.info.finalizeCompletedAndFailed = true
        }
      }
    })
      .catch(() => {
        checkFinalizationState()
      })
  }, 5000)
}
</script>

<template>
  <div v-if="shouldShow" data-cy="importFinalizeAlert" class="mb-0 mt-1">
    <Message
      v-if="finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeIsRunning"
      icon="fas fa-thumbs-up"
      severity="success">
      Successfully finalized
      <Tag severity="info">{{ finalizeInfo.numSkillsToFinalize }}</Tag>
      imported skill{{ pluralSupport.sOrNone(finalizeInfo.numSkillsToFinalize) }}! Please enjoy your day!
    </Message>
    <Message
      v-if="finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning"
      icon="fas fa-thumbs-down"
      severity="error">
      Well this is sad. Looks like finalization failed, please reach out to the SkillTree team for further assistance.
    </Message>
    <Message
      v-if="finalizeInfo.finalizeIsRunning && !finalizeInfo.finalizeSuccessfullyCompleted"
      icon="fas fa-running"
      :closable="false">
      Catalog finalization is in progress. Finalizing
      <Tag severity="info">{{ finalizeInfo.numSkillsToFinalize }}</Tag>
      imported skill{{ pluralSupport.sOrNone(finalizeInfo.numSkillsToFinalize) }}! The process may take a few
      minutes.
      <lengthy-operation-progress-bar
        style="height: 6px"
        :showValue="false"
        aria-label="Finalize Progress"
        class="mt-1" />
    </Message>
    <Message
      severity="warn"
      v-if="!finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning"
      :closable="false">
      <div class="sm:flex gap-2 items-center">
        <div>There {{ pluralSupport.areOrIs(finalizeInfo.numSkillsToFinalize) }}
          <Tag>{{ finalizeInfo.numSkillsToFinalize }}</Tag>
          imported skill{{ pluralSupport.sOrNone(finalizeInfo.numSkillsToFinalize) }} in this project that
          {{ pluralSupport.areOrIs(finalizeInfo.numSkillsToFinalize) }} not yet finalized. Once you have finished
          importing
          the skills you are interested in, finalize the import to enable those skills.
          Click <a :href="dashboardSkillsCatalogGuide" target="_blank">here <i class="fas fa-external-link-alt"></i></a>
          to learn more.
        </div>
        <div class="text-center mt-2">
          <SkillsButton
            id="finalizeImportBtn"
            icon="fas fa-check-double"
            label="Finalize"
            :track-for-focus="true"
            @click="showFinalizeModal = true"
            data-cy="finalizeBtn" />
        </div>
      </div>
    </Message>

    <finalize-preview-modal
      v-if="showFinalizeModal"
      v-model="showFinalizeModal" />
  </div>
</template>

<style scoped>

</style>