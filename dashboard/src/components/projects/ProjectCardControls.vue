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
<template>
  <div>
    <b-button
      :to="{ name:'Subjects', params: { projectId: this.project.projectId, project: this.project }}"
      variant="outline-primary" size="sm" class="mr-2"
      :data-cy="`projCard_${this.project.projectId}_manageBtn`"
      :aria-label="`manage project ${this.project.name}`">
      Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
    </b-button>
    <b-button v-if="isRootUser" class="mr-2" @click="$emit('unpin-project')" data-cy="unpin" size="sm"
              variant="outline-primary" :aria-label="'remove pin for project '+ project.name"
              :aria-pressed="project.pinned">
      <span class="d-none d-sm-inline">Unpin</span> <i class="fas fa-ban" style="font-size: 1rem;" aria-hidden="true"/>
    </b-button>
    <b-button-group size="sm" class="buttons mr-2">
      <b-button ref="editBtn"
                size="sm"
                variant="outline-primary"
                @click="$emit('edit-project')"
                title="Edit Project"
                data-cy="editProjBtn"><i class="fas fa-edit" aria-hidden="true"/></b-button>

      <span v-b-tooltip.hover="deleteDisabledText">
        <b-button variant="outline-primary"
                  class="last-right-group-btn"
                  size="sm"
                  @click="$emit('delete-project')"
                  :disabled="isDeleteDisabled"
                  title="Delete Project"><i class="text-warning fas fa-trash" aria-hidden="true"/></b-button>
      </span>
    </b-button-group>

    <b-button-group size="sm" class="buttons">
      <b-button variant="outline-primary"
                @click="$emit('move-up-project')"
                :disabled="project.isFirst"
                title="Sort Order - Move up"><i class="fas fa-arrow-circle-up text-info" aria-hidden="true"/></b-button>
      <b-button variant="outline-primary"
                @click="$emit('move-down-project')"
                :disabled="project.isLast"
                title="Sort Order - Move down"><i class="fas fa-arrow-circle-down text-info" aria-hidden="true"/>
      </b-button>
    </b-button-group>
  </div>
</template>

<script>
  export default {
    name: 'ProjectCardControls',
    props: {
      project: Object,
      isDeleteDisabled: Boolean,
      deleteDisabledText: String,
    },
    computed: {
      isRootUser() {
        return this.$store.getters['access/isRoot'];
      },
    },
    methods: {
      focusOnEdit() {
        this.$refs.editBtn.focus();
      },
    },
  };
</script>

<style scoped>
.last-right-group-btn {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: none;
}
</style>
