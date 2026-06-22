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
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue'
import { nextTick, ref } from 'vue'
import * as yup from 'yup'
import { number } from 'yup'

const model = defineModel()
const schema = yup.object().shape({
  'insertTableRows': number()
      .required()
      .min(1)
      .max(15)
      .label('Rows'),
  'insertTableColumns': number()
      .required()
      .min(1)
      .max(10)
      .label('Columns'),
})

const initialValues = ref({
  insertTableRows: 2,
  insertTableColumns: 3,
})

const emit = defineEmits(['insert-table'])

const tableDimensionsSaved = (values) => {
  const rows = values.insertTableRows
  const cols = values.insertTableColumns
  if (rows > 0 && cols > 0) {
    emit('insert-table', { rows, cols })
  }
  return Promise.resolve({})
}

const focusOnFirstInput = (type) => {
  nextTick(() => {
    initialValues.value = Object.assign({}, initialValues.value)
    const input = document.getElementById('inputinsertTableRows')
    input?.focus()
  })
}

</script>

<template>
  <div>
    <SkillsInputFormDialog
        :pt="{ header: { class: '!pb-2' }, footer: { class: '!pb-3' }}"
        v-model="model"
        :maximizable="false"
        :maximized="false"
        header="Insert Table"
        :style="{ width: '30rem' }"
        :enable-return-focus="true"
        :save-data-function="tableDimensionsSaved"
        :validation-schema="schema"
        :initial-values="initialValues"
        id="insertTableDialog"
        @show="focusOnFirstInput('show')"
        data-cy="insertTableDialog">
      <div class="flex flex-col gap-4 mb-4">
        <div class="flex flex-col gap-1">
          <label for="inputinsertTableRows">Rows</label>
          <SkillsNumberInput
              name="insertTableRows"
              :min="1"
              :max="15"
              :use-grouping="false"
              show-buttons
              data-cy="insertTableRowsInput"
              :autofocus="true"/>
        </div>

        <div class="flex flex-col gap-1">
          <label for="inputinsertTableColumns">Columns</label>
          <SkillsNumberInput
              name="insertTableColumns"
              :min="1"
              :max="10"
              :use-grouping="false"
              show-buttons
              data-cy="insertTableColumnsInput"/>
        </div>
      </div>
    </SkillsInputFormDialog>
  </div>
</template>

<style scoped>

</style>