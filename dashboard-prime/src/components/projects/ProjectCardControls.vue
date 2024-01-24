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
    <Button
        :to="{ name:'Subjects', params: { projectId: project.projectId, project: project }}"
        size="small" class="border-1 border-black-alpha-90 mr-2"
        :data-cy="'projCard_' + project.projectId + '_manageBtn'"
        :aria-label="'manage project + project.name'">
      <span v-if="readOnlyProject">View</span><span v-else>Manage</span> <i class="fas fa-arrow-circle-right ml-1" aria-hidden="true"/>
    </Button>
    <Button v-if="isRootUser"
            class="border-1 border-black-alpha-90 mr-2" @click="emit('unpin-project')" data-cy="unpin" size="small"
              aria-label="'remove pin for project '+ project.name"
              :aria-pressed="project.pinned">
      <span class="d-none d-sm-inline mr-1">Unpin</span> <i class="fas fa-ban" aria-hidden="true"/>
    </Button>
      <span class="p-buttonset mr-2" v-if="!readOnlyProject">
        <Button ref="editBtn"
                class="border-1 border-black-alpha-90"
                size="small"
                @click="emit('edit-project')"
                title="Edit Project"
                :aria-label="'Edit Project ' + project.name"
                role="button"
                data-cy="editProjBtn"><i class="fas fa-edit" aria-hidden="true"/></Button>

        <Button ref="copyBtn"
                class="border-1 border-black-alpha-90"
                size="small"
                @click="emit('copy-project')"
                title="Copy Project"
                :aria-label="'Copy Project ' + project.name"
                role="button"
                data-cy="copyProjBtn"><i class="fas fa-copy" aria-hidden="true"/></Button>

          <Button variant="outline-primary"
                  class="border-1 border-black-alpha-90"
                  ref="deleteBtn"
                  size="small"
                  @click="emit('delete-project')"
                  :disabled="isDeleteDisabled"
                  v-tooltip="deleteDisabledText"
                  title="Delete Project"
                  :aria-label="'Delete Project ' + project.name"
                  role="button"
                  data-cy="deleteProjBtn"><i class="text-warning fas fa-trash" aria-hidden="true"/></Button>
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
