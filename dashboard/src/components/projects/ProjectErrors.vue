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
  <div id="projectErrorsPanel">
    <sub-page-header title="Project Issues">
      <div class="row">
        <div class="col">
          <b-tooltip target="remove-button" title="Remove all project errors." :disabled="errors.length < 1"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" ref="removeAllErrors" @click="removeAllErrors" :disabled="errors.length < 1" size="sm"
                      data-cy="removeAllErrors">
              <span class="d-none d-sm-inline">Remove</span> All <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
            </b-button>
          </span>
        </div>
      </div>
    </sub-page-header>

    <b-card body-class="p-0">
      <skills-spinner :is-loading="loading" />

      <skills-b-table v-if="!loading"
                      :options="table.options"
                      :items="errors"
                      data-cy="projectErrorsTable"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable">

        <template v-slot:cell(typeAndError)="data">
          <div class="pl-3">
            <div class="row mb-2">
              {{ data.item.errorType }}
            </div>
            <div class="row small">
              {{ formatErrorMsg(data.item.errorType, data.item.error) }}
            </div>
          </div>
        </template>

        <template v-slot:cell(created)="data">
          <date-cell :value="data.value"/>
        </template>

        <template v-slot:cell(lastSeen)="data">
          <date-cell :value="data.value"/>
        </template>

        <template v-slot:cell(count)="data">
          {{ data.value }}
        </template>

        <template #cell(edit)="data">
          <b-button :ref="`delete_${data.item.error}`" @click="removeError(data.item)" variant="outline-info" size="sm"
                    :data-cy="`deleteErrorButton_${encodeURI(data.item.error)}`"
                    :aria-label="`delete error for reported skill ${data.item.error}`">
            <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
          </b-button>
        </template>

      </skills-b-table>
    </b-card>
  </div>

</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ProjectService from './ProjectService';
  import DateCell from '../utils/table/DateCell';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'ProjectErrors',
    components: {
      SkillsBTable,
      SkillsSpinner,
      SubPageHeader,
      DateCell,
    },
    mixins: [MsgBoxMixin],
    props: [],
    data() {
      return {
        loading: true,
        errors: [],
        table: {
          options: {
            sortBy: 'lastSeen',
            sortDesc: true,
            busy: true,
            stacked: 'md',
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 25],
            },
            fields: [
              {
                label: 'Error',
                key: 'typeAndError',
                sortable: true,
                sortKey: 'errorType',
              }, {
                key: 'created',
                label: 'First Seen',
                sortable: true,
              }, {
                key: 'lastSeen',
                label: 'Last Seen',
                sortable: true,
              }, {
                key: 'count',
                label: 'Times Seen',
                sortable: true,
              }, {
                key: 'edit',
                label: 'Delete',
                sortable: false,
              },

            ],
          },
        },
      };
    },
    mounted() {
      this.loadErrors();
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      formatErrorMsg(errorType, error) {
        if (errorType === 'SkillNotFound') {
          return `Reported Skill Id [${error}] does not exist in this Project`;
        }
        return error;
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadErrors();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadErrors();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadErrors();
      },
      loadErrors() {
        this.loading = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        ProjectService.getProjectErrors(this.$route.params.projectId, pageParams).then((res) => {
          this.errors = res.data;
          this.table.options.pagination.totalRows = res.totalCount;
        }).finally(() => {
          this.loading = false;
          this.table.options.busy = false;
        });
      },
      removeAllErrors() {
        const msg = 'Are you absolutely sure you want to remove all Project issues?';
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.loading = true;
              ProjectService.deleteAllProjectErrors(this.$route.params.projectId).then(() => {
                this.loadErrors();
                this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
              });
            }
          });
      },
      removeError(projectError) {
        const msg = `Are you absolutely sure you want to remove issue related to ${projectError.error}?`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.loading = true;
              ProjectService.deleteProjectError(projectError.projectId, projectError.errorType, projectError.error).then(() => {
                this.loadErrors();
                this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
              });
            }
          });
      },
    },
  };
</script>
