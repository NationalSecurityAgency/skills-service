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
      <div class="h6 mb-0 font-weight-bold">Approval History</div>
    </template>

    <div class="row px-3 pt-2">
      <div class="col-md">
        <b-form-group label="Skill Name:" label-for="skill-filter" label-class="text-muted">
          <b-form-input id="skill-filter" v-model="filters.skill" v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-skillNameFilter"/>
        </b-form-group>
      </div>
      <div class="col-md">
        <b-form-group label="User Id:" label-for="userId-filter" label-class="text-muted">
          <b-form-input id="userId-filter" v-model="filters.userId" v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-userIdFilter"/>
        </b-form-group>
      </div>
      <div class="col-md">
        <b-form-group label="Approver:" label-for="approverUserId-filter" label-class="text-muted">
          <b-form-input id="approverUserId-filter" v-model="filters.approverUserId" v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-approverUserIdFilter"/>
        </b-form-group>
      </div>
    </div>

    <div class="row pl-3 mb-4">
      <div class="col">
        <b-button variant="outline-success" @click="loadApprovalsHistory" data-cy="selfReportApprovalHistory-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
        <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="selfReportApprovalHistory-resetBtn"><i class="fa fa-times"/> Reset</b-button>
      </div>
    </div>

    <skills-b-table :options="options" :items="items"
                    @page-size-changed="pageSizeChanged"
                    @page-changed="pageChanged"
                    @sort-changed="sortTable"
                    data-cy="selfReportApprovalHistoryTable">
      <template #head(skillName)="data">
        <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" /> {{ data.label }}</span>
      </template>
      <template #head(userId)="data">
        <span class="text-primary"><i class="fas fa-user-check skills-color-badges"></i> {{ data.label }}</span>
      </template>
      <template #head(approverUserId)="data">
        <span class="text-primary"><i class="fas fa-thumbs-up skills-color-achievements"></i> {{ data.label }}</span>
      </template>
      <template #head(requestedOn)="data">
        <span class="text-primary"><i class="fas fa-user-clock skills-color-crossProjects"></i> {{ data.label }}</span>
      </template>
      <template #head(rejectedOn)="data">
        <span class="text-primary"><i class="fas fa-question-circle skills-color-access"></i> {{ data.label }}</span>
      </template>
      <template #head(approverActionTakenOn)="data">
        <span class="text-primary"><i class="fas fa-stopwatch skills-color-access"></i> {{ data.label }}</span>
      </template>

      <template v-slot:cell(skillName)="data">
        <div>
          <router-link
            :data-cy="`viewSkillLink_${data.item.skillId}`"
            :to="{ name:'SkillOverview', params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
            :aria-label="`View skill ${data.item.skillName}  via link`"
            target="_blank"><span v-if="data.item.skillNameHtml" v-html="data.item.skillNameHtml"></span><span v-else>{{ data.item.skillName }}</span>
          </router-link>
          <b-badge class="ml-2">+ {{ data.item.points }} Points</b-badge>
        </div>
        <div class="text-primary">by</div>
        <div class="font-italic"><span v-if="data.item.userIdHtml" v-html="data.item.userIdHtml"></span><span v-else>{{ data.item.userIdForDisplay }}</span></div>
        <div v-if="data.item.requestMsg"><span class="text-secondary">Request Note:</span> {{ data.item.requestMsg }}</div>
      </template>

      <template v-slot:cell(requestedOn)="data">
        <date-cell :value="data.value" />
      </template>
      <template v-slot:cell(approverActionTakenOn)="data">
        <date-cell :value="data.value" />
      </template>

      <template v-slot:cell(rejectedOn)="data">
        <div v-if="data.item.rejectedOn"><b-badge variant="danger"><i class="fas fa-thumbs-down"></i> <span class="text-uppercase">Rejected</span></b-badge></div>
        <div v-else><b-badge variant="success"><i class="fas fa-thumbs-up"></i> <span class="text-uppercase">Approved</span></b-badge></div>
        <div class="text-primary">by</div>
        <div class="font-italic"><span v-if="data.item.approverUserIdHtml" v-html="data.item.approverUserIdHtml"></span><span v-else>{{ data.item.approverUserIdForDisplay }}</span></div>
        <div v-if="data.item.rejectionMsg"><span class="text-primary">Explanation:</span> {{ data.item.rejectionMsg }}</div>
      </template>

    </skills-b-table>

  </b-card>
