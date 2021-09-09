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
      <div class="h6 mb-0 font-weight-bold">Self Reported Skills Required Approval</div>
    </template>
    <div class="row px-3 mb-3 mt-2">
      <div class="col">
        <b-button variant="outline-info" @click="loadApprovals" aria-label="Sync Records"
                  data-cy="syncApprovalsBtn" class="mr-2 mt-1">
          <i class="fas fa-sync-alt"></i>
        </b-button>
        <b-button variant="outline-info" @click="changeSelectionForAll(true)" data-cy="selectPageOfApprovalsBtn" class="mr-2 mt-1"><i class="fa fa-check-square"/> Select Page</b-button>
        <b-button variant="outline-info" @click="changeSelectionForAll(false)" data-cy="clearSelectedApprovalsBtn" class="mt-1"><i class="far fa-square"></i> Clear</b-button>
      </div>
      <div class="col text-right">
        <b-button variant="outline-danger" @click="reject.showModal=true" data-cy="rejectBtn" class="mt-1" :disabled="actionsDisabled"><i class="fa fa-times-circle"/> Reject</b-button>
        <b-button variant="outline-success" @click="approve" data-cy="approveBtn" class="mt-1 ml-2" :disabled="actionsDisabled"><i class="fa fa-check"/> Approve</b-button>
      </div>
    </div>

    <skills-b-table :options="table.options" :items="table.items"
                    @page-changed="pageChanged"
                    @page-size-changed="pageSizeChanged"
                    @sort-changed="sortTable"
                    data-cy="skillsReportApprovalTable">
      <template #head(request)="data">
        <span class="text-primary"><i class="fas fa-hand-pointer skills-color-skills" /> {{ data.label }}</span>
      </template>
      <template #head(userId)="data">
        <span class="text-primary"><i class="fas fa-user-plus skills-color-crossProjects" /> {{ data.label }}</span>
      </template>
      <template #head(requestedOn)="data">
        <span class="text-primary"><i class="fas fa-clock skills-color-access" /> {{ data.label }}</span>
      </template>

      <template v-slot:cell(userId)="data">
        {{ data.item.userIdForDisplay }}
      </template>

      <template v-slot:cell(request)="data">
        <div>
          <b-form-checkbox
            :id="`${data.item.userId}-${data.item.skillId}`"
            v-model="data.item.selected"
            :name="`checkbox--${data.item.skillId}`"
            :value="true"
            :unchecked-value="false"
            :inline="true"
            v-on:input="updateActionsDisableStatus"
            :data-cy="`approvalSelect_${data.item.userId}-${data.item.skillId}`"
          >
              <span>{{ data.item.skillName }}</span>
            <b-badge class="ml-2">+ {{ data.item.points }} Points</b-badge>
          </b-form-checkbox>

        </div>
        <div class="small text-muted">ID: {{ data.item.skillId }}</div>
        <div class="mt-2" style="font-size: 0.9rem;"><span class="text-muted">Note:</span>
          <span v-if="data.item.requestMsg && data.item.requestMsg.length > 0"> {{ data.item.requestMsg }}</span>
          <span v-else class="text-muted"> Not supplied</span>
        </div>

        <b-button size="sm" variant="outline-info"
                  class="mr-2 py-0 px-1 mt-1"
                  @click="data.toggleDetails"
                  :aria-label="`Expand details for ${data.item.name}`"
                  :data-cy="`expandDetailsBtn_${data.item.skillId}`">
          <i v-if="data.detailsShowing" class="fa fa-minus-square" />
          <i v-else class="fa fa-plus-square" />
          Skill Details
        </b-button>

        <b-button size="sm" variant="outline-info"
                  class="py-0 px-1 mt-1"
                  :data-cy="`viewSkillLink_${data.item.skillId}`"
                  :to="{ name:'SkillOverview',
                                  params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                  :aria-label="`View skill ${data.item.skillName}  via link`"
                  target="_blank">
          View Skill <i class="fas fa-external-link-alt" style="font-size: 0.7rem;"></i>
        </b-button>
      </template>

      <template v-slot:cell(requestedOn)="data">
        <date-cell :value="data.value" />
      </template>

      <template #row-details="row">
        <child-row-skills-display :project-id="row.item.projectId" :subject-id="row.item.subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                               :parent-skill-id="row.item.skillId" :refresh-counter="row.item.refreshCounter"
                               class="mr-3 ml-5 mb-3"></child-row-skills-display>
      </template>

    </skills-b-table>

    <ValidationObserver v-slot="{invalid}" slim>
    <b-modal id="rejectSkillsModal"
             :no-close-on-backdrop="true"
             :centered="true"
             v-model="reject.showModal">
      <template #modal-title>
        <div class="h5 text-uppercase">Reject Skills</div>
      </template>
      <div id="rejectionTitleInModal" class="row p-2" data-cy="rejectionTitle">
        <div class="col-auto text-center">
          <i class="far fa-thumbs-down text-warning" style="font-size: 3rem"/>
        </div>
        <div class="col">
          <p class="h6">This will <b class="font-weight-bold">permanently</b> reject user's request(s) to get points. Users will be notified and you can provide an optional message below.</p>
        </div>
      </div>
      <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" v-slot="{errors}"
                          name="Rejection Message">
        <input type="text" id="approvalRequiredMsg" v-model="reject.rejectMsg"
               aria-describedby="rejectionTitleInModal" aria-label="Optional Rejection Message"
              class="form-control" placeholder="Message (optional)" data-cy="rejectionInputMsg">
        <small class="form-text text-danger mb-3" data-cy="rejectionInputMsgError">{{ errors[0] }}</small>
      </ValidationProvider>
      <template #modal-footer>
        <button type="button" class="btn btn-outline-danger text-uppercase" @click="reject.showModal=false"
                data-cy="cancelRejectionBtn">
          <i class="fas fa-times-circle"></i> Cancel
        </button>
        <button type="button" class="btn btn-outline-success text-uppercase"
                :disabled="invalid"
                @click="doReject(); reject.showModal=false;" data-cy="confirmRejectionBtn">
          <i class="fas fa-arrow-alt-circle-right"></i> Reject
        </button>
      </template>
    </b-modal>
    </ValidationObserver>
  </b-card>
