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
  <div>
    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="User Id Filter" label-class="text-muted">
            <b-input v-model="filters.userId" v-on:keydown.enter="applyFilters" data-cy="privateProjectUsers-userIdFilter" aria-label="user id filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="privateProjectUsers-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="privateProjectUsers-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable"
                      tableStoredStateId="privateProjectUsersTable"
                      data-cy="privateProjectUsersTable">
        <template v-slot:cell(userId)="data">
          {{ getUserDisplay(data.item) }}

          <b-button-group class="float-right">
            <b-button @click="revokeAccess(data.value, getUserDisplay(data.item))"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover="'Remove user access to project'"
                      :aria-label="`Remove project access for ${getUserDisplay(data.item)}`"
                      data-cy="privateProjectUsersTable_revokeUserAccessBtn"><i class="fas fa-user-slash"/><span class="sr-only">Revoke Access</span>
            </b-button>
          </b-button-group>
        </template>
      </skills-b-table>
    </b-card>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import AccessService from '@/components/access/AccessService';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';

  export default {
    name: 'RemoveUserAccess',
    components: {
      SkillsBTable,
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        filters: {
          userId: '',
        },
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: true,
            fields: [
              {
                key: 'userId',
                label: 'User Id',
                sortable: true,
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
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite(`Revoke user access table has been filtered by ${this.filters.userId}`));
        });
      },
      reset() {
        this.filters.userId = '';
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite('Revoke user access table filters have been removed'));
        });
      },
      loadData() {
        this.table.options.busy = true;
        const pageParams = {
          query: this.filters.userId,
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };

        return AccessService.getUserRolesForProject(this.$route.params.projectId, 'ROLE_PRIVATE_PROJECT_USER', pageParams).then((res) => {
          this.table.items = res.data;
          this.table.options.pagination.totalRows = res.totalCount;
          this.table.options.busy = false;
        });
      },
      revokeAccess(userId, userIdForDisplay) {
        const msg = `Are you sure you want to revoke ${userIdForDisplay}'s access to this Project? ${userIdForDisplay}'s achievements will NOT be deleted,
        however ${userIdForDisplay} will no longer be able to access the training profile.`;
        this.msgConfirm(msg, 'Revoke Access', 'Yes, revoke access!').then((ok) => {
          if (ok) {
            this.table.options.busy = true;
            AccessService.deleteUserRole(this.$route.params.projectId, userId, 'ROLE_PRIVATE_PROJECT_USER').then(() => {
              this.loadData();
              this.$nextTick(() => {
                this.$announcer.polite(`Revoked project access for user ${userIdForDisplay}`);
              });
            }).finally(() => {
              this.table.options.busy = false;
            });
          }
        });
      },
      getUserDisplay(props) {
        const userDisplay = props.userIdForDisplay ? props.userIdForDisplay : props.userId;
        const { oAuthProviders } = this.$store.getters.config;
        if (oAuthProviders) {
          const indexOfDash = userDisplay.lastIndexOf('-');
          if (indexOfDash > 0) {
            const provider = userDisplay.substr(indexOfDash + 1);
            if (oAuthProviders.includes(provider)) {
              return userDisplay.substr(0, indexOfDash);
            }
          }
        }
        return userDisplay;
      },
    },
  };

</script>
