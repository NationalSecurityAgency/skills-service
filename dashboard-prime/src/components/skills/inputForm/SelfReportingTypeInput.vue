<script setup>
import { inject, ref } from 'vue'
import { useFieldValue } from 'vee-validate'

const approvalKey = 'Approval'
const videoKey = 'Video'

const enabled = ref(false)
const categories = ref([
  { name: 'Approval Queue', key: approvalKey },
  { name: 'Honor System', key: 'Honor' },
  { name: 'Quiz/Survey', key: 'Quiz' },
  { name: 'Video', key: videoKey, disabled: true }
])
const setFieldValue = inject('setFieldValue')
const onEnabledChanged = (newVal) => {
  setFieldValue('selfReportingType',  newVal ? categories.value[0].key : '')
}
const onTypeCanged = (newType) => {
  if (newType !== approvalKey) {
    justificationRequired.value = false
  }
}

const justificationRequired = ref(false)
const selfReportingType = useFieldValue('selfReportingType');
</script>

<template>
  <div class="flex">
    <div>
      <SkillsCheckboxInput
        @input="onEnabledChanged"
        :binary="true"
        v-model="enabled"
        inputId="selfReport"
        name="selfReportEnabled"
        data-cy=selfReportEnableCheckbox
        :value="true" />
      <label for="selfReport" class="ml-2"> Self Reporting </label>
    </div>

    <div class="card flex ml-4">
      <div class="flex flex-column gap-3">
        <div v-for="category in categories" :key="category.key" class="flex align-items-center">

          <SkillsRadioButtonInput
            @update:modelValue="onTypeCanged"
            :disabled="!enabled || category.disabled"
            :inputId="category.key"
            severity="info"
            name="selfReportingType"
            data-cy="selfReportTypeSelector"
            :value="category.key" />
          <label :for="category.key" class="ml-2">
            <span>{{ category.name }}</span>
            <span v-if="category.key === 'Approval'" class="ml-2">
              |
               <SkillsCheckboxInput
                 class="ml-2"
                 :binary="true"
                 :disabled="!enabled || selfReportingType !== 'Approval'"
                 v-model="justificationRequired"
                 inputId="selfReportJustificationRequired"
                 name="justificationRequired"
                 :value="true" />
                <label for="selfReportJustificationRequired" class="ml-2 font-italic">Justification Required</label>
            </span>
            <span v-if="category.key === 'Video'" class="ml-2 font-italic">(Please create skill and configure video settings first)</span>
          </label>
        </div>
      </div>
    </div>


  </div>
</template>

<style scoped>

</style>