</template>

<script>
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import DateCell from '../../utils/table/DateCell';
  import SelfReportService from './SelfReportService';
  import ChildRowSkillsDisplay from '../ChildRowSkillsDisplay';

  export default {
    name: 'SelfReportApproval',
    components: { ChildRowSkillsDisplay, DateCell, SkillsBTable },
    data() {
      return {
        projectId: this.$route.params.projectId,
        actionsDisabled: true,
        reject: {
          showModal: false,
          rejectMsg: '',
        },
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'requestedOn',
            sortDesc: true,
            emptyText: 'Nothing to approve',
            fields: [
              {
                key: 'request',
                label: 'Requested',
                sortable: false,
              },
              {
                key: 'userId',
                label: 'For User',
                sortable: true,
              },
              {
                key: 'requestedOn',
                label: 'Requested On',
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
        },
      };
    },
    mounted() {
      this.loadApprovals();
    },
    methods: {
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadApprovals();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadApprovals();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadApprovals();
      },
      loadApprovals() {
        this.table.options.busy = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        SelfReportService.getApprovals(this.projectId, pageParams)
          .then((res) => {
            this.table.items = res.data.map((item) => ({ selected: false, ...item }));
            this.table.options.pagination.totalRows = res.count;
            this.table.options.busy = false;
            this.updateActionsDisableStatus();
          });
      },
      changeSelectionForAll(selectedValue) {
        this.table.items.forEach((item) => {
          // eslint-disable-next-line no-param-reassign
          item.selected = selectedValue;
        });
        this.updateActionsDisableStatus();
      },
      updateActionsDisableStatus() {
        if (this.table.items.find((item) => item.selected) !== undefined) {
          this.actionsDisabled = false;
        } else {
          this.actionsDisabled = true;
        }
      },
      approve() {
        this.table.options.busy = true;
        const idsToApprove = this.table.items.filter((item) => item.selected).map((item) => item.id);
        SelfReportService.approve(this.projectId, idsToApprove)
          .then(() => {
            this.loadApprovals();
            this.$emit('approval-action', 'approved');
          });
      },
      doReject() {
        this.table.options.busy = true;
        const ids = this.table.items.filter((item) => item.selected).map((item) => item.id);
        SelfReportService.reject(this.projectId, ids, this.reject.rejectMsg)
          .then(() => {
            this.loadApprovals();
            this.$emit('approval-action', 'rejected');
          });
      },
    },
  };
</script>

<style scoped>

</style>
