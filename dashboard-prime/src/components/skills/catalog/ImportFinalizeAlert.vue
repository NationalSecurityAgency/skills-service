<script setup>
import { computed, onMounted, ref } from 'vue'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import FinalizePreviewModal from '@/components/skills/catalog/FinalizePreviewModal.vue'

const finalizeState = useFinalizeInfoState()
const pluralSupport = useLanguagePluralSupport()
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
</script>

<template>
  <div v-if="shouldShow" data-cy="importFinalizeAlert" class="mb-0 mt-1">
    <Message v-if="finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeIsRunning">
      <i class="fas fa-thumbs-up"></i> Successfully finalized
      <Tag severity="info">{{ finalizeInfo.numSkillsToFinalize }}</Tag>
      imported skill{{ pluralSupport.sOrNone(finalizeInfo.numSkillsToFinalize) }}! Please enjoy your day!
    </Message>
    <Message v-if="finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning">
      <i class="fas fa-thumbs-down"></i> Well this is sad. Looks like finalization failed, please
      reach out to the SkillTree team for further assistance.
    </Message>
    <Message v-if="finalizeInfo.finalizeIsRunning && !finalizeInfo.finalizeSuccessfullyCompleted" :closable="false">
      <i class="fas fa-running"></i> Catalog finalization is in progress. Finalizing
      <Tag severity="info">{{ finalizeInfo.numSkillsToFinalize }}</Tag>
      imported skill{{ pluralSupport.sOrNone(finalizeInfo.numSkillsToFinalize) }}! The process may take a few
      minutes.
      <!--      <lengthy-operation-progress-bar name="Finalize" class="mb-3"/>-->
    </Message>
    <Message
      severity="warn"
      v-if="!finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning"
      :closable="false">
      <div class="sm:flex gap-2 align-items-center">
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