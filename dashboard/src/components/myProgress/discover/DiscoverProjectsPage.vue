<template>
    <div class="container-fluid">
      <sub-page-header title="Discover Projects" class="pt-4">
      </sub-page-header>

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
      </b-table>
    </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import ProjectService from '../../projects/ProjectService';

  export default {
    name: 'DiscoverProjectsPage',
    components: { SubPageHeader },
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
                   sortable: true,
                 },
                 {
                   key: 'numSkills',
                   label: 'Skills',
                   sortable: true,
                 },
                 {
                   key: 'lastReportedSkill',
                   label: 'Last Reported Skill',
                   sortable: true,
                 },
                 {
                   key: 'created',
                   label: 'Created',
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
        ProjectService.getProjects()
          .then((response) => {
            this.projects = response;
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
