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
  <b-card body-class="p-0 mt-3">
    <template #header>
      <div>
        <i class="fas fa-graduation-cap text-primary" aria-hidden="true"/> Split Workload <span class="font-italic text-primary">By Skill</span>
      </div>
    </template>
    <div v-if="!(this.loadingMeta.skills || this.loadingMeta.subjects)" class="row no-gutters mx-1">
      <div class="col-md mx-1 mt-1 align-self-end">
          <div class="mb-1">Add a Single Skills</div>
          <skills-selector2
              :disabled="selectedSubject !== null || loading"
              :options="availableSkills"
              :selected="selectedSkills"
              @added="selectSkill"
              @removed="selectedSkills = []"
              placeholder="Select skill"
              :onlySingleSelectedValue="true"
              :warnBeforeRemoving="false"/>
      </div>
      <div class="col-md-auto mx-1 text-center align-self-end">
        <span class="mt-3">OR</span>
      </div>
      <div class="col-md mx-1 mt-1 align-self-end">
        <div class="mb-1">Add <b>ALL</b> Skills under a Subject</div>
        <subject-selector v-if="availableSubjects && availableSubjects.length > 0"
          :disabled="(selectedSkills && selectedSkills.length > 0) || loading"
          :options="availableSubjects"
          :selected="selectedSubject"
          @added="selectSubject"
          @removed="selectedSubject = null"
          :onlySingleSelectedValue="true"
          :warnBeforeRemoving="false"/>
      </div>
      <div class="col-md-auto mx-1 mt-1 text-center align-self-end">
        <b-button
          aria-label="Add Tag Value"
          @click="addSkillToConf"
          data-cy="addSkillConfBtn"
          :disabled="loading || (!selectedSubject && (!selectedSkills || selectedSkills.length === 0))"
          variant="outline-primary">Add <i class="fas fa-plus-circle" aria-hidden="true" />
        </b-button>
      </div>
    </div>

    <div v-if="!loading && addedSubjectSkillsStats.addedSubject" class="alert alert-success mt-1 mx-2" data-cy="skillsAddedAlert">
      <i class="fas fa-check-double" aria-hidden="true"/>
      Added <b-badge>{{ addedSubjectSkillsStats.numSkillsAdded }}</b-badge> skill{{ addedSubjectSkillsStats.numSkillsAdded === 1 ? '' : 's'}}.
      <span v-if="addedSubjectSkillsStats.numSkillsAlreadyConfigured > 0"><b-badge>{{ addedSubjectSkillsStats.numSkillsAlreadyConfigured }}</b-badge> already added!</span>
      <button type="button" @click="addedSubjectSkillsStats.addedSubject=false" class="close" data-dismiss="alert" aria-label="Close Skill Added Alert"
          data-cy="closeSkillsAddedAlertBtn">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>

    <skills-spinner v-if="loading" :is-loading="loading" class="mb-5"/>
    <div v-if="!loading">
      <skills-b-table v-if="hadData" class="mt-3"
                      :options="table.options" :items="table.items"
                      tableStoredStateId="skillApprovalConfSpecificUsersTable"
                      data-cy="skillApprovalSkillConfTable">
        <template v-slot:cell(skillName)="data">
          <div class="row" :data-cy="`skillCell-${data.item.skillId}`">
            <div class="col">
              {{ data.item.skillName }}
            </div>
            <div class="col-auto">
              <b-button title="Delete Skill"
                        variant="outline-danger"
                        :aria-label="`Remove ${data.value} tag.`"
                        data-cy="deleteBtn"
                        @click="removeSkill(data.item)"
                        :disabled="data.item.deleteInProgress"
                        size="sm">
                <b-spinner v-if="data.item.deleteInProgress" small></b-spinner>
                <i v-else class="fas fa-trash" aria-hidden="true"/>
              </b-button>
            </div>
          </div>

        </template>
        <template v-slot:cell(updated)="data">
          <date-cell :value="data.value" />
        </template>
      </skills-b-table>
      <no-content2 v-if="!hadData" title="Not Configured Yet..."
                 class="p-2 py-5"
                 icon-size="fa-2x"
                 data-cy="noSkillConf"
                 icon="fas fa-graduation-cap">
      You can split approval workload by routing approval requests for selected skills approval requests to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
    </no-content2>
    </div>
  </b-card>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';
  import SkillsService from '@/components/skills/SkillsService';
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';
  import NoContent2 from '@/components/utils/NoContent2';
  import SelfReportApprovalConfMixin
    from '@/components/skills/selfReport/SelfReportApprovalConfMixin';
  import SubjectSelector from '@/components/skills/SubjectSelector';
  import SubjectsService from '@/components/subjects/SubjectsService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'SelfReportApprovalConfSkill',
    components: {
      SkillsSpinner,
      SubjectSelector,
      NoContent2,
      SkillsSelector2,
      DateCell,
      SkillsBTable,
    },
    mixins: [SelfReportApprovalConfMixin],
    props: {
      userInfo: Object,
    },
    data() {
      return {
        loadingMeta: {
          skills: true,
          subjects: true,
          loadingSkillsUnderASubject: false,
          numSkillsToProcess: 0,
        },
        projectId: this.$route.params.projectId,
        currentSelectedUser: null,
        availableSkills: [],
        availableSubjects: [],
        selectedSkills: [],
        selectedSubject: null,
        addedSubjectSkillsStats: {
          addedSubject: false,
          numSkillsAdded: 0,
          numSkillsAlreadyConfigured: 0,
        },
        table: {
          items: [],
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'updated',
            sortDesc: true,
            emptyText: 'You are the only user',
            tableDescription: 'Configure Approval Workload',
            fields: [
              {
                key: 'skillName',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'updated',
                label: 'Configured On',
                sortable: true,
              },
            ],
            pagination: {
              remove: false,
              server: false,
              currentPage: 1,
              totalRows: 1,
              pageSize: 4,
              possiblePageSizes: [4, 10, 15, 20],
            },
          },
        },
      };
    },
    mounted() {
      const hasConf = this.userInfo.skillConf && this.userInfo.skillConf.length > 0;
      if (hasConf) {
        this.table.items = this.userInfo.skillConf.map((u) => ({ ...u }));
        this.updatePaging();
      }
      this.loadAvailableSkills();
    },
    computed: {
      loading() {
        return this.loadingMeta.skills || this.loadingMeta.subjects || this.loadingMeta.loadingSkillsUnderASubject || this.loadingMeta.numSkillsToProcess > 0;
      },
      pkiAuthenticated() {
        return this.$store.getters.isPkiAuthenticated;
      },
      hadData() {
        return this.table.items && this.table.items.length > 0;
      },
    },
    methods: {
      updatePaging() {
        this.table.options.pagination.totalRows = this.table.items.length;
      },
      removeSkill(item) {
        this.resetSubjAddedInfo();
        this.removeTagConf(item)
          .then(() => {
            this.updatePaging();
          });
      },
      loadAvailableSkills() {
        SkillsService.getProjectSkills(this.projectId, null, false, true)
          .then((loadedSkills) => {
            const alreadySelectedSkillIds = this.table.items.map((item) => item.skillId);
            this.availableSkills = loadedSkills.filter((item) => !alreadySelectedSkillIds.includes(item.skillId));
          }).finally(() => {
            this.loadingMeta.skills = false;
          });
        SubjectsService.getSubjects(this.projectId)
          .then((subjects) => {
            this.availableSubjects = subjects;
          }).finally(() => {
            this.loadingMeta.subjects = false;
          });
      },
      selectSkill(newItem) {
        this.resetSubjAddedInfo();
        this.selectedSkills = [newItem];
      },
      resetSubjAddedInfo() {
        this.addedSubjectSkillsStats.numSkillsAdded = 0;
        this.addedSubjectSkillsStats.numSkillsAlreadyConfigured = 0;
        this.addedSubjectSkillsStats.addedSubject = false;
      },
      addSkillToConf() {
        this.resetSubjAddedInfo();

        if (this.selectedSkills && this.selectedSkills.length > 0) {
          const { skillId } = this.selectedSkills[0];
          SelfReportService.configureApproverForSkillId(this.projectId, this.userInfo.userId, skillId)
            .then((res) => {
              this.table.items.push(res);
              this.updatePaging();
              this.$emit('conf-added', res);
              this.availableSkills = this.availableSkills.filter((item) => item.skillId !== skillId);
              this.selectedSkills = [];
              this.$nextTick(() => this.$announcer.polite(`Added workload configuration successfully for ${skillId} skill.`));
            });
        }
        if (this.selectedSubject) {
          const existingSkills = this.table.items.map((s) => s.skillId);
          this.loadingMeta.loadingSkillsUnderASubject = true;
          const { subjectId } = this.selectedSubject;
          this.selectedSubject = null;
          SkillsService.getSubjectSkills(this.projectId, subjectId)
            .then((subjectSkills) => {
              const skillsToAdd = subjectSkills.filter((s) => existingSkills.indexOf(s.skillId) < 0);
              const numSkillsToAdd = skillsToAdd.length;
              this.loadingMeta.numSkillsToProcess = numSkillsToAdd;

              this.addedSubjectSkillsStats.addedSubject = true;
              this.addedSubjectSkillsStats.numSkillsAdded = numSkillsToAdd;
              this.addedSubjectSkillsStats.numSkillsAlreadyConfigured = subjectSkills.length - numSkillsToAdd;

              skillsToAdd.forEach((sToAdd) => {
                SelfReportService.configureApproverForSkillId(this.projectId, this.userInfo.userId, sToAdd.skillId)
                  .then((res) => {
                    this.table.items.push(res);
                    this.updatePaging();
                    this.$emit('conf-added', res);
                    this.availableSkills = this.availableSkills.filter((item) => item.skillId !== sToAdd.skillId);
                    this.loadingMeta.numSkillsToProcess -= 1;
                    if (this.loadingMeta.numSkillsToProcess === 0) {
                      this.$nextTick(() => this.$announcer.polite(`Added workload configuration successfully for ${numSkillsToAdd} skills.`));
                    }
                  });
              });
            }).finally(() => {
              this.loadingMeta.loadingSkillsUnderASubject = false;
            });
        }
      },
      selectSubject(newItem) {
        this.resetSubjAddedInfo();
        this.selectedSubject = newItem;
      },
    },
  };
</script>

<style scoped>
.approver-conf-skills-selector .st-skills-selector input {
  height: 3rem !important;
}
</style>
