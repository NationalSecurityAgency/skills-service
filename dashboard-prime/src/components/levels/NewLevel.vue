<script setup>
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";

const props = defineProps({
  levelAsPoints: Boolean,
  level: Object,
  boundaries: Object,
  isEdit: Boolean,
  value: Boolean,
  allLevels: Array,
});

const model = defineModel()
const isDisabled = false;
const saveLevel = () => {

};

const cancel = () => {

}

</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model:visible="model"
      :header="isEdit ? 'Edit Level' : 'New Level'"
      cancel-button-severity="secondary"
      ok-button-severity="danger"
      :ok-button-icon="'fas fa-trash'"
      ok-button-label="Save"
      :ok-button-disabled="isDisabled"
      @on-ok="saveLevel"
      @on-cancel="cancel"
      :enable-return-focus="true"
      :style="{ width: '40rem !important' }">

  </SkillsDialog>
</template>