</template>

<script>
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import DateCell from '../../utils/table/DateCell';
  import SelfReportService from './SelfReportService';

  export default {
    name: 'SelfReportApprovalHistory',
    components: { DateCell, SkillsBTable },
    data() {
      return {
        projectId: this.$route.params.projectId,
        filters: {
          skill: '',
          userId: '',
          approverUserId: '',
        },
        items: [],
        options: {
          busy: false,
          bordered: true,
          outlined: true,
          stacked: 'md',
          sortBy: 'approverActionTakenOn',
          sortDesc: true,
          fields: [
            {
              key: 'skillName',
              label: 'Requested',
              sortable: true,
            },
            {
              key: 'rejectedOn',
              label: 'Response',
              sortable: true,
            },
            {
              key: 'requestedOn',
              label: 'Requested On',
              sortable: true,
            },
            {
              key: 'approverActionTakenOn',
              label: 'Response On',
              sortable: true,
            },
          ],
          pagination: {
            server: true,
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20],
          },
        },
      };
    },
    mounted() {
      this.loadApprovalsHistory();
    },
    methods: {
      reset() {
        this.filters.skill = '';
        this.filters.userId = '';
        this.filters.approverUserId = '';
        this.loadApprovalsHistory();
      },
      loadApprovalsHistory() {
        this.options.busy = true;

        const pageParams = {
          limit: this.options.pagination.pageSize,
          ascending: !this.options.sortDesc,
          page: this.options.pagination.currentPage,
          orderBy: this.options.sortBy,
          skillNameFilter: this.filters.skill,
          userIdFilter: this.filters.userId,
          approverUserIdFilter: this.filters.approverUserId,
        };
        SelfReportService.getApprovalsHistory(this.projectId, pageParams)
          .then((res) => {
            let approvals = res.data.map((item) => ({ selected: false, ...item }));
            this.options.pagination.totalRows = res.count;
            this.options.busy = false;

            const skillNameFilterExist = pageParams.skillNameFilter && pageParams.skillNameFilter.trim().length > 0;
            const userIdFilterExist = pageParams.userIdFilter && pageParams.userIdFilter.trim().length > 0;
            const approverUserIdFilterFilterExist = pageParams.approverUserIdFilter && pageParams.approverUserIdFilter.trim().length > 0;

            const hasFilter = skillNameFilterExist || userIdFilterExist || approverUserIdFilterFilterExist;
            if (hasFilter) {
              approvals = approvals.map((item) => {
                const skillNameHtml = skillNameFilterExist ? this.highlight(item.skillName, pageParams.skillNameFilter) : null;
                const userIdHtml = userIdFilterExist ? this.highlight(item.userIdForDisplay, pageParams.userIdFilter) : null;
                const approverUserIdHtml = approverUserIdFilterFilterExist ? this.highlight(item.approverUserIdForDisplay, pageParams.approverUserIdFilter) : null;
                return Object.assign(item, { skillNameHtml, userIdHtml, approverUserIdHtml });
              });
            }

            this.items = approvals;
          });
      },
      highlight(value, searchStr) {
        const searchStringNorm = searchStr.trim().toLowerCase();
        const index = value.toLowerCase().indexOf(searchStringNorm);
        const htmlRel = `${value.substring(0, index)}<mark>${value.substring(index, index + searchStringNorm.length)}</mark>${value.substring(index + searchStringNorm.length)}`;
        return htmlRel;
      },
      pageSizeChanged(newSize) {
        this.options.pagination.pageSize = newSize;
        this.loadApprovalsHistory();
      },
      pageChanged(pageNum) {
        this.options.pagination.currentPage = pageNum;
        this.loadApprovalsHistory();
      },
      sortTable(sortContext) {
        this.options.sortBy = sortContext.sortBy;
        this.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.options.pagination.currentPage = 1;
        this.loadApprovalsHistory();
      },
    },
  };
</script>

<style scoped>

</style>
