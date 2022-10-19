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
    <div class="row pt-3">
      <div class="col-12">
        <b-form-group label="Recipient Filter" label-class="text-muted">
          <b-input v-model="recipientFilter" v-on:keydown.enter="loadData" data-cy="pendingInvite-recipientFilter" aria-label="recipient email filter"/>
        </b-form-group>
      </div>
      <div class="col-md">
      </div>
    </div>

    <div class="row pb-3">
      <div class="col">
        <b-button variant="outline-info" @click="loadData" data-cy="pendingInvite-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
        <b-button variant="outline-info" @click="resetFilter" class="ml-1" data-cy="pendingInvite-resetBtn"><i class="fa fa-times"/> Reset</b-button>
      </div>
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
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT30M')" :data-cy="`invite-${data.index}-extension`">30 minutes</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT8H')" :data-cy="`invite-${data.index}-extension`">8 hours</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'PT24H')" :data-cy="`invite-${data.index}-extension`">24 hours</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'P7D')" :data-cy="`invite-${data.index}-extension`">7 days</b-dropdown-item>
              <b-dropdown-item @click="extendExpiration(data.item.recipientEmail, 'P30D')" :data-cy="`invite-${data.index}-extension`">30 days</b-dropdown-item>
            </b-dropdown>
            <b-button variant="outline-primary" :aria-label="'remind user'"
                      data-cy="remindUser"
                      :disabled="isExpired(data.item.expires)"
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
    <div id="accessNotificationPanel" class="text-info mt-2">
      <div v-if="showNotificationSending">
        <b-spinner small variant="outline-primary" label="Sending reminder notification"></b-spinner> Sending notification reminder
      </div>
      <div v-if="showNotificationSuccess">
        <i class="fa fa-check" aria-hidden="true"/> Invite reminder sent!
      </div>
    </div>

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

  const MESSAGE_DURATION = 8000;

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
        showNotificationSending: false,
        showNotificationSuccess: false,
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
        AccessService.extendInvite(this.projectId, recipientEmail, extension).then(() => {
          this.$announcer.polite(`the expiration of project invite for ${recipientEmail} has been extended`);
          this.loadData();
        });
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
          if (this.$refs[this.deleteRecipientRef]) {
            this.$refs[this.deleteRecipientRef].focus();
          }
          this.deleteRecipientRef = null;
        });
      },
      remindUser(recipientEmail) {
        this.showNotificationSuccess = false;
        this.showNotificationSending = true;
        AccessService.remindInvitedUser(this.projectId, recipientEmail).then(() => {
          this.$announcer.polite(`Invite reminder sent to ${recipientEmail}`);
          this.showNotificationSending = false;
          this.showNotificationSuccess = true;
          setTimeout(() => {
            this.showNotificationSuccess = false;
          }, MESSAGE_DURATION);
        }).catch((err) => {
          if (err.response.data && err.response.data.errorCode && err.response.data.errorCode === 'ExpiredProjectInvite') {
            this.msgOk(`The project invite for ${recipientEmail} has expired, reminders cannot be sent for expired invites, please extend the expiration for this invite and try again.`, 'Expired Invite');
            this.loadData();
          } else {
            throw err;
          }
        }).finally(() => {
          this.showNotificationSending = false;
        });
      },
      resetFilter() {
        this.recipientFilter = '';
        this.loadData();
      },
    },
  };
</script>

<style scoped>
</style>

<style>
</style>
