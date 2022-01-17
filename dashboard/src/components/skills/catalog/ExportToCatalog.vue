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

    <div v-if="loadingData" class="mb-5">
      <skills-spinner :is-loading="loadingData" />
      <div class="h5 text-center text-primary mt-1">Checking Catalog...</div>
    </div>
    <div v-if="!loadingData">
      <div v-if="allSkillsExportedAlready">
        All selected <b-badge variant="info">{{ skills.length }}</b-badge> skill(s) are already in the Skill Catalog.
      </div>

      <b-overlay v-if="!allSkillsExportedAlready && !state.exported" :show="state.exporting" rounded="sm" opacity="0.5"
                 spinner-variant="info" spinner-type="grow" spinner-small>
        <p v-if="!allSkillsAreDups">
          This will export <span v-if="isSingleId">Skill with id
          <b class="text-primary">[{{ firstSkillId }}]</b></span><span v-else><b-badge variant="info">{{ skillsFiltered.length }}</b-badge> Skills</span> to the <b-badge>SkillTree Catalog <i class="fas fa-book" aria-hidden="true" /></b-badge>.
          Other project administrators will then be able to import a <b class="text-primary">read-only</b> version of this skill.
        </p>
        <p v-if="numAlreadyExported > 0">
          <span class="font-italic"><i class="fas fa-exclamation-triangle text-warning" /> Note:</span> The are already <b-badge variant="info">{{ numAlreadyExported }}</b-badge> skill(s) in the Skill Catalog from the provided selection.
        </p>

        <div v-if="notExportableSkills && notExportableSkills.length > 0">
          Cannot export <b-badge variant="primary">{{ notExportableSkills.length }}</b-badge> skill(s):
          <ul>
            <li v-for="dupSkill in notExportableSkillsToShow" :key="dupSkill.skillId" :data-cy="`dupSkill-${dupSkill.skillId}`">
              {{ dupSkill.name }} <span class="text-secondary font-italic">(ID: {{ dupSkill.skillId}} )</span>
              <b-badge variant="warning" v-if="dupSkill.skillNameConflictsWithExistingCatalogSkill" class="ml-1">Name Conflict</b-badge>
              <b-badge variant="warning" v-if="dupSkill.skillIdConflictsWithExistingCatalogSkill" class="ml-1">ID Conflict</b-badge>
              <b-badge variant="warning" v-if="dupSkill.hasDependencies" class="ml-1"
                       v-b-tooltip.hover="'Skills that have dependencies cannot be exported to the catalog.'">Has Dependencies</b-badge>
            </li>
            <li v-if="notExportableSkills.length > notExportableSkillsToShow.length" data-cy="cantExportTruncatedMsg">
              <span class="text-primary font-weight-bold">{{ notExportableSkills.length - notExportableSkillsToShow.length }}</span> <span class="font-italic">more items...</span>
            </li>
          </ul>
        </div>

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

    </div>

    <div v-if="allSkillsExportedAlready || state.exported || allSkillsAreDups" slot="modal-footer" class="w-100">
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="okButton">
        OK
      </b-button>
    </div>

    <div v-if="!allSkillsExportedAlready && !state.exported && !allSkillsAreDups" slot="modal-footer" class="w-100">
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
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import CatalogService from './CatalogService';

  export default {
    name: 'ExportToCatalog',
    components: { SkillsSpinner },
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
        loadingData: true,
        visibilityToAllProjects: true,
        selectedProject: null,
        skillsFiltered: [],
        notExportableSkills: [],
        notExportableSkillsToShow: [],
        numAlreadyExported: 0,
        allSkillsExportedAlready: false,
        isSingleId: false,
        firstSkillId: null,
        allSkillsAreDups: false,
        state: {
          exporting: false,
          exported: false,
        },
      };
    },
    mounted() {
      this.prepSkillsForExport();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
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
      prepSkillsForExport() {
        const skillIds = this.skills.map((skill) => skill.skillId);
        CatalogService.areSkillsExportable(this.$route.params.projectId, skillIds)
          .then((res) => {
            let enrichedSkills = this.skills.map((skillToUpdate) => {
              const enhanceWith = res[skillToUpdate.skillId];
              return ({
                ...skillToUpdate,
                hasDependencies: enhanceWith.hasDependencies,
                skillAlreadyInCatalog: enhanceWith.skillAlreadyInCatalog,
                skillIdConflictsWithExistingCatalogSkill: enhanceWith.skillIdConflictsWithExistingCatalogSkill,
                skillNameConflictsWithExistingCatalogSkill: enhanceWith.skillNameConflictsWithExistingCatalogSkill,
              });
            });

            // re-filter if another user added to the filter or if the changes was made in another tab
            enrichedSkills = enrichedSkills.filter((skill) => !skill.skillAlreadyInCatalog);
            const isExportableSkill = (skill) => !skill.skillIdConflictsWithExistingCatalogSkill && !skill.skillNameConflictsWithExistingCatalogSkill && !skill.hasDependencies;

            this.notExportableSkills = enrichedSkills.filter((skill) => !isExportableSkill(skill));
            this.notExportableSkillsToShow = this.notExportableSkills.length > 9 ? this.notExportableSkills.slice(0, 8) : this.notExportableSkills;
            this.allSkillsAreDups = enrichedSkills.length === this.notExportableSkills.length;
            this.skillsFiltered = enrichedSkills.filter((skill) => isExportableSkill(skill));
            this.numAlreadyExported = this.skills.length - this.skillsFiltered.length - this.notExportableSkills.length;
            this.allSkillsExportedAlready = this.skillsFiltered.length === 0 && this.notExportableSkills.length === 0;
            this.isSingleId = this.skillsFiltered.length === 1;
            this.firstSkillId = this.skillsFiltered && this.skillsFiltered.length > 0 ? this.skillsFiltered[0].skillId : null;
          }).finally(() => {
            this.loadingData = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
