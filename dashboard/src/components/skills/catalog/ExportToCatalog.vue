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

    <div v-if="allSkillsExportedAlready">
      All selected <b-badge variant="info">{{ skills.length }}</b-badge> skill(s) are already in the Skill Catalog.
    </div>

    <b-overlay v-if="!allSkillsExportedAlready && !state.exported" :show="state.exporting" rounded="sm" opacity="0.5"
               spinner-variant="info" spinner-type="grow" spinner-small>
      <p>
        This will export <span v-if="isSingleId">Skill with id
        <b class="text-primary">[{{ firstSkillId }}]</b></span><span v-else><b-badge variant="info">{{ skillsFiltered.length }}</b-badge> Skills</span> to the <b-badge>SkillTree Catalog <i class="fas fa-book" aria-hidden="true" /></b-badge>.
        Other project administrators will then be able to import a <b class="text-primary">read-only</b> version of this skill.
      </p>
      <p v-if="numAlreadyExported > 0">
        <span class="font-italic"><i class="fas fa-exclamation-triangle text-warning" /> Note:</span> The are already <b-badge variant="info">{{ numAlreadyExported }}</b-badge> skill(s) in the Skill Catalog from the provided selection.
      </p>

<!-- Keeping this code for the follow-on ticket-->
<!--      <hr/>-->
<!--      <div class="h6">Visibility:-->
<!--        <b-form-checkbox v-model="visibilityToAllProjects" @change="onVisibilityToAllProjects" class="mt-2 d-inline"-->
<!--                         data-cy="shareWithAllProjectsCheckbox">-->
<!--          <small>Share With All Projects </small>-->
<!--        </b-form-checkbox>-->
<!--      </div>-->
<!--      <project-selector :project-id="$route.params.projectId" :selected="selectedProject"-->
<!--                        v-on:selected="onSelectedProject"-->
<!--                        v-on:unselected="onUnSelectedProject"-->
<!--                        :only-single-selected-value="true"-->
<!--                        :disabled="visibilityToAllProjects">-->
<!--      </project-selector>-->
    </b-overlay>

    <p v-if="state.exported">
      <i class="fas fa-check-circle text-success"></i>
      <span v-if="isSingleId"> Skill with id <b class="text-primary">{{ firstSkillId }}</b> was</span>
      <span v-else><b-badge variant="info" class="ml-2">{{ skillsFiltered.length }}</b-badge>
        Skills were</span>  <span class="text-success font-weight-bold">successfully</span> exported to the catalog!
    </p>

    <div v-if="allSkillsExportedAlready || state.exported" slot="modal-footer" class="w-100">
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="okButton">
        OK
      </b-button>
    </div>

    <div v-if="!allSkillsExportedAlready && !state.exported" slot="modal-footer" class="w-100">
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
  import CatalogService from './CatalogService';

  export default {
    name: 'ExportToCatalog',
    props: {
      skills: {
        type: Array,
        required: true,
      },
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
        skillsFiltered: this.skills.filter((skill) => !skill.sharedToCatalog),
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
      allSkillsExportedAlready() {
        return this.skillsFiltered.length === 0;
      },
      numAlreadyExported() {
        return this.skills.length - this.skillsFiltered.length;
      },
      isSingleId() {
        return this.skillsFiltered.length === 1;
      },
      firstSkillId() {
        return this.skillsFiltered && this.skillsFiltered.length > 0 ? this.skillsFiltered[0].skillId : null;
      },
    },
    methods: {
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (this.state.exported) {
          const res = this.skillsFiltered.map((skill) => ({ ...skill, sharedToCatalog: true }));
          this.$emit('exported', res);
        }
        this.$emit('hidden', { ...e });
      },
      handleExport() {
        this.state.exporting = true;
        CatalogService.bulkExport(this.$route.params.projectId, this.skillsFiltered.map((skill) => skill.skillId))
          .then(() => {
            this.state.exported = true;
          })
          .finally(() => {
            this.state.exporting = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
