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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjConfig } from '@/stores/UseProjConfig.js'

const projConfig = useProjConfig();
const props = defineProps({
  options: Object,
  to: Object,
  buttonIdSuffix: {
    type: String,
    required: true
  }
})
const emit = defineEmits(['edit', 'delete'])
const router = useRouter()

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);


const editBtn = ref();
const deleteBtn = ref();

const handleManageClick = () => {
  if (props.to) {
    router.push(props.to)
  }
}

const focusOnEdit = () => {
  // editBtn.value.focus();
}

const focusOnDelete = () => {
  // deleteBtn.value.focus();
}

defineExpose({
  focusOnEdit,
  focusOnDelete,
})

</script>

<template>
  <div class="flex" :class="{ 'justify-content-center' : isReadOnlyProj }">
    <div class="">
      <SkillsButton
        size="small"
        outlined
        severity="info"
        @click="handleManageClick"
        :aria-label="`Manage ${options.type} ${options.name}`"
        icon="fas fa-arrow-circle-right"
        :label="isReadOnlyProj ? 'View' : 'Manage'"
        :data-cy="`manageBtn_${options.id}`">
      </SkillsButton>
    </div>

    <div v-if="!isReadOnlyProj" class="flex-1 text-right">
      <ButtonGroup>
        <SkillsButton
          :id="`editBtn${buttonIdSuffix}`"
          ref="editBtn"
          icon="fas fa-edit"
          size="small"
          outlined
          severity="info"
          @click="emit('edit')"
          :track-for-focus="true"
          :title="`Edit ${options.type}`"
          :aria-label="`Edit ${options.type} ${options.name}`"
          role="button"
          label=""
          data-cy="editBtn" />

        <SkillsButton
          :id="`deleteBtn${buttonIdSuffix}`"
          variant="outline-primary"
          v-tooltip="options.deleteDisabledText"
          ref="deleteBtn"
          size="small"
          outlined
          severity="info"
          @click="emit('delete')"
          :disabled="options.isDeleteDisabled"
          :title="`Delete ${options.type}`"
          :track-for-focus="true"
          :aria-label="options.deleteDisabledText ? options.deleteDisabledText : `Delete ${options.type} ${options.name}`"
          role="button"
          label="" icon="text-warning fas fa-trash"
          data-cy="deleteBtn"></SkillsButton>
      </ButtonGroup>
    </div>
  </div>
</template>

<style scoped>

</style>
