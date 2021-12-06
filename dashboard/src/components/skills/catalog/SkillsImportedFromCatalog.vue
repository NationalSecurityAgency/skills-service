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
/*
Copyright 2021 SkillTree

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
  <div id="importedCatalogSkills">
    <sub-page-header title="Imported Skills">
      <!--div class="row">
        <div class="col">
          <b-tooltip target="remove-button" title="Remove all project errors." :disabled="errors.length < 1"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" ref="removeAllErrors" @click="removeAllErrors" :disabled="errors.length < 1" size="sm"
                      data-cy="removeAllErrors">
              <span class="d-none d-sm-inline">Remove</span> All <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
            </b-button>
          </span>
        </div>
      </div-->
    </sub-page-header>

    <loading-container v-bind:is-loading="loading">
      <b-card body-class="p-0">
        <div class="row px-3 pt-3">
          <div class="col-12">
            <b-form-group label="Skill Filter" label-class="text-muted">
              <b-input v-model="table.filter.name" v-on:keyup.enter="applyFilters"
                       data-cy="skillsTable-skillFilter" aria-label="skill name filter"/>
            </b-form-group>
          </div>
          <div class="col-md">
          </div>
        </div>

        <div class="row pl-3 mb-3">
          <div class="col">
            <b-button variant="outline-info" @click="applyFilters" data-cy="importedSkills-filterBtn"><i class="fa fa-filter"/> Filter</b-button>
            <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="importedSkills-resetBtn"><i class="fa fa-times"/> Reset</b-button>
          </div>
        </div>

        <div class="row mb-2">
          <div class="col"></div>
          <div class="col-auto text-right" data-cy="skillsTable-additionalColumns">
            <span class="text-secondary mr-2">Additional Columns:</span>
            <b-form-checkbox-group class="d-inline"
                                   id="skillsAdditionalColumns"
                                   v-model="table.extraColumns.selected"
                                   :options="table.extraColumns.options"
                                   name="Skills Table Additional Columns"
            ></b-form-checkbox-group>
          </div>
        </div>

        <skills-b-table :options="table.options" :items="importedSkills"
                        data-cy="importedSkillsTable">

          <template v-slot:cell(name)="data">
            <div class="row">
              <div class="col-auto pr-0">
                <b-button size="sm" @click="data.toggleDetails" class="mr-2 py-0 px-1 btn btn-info"
                          :aria-label="`Expand details for ${data.item.name}`"
                          :data-cy="`expandDetailsBtn_${data.item.skillId}`">
                  <i v-if="data.detailsShowing" class="fa fa-minus-square" />
                  <i v-else class="fa fa-plus-square" />
                </b-button>
              </div>
              <div class="col pl-0">
                  <div class="h5">{{ data.item.name }}</div>
                  <div class="text-muted" style="font-size: 0.9rem;">ID: {{ data.item.skillId }}</div>
              </div>
            </div>
          </template>

          <template v-slot:cell(totalPoints)="data">
            <div>{{ data.value }}</div>
            <div class="small text-secondary">{{ data.item.pointIncrement | number }} pts x {{ data.item.numPerformToCompletion | number }} repetitions</div>
          </template>

          <template v-slot:cell(timeWindow)="data">
            <div>{{ timeWindowTitle(data.item) }}
              <i v-if="!timeWindowHasLength(data.item)" class="fas fa-question-circle text-muted" v-b-tooltip.hover="`${timeWindowDescription(data.item)}`"></i>
            </div>
          </template>

          <template v-slot:cell(displayOrder)="data">
            <div class="row">
              <div class="col">
                <span>{{data.value}}</span>
              </div>
            </div>
          </template>
          <template v-slot:cell(created)="data">
            <div>
              <span>{{ data.value | date }}</span>
              <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
            </div>
            <div class="text-muted small">
              {{ data.value | timeFromNow }}
            </div>
          </template>
          <template v-slot:cell(selfReportingType)="data">
            {{ getSelfReportingTypePretty(data.item.selfReportingType) }}
          </template>
          <template #row-details="row">
            <ChildRowSkillsDisplay :project-id="projectId" :subject-id="subjectId" v-skills-onMount="'ExpandSkillDetailsSkillsPage'"
                                   :parent-skill-id="row.item.skillId" :refresh-counter="row.item.refreshCounter"
                                   class="mr-3 ml-5 mb-3"></ChildRowSkillsDisplay>
          </template>
        </skills-b-table>

      </b-card>

    </loading-container>

  </div>
</template>

<script>
 /* import { SkillsReporter } from '@skilltree/skills-client-vue';
  import dayjs from '@/common-components/DayJsCustomizer'; */
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import SkillsService from '@/components/skills/SkillsService';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  /* import DateCell from '@/components/utils/table/DateCell'; */
  /* import NoContent2 from '../../utils/NoContent2'; */
  import ChildRowSkillsDisplay from '../ChildRowSkillsDisplay';
  import LoadingContainer from '../../utils/LoadingContainer';
  import TimeWindowMixin from '../TimeWindowMixin';

  export default {
    name: 'SkillsImportedFromCatalog',
    mixins: [TimeWindowMixin],
    components: {
      SkillsBTable,
      SubPageHeader,
      /* DateCell, */
      /* NoContent2, */
      ChildRowSkillsDisplay,
      LoadingContainer,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        loading: true,
        projId: this.projectId,
        importedSkills: [],
        table: {
          extraColumns: {
            options: [{
              value: 'totalPoints',
              text: 'Points',
            }, {
              value: 'selfReportingType',
              text: 'Self Report',
            }, {
              value: 'timeWindow',
              text: 'Time Window',
            }, {
              value: 'version',
              text: 'Version',
            }],
            selected: [],
          },
          filter: {
            name: '',
          },
          options: {
            rowDetailsControls: false,
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'created',
            sortDesc: true,
            fields: [
              {
                key: 'name',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'displayOrder',
                label: 'Display Order',
                sortable: true,
              },
              {
                key: 'created',
                label: 'Created',
                sortable: true,
              },

            ],
            pagination: {
              currentPage: 1,
              totalRows: 1,
              pageSize: 10,
              possiblePageSizes: [10, 15, 25],
            },
          },
        },
      };
    },
    watch: {
      '$route.params.projectId': function watcher() {
        this.projectId = this.$route.params.projectId;
      },
      'table.extraColumns.selected': function updateColumns(newList) {
        const extraColLookup = {
          totalPoints: {
            key: 'totalPoints',
            label: 'Points',
            sortable: true,
          },
          version: {
            key: 'version',
            label: 'Version',
            sortable: true,
          },
          timeWindow: {
            key: 'timeWindow',
            label: 'Time Window',
            sortable: false,
          },
          selfReportingType: {
            key: 'selfReportingType',
            label: 'Self Report Type',
            sortable: true,
          },
        };

        Object.keys(extraColLookup).forEach((key) => {
          if (newList.includes(key)) {
            this.table.options.fields.push(extraColLookup[key]);
          } else {
            this.table.options.fields = this.table.options.fields.filter((item) => item.key !== key);
          }
        });
      },
    },
    mounted() {
      this.loadImportedSkills();
    },
    methods: {
      applyFilters() {
        if (this.table.filter.name && this.table.filter.name.length > 0) {
          this.skills = this.skillsOriginal.filter((item) => {
            const filter = this.table.filter.name.trim().toLowerCase();
            if (item.name.trim().toLowerCase().indexOf(filter) !== -1
              || item.skillId.trim().toLowerCase().indexOf(filter) !== -1) {
              return true;
            }
            return false;
          });
        } else {
          this.reset();
        }
      },
      reset() {
        this.table.filter.name = '';
        this.skills = this.skillsOriginal.map((item) => item);
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadImportedSkills();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadImportedSkills();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadImportedSkills();
      },
      loadImportedSkills() {
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        this.loading = true;
        SkillsService.getSkillsImportedFromCatalog(this.projId, pageParams).then((data) => {
          this.importedSkills = data;
        }).finally(() => {
          this.loading = false;
          this.table.options.busy = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>
