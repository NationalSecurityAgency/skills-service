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
  <b-modal id="importSkillsFromCatalog" size="xl" title="Import Skills from the Catalog"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true" body-class="px-0 mx-0"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hide="publishHidden">
    <skills-spinner :is-loading="loading"/>

    <no-content2 v-if="!loading && emptyCatalog && !isInFinalizeState" class="mt-4 mb-5"
                 icon="fas fa-user-clock"
                 title="Nothing Available for Import" data-cy="catalogSkillImportModal-NoData">
      When other projects export Skills to the Catalog then they will be available here
      to be imported.
    </no-content2>

    <no-content2 v-if="!loading && isInFinalizeState" class="mt-4 mb-5"
                 title="Finalization in Progress" data-cy="catalogSkillImport-finalizationInProcess">
      Cannot import while imported skills are being finalized. Unfortunately will have to wait, thank you for the patience!
    </no-content2>

    <div v-if="!loading && !emptyCatalog && !isInFinalizeState">
      <div class="row px-3 pt-1">
        <div class="col-md border-right">
          <b-form-group label="Skill Name:" label-for="skill-name-filter" label-class="text-muted">
            <b-form-input id="skill-name-filter" v-model="filters.skillName"
                          v-on:keydown.enter="loadData"
                          maxlength="50"
                          data-cy="skillNameFilter"/>
          </b-form-group>
        </div>
        <div class="col-md border-right">
          <b-form-group label="Project Name:" label-for="project-name-filter" label-class="text-muted">
            <b-form-input id="project-name-filter" v-model="filters.projectName"
                          v-on:keydown.enter="loadData"
                          maxlength="50"
                          data-cy="projectNameFilter"/>
          </b-form-group>
        </div>
        <div class="col-md">
          <b-form-group label="Subject Name:" label-for="subject-name-filter" label-class="text-muted">
            <b-form-input id="subject-name-filter" v-model="filters.subjectName"
                          v-on:keydown.enter="loadData"
                          maxlength="50"
                          data-cy="subjectNameFilter"/>
          </b-form-group>
        </div>
      </div>

      <div class="row px-3 mb-3 mt-2">
        <div class="col">
          <div class="pr-2 border-right mr-2 d-inline-block">
            <b-button variant="outline-primary" @click="loadData"
                      class="mt-1" data-cy="filterBtn"><i
              class="fa fa-filter"/> Filter
            </b-button>
            <b-button variant="outline-primary" @click="reset" class="ml-1 mt-1" data-cy="filterResetBtn"><i class="fa fa-times"/> Reset</b-button>
          </div>
          <b-button variant="outline-info" @click="changeSelectionForAll(true)"
                    data-cy="selectPageOfSkillsBtn" class="mr-2 mt-1"><i
            class="fa fa-check-square"/> Select Page
          </b-button>
          <b-button variant="outline-info" @click="changeSelectionForAll(false)"
                    data-cy="clearSelectedBtn" class="mt-1"><i class="far fa-square"></i>
            Clear
          </b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-size-changed="pageSizeChanged"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      data-cy="importSkillsFromCatalogTable">
        <template #head(skillId)="data">
          <span class="text-primary"><i
            class="fas fa-graduation-cap skills-color-skills"/> {{ data.label }}</span>
        </template>
        <template #head(projectId)="data">
          <span class="text-primary"><i
            class="fas fa-tasks skills-color-projects"></i> {{ data.label }}</span>
        </template>
        <template #head(subjectId)="data">
          <span class="text-primary"><i
            class="fas fa-cubes skills-color-subjects"></i> {{ data.label }}</span>
        </template>
        <template #head(totalPoints)="data">
          <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points"></i> {{
              data.label
            }}</span>
        </template>

        <template v-slot:cell(skillId)="data">
          <skill-already-existing-warning :skill="data.item"/>
          <div class="row">
            <div class="col">
              <import-checkbox
                :skill-name="data.item.name"
                :skill-id="data.item.skillId"
                :project-id="data.item.projectId"
                :disabled="data.item.skillIdAlreadyExist || data.item.skillNameAlreadyExist"
                :selected="isSelected(data.item)"
                @importSelection="handleImportSelection($event, data.item)"
                @input="updateActionsDisableStatus"
                :disableSelection="maxBulkImportExceeded || maxSkillsInSubjectExceeded"
              />
            </div>
            <div class="col-auto">
              <b-button size="sm" variant="outline-info"
                        class="mr-2 py-0 px-1 mt-1"
                        @click="data.toggleDetails"
                        :aria-label="`Expand details for ${data.item.name}`"
                        :data-cy="`expandDetailsBtn_${data.item.projectId}_${data.item.skillId}`">
                <i v-if="data.detailsShowing" class="fa fa-minus-square"/>
                <i v-else class="fa fa-plus-square"/>
                Skill Details
              </b-button>
            </div>
          </div>
        </template>

        <template v-slot:cell(projectId)="data">
          <div class="row">
            <div class="col">
              <div class="text-primary">
                {{ data.item.projectName }}
              </div>
            </div>
            <div class="col-auto text-info">
              <b-button variant="link"
                        class="p-0"
                        @click="setProjectFilter(data.item.projectName)"
                        aria-label="Filter by Project Name"
                        data-cy="addProjectFilter">
                <i class="fas fa-search-plus" aria-hidden="true"></i>
              </b-button>
            </div>
          </div>
        </template>

        <template v-slot:cell(subjectId)="data">
          <div class="row">
            <div class="col">
              <div class="text-primary">
                {{ data.item.subjectName }}
              </div>
            </div>
            <div class="col-auto text-info">
              <b-button variant="link"
                        class="p-0"
                        @click="setSubjectFilter(data.item.subjectName)"
                        aria-label="Filter by Subject Name"
                        data-cy="addSubjectFilter">
                <i class="fas fa-search-plus" aria-hidden="true"></i>
              </b-button>
            </div>
          </div>
        </template>

        <template #row-details="row">
          <skill-to-import-info :skill="row.item" />
        </template>

      </skills-b-table>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button v-if="!emptyCatalog" variant="success" size="sm" class="float-right ml-2"
                @click="importSkills" data-cy="importBtn" :disabled="importDisabled || validatingImport || maxBulkImportExceeded || maxSkillsInSubjectExceeded"><i
        class="far fa-arrow-alt-circle-down"></i> Import <b-badge variant="primary" data-cy="numSelectedSkills">{{ numSelectedSkills }}</b-badge>
        <b-spinner v-if="validatingImport" small label="Small Spinner" class="ml-1"></b-spinner>
      </b-button>
      <b-button v-if="!emptyCatalog" variant="secondary" size="sm" class="float-right ml-2" @click="close"
                data-cy="closeButton">
        <i class="fas fa-times"></i> Cancel
      </b-button>

      <b-button v-if="emptyCatalog" variant="success" size="sm" class="float-right" @click="close"
                data-cy="okButton">
        <i class="fas fa-thumbs-up"></i> OK
      </b-button>

      <span v-if="maxBulkImportExceeded || maxSkillsInSubjectExceeded" class="float-right ml-2 text-danger" data-cy="maximum-selected">
        <i class="fas fa-exclamation-circle text-warning"/> {{this.maxExceededMsg}}
      </span>
    </div>
  </b-modal>
