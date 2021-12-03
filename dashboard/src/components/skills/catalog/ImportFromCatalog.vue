<template>
  <b-modal :id="id" size="xl" :title="`Import ${importType} to the Catalog`" v-model="show"
           :no-close-on-backdrop="true" :centered="true" :hide-footer="true" body-class="px-0 mx-0"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
           :aria-label="isSkill?'Import Skill to the Catalog':'Import Subject to the Catalog'">

    <skills-b-table :options="table.options" :items="table.items"
                    @page-size-changed="pageSizeChanged"
                    @page-changed="pageChanged"
                    @sort-changed="sortTable"
                    data-cy="selfReportApprovalHistoryTable">
      <template #head(skillId)="data">
        <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" /> {{ data.label }}</span>
      </template>
      <template #head(projectId)="data">
        <span class="text-primary"><i class="fas fa-tasks skills-color-projects"></i> {{ data.label }}</span>
      </template>
      <template #head(totalPoints)="data">
        <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points"></i> {{ data.label }}</span>
      </template>

      <template v-slot:cell(skillId)="data">
        <div class="text-primary">
          {{ data.item.name }}
        </div>
        <div class="text-secondary sub-info">
          <span>ID:</span> {{ data.item.skillId }}
        </div>
      </template>

      <template v-slot:cell(projectId)="data">
        <div class="text-primary">
          TBD
        </div>
        <div class="text-secondary sub-info">
          <span>ID:</span> {{ data.item.projectId }}
        </div>
      </template>

      <template v-slot:cell(totalPoints)="data">
        <div>
          {{ data.value }}
        </div>
        <div class="text-secondary sub-info">
          {{ data.item.pointIncrement }} Increment x {{ data.item.numPerformToCompletion }} Occurrences
        </div>
      </template>

      <template v-slot:cell(controls)="data">
        <b-button id="importFromCatalogBtn" ref="importFromCatalogBtn" @click="importSkill(data.item)" variant="outline-primary" size="sm"
                  aria-label="import from catalog"
                  data-cy="importFromCatalogBtn">
          <span class="">Import</span> <i class="fas fa-book" aria-hidden="true"/>
        </b-button>
      </template>
    </skills-b-table>

  </b-modal>
</template>

<script>
  import CatalogService from './CatalogService';
  import SkillsBTable from '../../utils/table/SkillsBTable';

  export default {
    name: 'ImportFromCatalog',
    components: { SkillsBTable },
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
                key: 'projectId',
                label: 'Project',
                sortable: true,
              },
              {
                key: 'skillId',
                label: 'Skill',
                sortable: true,
              },
              {
                key: 'totalPoints',
                label: 'Points',
                sortable: true,
              },
              {
                key: 'controls',
                label: '',
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
    },
    methods: {
      loadData() {
        const params = {
          limit: 5,
          page: 1,
          orderBy: 'skill.skillId',
          ascending: true,
        };
        CatalogService.getCatalogSkills(this.$route.params.projectId, params)
          .then((res) => {
            this.table.items = res;
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
      importSkill(skill) {
        console.log(skill);
      },
    },
  };
</script>

<style scoped>
.sub-info {
  font-size: 0.9rem;
}
</style>
