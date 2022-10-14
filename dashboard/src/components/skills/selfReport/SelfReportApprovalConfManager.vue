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
  <b-card body-class="p-0">
    <template #header>
      <div class="row">
        <div class="col h6 mb-0 font-weight-bold"><i class="fas fa-cogs" aria-hidden="true"/> Configure Approval Workload</div>
        <div v-if="numUsersConfigured > 0" class="col text-right">
          <b-badge variant="success">{{ numUsersConfigured }}</b-badge> User{{ numUsersConfigured > 1 ? 's' : ''}} Configured
        </div>
      </div>
    </template>

    <skills-b-table :options="table.options" :items="table.items"
                    tableStoredStateId="skillApprovalConfTable"
                    data-cy="skillApprovalConfTable">
      <template #head(userIdForDisplay)="data">
        <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"/> {{ data.label }}</span>
      </template>
      <template #head(roleName)="data">
        <span class="text-primary"><i class="fas fa-id-card text-danger" aria-hidden="true"/> {{ data.label }}</span>
      </template>
      <template #head(workload)="data">
        <span class="text-primary"><i class="fas fa-users skills-color-access" aria-hidden="true"/> {{ data.label }}</span>
      </template>

      <template v-slot:cell(roleName)="data">
        {{ data.value | userRole }}
      </template>

      <template v-slot:cell(workload)="data">
        <div class="row">
          <div class="col">
            <div v-if="!data.item.hasConf" >Fallback - All Requests</div>
            <div v-if="data.item.tagConf && data.item.tagConf.length > 0">
              <div v-for="tConf in data.item.tagConf" :key="tConf.userTagKey">Users in <span class="font-italic text-secondary">{{tConf.userTagKey}}:</span> <span>{{tConf.userTagValue}}</span></div>
            </div>
            <div v-if="data.item.userConf && data.item.userConf.length > 0" >
              <b-badge variant="success">{{data.item.userConf.length}}</b-badge> specific users
            </div>
            <div v-if="data.item.skillConf && data.item.skillConf.length > 0" >
              <b-badge variant="info">{{ data.item.skillConf.length }}</b-badge> Specific Skill{{ data.item.skillConf.length  > 1 ? 's' : '' }}
            </div>
          </div>
          <div class="col-auto">
            <b-button size="sm" variant="outline-primary" @click="data.toggleDetails"><i class="fas fa-edit" aria-hidden="true" /> Edit</b-button>
          </div>
        </div>
      </template>

      <template #row-details="row">
        <div class="ml-3">
          <self-report-approval-conf-user-tag :user="row.item" class="mt-3"/>
          <self-report-approval-conf-skill :user="row.item" class="mt-3"/>
          <self-report-approval-conf-specific-users :user="row.item" class="mt-3"/>
        </div>
      </template>

    </skills-b-table>
  </b-card>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SelfReportApprovalConfSpecificUsers
    from '@/components/skills/selfReport/SelfReportApprovalConfSpecificUsers';
  import SelfReportApprovalConfUserTag
    from '@/components/skills/selfReport/SelfReportApprovalConfUserTag';
  import SelfReportApprovalConfSkill
    from '@/components/skills/selfReport/SelfReportApprovalConfSkill';
  import AccessService from '@/components/access/AccessService';
  import RoleName from '@/components/access/RoleName';
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';

  export default {
    name: 'SelfReportApprovalConfManager',
    components: {
      SelfReportApprovalConfSkill,
      SelfReportApprovalConfUserTag,
      SelfReportApprovalConfSpecificUsers,
      SkillsBTable,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'requestedOn',
            sortDesc: true,
            emptyText: 'You are the only user',
            tableDescription: 'Configure Approval Workload',
            fields: [
              {
                key: 'userIdForDisplay',
                label: 'Approver',
                sortable: true,
              },
              {
                key: 'roleName',
                label: 'Role',
                sortable: true,
              },
              {
                key: 'workload',
                label: 'Approval Workload',
                sortable: false,
              },
            ],
            pagination: {
              remove: true,
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      numUsersConfigured() {
        if (!this.table.items) {
          return 0;
        }

        return this.table.items.filter((item) => item.hasConf).length;
      },
    },
    methods: {
      loadData() {
        const pageParams = {
          limit: 200,
          ascending: true,
          page: 1,
          orderBy: 'userId',
        };
        const roles = [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER];
        AccessService.getUserRoles(this.projectId, roles, pageParams)
          .then((users) => {
            SelfReportService.getApproverConf(this.projectId)
              .then((approverConf) => {
                this.table.items = this.buildDisplayModel(users.data, approverConf);
              }).finally(() => {
                this.table.options.busy = false;
              });
          });
      },
      buildDisplayModel(users, approverConf) {
        const res = users.map((u) => {
          const allConf = approverConf.filter((c) => c.approverUserId === u.userId);
          const tagConf = allConf.filter((c) => c.userTagKey);
          const userConf = allConf.filter((c) => c.userId);
          const skillConf = allConf.filter((c) => c.skillId);
          return {
            userIdForDisplay: u.userIdForDisplay,
            userId: u.userId,
            roleName: u.roleName,
            tagConf,
            userConf,
            skillConf,
            allConf,
            hasConf: allConf && allConf.length > 0,
          };
        });
        return res;
      },
    },
  };
</script>

<style scoped>

</style>
