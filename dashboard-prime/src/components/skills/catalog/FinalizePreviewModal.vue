<script setup>
import { computed, onMounted, ref } from 'vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const model = defineModel()
const pluralSupport = useLanguagePluralSupport()
const numberFormat = useNumberFormat()
const route = useRoute()
const appConfig = useAppConfig()
const finalizeInfoState = useFinalizeInfoState()
const canFinalize = ref(false)
const finalize = () => {
  CatalogService.finalizeImport(route.params.projectId)
    .finally(() => {
      finalizeInfoState.info.finalizeIsRunning = true
      model.value = false
    })
}

onMounted(() => {
  loadFinalizeInfo()
})

const loadingTotalPointsNotFinalized = ref(true)
const loading = computed(() => loadingTotalPointsNotFinalized.value || finalizeInfoState.isLoading)
const noFinalizeMsg = ref('')
const loadFinalizeInfo = () => {
  // finalizeInfoState.loadInfo()

  loadingTotalPointsNotFinalized.value = true
  CatalogService.getTotalPointsIncNotFinalized(route.params.projectId)
    .then((countData) => {
      if (countData.insufficientProjectPoints || countData.subjectsWithInsufficientPoints.length > 0) {
        canFinalize.value = false
        if (countData.insufficientProjectPoints) {
          noFinalizeMsg.value = `Finalization cannot be performed until ${countData.projectName} has at least ${appConfig.minimumProjectPoints} points. Finalizing currently imported Skills would only bring ${countData.projectName} to ${countData.projectTotalPoints} points.`
        } else {
          const insufficientSubjects = countData.subjectsWithInsufficientPoints.map((c) => c.subjectName).join(', ')
          const insufficientSubjectsWithPts = countData.subjectsWithInsufficientPoints.map((c) => `${c.subjectName}: ${c.totalPoints} points`).join(', ')
          noFinalizeMsg.value = `Finalization cannot be performed until ${insufficientSubjects} ${countData.subjectsWithInsufficientPoints.length > 1 ? 'have' : 'has'}
                at least ${appConfig.minimumSubjectPoints} points. Finalizing the currently imported skills would only result in ${insufficientSubjectsWithPts}.`
        }
      } else {
        canFinalize.value = true
      }
    })
    .finally(() => {
      loadingTotalPointsNotFinalized.value = false
    })
}

const finalizeInfo = computed(() => finalizeInfoState.info)

const showFinalizeWarningSkillsPointsTable = ref (false)
const close = () => {
  model.value = false
}
</script>

<template>
  <SkillsDialog
    :maximizable="false"
    v-model="model"
    header="Finalize Imported Skills"
    cancel-button-severity="secondary"
    ok-button-severity="danger"
    ok-button-icon="fas fa-check-double"
    ok-button-label="Let's Finalize!"
    :ok-button-disabled="!canFinalize"
    @on-ok="finalize"
    @on-cancel="close"
    :enable-return-focus="true">
    <skills-spinner :is-loading="loading" class="mb-5" />
    <div v-if="!loading">
      <p>
        There {{ pluralSupport.areOrIs(finalizeInfo.numSkillsToFinalize) }}
        <Tag>{{ finalizeInfo.numSkillsToFinalize }}</Tag>
        skill{{ pluralSupport.plural(finalizeInfo.numSkillsToFinalize) }} to finalize.
        Please note that the finalization process may take <i>several moments</i>.
      </p>
      <p>
        The finalization process includes:
        <ul>
          <li>Imported skills will <b>now</b> contribute to the overall project and subject points.</li>
          <li>Skill points are migrated to this project for <b>all of the users</b> who made progress in the imported
            skills <i>(in the original project)</i>.
          </li>
          <li>Project and subject <b>level</b> achievements are calculated for the users that have points for the
            imported skills.
          </li>
        </ul>
      </p>
      <p v-if="finalizeInfo.skillsWithOutOfBoundsPoints && finalizeInfo.skillsWithOutOfBoundsPoints.length > 0"
         class="alert alert-danger" data-cy="outOfRangeWarning">
        <i class="fas fa-exclamation-triangle"></i> Your Project skills point values range from <span
        class="text-primary font-weight-bold">[{{ numberFormat.pretty(finalizeInfo.projectSkillMinPoints) }}]</span> to
        <span class="text-primary font-weight-bold">[{{ numberFormat.pretty(finalizeInfo.projectSkillMaxPoints)
          }}]</span>.
        <Tag variant="info">{{ numberFormat.pretty(finalizeInfo.skillsWithOutOfBoundsPoints.length) }}</Tag>
        skills you are importing fall outside of that point value. This could cause the imported
        skills to have an outsized impact on the achievements within your Project. Please consider changing the <b>Point
        Increment</b> of the imported skills.
        <SkillsButton
          :label="`View ${numberFormat.pretty(finalizeInfo.skillsWithOutOfBoundsPoints.length)} skills`"
          size="small"
          severity="info"
          @click="showFinalizeWarningSkillsPointsTable = !showFinalizeWarningSkillsPointsTable"
          data-cy="viewSkillsWithPtsOutOfRange"/>
        outside of the point value.

        <!--        <finalize-warning-skills-points-table v-if="showFinalizeWarningSkillsPointsTable" class="mt-2"-->
        <!--                                              :project-skill-min-points="finalizeInfo.projectSkillMinPoints"-->
        <!--                                              :project-skill-max-points="finalizeInfo.projectSkillMaxPoints"-->
        <!--                                              :skills-with-out-of-bounds-points="finalizeInfo.skillsWithOutOfBoundsPoints" />-->
      </p>
      <p v-if="!canFinalize" data-cy="no-finalize">
        <i class="fas fa-exclamation-circle mr-1 text-warning" aria-hidden="true" /> {{ noFinalizeMsg }}
      </p>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>