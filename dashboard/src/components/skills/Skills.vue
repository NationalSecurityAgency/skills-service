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
    <sub-page-header ref="subPageHeader" title="Skills" :is-loading="loadingSubjectSkills"
                     :disabled="addSkillDisabled" :disabled-msg="addSkillsDisabledMsg" aria-label="new skill">
      <i v-if="addSkillDisabled" class="fas fa-exclamation-circle text-warning ml-1 mr-1" style="pointer-events: all; font-size: 1.5rem;" v-b-tooltip.hover="addSkillsDisabledMsg"/>
      <b-button id="importFromCatalogBtn" ref="importFromCatalogBtn" @click="importCatalog.show=true" variant="outline-primary" size="sm"
                aria-label="import from catalog"
                data-cy="importFromCatalogBtn">
        <span class="">Import</span> <i class="fas fa-book" aria-hidden="true"/>
      </b-button>
      <b-button id="newGroupBtn" ref="newGroupButton" @click="newGroup" variant="outline-primary" size="sm"
                aria-label="new skills group" class="ml-1"
                aria-describedby="newGroupSrText"
                data-cy="newGroupButton" :aria-disabled="addSkillDisabled" :disabled="addSkillDisabled">
        <span class="">Group</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
        <span id="newGroupSrText" class="sr-only">
            {{ addSkillDisabled ? addSkillDisabled : 'Create a new Skill Group'}}
          </span>
      </b-button>
      <b-button id="newSkillBtn" ref="newSkillButton" @click="newSkill" variant="outline-primary" size="sm"
                aria-label="new skill"
                aria-describedby="newSkillSrText"
                data-cy="newSkillButton" class="ml-1" :aria-disabled="addSkillDisabled" :disabled="addSkillDisabled">
        <span class="">Skill</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
        <span id="newSkillSrText" class="sr-only">
            {{ addSkillDisabled ? addSkillDisabled : 'Create a new Skill'}}
          </span>
      </b-button>
    </sub-page-header>

    <loading-container v-bind:is-loading="loadingSubjectSkills">
      <b-card body-class="p-0">
        <skills-table ref="skillsTable"
          :skills-prop="subjectSkills" :is-top-level="true" :project-id="this.$route.params.projectId" :subject-id="subjectId"
                      v-on:skills-change="skillsChanged"
                      @skill-removed="skillDeleted" />
      </b-card>
    </loading-container>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subjectId" @skill-saved="skillCreatedOrUpdated" @hidden="focusOnNewSkillButton"/>

    <edit-skill-group v-if="editGroupInfo.show" v-model="editGroupInfo.show" :group="editGroupInfo.group" :is-edit="false"
                      @group-saved="skillCreatedOrUpdated" @hidden="focusOnNewGroupButton"/>
    <import-from-catalog v-if="importCatalog.show" v-model="importCatalog.show" :current-project-skills="skills"
                         @to-import="importFromCatalog" @hidden="focusOnImportFromCatalogButton"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import EditSkill from './EditSkill';
  import EditSkillGroup from './skillsGroup/EditSkillGroup';
  import ImportFromCatalog from './catalog/ImportFromCatalog';

  const { mapActions, mapGetters } = createNamespacedHelpers('subjects');
  const subjectSkills = createNamespacedHelpers('subjectSkills');
  const finalizeInfo = createNamespacedHelpers('finalizeInfo');

  export default {
    name: 'Skills',
    components: {
      ImportFromCatalog,
      EditSkillGroup,
      EditSkill,
      SubPageHeader,
      SkillsTable,
      LoadingContainer,
    },
    data() {
      return {
        skills: [],
        projectId: null,
        subjectId: null,
        editSkillInfo: {
          isEdit: false,
          isCopy: false,
          show: false,
          skill: {},
        },
        editGroupInfo: {
          isEdit: false,
          show: false,
          group: {},
        },
        importCatalog: {
          show: false,
        },
      };
    },
    watch: {
      '$route.params.subjectId': function foo(newVal) {
        // if this is caused by the page being reloaded, mount() will take care of setting this to the proper value
        // the reassignment here will make sure that any child components that depend on the subjectId properly update in the case
        // that the subjectId is updated via router.replace caused by editing the id of the currently viewed Subject
        this.subjectId = newVal;
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      ...subjectSkills.mapActions([
        'loadSubjectSkills',
      ]),
      ...subjectSkills.mapMutations([
        'setLoadingSubjectSkills',
      ]),
      ...finalizeInfo.mapActions([
        'loadFinalizeInfo',
      ]),
      skillCreatedOrUpdated(skill) {
        this.$refs.skillsTable.skillCreatedOrUpdated(skill);
      },
      focusOnNewSkillButton() {
        this.focusOn(this.$refs.newSkillButton);
      },
      focusOnNewGroupButton() {
        this.focusOn(this.$refs.newGroupButton);
      },
      focusOnImportFromCatalogButton() {
        this.focusOn(this.$refs.importFromCatalogBtn);
      },
      focusOn(ref) {
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      loadSkills(focusOnImportBtnAfter = false) {
        this.loadSubjectSkills({ projectId: this.projectId, subjectId: this.subjectId })
          .then((skills) => {
            this.skills = skills;
        }).finally(() => {
          if (focusOnImportBtnAfter) {
            this.focusOnImportFromCatalogButton();
          }
        });
      },
      skillDeleted(skill) {
        this.skillsChanged(skill, true);
      },
      importFromCatalog(skillsInfoToImport) {
        this.setLoadingSubjectSkills(true);
        CatalogService.bulkImport(this.$route.params.projectId, this.$route.params.subjectId, skillsInfoToImport)
          .then(() => {
            this.loadSkills(true);
            this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subject.subjectId });
            this.loadFinalizeInfo({ projectId: this.projectId });
          });
      },
      skillsChanged(skill, deleted = false) {
        if (!deleted) {
          const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
          if (item1Index >= 0) {
            this.skills.splice(item1Index, 1, skill);
          } else {
            this.skills.push(skill);
          }
        } else {
          const index = this.skills.findIndex((item) => item.skillId === skill.skillId);
          this.skills.splice(index, 1);
        }

        this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subject.subjectId });
        this.$emit('skills-change', skill.skillId);
      },
      newSkill() {
        this.editSkillInfo = {
          skill: {
            projectId: this.projectId,
            subjectId: this.subject.subjectId,
            type: 'Skill',
          },
          show: true,
          isEdit: false,
          isCopy: false,
        };
      },
      newGroup() {
        this.editGroupInfo = {
          isEdit: false,
          show: true,
          group: {
            projectId: this.projectId,
            subjectId: this.subject.subjectId,
            type: 'SkillsGroup',
          },
        };
      },
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
      this.loadSkills();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      ...subjectSkills.mapGetters([
        'subjectSkills',
        'loadingSubjectSkills',
      ]),
      numSubjectSkills() {
        return this.subject.numSkills;
      },
      addSkillDisabled() {
        return this.skills && this.$store.getters.config && this.numSubjectSkills >= this.$store.getters.config.maxSkillsPerSubject;
      },
      addSkillsDisabledMsg() {
        if (this.$store.getters.config) {
          return `The maximum number of Skills allowed is ${this.$store.getters.config.maxSkillsPerSubject}`;
        }
        return '';
      },
    },
  };
</script>

<style scoped>
</style>
