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
  <b-card header="Assign Specific Users" body-class="p-0 mt-3">
    <template #header>
      <div>
        <i class="fas fa-user-plus text-primary" aria-hidden="true"/> Split Workload <span class="font-italic text-primary">By Specific Users</span>
      </div>
    </template>
    <ValidationProvider name="User Id" v-slot="{errors}" rules="userNoSpaceInUserIdInNonPkiMode">
      <div class="row mx-2 no-gutters">
        <div class="col px-1">
          <existing-user-input :project-id="projectId"
                             v-model="currentSelectedUser"
                             :can-enter-new-user="false"
                             name="User Id"
                             aria-errormessage="userIdInputError"
                             aria-describedby="userIdInputError"
                             :aria-invalid="errors && errors.length > 0"
                             data-cy="userIdInput"/>
          <small role="alert" id="userIdInputError" class="form-text text-danger" v-show="errors[0]">{{ errors[0]}}</small>
        </div>
        <div class="col-auto px-1">
          <b-button
            aria-label="Add Tag Value"
            @click="addConf"
            :disabled="!currentSelectedUser || (errors && errors.length > 0)"
            variant="outline-primary">Add <i class="fas fa-plus-circle" aria-hidden="true" />
          </b-button>
        </div>
      </div>
    </ValidationProvider>

    <skills-b-table v-if="hadData" class="mt-3"
                    :options="table.options" :items="table.items"
                    tableStoredStateId="skillApprovalConfSpecificUsersTable"
                    data-cy="skillApprovalConfSpecificUsersTable">
      <template v-slot:cell(userId)="data">
        <div class="row">
          <div class="col">
            {{ data.item.userIdForDisplay }}
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
                 icon="fas fa-user-plus">
      You can split the approval workload by routing approval requests for specific users to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
    </no-content2>
  </b-card>
</template>

<script>
  import ExistingUserInput from '@/components/utils/ExistingUserInput';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';
  import SelfReportApprovalConfMixin
    from '@/components/skills/selfReport/SelfReportApprovalConfMixin';
  import NoContent2 from '@/components/utils/NoContent2';

  export default {
    name: 'SelfReportApprovalConfSpecificUsers',
    components: {
      NoContent2, DateCell, SkillsBTable, ExistingUserInput,
    },
    mixins: [SelfReportApprovalConfMixin],
    props: {
      userInfo: Object,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        currentSelectedUser: null,
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
                key: 'userId',
                label: 'User',
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
    computed: {
      pkiAuthenticated() {
        return this.$store.getters.isPkiAuthenticated;
      },
      hadData() {
        return this.table.items && this.table.items.length > 0;
      },
    },
    mounted() {
      const hasConf = this.userInfo.userConf && this.userInfo.userConf.length > 0;
      if (hasConf) {
        this.table.items = this.userInfo.userConf.map((u) => ({ ...u }));
      }
    },
    methods: {
      addConf() {
        SelfReportService.configureApproverForUserId(this.projectId, this.userInfo.userId, this.currentSelectedUser.userId)
          .then((res) => {
            this.table.items.push(res);
            this.$emit('conf-added', res);
            this.$nextTick(() => this.$announcer.polite(`Added workload configuration successfully for ${this.currentSelectedUser.userId} user.`));
            this.currentSelectedUser = null;
          });
      },
    },
  };
</script>

<style scoped>

</style>
