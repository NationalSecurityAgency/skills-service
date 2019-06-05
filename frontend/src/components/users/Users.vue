<template>
  <div class="usersTable">
    <sub-page-header title="Users"/>

    <simple-card>
      <v-server-table ref="table" :columns="columns" :url="getUrl()" :options="options" class="vue-table-2"
                      v-on:loaded="emit('loaded', $event)" v-on:error="emit('error', $event)">
        <div slot="lastUpdated" slot-scope="props" class="field has-addons">
          {{ getDate(props) }}
        </div>

        <div slot="viewDetail" slot-scope="props" class="">
          <router-link :to="{ name:'ClientDisplayPreview',
                  params: { projectId: $route.params.projectId, userId: props.row.userId }}"
                       tag="button" class="btn btn-outline-primary">
            <span class="d-none d-sm-inline">Details</span><i class="fas fa-arrow-circle-right ml-sm-1"/>
          </router-link>
        </div>
      </v-server-table>
    </simple-card>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';

  const dictionary = {
    en: {
      attributes: {
        user: 'User',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'Users',
    components: { SimpleCard, SubPageHeader },
    data() {
      return {
        userId: '',
        data: [],
        columns: ['userId', 'totalPoints', 'lastUpdated', 'viewDetail'],
        options: {
          headings: {
            userId: 'User ID',
            totalPoints: 'Total Points',
            lastUpdated: 'Last Updated',
            viewDetail: '',
          },
          columnsClasses: {
            viewDetail: 'control-column',
          },
          sortable: ['userId', 'totalPoints', 'lastUpdated'],
          orderBy: {
            column: 'userId',
            ascending: true,
          },
          dateColumns: ['lastUpdated'],
          dateFormat: 'YYYY-MM-DD HH:mm',
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: true,
          highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
        },
      };
    },
    methods: {
      getUrl() {
        let url = `/admin/projects/${this.$route.params.projectId}`;
        if (this.$route.params.subjectId) {
          url += `/subjects/${this.$route.params.subjectId}`;
        } else if (this.$route.params.skillId) {
          url += `/skills/${this.$route.params.skillId}`;
        } else if (this.$route.params.badgeId) {
          url += `/badges/${this.$route.params.badgeId}`;
        }
        url += '/users';
        return url;
      },
      emit(name, event) {
        this.$emit(name, event, this);
      },
      clear() {
        this.$refs.table.data = [];
        this.$refs.table.count = 0;
      },
      getDate(props) {
        return window.moment(props.row.lastUpdated).format('LLL');
      },
    },
  };
</script>

<style>
  .usersTable .control-column{
    width: 8rem;
  }
  /* on the mobile platform some of the columns will be removed
   so let's allow the table to size on its own*/
  @media (max-width: 576px) {
    .usersTable .control-column {
      width: unset;
    }
  }
</style>
