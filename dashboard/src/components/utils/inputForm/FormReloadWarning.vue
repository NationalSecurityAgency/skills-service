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
import { ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const announcer = useSkillsAnnouncer()
const emit = defineEmits(['discard-changes'])
const visible = ref(true)

const handleDiscard = () => {
  emit('discard-changes', true)
  visible.value = false
  announcer.polite('Restored form\'s values were discarded')
}
</script>

<template>
  <Message
    v-if="visible"
    icon="fas fa-trash-restore"
    severity="error"
    :closable="false" :visible="visible">
      <div class="flex items-center gap-4">
        <div  data-cy="contentRestoredMessage">
          Form's values have been restored from backup.
        </div>
        <div class="flex-1 text-right">
          <SkillsButton
            data-cy="discardContentButton"
            label="Discard Restored"
            icon="fas fa-trash"
            severity="warn"
            size="small"
            @click="handleDiscard"
          />
        </div>
      </div>
  </Message>
</template>

<style scoped>

</style>