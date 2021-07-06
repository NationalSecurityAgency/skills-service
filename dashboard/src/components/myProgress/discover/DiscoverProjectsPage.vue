<template>
  <div class="container-fluid">
    <sub-page-header title="Discover Projects" class="pt-4">
      <b-button :to="{ name: 'MyProgressPage' }" variant="outline-primary"><i class="fas fa-arrow-alt-circle-left" aria-hidden="true"/> Back to My Progress and Ranking</b-button>
    </sub-page-header>

    <skills-spinner :is-loading="isLoading"/>
    <div v-if="!isLoading">

      <b-row class="mb-3">
        <b-col>
          <b-form-group
            id="searchProjectsFormGroup"
            label="Search:"
            label-for="searchProjectsInput"
          >
          <b-input-group id="searchProjectsInput">
            <template #append>
              <b-button variant="outline-secondary" @click="searchValue=''" data-cy="pinProjectsClearSearch" aria-label="clear search button"><i class="fas fa-times" aria-hidden="true"/></b-button>
            </template>
            <b-input v-focus v-model="searchValue" data-cy="pinProjectsSearchInput" aria-label="search for projects to pin"></b-input>
          </b-input-group>
          </b-form-group>
        </b-col>
          <b-col>
            <b-form-group
              id="searchProjectsFormGroup"
              label="Filter:"
              label-for="searchProjectsInput"
            >
              <h5><b-badge variant="primary">All</b-badge> <b-badge>Unpinned</b-badge> <b-badge>Pinned</b-badge></h5>
            </b-form-group>
        </b-col>
      </b-row>

      <b-card body-class="p-0">
      <b-table striped hover
               stacked="sm"
               :items="projects"
               :fields="fields"
               :per-page="paging.perPage"
               :current-page="paging.currentPage"
               :sort-by.sync="sortBy"
               :sort-desc.sync="sortDesc"
               :no-sort-reset="true"
               data-cy="discoverProjectsTable">
        <template #head(name)="data">
          <span class="text-primary"><i class="fas fa-list-alt skills-color-projects" /> {{ data.label }}</span>
        </template>
        <template #head(numSkills)="data">
          <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" /> {{ data.label }}</span>
        </template>
        <template #head(numSubjects)="data">
          <span class="text-primary"><i class="fas fa-cubes skills-color-subjects" /> {{ data.label }}</span>
        </template>
        <template #head(numBadges)="data">
          <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points" /> {{ data.label }}</span>
        </template>
        <template #head(totalPoints)="data">
          <span class="text-primary"><i class="fas fa-award skills-color-badges" /> {{ data.label }}</span>
        </template>

        <template #cell(name)="data">
          <b-row>
            <b-col>{{ data.value }}</b-col>
            <b-col cols="auto">
              <b-button v-if="!data.item.pinned" @click="pinProject(data.item)" variant="outline-primary"
                        size="sm"
                        v-b-tooltip.hover="'Pin'"
                        data-cy="pinButton"
                        :aria-label="`pin project ${data.item.projectId}`">
                <i class="fas fa-thumbtack" style="width: 1rem;" aria-hidden="true"/> Pin
              </b-button>
            </b-col>
          </b-row>
        </template>
        <template #cell(numSkills)="data">
          {{ data.value | number }}
        </template>
        <template #cell(numSubjects)="data">
          {{ data.value | number }}
        </template>
        <template #cell(numBadges)="data">
          {{ data.value | number }}
        </template>
        <template #cell(totalPoints)="data">
          {{ data.value | number }}
        </template>
      </b-table>

      <b-row align-h="center">
        <b-col>

        </b-col>
        <b-col cols="12" sm="auto">
          <b-pagination
            v-model="paging.currentPage"
            :total-rows="paging.totalRows"
            :per-page="paging.perPage"
            align="fill"
            pills
            data-cy="discoverProjectsPaging"/>
        </b-col>
        <b-col>
          <div class="small text-right px-3" data-cy="pinProjectsSearchResultsNumRows">
            <span class="text-muted">Rows:</span> {{ paging.totalRows | number}}
          </div>
        </b-col>
      </b-row>
      </b-card>
    </div>
  </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import ProjectService from '../../projects/ProjectService';
  import SkillsSpinner from '../../utils/SkillsSpinner';

  export default {
    name: 'DiscoverProjectsPage',
    components: {
      SkillsSpinner,
      SubPageHeader,
    },
    props: [],
    mounted() {
      this.loadAll();
    },
    data() {
      return {
        isLoading: true,
        searchValue: '',
        projects: [],
        fields: [{
                   key: 'name',
                   label: 'Project',
                   sortable: true,
                 },
                 {
                   key: 'numSubjects',
                   label: 'Subjects',
                   sortable: true,
                 },
                 {
                   key: 'numSkills',
                   label: 'Skills',
                   sortable: true,
                 },
                 {
                   key: 'numBadges',
                   label: 'Badges',
                   sortable: true,
                 },
                 {
                   key: 'totalPoints',
                   label: 'Points',
                   sortable: true,
                 }],
        sortBy: 'name',
        sortDesc: false,
        paging: {
          totalRows: 1,
          currentPage: 1,
          perPage: 5,
          pageOptions: [5, 10, 15],
        },
      };
    },
    methods: {
      loadAll() {
        this.searchValue = '';
        this.isLoading = true;
        ProjectService.getAvailableProjectsInProduction()
          .then((response) => {
            this.projects = response;
            this.paging.totalRows = this.projects.length;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