</template>

<script>
  import SkillAlreadyExistingWarning from '@/components/skills/catalog/SkillAlreadyExistingWarning';
  import SettingsService from '@/components/settings/SettingsService';
  import CatalogService from './CatalogService';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import NoContent2 from '../../utils/NoContent2';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import SkillToImportInfo from './SkillToImportInfo';
  import ImportCheckbox from './ImportCheckbox';

  export default {
    name: 'ImportFromCatalog',
    components: {
      SkillAlreadyExistingWarning,
      SkillToImportInfo,
      SkillsSpinner,
      NoContent2,
      SkillsBTable,
      ImportCheckbox,
    },
    props: {
      value: {
        type: Boolean,
        required: true,
      },
      currentProjectSkills: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        loading: true,
        isInFinalizeState: false,
        validatingImport: false,
        initialLoadHadData: false,
        importDisabled: true,
        numSelectedSkills: 0,
        filters: {
          skillName: '',
          projectName: '',
          subjectName: '',
        },
        selected: { },
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'skillId',
            sortDesc: false,
            tableDescription: 'Skills',
            fields: [
              {
                key: 'skillId',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'projectId',
                label: 'Project',
                sortable: true,
              },
              {
                key: 'subjectId',
                label: 'Subject',
                sortable: true,
              },
              {
                key: 'totalPoints',
                label: 'Points',
                sortable: true,
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 25, 50],
            },
          },
          items: [],
        },
      };
    },
    mounted() {
      this.loadFinalizationState()
        .then(() => {
          this.loadData(true);
        });
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isSkill() {
        return this.exportType === 'Skill';
      },
      emptyCatalog() {
        return !this.initialLoadHadData;
      },
      maxProjectNameLength() {
        return this.$store.state.maxProjectNameLength;
      },
      maxBulkImportExceeded() {
        return Object.values(this.selected).filter((item) => item.selected).length > this.$store.getters.config.maxSkillsInBulkImport;
      },
      maxSkillsInSubjectExceeded() {
        return Object.values(this.selected).filter((item) => item.selected).length + this.currentSkillCount
          > this.$store.getters.config.maxSkillsPerSubject;
      },
      currentSkillCount() {
        const initValue = 0;
        const currentSkills = this.currentProjectSkills || [];
        const totalCurrentSkills = currentSkills.reduce((previousValue, currentValue) => {
          if (currentValue.numSkillsInGroup !== null) {
            return previousValue + currentValue.numSkillsInGroup;
          }
          return previousValue + 1;
        }, initValue);
        return totalCurrentSkills;
      },
      maxExceededMsg() {
        if (this.maxBulkImportExceeded) {
          return `cannot import more than ${this.$store.getters.config.maxSkillsInBulkImport} Skills at once`;
        }
        if (this.maxSkillsInSubjectExceeded) {
          return `No more than ${this.$store.getters.config.maxSkillsPerSubject} Skills per Subject are allowed, this project already has ${this.currentSkillCount}`;
        }
        return '';
      },
    },
    methods: {
      loadFinalizationState() {
        return SettingsService.getProjectSetting(this.$route.params.projectId, 'catalog.finalize.state')
          .then((res) => {
            this.isInFinalizeState = res && res.value === 'RUNNING';
          });
      },
      isSelected(dataItem) {
        if (dataItem.selected === true) {
          return true;
        }
        const key = this.dataItemKey(dataItem);
        return this.selected[key]?.selected === true;
      },
      toggleSelected(changeEvent, dataItem) {
        const { selected } = changeEvent;
        if (dataItem.skillIdAlreadyExist || dataItem.skillNameAlreadyExist) {
          return;
        }
        // eslint-disable-next-line no-param-reassign
        dataItem.selected = selected;
        const key = this.dataItemKey(dataItem);
        if (selected === true) {
          this.$set(this.selected, key, { projectId: dataItem.projectId, skillId: dataItem.skillId, selected: true });
        } else {
          this.$delete(this.selected, key);
        }
      },
      dataItemKey(dataItem) {
        return `${dataItem.projectId}_${dataItem.skillId}`;
      },
      loadData(isInitial = undefined) {
        if (isInitial === true) {
          this.loading = true;
        }
        this.table.options.busy = true;
        const params = {
          limit: this.table.options.pagination.pageSize,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
          ascending: !this.table.options.sortDesc,
          projectNameSearch: encodeURIComponent(this.filters.projectName.trim()),
          subjectNameSearch: encodeURIComponent(this.filters.subjectName.trim()),
          skillNameSearch: encodeURIComponent(this.filters.skillName.trim()),
        };
        CatalogService.getCatalogSkills(this.$route.params.projectId, params)
          .then((res) => {
            const dataSkills = res.data;
            if (dataSkills) {
              this.table.items = dataSkills.map((item) => ({
                selected: false,
                ...item,
              }));
              this.table.options.pagination.totalRows = res.totalCount;
              if (this.table.items.length > 0) {
                this.initialLoadHadData = true;
              }
            }
          })
          .finally(() => {
            this.loading = false;
            this.table.options.busy = false;
          });
      },
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        this.$emit('hidden', { ...e });
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
      updateActionsDisableStatus() {
        this.numSelectedSkills = Object.values(this.selected).reduce((total, item) => (item.selected === true ? total + 1 : total), 0);
        this.importDisabled = this.numSelectedSkills === 0;
      },
      importSkills() {
        this.validatingImport = true;
        this.loadFinalizationState()
          .then(() => {
            if (!this.isInFinalizeState) {
              const selected = Object.values(this.selected).filter((item) => item.selected);
              const projAndSkillIds = selected.map((skill) => ({
                projectId: skill.projectId,
                skillId: skill.skillId,
              }));
              this.$emit('to-import', projAndSkillIds);
              this.show = false;
            } else {
              this.changeSelectionForAll(false);
            }
          }).finally(() => {
            this.validatingImport = false;
          });
      },
      changeSelectionForAll(selectedValue) {
        this.table.items.forEach((item) => {
          this.toggleSelected({ selected: selectedValue }, item);
        });
        this.updateActionsDisableStatus();
      },
      handleImportSelection(event, dataItem) {
        this.toggleSelected(event, dataItem);
        this.updateActionsDisableStatus();
      },
      setProjectFilter(projectName) {
        this.filters.projectName = projectName;
        this.loadData();
      },
      setSubjectFilter(subjectName) {
        this.filters.subjectName = subjectName;
        this.loadData();
      },
      reset() {
        this.filters.skillName = '';
        this.filters.projectName = '';
        this.filters.subjectName = '';
        this.loadData();
      },
    },
  };
</script>

<style scoped>
.sub-info {
  font-size: 0.9rem;
}
</style>
