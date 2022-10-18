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
    <skills-selector2 class="mx-3 mb-3 mt-2"
                      :options="availableSkills"
                      :selected="selectedSkills"
                      @added="addSkillToConf"
                      placeholder="Select skill"
                      :onlySingleSelectedValue="true"></skills-selector2>

    <skills-b-table v-if="hadData" class=""
                    :options="table.options" :items="table.items"
                    tableStoredStateId="skillApprovalConfSpecificUsersTable"
                    data-cy="skillApprovalConfSpecificUsersTable">
      <template v-slot:cell(skillId)="data">
        <div class="row">
          <div class="col">
            {{ data.item.skillName }}
          </div>
          <div class="col-auto">
            <b-button title="Delete Skill"
                      variant="outline-danger"
                      :aria-label="`Remove ${data.value} tag.`"
                      @click="removeTagConf(data.item)"
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
                 class="my-5"
                 icon-size="fa-2x"
                 icon="fas fa-graduation-cap">
      You can split approval workload by routing approval requests for selected skills approval requests to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
    </no-content2>

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

  export default {
    name: 'SelfReportApprovalConfSkill',
    components: {
      NoContent2, SkillsSelector2, DateCell, SkillsBTable,
    },
    mixins: [SelfReportApprovalConfMixin],
    props: {
      userInfo: Object,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        currentSelectedUser: null,
        availableSkills: [],
        selectedSkills: [],
        table: {
          items: [],
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'requestedOn',
            sortDesc: true,
            emptyText: 'You are the only user',
            tableDescription: 'Configure Approval Workload',
            fields: [
              {
                key: 'skillId',
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
              remove: true,
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
      }
      this.loadAvailableSkills();
    },
    computed: {
      pkiAuthenticated() {
        return this.$store.getters.isPkiAuthenticated;
      },
      hadData() {
        return this.table.items && this.table.items.length > 0;
      },
    },
    methods: {
      loadAvailableSkills() {
        SkillsService.getProjectSkills(this.projectId, null, false, true)
          .then((loadedSkills) => {
            const alreadySelectedSkillIds = this.table.items.map((item) => item.skillId);
            this.availableSkills = loadedSkills.filter((item) => !alreadySelectedSkillIds.includes(item.skillId));
          });
      },
      addSkillToConf(newItem) {
        SelfReportService.configureApproverForSkillId(this.projectId, this.userInfo.userId, newItem.skillId)
          .then((res) => {
            this.table.items.push(res);
            this.$emit('conf-added', res);
            this.availableSkills = this.availableSkills.filter((item) => item.skillId !== newItem.skillId);
            this.selectedSkills = [];
            this.$nextTick(() => this.$announcer.polite(`Added workload configuration successfully for ${newItem.skillId} skill.`));
          });
      },
    },
  };
</script>

<style scoped>

</style>
