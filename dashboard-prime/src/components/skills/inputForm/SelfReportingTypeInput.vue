<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useFieldValue } from 'vee-validate'
import QuizSelector from "@/components/skills/selfReport/QuizSelector.vue";

const props = defineProps({
  initialSkillData: Object,
  isEdit: Boolean
})
const emit = defineEmits(['quizIdChanged', 'selfReportingTypeChanged'])

const approvalKey = 'Approval'
const honorKey = 'HonorSystem'
const quizKey = 'Quiz'
const videoKey = 'Video'

// const enabled = ref(props.initialSkillData.selfReportingEnabled)
const categories = ref([
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: honorKey },
  { name: 'Quiz/Survey', key: quizKey },
  { name: 'Video', key: videoKey }
])
const setFieldValue = inject('setFieldValue')
const onEnabledChanged = (newVal) => {
  const selfReportingType = newVal ? approvalKey : ''
  setFieldValue('selfReportingType', selfReportingType)
  onTypeChanged(selfReportingType)
}
const onTypeChanged = (newType) => {
  if (newType !== approvalKey) {
    // justificationRequired.value = false
    setFieldValue('justificationRequired', false)
  }
  if (newType === quizKey || newType === videoKey) {
    setFieldValue('numPerformToCompletion', 1)
    setFieldValue('timeWindowEnabled', false)
    setFieldValue('numPointIncrementMaxOccurrences', 1)
  }
  emit('selfReportingTypeChanged', newType)
}
const quizIdSelected = (quizId) => {
  emit('quizIdChanged', quizId)
}

const justificationRequired = useFieldValue('justificationRequired');
const selfReportingType = useFieldValue('selfReportingType');
const enabled = useFieldValue('selfReportingEnabled');
const quizSelected = computed(() => selfReportingType.value === quizKey)
const isVideoChoiceDisabled = computed(() => !enabled.value || !props.isEdit || (props.isEdit && !props.initialSkillData.hasVideoConfigured))
const isVideoConfigured = computed(() => props.initialSkillData.hasVideoConfigured )
</script>

<template>
  <div class="flex flex-wrap md:flex-no-wrap">
    <!--        @input="onEnabledChanged"-->
    <div class="w-min-10rem mt-4 w-full md:w-auto">
      <SkillsCheckboxInput
        @update:modelValue="onEnabledChanged"
        :binary="true"
        v-model="enabled"
        inputId="selfReport"
        name="selfReportingEnabled"
        data-cy=selfReportEnableCheckbox />
      <label for="selfReport" class="ml-2"> Self Reporting </label>
    </div>

    <div class="card flex flex-1 sm:ml-4 mt-4">
      <div class="flex flex-column gap-3 w-full" data-cy="selfReportTypeSelector">
        <div v-for="category in categories" :key="category.key" class="flex align-items-start">
          <SkillsRadioButtonInput
            @update:modelValue="onTypeChanged"
            :disabled="!enabled || (category.key === videoKey && isVideoChoiceDisabled)"
            :inputId="category.key"
            severity="info"
            name="selfReportingType"
            :data-cy="`${category.key.toLowerCase()}Radio`"
            :value="category.key" />
          <label :for="category.key" class="ml-2 flex w-full">
            <span :class="{ 'text-color-secondary' : !enabled || (category.key === videoKey && isVideoChoiceDisabled) }">{{ category.name }}</span>
            <div v-if="category.key === approvalKey" class="ml-2">
              |
               <SkillsCheckboxInput
                 class="ml-2"
                 :binary="true"
                 :disabled="!enabled || selfReportingType !== 'Approval'"
                 inputId="selfReportJustificationRequired"
                 name="justificationRequired"
                 :value="true" />
                <label
                  for="selfReportJustificationRequired"
                  :class="{ 'text-color-secondary' : !enabled || selfReportingType !== 'Approval' }"
                  class="ml-2 font-italic">Justification Required</label>
            </div>
            <div v-if="category.key === quizKey" class="pl-2 w-full">
              <QuizSelector class="mb-0" v-if="category.key === quizKey && enabled && quizSelected" :initiallySelectedQuizId="initialSkillData.quizId" @changed="quizIdSelected" />
            </div>
            <span v-if="category.key === videoKey && !isVideoConfigured" class="ml-2 font-italic" :class="{ 'text-color-secondary' : !enabled || (category.key === videoKey && isVideoChoiceDisabled) }" data-cy="videoSelectionMsg">
                  <span v-if="!isEdit">(Please create skill and configure video settings first)</span>
                  <span v-else>(Please configure video settings first)</span>
                </span>
          </label>
        </div>
      </div>
    </div>


  </div>
</template>

<style scoped>

</style>