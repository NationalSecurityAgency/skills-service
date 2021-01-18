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
    <sub-page-header title="Performed Skills"/>

    <b-card body-class="p-0">
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="Skill Id Filter" label-class="text-muted">
            <b-input v-model="filters.skillId" data-cy="performedSkills-skillIdFilter" aria-label="skill id filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="performedSkills-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="performedSkills-resetBtn"><i class="fa fa-times"/> Reset</b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable"
                      data-cy="performedSkillsTable">

        <template v-slot:cell(performedOn)="data">
          <date-cell :value="data.value" />
        </template>
        <template v-slot:cell(control)="data">
          <b-button @click="deleteSkill(data.item)" variant="outline-info" size="sm" :aria-label="`remove skill ${data.item.skillId} from user`"><i class="fas fa-trash" aria-hidden="true"/></b-button>
        </template>
      </skills-b-table>

    </b-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';
  import UsersService from './UsersService';
  import dayjs from '../../DayJsCustomizer';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import DateCell from '../utils/table/DateCell';

  const { mapActions } = createNamespacedHelpers('users');

  export default {
    name: 'UserSkillsPerformed',
    mixins: [MsgBoxMixin, ToastSupport],
    components: {
      DateCell,
      SkillsBTable,
      SubPageHeader,
    },
    data() {
      return {
        displayName: 'Skills Performed Table',
        isLoading: true,
        data: [],
        columns: ['skillId', 'performedOn', 'delete'],
        filters: {
          skillId: '',
        },
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'performedOn',
            sortDesc: true,
            fields: [
              {
                key: 'skillId',
                label: 'Skill Id',
                sortable: true,
              },
              {
                key: 'performedOn',
                label: 'Performed ON',
                sortable: true,
              },
              {
                key: 'control',
                label: 'Delete',
                sortable: false,
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
        options: {
          headings: {
            skillId: 'Skill ID',
            performedOn: 'Performed On',
            delete: '',
          },
          sortable: ['skillId', 'performedOn'],
          orderBy: {
            column: 'performedOn',
            ascending: false,
          },
          dateColumns: ['performedOn'],
          dateFormat: 'YYYY-MM-DD HH:mm',
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: true,
          highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
        },
        projectId: null,
        userId: null,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
    },
    mounted() {
      this.loadData();
    },
    methods: {
      ...mapActions([
        'loadUserDetailsState',
      ]),
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      reset() {
        this.filters.skillId = '';
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
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
      loadData() {
        this.table.options.busy = true;
        const url = this.getUrl();
        UsersService.ajaxCall(url, {
          query: this.filters.skillId,
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          byColumn: 0,
          orderBy: this.table.options.sortBy,
        }).then((res) => {
          this.table.items = res.data;
          this.table.options.pagination.totalRows = res.count;
          this.table.options.busy = false;
        });
      },
      onLoading() {
        this.isLoading = true;
      },
      onLoaded(event) {
        this.isLoading = false;
        this.$emit('loaded', event);
      },
      getUrl() {
        return `/admin/projects/${this.projectId}/performedSkills/${this.userId}`;
      },
      emit(name, event) {
        this.$emit(name, event, this);
      },
      clear() {
        this.$refs.table.data = [];
        this.$refs.table.count = 0;
      },
      getDate(row) {
        return dayjs(row.performedOn)
          .format('LLL');
      },
      deleteSkill(row) {
        this.msgConfirm(`Removing skill [${row.skillId}] performed on [${this.getDate(row)}]. This will permanently remove this user's performed skill and cannot be undone.`)
          .then((res) => {
            if (res) {
              this.doDeleteSkill(row);
            }
          });
      },
      doDeleteSkill(skill) {
        this.isLoading = true;
        UsersService.deleteSkillEvent(this.projectId, skill, this.userId)
          .then((data) => {
            if (data.success) {
              // server table must not manually remove items but rather refresh the table
              this.$refs.table.refresh();
              this.loadUserDetailsState({ projectId: this.projectId, userId: this.userId });
              this.successToast('Removed Skill', `Skill '${skill.skillId}' was removed.`);
            } else {
              this.errorToast('Unable to Remove Skill', `Skill '${skill.skillId}' was not removed.  ${data.explanation}`);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
  .vue-table-2 table {
    width: 100%;
    position: relative;
  }
</style>
