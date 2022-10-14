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
        <i class="fas fa-user-tag text-primary" aria-hidden="true"/> Split Workload <span class="font-italic text-primary">By Org</span>
      </div>
    </template>
    <div class="row mx-2 no-gutters">
      <div class="col px-1">
        <b-form-input id="user-name-filter" data-cy="achievementsNavigator-usernameInput"/>
      </div>
      <div class="col-auto px-1">
        <b-button variant="outline-primary">Add <i class="fas fa-plus-circle" aria-hidden="true" /></b-button>
      </div>
    </div>

    <skills-b-table v-if="user.tagConf && tagConf.tagConf.length > 0" class="mt-3"
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

  export default {
    name: 'SelfReportApprovalConfUserTag',
    components: { DateCell, SkillsBTable },
    props: {
      user: Object,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        currentSelectedUser: null,
        table: {
          items: [{
            user: 'XYZ',
            created: new Date().getTime() - 100000,
          }, {
            user: 'AB12',
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
                label: 'Org',
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
