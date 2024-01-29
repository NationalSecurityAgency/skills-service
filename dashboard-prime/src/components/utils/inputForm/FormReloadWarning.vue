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
    severity="error"
    :closable="false" :visible="visible">
    <template #container>
      <div class="flex w-full p-message-wrapper">
        <div class="p-message-text">
          <i class="fas fa-trash-restore pr-2 text-xl" aria-hidden="true"></i>
          Form's values have been restored from backup.
        </div>
        <div class="flex-1 text-right">
          <SkillsButton
            label="Discard Restored"
            icon="fas fa-trash"
            severity="warning"
            size="small"
            @click="handleDiscard"
          />
        </div>
      </div>
    </template>
  </Message>
</template>

<style scoped>

</style>