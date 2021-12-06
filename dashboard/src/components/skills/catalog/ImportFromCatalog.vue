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
  <b-modal id="importSkillsFromCatalog" size="xl" :title="`Import ${importType} from the Catalog`"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true" :hide-footer="true" body-class="px-0 mx-0"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hide="publishHidden"
           :aria-label="isSkill?'Import Skill from the Catalog':'Import Subject from the Catalog'">

    <skills-spinner :is-loading="loading"/>

    <no-content2 v-if="!loading && emptyCatalog" class="mt-4 mb-5"
                 title="Catalog is Empty">
      When other projects export {{ importType }}s to the Catalog then they will be available here
      to be imported.
    </no-content2>

    <div v-if="!loading && !emptyCatalog">
      <div class="row px-3 pt-1">
        <div class="col-md border-right">
          <b-form-group label="Skill Name:" label-for="user-name-filter" label-class="text-muted">
            <b-form-input id="user-name-filter" v-model="filters.skillName"
                          v-on:keydown.enter="reloadTable"
                          data-cy="achievementsNavigator-usernameInput"/>
          </b-form-group>
        </div>
        <div class="col-md border-right">
          <b-form-group label="Project Name:" label-for="user-name-filter" label-class="text-muted">
            <b-form-input id="user-name-filter" v-model="filters.projectName"
                          v-on:keydown.enter="reloadTable"
                          data-cy="achievementsNavigator-usernameInput"/>
          </b-form-group>
        </div>
        <div class="col-md">
          <b-form-group label="Subject Name:" label-for="user-name-filter" label-class="text-muted">
            <b-form-input id="user-name-filter" v-model="filters.subjectName"
                          v-on:keydown.enter="reloadTable"
                          data-cy="achievementsNavigator-usernameInput"/>
          </b-form-group>
        </div>
      </div>

      <div class="row px-3 mb-3 mt-2">
        <div class="col">
          <div class="pr-2 border-right mr-2 d-inline-block">
            <b-button variant="outline-primary" @click="reloadTable"
                      data-cy="" class="mt-1"><i
              class="fa fa-filter"/> Filter
            </b-button>
          </div>
          <b-button variant="outline-info" @click="changeSelectionForAll(true)"
                    data-cy="selectPageOfApprovalsBtn" class="mr-2 mt-1"><i
            class="fa fa-check-square"/> Select Page
          </b-button>
          <b-button variant="outline-info" @click="changeSelectionForAll(false)"
                    data-cy="clearSelectedApprovalsBtn" class="mt-1"><i class="far fa-square"></i>
            Clear
          </b-button>
        </div>
        <div class="col text-right">
          <b-button variant="outline-success" @click="importSkills" data-cy="approveBtn"
                    class="mt-1 ml-2" :disabled="importDisabled"><i
            class="far fa-arrow-alt-circle-down"></i> Import
          </b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-size-changed="pageSizeChanged"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      data-cy="selfReportApprovalHistoryTable">
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
          <div class="text-primary">
            <b-form-checkbox
              :id="`${data.item.projectId}-${data.item.skillId}`"
              v-model="data.item.selected"
              :name="`checkbox_${data.item.projectId}_${data.item.skillId}`"
              :value="true"
              :unchecked-value="false"
              :inline="true"
              v-on:input="updateActionsDisableStatus"
              :data-cy="`approvalSelect_${data.item.projectId}-${data.item.skillId}`"
            >
              <span>{{ data.item.name }}</span>
            </b-form-checkbox>
          </div>
          <div class="text-secondary sub-info">
            <span>ID:</span> {{ data.item.skillId }}
          </div>

          <b-button size="sm" variant="outline-info"
                    class="mr-2 py-0 px-1 mt-1"
                    @click="data.toggleDetails"
                    :aria-label="`Expand details for ${data.item.name}`"
                    :data-cy="`expandDetailsBtn_${data.item.projectId}_${data.item.skillId}`">
            <i v-if="data.detailsShowing" class="fa fa-minus-square"/>
            <i v-else class="fa fa-plus-square"/>
            Skill Details
          </b-button>
        </template>

        <template v-slot:cell(projectId)="data">
          <div class="text-primary">
            {{ data.item.projectName }}
          </div>
          <div class="text-secondary sub-info">
            <span>ID:</span> {{ data.item.projectId }}
          </div>
        </template>

        <template v-slot:cell(subjectId)="data">
          <div class="text-primary">
            {{ data.item.subjectName }}
          </div>
          <div class="text-secondary sub-info">
            <span>ID:</span> {{ data.item.subjectId }}
          </div>
        </template>

        <template v-slot:cell(totalPoints)="data">
          <div>
            {{ data.value }}
          </div>
          <div class="text-secondary sub-info">
            {{ data.item.pointIncrement }} Increment x {{ data.item.numPerformToCompletion }}
            Occurrences
          </div>
        </template>

        <template #row-details="row">
          <skill-to-import-info :skill="row.item" />
        </template>

      </skills-b-table>
    </div>

  </b-modal>
</template>

<script>
  import CatalogService from './CatalogService';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import NoContent2 from '../../utils/NoContent2';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import SkillToImportInfo from './SkillToImportInfo';

  export default {
    name: 'ImportFromCatalog',
    components: {
      SkillToImportInfo,
      SkillsSpinner,
      NoContent2,
      SkillsBTable,
    },
    props: {
      importType: {
        type: String,
        default: 'Skill',
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        loading: false,
        importDisabled: true,
        filters: {
          skillName: '',
          projectName: '',
          subjectName: '',
        },
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'approverActionTakenOn',
            sortDesc: true,
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
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
          items: [],
        },
      };
    },
    mounted() {
      this.loadData();
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
        return this.table.items && this.table.items.length === 0;
      },
    },
    methods: {
      loadData() {
        this.loading = true;
        const params = {
          limit: 5,
          page: 1,
          orderBy: 'skillId',
          ascending: true,
        };
        CatalogService.getCatalogSkills(this.$route.params.projectId, params)
          .then((res) => {
            this.table.items = res.map((item) => ({ selected: false, ...item }));
          })
          .finally(() => {
            this.loading = false;
          });
      },
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        this.$emit('hidden', { importType: this.importType, ...e });
      },
      pageSizeChanged() {

      },
      pageChanged() {

      },
      sortTable() {

      },
      updateActionsDisableStatus() {
        if (this.table.items.find((item) => item.selected) !== undefined) {
          this.importDisabled = false;
        } else {
          this.importDisabled = true;
        }
      },
      importSkills() {
        const selected = this.table.items.filter((item) => item.selected);
        const promises = selected.map((skill) => new Promise((resolve) => {
          CatalogService.import(this.$route.params.projectId, this.$route.params.subjectId, skill.projectId, skill.skillId)
            .then((res) => resolve(res));
        }));
        Promise.all(promises).then(() => {
          this.loadData();
        });
      },
      importSkill(skill) {
        console.log(skill);
      },
      reloadTable() {

      },
      changeSelectionForAll(selectedValue) {
        this.table.items.forEach((item) => {
          // eslint-disable-next-line no-param-reassign
          item.selected = selectedValue;
        });
        this.updateActionsDisableStatus();
      },
    },
  };
</script>

<style scoped>
.sub-info {
  font-size: 0.9rem;
}
</style>
