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
    <loading-container v-bind:is-loading="isLoading">
      <sub-page-header ref="subPageHeader" title="Skills"
                       :disabled="addSkillDisabled" :disabled-msg="addSkillsDisabledMsg" aria-label="new skill">

        <b-button id="importFromCatalogBtn" ref="importFromCatalogBtn" @click="importCatalog.show=true" variant="outline-primary" size="sm"
                  aria-label="import from catalog"
                  data-cy="importFromCatalogBtn">
          <span class="">Import</span> <i class="fas fa-book" aria-hidden="true"/>
        </b-button>
        <b-button id="newGroupBtn" ref="newGroupButton" @click="newGroup" variant="outline-primary" size="sm"
                  aria-label="new skills group"
                  data-cy="newGroupButton" class="ml-1">
          <span class="">Group</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
        </b-button>
        <b-button id="newSkillBtn" ref="newSkillButton" @click="newSkill" variant="outline-primary" size="sm"
                  aria-label="new skill"
                  data-cy="newSkillButton" class="ml-1">
          <span class="">Skill</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
        </b-button>
      </sub-page-header>

      <b-card body-class="p-0">
        <skills-table ref="skillsTable"
          :skills-prop="skills" :is-top-level="true" :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" v-on:skills-change="skillsChanged"/>
      </b-card>
    </loading-container>

    <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :skillId="editSkillInfo.skill.skillId" :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
                :project-id="projectId" :subject-id="subjectId" @skill-saved="skillCreatedOrUpdated" @hidden="focusOnNewSkillButton"/>

    <edit-skill-group v-if="editGroupInfo.show" v-model="editGroupInfo.show" :group="editGroupInfo.group" :is-edit="false"
                      @group-saved="skillCreatedOrUpdated" @hidden="focusOnNewGroupButton"/>
    <import-from-catalog v-if="importCatalog.show" v-model="importCatalog.show" @to-import="importFromCatalog"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import dayjs from '@/common-components/DayJsCustomizer';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SkillsService from './SkillsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import EditSkill from './EditSkill';
  import EditSkillGroup from './skillsGroup/EditSkillGroup';
  import ImportFromCatalog from './catalog/ImportFromCatalog';
  import CatalogService from '@/components/skills/catalog/CatalogService';

  const { mapActions, mapGetters } = createNamespacedHelpers('subjects');

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
        isLoading: true,
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
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
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
      focusOn(ref) {
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      loadSkills() {
        this.isLoading = true;
        SkillsService.getSubjectSkills(this.projectId, this.subjectId)
          .then((skills) => {
            const loadedSkills = skills;
            this.skills = loadedSkills.map((loadedSkill) => {
              const copy = { ...loadedSkill };
              copy.created = dayjs(loadedSkill.created);
              return copy;
            });
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      importFromCatalog(skillsInfoToImport) {
        this.isLoading = true;
        CatalogService.bulkImport(this.$route.params.projectId, this.$route.params.subjectId, skillsInfoToImport)
          .then(() => {
            this.loadSkills();
            this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subject.subjectId });
          });
      },
      skillsChanged(skillId) {
        this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subject.subjectId });
        this.$emit('skills-change', skillId);
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
      addSkillDisabled() {
        return this.skills && this.$store.getters.config && this.skills.length >= this.$store.getters.config.maxSkillsPerSubject;
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
