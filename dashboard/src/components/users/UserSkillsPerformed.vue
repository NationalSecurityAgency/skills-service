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
      <skills-spinner :is-loading="!table.options.fields" class="mb-5"/>

      <div v-if="table.options.fields">
        <div class="row px-3 pt-3">
          <div class="col-12">
            <b-form-group label="Skill Filter" label-class="text-muted">
              <b-input v-model="filters.skillId" v-on:keydown.enter="applyFilters" data-cy="performedSkills-skillIdFilter" aria-label="skill id filter"/>
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
          <div class="col">
            <b-button @click="deleteAllSkills" variant="outline-info" :disabled="table.items.length === 0"
                      :aria-label="`remove all skill events from user`" style="float: right; margin-right: 15px">
              <i class="fas fa-trash" aria-hidden="true"/> Delete All
            </b-button>
          </div>
        </div>

        <skills-b-table v-if="table.options.fields" :options="table.options" :items="table.items" tableStoredStateId="performedSkillsTable"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable"
                      data-cy="performedSkillsTable">
        <template v-slot:cell(skillId)="data">
          <div class="row">
            <div class="col">
              <div class="text-primary">
                <span v-if="data.item.skillNameHtml" v-html="data.item.skillNameHtml"></span><span v-else>{{ data.item.skillName }}</span>
                <b-badge v-if="data.item.importedSkill === true" variant="success" class="text-uppercase ml-1" data-cy="importedTag">Imported</b-badge>
              </div>
              <div>
                <show-more :limit="50" :contains-html="true" :text="`ID: ${data.item.skillIdHtml ? data.item.skillIdHtml : data.item.skillId}`"/>
              </div>
            </div>
            <div class="col-auto text-info">
              <b-button variant="link"
                        class="p-0"
                        @click="setSkillFilter(data.item.skillName)"
                        aria-label="Filter by Skill Name"
                        data-cy="addSkillFilter">
                <i class="fas fa-search-plus" aria-hidden="true"></i>
              </b-button>
            </div>
          </div>
        </template>
        <template v-slot:cell(performedOn)="data">
          <date-cell :value="data.value" />
        </template>
        <template v-slot:cell(control)="data">
          <b-button @click="deleteSkill(data.item)" variant="outline-info" size="sm"
                    data-cy="deleteEventBtn"
                    v-if="data.item.importedSkill === false"
                    :aria-label="`remove skill ${data.item.skillId} from user`">
            <i class="fas fa-trash" aria-hidden="true"/>
          </b-button>
        </template>
      </skills-b-table>
      </div>
    </b-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import dayjs from '@/common-components/DayJsCustomizer';
  import ShowMore from '@/components/skills/selfReport/ShowMore';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import ToastSupport from '@/components/utils/ToastSupport';
  import UsersService from '@/components/users/UsersService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  const { mapActions } = createNamespacedHelpers('users');

  export default {
    name: 'UserSkillsPerformed',
    mixins: [MsgBoxMixin, ToastSupport, ProjConfigMixin],
    components: {
      SkillsSpinner,
      ShowMore,
      DateCell,
      SkillsBTable,
      SubPageHeader,
    },
    data() {
      return {
        displayName: 'Skills Performed Table',
        isLoading: true,
        data: [],
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
            tableDescription: 'User\'s Skill Events',
            fields: null,
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 10,
              possiblePageSizes: [5, 10, 15, 20, 50],
            },
          },
        },
        projectId: null,
        userId: null,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
      this.loadProjConfig()
        .then(() => {
          const fields = [
            {
              key: 'skillId',
              label: 'Skill Id',
              sortable: true,
            },
            {
              key: 'performedOn',
              label: 'Performed On',
              sortable: true,
            },
          ];
          if (!this.isReadOnlyProj) {
            fields.push({
              key: 'control',
              label: 'Delete',
              sortable: false,
            });
          }
          this.table.options.fields = fields;
        });
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
        this.loadTableData();
      },
      loadTableData() {
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
          this.table.items = res.data?.map((item) => {
            const skillNameHtml = item.skillName && this.filters.skillId ? StringHighlighter.highlight(item.skillName, this.filters.skillId) : null;
            const skillIdHtml = item.skillId && this.filters.skillId ? StringHighlighter.highlight(item.skillId, this.filters.skillId) : null;
            return { skillNameHtml, skillIdHtml, ...item };
          });
          this.table.options.pagination.totalRows = res.count;
          this.table.options.busy = false;
        });
      },
      getUrl() {
        return `/admin/projects/${encodeURIComponent(this.projectId)}/performedSkills/${encodeURIComponent(this.userId)}`;
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
      deleteAllSkills() {
        this.msgConfirm(`Removing all skills for [${this.userId}].  This will permanently remove all of this user's skill events and cannot be undone.`).then((res) => {
          if (res) {
            this.doDeleteAllSkills();
          }
        });
      },
      doDeleteSkill(skill) {
        this.isLoading = true;
        UsersService.deleteSkillEvent(this.projectId, skill, this.userId)
          .then((data) => {
            if (data.success) {
              this.loadData();
              this.loadUserDetailsState({ projectId: this.projectId, userId: this.userId });
            } else {
              this.errorToast('Unable to Remove Skill', `Skill '${skill.skillId}' was not removed.  ${data.explanation}`);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      doDeleteAllSkills() {
        this.isLoading = true;
        UsersService.deleteAllSkillEvents(this.projectId, this.userId)
          .then((data) => {
            if (data.success) {
              this.loadData();
              this.loadUserDetailsState({ projectId: this.projectId, userId: this.userId });
            } else {
              this.errorToast('Unable to Remove User Skills', `Skill events were not removed.  ${data.explanation}`);
            }
          }).finally(() => {
            this.isLoading = false;
          });
      },
      setSkillFilter(filterValue) {
        this.filters.skillId = filterValue;
        this.loadData();
      },
    },
  };
</script>

<style scoped>

</style>
