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
       <div class="h6 mb-0 font-weight-bold"><i class="fas fa-cogs" aria-hidden="true"/> Configure Approval Workload</div>
    </template>

    <skills-spinner :is-loading="loading" class="mb-5"/>
    <div v-if="!loading">
      <skills-b-table v-if="hasMoreThanOneApprover"
                    :options="table.options"
                    :items="table.items"
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

      <template v-slot:cell(userIdForDisplay)="data">
        <div :class="{'font-weight-bold text-primary' : data.detailsShowing }">{{ data.value }}</div>
        <div v-if="data.detailsShowing"><i class="fas fa-user-edit animate__bounceIn" aria-hidden="true"/> Editing...</div>
      </template>

      <template v-slot:cell(roleName)="data">
        {{ data.value | userRole }}
      </template>

      <template v-slot:cell(workload)="data">
        <div class="row" :data-cy="`workloadCell_${data.item.userId}`">
          <div class="col">
            <div v-if="!data.item.hasConf">
              <b-form-checkbox
                  :name="`Enable and disable fallback for ${data.item.userId} approver`"
                  @change="handleFallback($event, data.item)"
                  data-cy="fallbackSwitch"
                  :checked="data.item.isFallbackConfPresent" switch>
                <span v-if="!data.item.hasAnyFallbackConf">Default Fallback - All Unmatched Requests</span>
                <span v-if="data.item.hasAnyFallbackConf && !data.item.fallbackConf">Not Handling Approval Workload</span>
                <span v-if="data.item.fallbackConf">Assigned Fallback - All Unmatched Requests</span>
              </b-form-checkbox>
            </div>
            <div v-if="data.item.tagConf && data.item.tagConf.length > 0">
              <div v-for="tConf in data.item.tagConf" :key="tConf.userTagValue">Users in <span class="font-italic text-secondary">{{tConf.userTagKey}}:</span> <span>{{tConf.userTagValue}}</span></div>
            </div>
            <div v-if="data.item.userConf && data.item.userConf.length > 0" >
              <b-badge variant="success">{{data.item.userConf.length}}</b-badge> Specific User{{ data.item.userConf.length > 1 ? 's' : '' }}
            </div>
            <div v-if="data.item.skillConf && data.item.skillConf.length > 0" >
              <b-badge variant="info">{{ data.item.skillConf.length }}</b-badge> Specific Skill{{ data.item.skillConf.length  > 1 ? 's' : '' }}
            </div>
          </div>
          <div class="col-auto">
            <b-button size="sm"
                      :aria-label="`Edit ${data.item.userIdForDisplay} approval workload`"
                      variant="outline-primary"
                      :disabled="data.item.isFallbackConfPresent"
                      data-cy="editApprovalBtn"
                      @click="data.toggleDetails">
              <span v-if="!data.detailsShowing"><i class="fas fa-edit" aria-hidden="true" /> Edit</span>
              <span v-if="data.detailsShowing"><i class="fas fa-arrow-alt-circle-up" aria-hidden="true" /> Collapse</span>
            </b-button>
          </div>
        </div>
      </template>

      <template #row-details="row">
        <div class="ml-5" :data-cy="`expandedChild_${row.item.userId}`">
          <self-report-approval-conf-user-tag v-if="userTagConfKey"
            :user-info="row.item"
            :tag-key="userTagConfKey"
            :tag-label="userTagConfLabel"
            @conf-added="updatedConf"
            @conf-removed="removeConf"
            class="mt-3"/>
          <self-report-approval-conf-skill
            :user-info="row.item"
            @conf-added="updatedConf"
            @conf-removed="removeConf"
            class="mt-3"/>
          <self-report-approval-conf-specific-users
            :user-info="row.item"
            @conf-added="updatedConf"
            @conf-removed="removeConf"
            class="mt-3"/>
        </div>
      </template>

    </skills-b-table>

      <no-content2 v-if="!hasMoreThanOneApprover" title="Not Available"
                 class="my-5"
                 icon-size="fa-2x"
                 icon="fas fa-cogs"
                 data-cy="approvalConfNotAvailable">
        The ability to split the approval workload is unavailable because there is only<b-badge variant="info">1</b-badge> Admin for this project.
        Please add <b>Admins</b> or <b>Approvers</b> on the <b-link :to="{ name: 'ProjectAccess' }" style="text-decoration: underline" data-cy="navToAccessPage"><i class="fas fa-shield-alt skills-color-access" aria-hidden="true"/>Access</b-link> page in order to start using this feature.
    </no-content2>
    </div>
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
  import NoContent2 from '@/components/utils/NoContent2';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'SelfReportApprovalConfManager',
    components: {
      SkillsSpinner,
      NoContent2,
      SelfReportApprovalConfSkill,
      SelfReportApprovalConfUserTag,
      SelfReportApprovalConfSpecificUsers,
      SkillsBTable,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        loading: true,
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
      hasMoreThanOneApprover() {
        return this.table.items && this.table.items.length > 1;
      },
      userTagConfKey() {
        return this.$store.getters.config.approvalConfUserTagKey;
      },
      userTagConfLabel() {
        return this.$store.getters.config.approvalConfUserTagLabel ? this.$store.getters.config.approvalConfUserTagLabel : this.$store.getters.config.approvalConfUserTagKey;
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
                const basicTableInfo = users.data.map((u) => {
                  const allConf = approverConf.filter((c) => c.approverUserId === u.userId);
                  return {
                    userIdForDisplay: u.userIdForDisplay,
                    userId: u.userId,
                    roleName: u.roleName,
                    allConf,
                  };
                });
                this.updateTable(basicTableInfo);
              }).finally(() => {
                this.loading = false;
              });
          });
      },
      updateTable(basicTableInfo) {
        let hasAnyFallbackConf = false;
        let res = basicTableInfo.map((row) => {
          const { allConf } = row;
          const tagConf = allConf.filter((c) => c.userTagKey);
          const userConf = allConf.filter((c) => c.userId);
          const skillConf = allConf.filter((c) => c.skillId);
          const fallbackConf = allConf.find((c) => !c.skillId && !c.userId && !c.userTagKey);
          if (fallbackConf) {
            hasAnyFallbackConf = true;
          }
          return {
            ...row,
            tagConf,
            userConf,
            skillConf,
            allConf,
            fallbackConf,
            isFallbackConfPresent: fallbackConf !== null && fallbackConf !== undefined,
            hasAnyFallbackConf,
            hasConf: tagConf?.length > 0 || userConf?.length > 0 || skillConf?.length > 0,
          };
        });
        if (hasAnyFallbackConf) {
          res = res.map((item) => ({ ...item, hasAnyFallbackConf: true }));
        }
        this.table.items = res;
      },
      removeTagConf(removedConf) {
        this.removeConf(removedConf, 'tagConf');
      },
      updatedConf(newConf) {
        const itemToUpdate = this.table.items.find((i) => i.userId === newConf.approverUserId);
        itemToUpdate.allConf.push(newConf);
        this.updateTable(this.table.items);
      },
      removeConf(removedConf) {
        const itemToUpdate = this.table.items.find((i) => i.userId === removedConf.approverUserId);
        itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== removedConf.id);
        this.updateTable(this.table.items);
      },
      handleFallback(checked, rowItem) {
        const itemToUpdate = this.table.items.find((i) => i.userId === rowItem.userId);
        itemToUpdate.loading = true;
        if (checked) {
          SelfReportService.configureApproverForFallback(this.projectId, rowItem.userId)
            .then((newConf) => {
              itemToUpdate.allConf.push(newConf);
              this.updateTable(this.table.items);
              // close expanded child
              this.table.items = this.table.items.map((i) => ({ ...i, _showDetails: false }));
              this.$nextTick(() => this.$announcer.polite(`Assigned ${newConf.approverUserId} as a fallback approver.`));
            })
            .finally(() => {
              itemToUpdate.loading = false;
            });
        } else {
          SelfReportService.removeApproverConfig(this.projectId, rowItem.fallbackConf.id)
            .then(() => {
              itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== rowItem.fallbackConf.id);
              this.updateTable(this.table.items);
              this.$nextTick(() => this.$announcer.polite('Removed workload configuration successfully.'));
            }).finally(() => {
              itemToUpdate.loading = false;
            });
        }
      },
    },
  };
</script>

<style scoped>

</style>
