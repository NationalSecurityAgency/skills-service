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
      <Button
          :to="options.navTo"
          class="border-1 border-black-alpha-90" size="small"
          :aria-label="`Manage ${options.type} ${options.name}`"
          :data-cy="`manageBtn_${options.id}`">
        <span v-if="isReadOnlyProj">View</span><span v-else>Manage</span> <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
      </Button>
    </div>

    <div v-if="!isReadOnlyProj" class="col text-right">
      <span class="p-buttonset">
        <Button v-if="options.showShare === true"
                ref="shareBtn"
                size="small"
                class="border-1 border-black-alpha-90"
                @click="handleShareClick"
                :title="shareTitle"><i :class="shareBtnIcon" aria-hidden="true"/></Button>
        <Button ref="editBtn"
                size="small"
                class="border-1 border-black-alpha-90"
                @click="emit('edit')"
                :title="`Edit ${options.type}`"
                :aria-label="`Edit ${options.type} ${options.name}`"
                role="button"
                data-cy="editBtn"><i class="fas fa-edit" aria-hidden="true"/></Button>

          <Button variant="outline-primary"
                  class="border-1 border-black-alpha-90"
                  v-tooltip="options.deleteDisabledText"
                  ref="deleteBtn"
                  size="small"
                  @click="emit('delete')"
                  :disabled="options.isDeleteDisabled"
                  :title="`Delete ${options.type}`"
                  :aria-label="options.deleteDisabledText ? options.deleteDisabledText : `Delete ${options.type} ${options.name}`"
                  role="button"
                  data-cy="deleteBtn"><i class="text-warning fas fa-trash" aria-hidden="true"/></Button>
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
