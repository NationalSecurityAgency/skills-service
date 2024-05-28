<script setup>
import { ref, watch } from 'vue';
import LengthyOperationProgressBar from "@/components/utils/LengthyOperationProgressBar.vue";

const model = defineModel();

const emit = defineEmits(['operation-done']);
defineProps({
  title: {
    type: String,
    required: true,
  },
  progressMessage: {
    type: String,
    required: true,
  },
  isComplete: {
    type: Boolean,
    required: true,
  },
  successMessage: {
    type: String,
    default: 'Operation completed successfully!',
  },
});

const allDone = () => {
  emit('operation-done');
  model.value = false;
};
</script>

<template>
  <Dialog modal v-model:visible="model" :maximizable="false" :closable="false" :header="title"
          :pt="{ maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }">

    <div class="text-center" data-cy="lengthyOpModal">
      <div v-if="!isComplete">
        <i class="fas fa-running p-2 mb-1 p-badge p-badge-info" style="font-size: 2.5rem; height: 100%;"/>
        <div class="h4 text-primary mb-3" data-cy="title">{{ progressMessage }}</div>
        <lengthy-operation-progress-bar :showValue="false" height="15px" :animated="true"/>
        <div class="text-secondary mt-1">This operation takes a little while so buckle up!</div>
      </div>
      <div v-else>
        <i class="fas fa-check-double p-2 mb-1 p-badge p-badge-info" style="font-size: 2.5rem; height: 100%;"/>
        <div class="h4 text-primary mb-1 mt-1">We are all done!</div>
        <div class="text-secondary" data-cy="successMessage">{{ successMessage }}</div>
      </div>
    </div>

    <template #footer>
      <SkillsButton v-if="isComplete" variant="success" size="small" class="float-right" @click="allDone" data-cy="allDoneBtn">
        Done
      </SkillsButton>
    </template>
  </Dialog>
</template>

<style scoped>

</style>