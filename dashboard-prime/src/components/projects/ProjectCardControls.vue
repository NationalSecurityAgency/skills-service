/*
Copyright 2020 SkillTree

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
import { computed } from 'vue';
import { useStore } from 'vuex';

const store = useStore();
const props = defineProps(['project', 'isDeleteDisabled', 'deleteDisabledText', 'readOnlyProject'])
const emit = defineEmits(['edit-project', 'unpin-project', 'copy-project', 'delete-project'])

const isRootUser = computed(() => {
  return store.getters['access/isRoot'];
});

const focusOnEdit = () => {
  this.$refs.editBtn.focus();
};

const focusOnCopy = () => {
  this.$refs.copyBtn.focus();
};

const focusOnDelete = () => {
  this.$nextTick(() => this.$refs.deleteBtn.focus());
};
</script>

<template>
  <div class="flex justify-content-end">
    <SkillsButton
        :to="{ name:'Subjects', params: { projectId: project.projectId, project: project }}"
        size="small" class="border-1 border-black-alpha-90 mr-2"
        :data-cy="'projCard_' + project.projectId + '_manageBtn'"
        :label="readOnlyProject ? 'View' : 'Manage'"
        icon="fas fa-arrow-circle-right ml-1"
        :aria-label="'manage project + project.name'">
    </SkillsButton>
    <SkillsButton v-if="isRootUser"
            class="border-1 border-black-alpha-90 mr-2" @click="emit('unpin-project')" data-cy="unpin" size="small"
              aria-label="'remove pin for project '+ project.name"
              label="Unpin" icon="fas fa-ban"
              :aria-pressed="project.pinned">
      <span class="d-none d-sm-inline mr-1">Unpin</span> <i class="fas fa-ban" aria-hidden="true"/>
    </SkillsButton>
      <span class="p-buttonset mr-2" v-if="!readOnlyProject">
        <SkillsButton ref="editBtn"
                class="border-1 border-black-alpha-90"
                size="small"
                @click="emit('edit-project')"
                title="Edit Project"
                :aria-label="'Edit Project ' + project.name"
                role="button"
                label="" icon="fas fa-edit"
                data-cy="editProjBtn"></SkillsButton>

        <SkillsButton ref="copyBtn"
                class="border-1 border-black-alpha-90"
                size="small"
                @click="emit('copy-project')"
                title="Copy Project"
                :aria-label="'Copy Project ' + project.name"
                role="button"
                icon="fas fa-copy"
                label=""
                data-cy="copyProjBtn"></SkillsButton>

          <SkillsButton variant="outline-primary"
                  class="border-1 border-black-alpha-90"
                  ref="deleteBtn"
                  size="small"
                  @click="emit('delete-project')"
                  :disabled="isDeleteDisabled"
                  v-tooltip="deleteDisabledText"
                  title="Delete Project"
                  :aria-label="'Delete Project ' + project.name"
                  role="button"
                  label=""
                  icon="text-warning fas fa-trash"
                  data-cy="deleteProjBtn"></SkillsButton>
      </span>

  </div>
</template>

<style scoped>
.last-right-group-btn {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: none;
}
</style>
