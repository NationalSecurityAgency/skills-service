<script setup>
import { computed } from 'vue';

const props = defineProps(['options']);
const emit = defineEmits(['edit', 'delete', 'share', 'unshare']);

let isReadOnlyProj = false;

const shareBtnIcon = computed(() => {
  return props.options?.shareEnabled === true ? 'fas fa-hands-helping' : 'fas fa-handshake-alt-slash';
});

const shareTitle = computed(() => {
  return props.options?.shareEnabled === true ? `Share ${props.options?.type}` : `Unshare ${props.options?.type}`;
});

const handleShareClick = () => {
  let eventName = 'share';
  if (props.options.shareEnabled === false) {
    eventName = 'unshare';
  }
  emit(eventName);
};

const focusOnEdit = () => {
  this.$refs.editBtn.focus();
};

const focusOnDelete = () => {
  this.$refs.deleteBtn.focus();
};
</script>

<template>
  <div class="flex" :class="{ 'justify-content-center' : isReadOnlyProj }">
    <div class="col-auto">
      <SkillsButton
          :to="options.navTo"
          class="border-1 border-black-alpha-90" size="small"
          :aria-label="`Manage ${options.type} ${options.name}`"
          icon="fas fa-arrow-circle-right"
          :label="isReadOnlyProj ? 'View' : 'Manage'"
          :data-cy="`manageBtn_${options.id}`">
      </SkillsButton>
    </div>

    <div v-if="!isReadOnlyProj" class="col text-right">
      <span class="p-buttonset">
        <SkillsButton v-if="options.showShare === true"
                ref="shareBtn"
                size="small"
                label=""
                :icon="shareBtnIcon"
                class="border-1 border-black-alpha-90"
                @click="handleShareClick"
                :title="shareTitle"></SkillsButton>
        <SkillsButton ref="editBtn"
                size="small"
                class="border-1 border-black-alpha-90"
                @click="emit('edit')"
                :title="`Edit ${options.type}`"
                :aria-label="`Edit ${options.type} ${options.name}`"
                role="button"
                label=""
                icon="fas fa-edit"
                data-cy="editBtn"><i class="fas fa-edit" aria-hidden="true"/></SkillsButton>

          <SkillsButton variant="outline-primary"
                  class="border-1 border-black-alpha-90"
                  v-tooltip="options.deleteDisabledText"
                  ref="deleteBtn"
                  size="small"
                  @click="emit('delete')"
                  :disabled="options.isDeleteDisabled"
                  :title="`Delete ${options.type}`"
                  :aria-label="options.deleteDisabledText ? options.deleteDisabledText : `Delete ${options.type} ${options.name}`"
                  role="button"
                  label="" icon="text-warning fas fa-trash"
                  data-cy="deleteBtn"></SkillsButton>
      </span>
    </div>
  </div>
</template>

<style scoped>
.last-right-group-btn {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: none;
}
</style>
