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
  <div class="w-100">
    <div v-if="showRefresh" class="text-info m-2">
      Invite expiration has been extended,
      <b-button variant="info" @click="refresh" class="text-uppercase" size="sm"
                                                     data-cy="refreshInviteStatus"><i class="fas fa-redo-alt" aria-hidden="true"></i> refresh
      </b-button> table?
    </div>
    <skills-b-table :options="table.options"
                    :items="data"
                    @page-changed="pageChanged"
                    @page-size-changed="pageSizeChanged"
                    @sort-changed="sortTable"
                    tableStoredStateId="projectInviteStatusTable"
                    data-cy="projectInviteStatusTable">
      <template v-slot:cell(created)="data">
          {{ data.value | relativeTime }}
      </template>
      <template v-slot:cell(expires)="data">
        <span v-if="isExpired(data.value)" class="text-danger">
            expired
        </span>
        <span v-else>
        {{ data.value | timeFromNow }}
        </span>
      </template>
      <template v-slot:cell(controls)="data">
        <div>
          <b-button-group class="float-left">
            <b-dropdown :no-caret="true" :lazy="true" variant="outline-primary" :aria-label="`extend invite expiration`" :id="`extend-${data.index}`">
              <template #button-content>
                <span v-b-tooltip="`Extend ${data.item.recipientEmail}'s invite expiration`">
                  <span class="sr-only">extend expiration</span>
                  <i class="fas fa-hourglass-half" aria-hidden="true"/>
                </span>
              </template>
              <b-dropdown-header>
                Extend expiration by
              </b-dropdown-header>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT30M')">30 minutes</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT8H')">8 hours</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT24H')">24 hours</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'P7D')">7 days</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'P30D')">30 days</b-dropdown-item>
            </b-dropdown>
            <b-button variant="outline-primary" :aria-label="'remind user'"
                      data-cy="remindUser"
                      v-b-tooltip="`Send ${data.item.recipientEmail} a reminder`"
                      @click="remindUser(data.item.recipientEmail)">
              <i class="fas fa-paper-plane" aria-hidden="true"/>
            </b-button>
            <b-button :ref="`${data.item.recipientEmail}_delete`" variant="outline-primary" :aria-label="'delete project invite'"
                      v-b-tooltip="`Delete invite for ${data.item.recipientEmail}`"
                      data-cy="deleteInvite"
                      @click="deletePendingInvite(data.item.recipientEmail, `${data.item.recipientEmail}_delete`)">
              <i class="text-warning fas fa-trash" aria-hidden="true"/>
            </b-button>
          </b-button-group>
        </div>
      </template>
    </skills-b-table>

    <removal-validation v-if="showDeleteDialog" v-model="showDeleteDialog" @do-remove="doDeletePendingInvite" @hidden="handleDeleteCancelled">
      <p>
        This will delete the project invite for <span class="text-primary font-weight-bold">{{ this.recipientToDelete }}</span>.
      </p>
    </removal-validation>

  </div>
</template>

<script>
  import dayjs from '@/common-components/DayJsCustomizer';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';
  import AccessService from './AccessService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  const SHOW_REFRESH_DURATION = 8000;

  export default {
    name: 'InviteStatuses',
    mixins: [MsgBoxMixin, NavigationErrorMixin],
    components: { SkillsBTable, RemovalValidation },
    props: {
      projectId: {
        type: String,
        default: null,
      },
    },
    data() {
      return {
        data: [],
        showDeleteDialog: false,
        recipientFilter: '',
        isSaving: false,
        extensionInProgress: false,
        extensionDone: false,
        showRefresh: false,
        recipientToDelete: '',
        deleteRecipientRef: null,
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'md',
            sortBy: 'expires',
            sortDesc: false,
            fields: [
              {
                key: 'recipientEmail',
                label: 'Recipient',
                sortable: true,
              },
              {
                key: 'created',
                label: 'Created',
                sortable: true,
              },
              {
                key: 'expires',
                label: 'Expires',
                sortable: true,
              },
              {
                key: 'controls',
                label: 'Actions',
                sortable: false,
              },
            ],
            pagination: {
              hideUnnecessary: true,
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
            tableDescription: 'project invite status table',
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      isExpired(expirationDate) {
        return dayjs(expirationDate).isBefore(dayjs());
      },
      loadData() {
        this.table.options.busy = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        AccessService.getInviteStatuses(this.projectId, this.recipientFilter, pageParams)
          .then((result) => {
            this.table.options.busy = false;
            this.data = result.data;
            this.table.options.pagination.totalRows = result.totalCount;
          });
      },
      extendExpiration(recipientEmail, extension) {
        this.extensionInProgress = true;
        this.extensionDone = false;
        AccessService.extendInvite(this.projectId, recipientEmail, extension).then(() => {
          this.showRefresh = true;
          setTimeout(() => {
            this.showRefresh = false;
          }, SHOW_REFRESH_DURATION);
          this.$announcer.polite(`the expiration of project invite for ${recipientEmail} has been extended`);
        }).finally(() => {
          this.extensionInProgress = false;
        });
      },
      refresh() {
        this.showRefresh = false;
        this.loadData();
      },
      deletePendingInvite(recipient, deleteBtnRef) {
        this.recipientToDelete = recipient;
        this.deleteRecipientRef = deleteBtnRef;
        this.showDeleteDialog = true;
      },
      doDeletePendingInvite() {
        this.table.options.busy = true;
        AccessService.deleteInvite(this.projectId, this.recipientToDelete).then(() => {
          const email = this.recipientToDelete;
          this.deleteRecipientRef = null;
          this.recipientToDelete = null;
          this.$announcer.polite(`the project invite for ${email} has been deleted`);
          this.loadData();
        });
      },
      handleDeleteCancelled() {
        this.$nextTick(() => {
          this.$refs[this.deleteRecipientRef].focus();
          this.deleteRecipientRef = null;
        });
      },
      remindUser(recipientEmail) {
        //how are we going to indicate visually to the user that their button click caused something to happen?
        //would need to set the userId as the
        AccessService.remindInvitedUser(this.projectId, recipientEmail).finally(() => {

        });
      },
    },
  };
</script>

<style scoped>
</style>

<style>
</style>
