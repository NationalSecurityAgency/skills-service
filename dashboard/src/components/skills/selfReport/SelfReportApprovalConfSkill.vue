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
    <skills-selector2 :options="[]" class="mx-3 mb-3 mt-2"
                      :onlySingleSelectedValue="true"></skills-selector2>

    <skills-b-table class=""
                    :options="table.options" :items="table.items"
                    tableStoredStateId="skillApprovalConfSpecificUsersTable"
                    data-cy="skillApprovalConfSpecificUsersTable">
      <template v-slot:cell(user)="data">
        <div class="row">
          <div class="col">
            {{ data.value }}
          </div>
          <div class="col-auto">
            <b-button title="Delete Skill"
                      variant="outline-danger"
                      size="sm">
              <i class="fas fa-trash" aria-hidden="true"/>
            </b-button>
          </div>
        </div>

      </template>
      <template v-slot:cell(created)="data">
        <date-cell :value="data.value" />
      </template>
    </skills-b-table>

  </b-card>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';

  export default {
    name: 'SelfReportApprovalConfSkill',
    components: { SkillsSelector2, DateCell, SkillsBTable },
    props: {
      user: Object,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        currentSelectedUser: null,
        table: {
          items: [{
            user: 'Great Skill 1',
            created: new Date().getTime() - 100000,
          }, {
            user: 'Another Fun Skill',
            created: new Date().getTime() - 100000,
          }],
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
                key: 'user',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'created',
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
    computed: {
      pkiAuthenticated() {
        return this.$store.getters.isPkiAuthenticated;
      },
    },
  };
</script>

<style scoped>

</style>
