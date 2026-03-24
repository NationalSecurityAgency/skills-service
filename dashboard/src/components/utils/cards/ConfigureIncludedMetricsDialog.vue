/*
Copyright 2026 SkillTree

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
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import {computed, onMounted, ref} from "vue";
import PickList from 'primevue/picklist';
import ProjectService from "@/components/projects/ProjectService.js";
import SettingsService from "@/components/settings/SettingsService.js";
import QuizService from "@/components/quiz/QuizService.js";

const props = defineProps({
  type: {
    type: String,
    default: 'Project',
    validator: (value) => ['Project', 'Quiz'].includes(value)
  }
})
const model = defineModel()
const emits = defineEmits(['on-saved'])
const loading = ref(true)
const submitting = ref(false)
const pickListArray = ref([])

const isQuiz = props.type === 'Quiz'
const isProj = !isQuiz
const itemsName = isQuiz ? 'Assessments' : 'Projects'

const settingName = 'globalMetricsExcludedItem'
onMounted(() => {
  SettingsService.getUserGlobalMetricsSettings(settingName).then((excludedItems) => {
    let excludedIds = isProj ?
        excludedItems?.filter((item) => item.projectId)?.map(item => item.projectId)
        : excludedItems?.filter((item) => item.quizId)?.map(item => item.quizId)

    excludedIds = excludedIds || []
    const dataLoader = isProj ? loadProjects : loadQuizzes
    dataLoader().then((resItems) => {
      const excludedItems = resItems.filter(item => excludedIds.includes(item.itemId))
      const includedItems = resItems.filter(item => !excludedIds.includes(item.itemId))
      pickListArray.value = [includedItems, excludedItems]
    }).finally(() => {
      loading.value = false
    })
  })
})

const loadProjects = () => {
  return ProjectService.getProjects()
      .then((response) => response.map((proj) => ({...proj, itemId: proj.projectId}) ))
}

const loadQuizzes = () => {
  return QuizService.getQuizDefs()
      .then((response) => response.map((proj) => ({...proj, itemId: proj.quizId}) ))
}

const close = () => {
  model.value = false
}

const onOk = () => {
  submitting.value = true

  const toSave = []
  toSave.push(...pickListArray.value[0].map(item => ({projectId: isProj ? item.itemId : null, quizId: isQuiz ? item.itemId : null, setting: settingName})))
  toSave.push(...pickListArray.value[1].map(item => ({projectId: isProj ? item.itemId : null, quizId: isQuiz ? item.itemId : null, setting: settingName, value: true})))
  SettingsService.saveUserGlobalMetricsSettings(toSave).finally(() => {
    submitting.value = false
    emits('on-saved')
    close()
  })

}
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      :header="`Configure Included ${itemsName}`"
      cancel-button-severity="secondary"
      ok-button-label="Save"
      @on-ok="onOk"
      @on-cancel="close"
      :loading="loading"
      :submitting="submitting"
      :enable-return-focus="true"
      :style="{ 'max-width': '50rem' }">
    <div class="pb-5 flex flex-col gap-4" data-cy="confIncludedMetricsDialog">
      <BlockUI :blocked="submitting">
        <PickList v-model="pickListArray"
                  dataKey="itemId"
                  :show-source-controls="false"
                  :show-target-controls="false">
          <template #option="{ option  }">
            <div class="flex gap-1">
              <div>{{ option.name }}</div>
              <div v-if="isQuiz" class="italic">({{ option.type }})</div>
            </div>
          </template>
          <template #sourceheader>
            <div class="border-b">Included {{ itemsName }}</div>
          </template>
          <template #targetheader>
            <div class="border-b">Excluded {{ itemsName }}</div>
          </template>
        </PickList>
      </BlockUI>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>