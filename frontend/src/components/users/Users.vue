<template>
  <div>
    <div class="columns">
      <div class="column is-full">
        <span class="title is-3">Users</span>
      </div>
    </div>

    <v-server-table ref="table" :columns="columns" :url="getUrl()" :options="options"
                    v-on:loaded="emit('loaded', $event)" v-on:error="emit('error', $event)">
      <div slot="lastUpdated" slot-scope="props" class="field has-addons">
        {{ getDate(props) }}
      </div>

      <div slot="viewDetail" slot-scope="props" class="">
        <router-link :to="{ name:'UserPage',
                params: { projectId: projectId, userId: props.row.userId, totalPoints: props.row.totalPoints }}"
                     class="button is-outlined is-info">
          <span>View Details</span>
          <span class="icon is-small">
                    <i class="fas fa-edit"/>
                  </span>
        </router-link>
      </div>
    </v-server-table>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import '@skills/user-skills/dist/userSkills.css';
  import BLoading from 'buefy/src/components/loading/Loading';
  import BTabs from 'buefy/src/components/tabs/Tabs';
  import BTabItem from 'buefy/src/components/tabs/TabItem';
  import NoContent from '../utils/NoContent';
  import LoadingContainer from '../utils/LoadingContainer';

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
    components: { BTabItem, BTabs, BLoading, NoContent, LoadingContainer },
    props: ['projectId', 'subjectId', 'skillId', 'badgeId'],
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
        let url = `/admin/projects/${this.projectId}`;
        if (this.subjectId) {
          url += `/subjects/${this.subjectId}`;
        } else if (this.skillId) {
          url += `/skills/${this.skillId}`;
        } else if (this.badgeId) {
          url += `/badges/${this.badgeId}`;
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

<style scoped>
  .control-column{
    width: 4rem;
  }
</style>

