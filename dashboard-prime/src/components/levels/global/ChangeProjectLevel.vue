<script setup>
import { ref } from 'vue';
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import LevelSelector from "@/components/levels/global/LevelSelector.vue";

const emit = defineEmits(['level-changed', 'hidden']);
const props = defineProps({
  projectId: {
    type: String,
    required: true,
  },
  currentLevel: {
    type: Number,
    required: true,
  },
  title: {
    type: String,
    required: false,
    default: '',
  },
});

const show = ref(true);
const newLevel = ref(null);
const oldLevel = ref(props.currentLevel);

const saveLevelChange = () => {
  emit('level-changed', {
    projectId: props.projectId,
    oldLevel: props.currentLevel,
    newLevel: newLevel.value,
  });
  closeMe();
};

const closeMe = () => {
  show.value = false;
  publishHidden();
};

const publishHidden = () => {
  emit('hidden', { projectId: props.projectId });
};

const selectLevel = (level) => {
  newLevel.value = level;
}
</script>

<template>
  <SkillsDialog
      v-model="show"
      header="Change Level"
      :enable-return-focus="true"
      cancel-button-label="Cancel"
      cancel-button-icon=""
      cancel-button-severity="success"
      @on-ok="saveLevelChange"
      @on-cancel="closeMe"
      :style="{ width: '40rem !important' }">

    <div class="mb-4">
      <div>
        <label for="existingLevel">Existing level</label>
      </div>
      <div>
        <InputNumber id="existingLevel" :disabled="true" v-model="oldLevel" class="w-full" />
      </div>
    </div>

    <div class="mb-4">
      <label for="newLevel">New level</label>
      <level-selector v-model="newLevel"
                      :load-immediately="true"
                      @input="selectLevel"
                      inputId="newLevel"
                      :project-id="projectId"
                      :selectedLevel="oldLevel"
                      placeholder="select new project level"></level-selector>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>