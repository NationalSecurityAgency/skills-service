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
import { computed } from 'vue'
import { useAccessState } from '@/stores/UseAccessState.js'
import { useAdminProjectsState } from '@/stores/UseAdminProjectsState.js'

const accessState = useAccessState()
const projectsState = useAdminProjectsState()

const props = defineProps(['project', 'readOnlyProject'])
const emit = defineEmits(['edit-project', 'unpin-project', 'copy-project', 'delete-project'])

const isRootUser = computed(() => accessState.isRoot)
</script>

<template>
  <div class="flex flex-wrap gap-2"
    :class="{
      'flex-col justify-center items-center': projectsState.shouldTileProjectsCards,
      'justify-end': !projectsState.shouldTileProjectsCards
    }">
    <div class="flex gap-2">
      <router-link :to="{ name:'Subjects', params: { projectId: project.projectId }}" tabindex="-1">
        <SkillsButton
            size="small"
            outlined
            severity="info"
            :data-cy="'projCard_' + project.projectId + '_manageBtn'"
            :label="readOnlyProject ? 'View' : 'Manage'"
            icon="fas fa-arrow-circle-right"
            :aria-label="'manage project ' + project.name">
        </SkillsButton>
      </router-link>
      <SkillsButton
        v-if="isRootUser"
        outlined
        severity="info"
        @click="emit('unpin-project')"
        data-cy="unpin"
        size="small"
        class="mr-2"
        :aria-label="'remove pin for project '+ project.name"
        label="Unpin"
        icon="fas fa-ban"
        :aria-pressed="project.pinned">
    </SkillsButton>
    </div>
    <ButtonGroup class="mr-4 p-0 flex" v-if="!readOnlyProject">
      <SkillsButton
        :id="`editProjBtn${project.projectId}`"
        :track-for-focus="true"
        ref="editBtn"
        outlined
        severity="info"
        size="small"
        @click="emit('edit-project', $event.target.value)"
        title="Edit Project"
        :aria-label="'Edit Project ' + project.name"
        role="button"
        icon="fas fa-edit"
        data-cy="editProjBtn" />

      <SkillsButton
        :id="`copyProjBtn${project.projectId}`"
        ref="copyBtn"
        outlined
        severity="info"
        :track-for-focus="true"
        size="small"
        @click="emit('copy-project', $event.target.value)"
        title="Copy Project"
        :aria-label="'Copy Project ' + project.name"
        role="button"
        icon="fas fa-copy"
        label=""
        data-cy="copyProjBtn" />

      <SkillsButton
        :id="`deleteProjBtn${project.projectId}`"
        v-if="!project.isDeleteProtected"
        outlined
        severity="info"
        ref="deleteBtn"
        size="small"
        :track-for-focus="true"
        class="p-text-secondary"
        @click="emit('delete-project', $event.target.value)"
        title="Delete Project"
        :aria-label="'Delete Project ' + project.name"
        role="button"
        icon="text-warning fas fa-trash p-text-warning"
        data-cy="deleteProjBtn" />
    </ButtonGroup>

  </div>
</template>

<style scoped>
</style>
