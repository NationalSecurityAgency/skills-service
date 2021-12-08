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
  <b-modal :id="firstSkillId" size="md" :title="`Export Skill to the Catalog`" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
           aria-label="'Export Skill to the Catalog'">
    <b-overlay v-if="!state.exported" :show="state.exporting" rounded="sm" opacity="0.5"
               spinner-variant="info" spinner-type="grow" spinner-small>
      <p>
        This will export <span v-if="isSingleId">Skill with id <b>[{{ firstSkillId }}]</b></span><span v-else><b-badge variant="info">{{ skillIds.length }}</b-badge> Skills</span> to the SkillTree Catalog <i class="fas fa-book" aria-hidden="true" />.
        Other project administrators will then be able to import a read-only version of this skill.
      </p>

      <hr/>
      <div class="h6">Visibility:
        <b-form-checkbox v-model="visibilityToAllProjects" @change="onVisibilityToAllProjects" class="mt-2 d-inline"
                         data-cy="shareWithAllProjectsCheckbox">
          <small>Share With All Projects </small>
        </b-form-checkbox>
      </div>
      <project-selector :project-id="$route.params.projectId" :selected="selectedProject"
                        v-on:selected="onSelectedProject"
                        v-on:unselected="onUnSelectedProject"
                        :only-single-selected-value="true"
                        :disabled="visibilityToAllProjects">
      </project-selector>
    </b-overlay>

    <p v-if="state.exported">
      <i class="fas fa-check-circle text-success"></i>
      <span v-if="isSingleId">Skill with id <b>[{{ firstSkillId }}]</b> was</span>
      <span v-else><b-badge variant="info" class="ml-2">{{ skillIds.length }}</b-badge>
        Skills were</span>  <span class="text-success font-weight-bold">successfully</span> exported to the catalog!
    </p>

    <div v-if="state.exported" slot="modal-footer" class="w-100">
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeButton">
        OK
      </b-button>
    </div>

    <div v-if="!state.exported" slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right"
                @click="handleExport"
                data-cy="exportToCatalogButton">
        Export
      </b-button>
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeButton">
        Cancel
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import ProjectSelector from '../crossProjects/ProjectSelector';
  import CatalogService from './CatalogService';

  export default {
    name: 'ExportToCatalog',
    components: { ProjectSelector },
    props: {
      skillIds: Array,
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        visibilityToAllProjects: true,
        selectedProject: null,
        state: {
          exporting: false,
          exported: false,
        },
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isSingleId() {
        return this.skillIds.length === 1;
      },
      firstSkillId() {
        return this.skillIds[0];
      },
    },
    methods: {
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (this.state.exported) {
          this.$emit('exported', this.skillIds);
        }
        this.$emit('hidden', { ...e });
      },
      handleExport() {
        this.state.exporting = true;
        CatalogService.bulkExport(this.$route.params.projectId, this.skillIds)
          .then(() => {
            this.state.exported = true;
          })
          .finally(() => {
            this.state.exporting = false;
          });
      },
      onVisibilityToAllProjects() {

      },
      onSelectedProject() {

      },
      onUnSelectedProject() {

      },
    },
  };
</script>

<style scoped>

</style>
