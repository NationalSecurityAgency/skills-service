<template>
  <div class="usersTable">
    <sub-page-header title="Users"/>

      <simple-card style="min-height: 30rem;">
        <div>
          <h4 class="border-bottom text-center text-lg-left text-secondary">
            <i class="fa fa-users mr-2"/>
            <span class="">
            <template v-if="initialLoad">
              <b-spinner label="Loading..." style="width: 1rem; height: 1rem;" variant="info"/>
            </template>
            <template v-else>
              <strong>{{ totalNumUsers | number}}</strong>
            </template>
             Total Users</span>
          </h4>

          <v-server-table ref="table" :columns="columns" :url="getUrl()" :options="options"
                          class="vue-table-2"
                          @loaded="onLoaded" @loading="onLoading" v-on:error="emit('error', $event)">

            <server-table-loading-mask v-if="loading" slot="afterBody" />

            <div slot="userId" slot-scope="props" class="field has-addons">
              {{ getUserDisplay(props) }}
            </div>

            <div slot="lastUpdated" slot-scope="props" class="field has-addons">
              {{ getDate(props) }}
            </div>

            <div slot="viewDetail" slot-scope="props" class="">
              <router-link :to="{ name:'ClientDisplayPreview',
                      params: { projectId: $route.params.projectId, userId: props.row.userId, dn: props.row.dn }}"
                           tag="button" class="btn btn-outline-primary">
                <span class="d-none d-sm-inline">Details</span><i class="fas fa-arrow-circle-right ml-sm-1"/>
              </router-link>
            </div>
          </v-server-table>

        </div>
      </simple-card>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import ServerTableLoadingMask from '../utils/ServerTableLoadingMask';

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
    components: {
      SimpleCard, SubPageHeader, ServerTableLoadingMask,
    },
    data() {
      const self = this;
      return {
        loading: true,
        initialLoad: true,
        data: [],
        totalNumUsers: '...',
        columns: ['userId', 'totalPoints', 'lastUpdated', 'viewDetail'],
        options: {
          headings: {
            userId: 'User',
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
          /* eslint-disable */
          requestFunction: function (data) {
            return axios.get(this.url, {
              params: data
            }).then((res) => {
              self.loading = false;
              self.totalNumUsers = res.data.totalCount;
              return res;
            }).catch(function (e) {
              self.loading = false;
              self.totalNumUsers = 0;
              this.dispatch('error', e);
            }.bind(this));
          },
          /* eslint-enable */
        },
      };
    },
    mounted() {
    },
    methods: {
      onLoading() {
        this.loading = true;
      },
      onLoaded(event) {
        this.loading = false;
        this.initialLoad = false;
        this.$emit('loaded', event);
      },
      getUrl() {
        let url = `/admin/projects/${this.$route.params.projectId}`;
        if (this.$route.params.skillId) {
          url += `/skills/${this.$route.params.skillId}`;
        } else if (this.$route.params.badgeId) {
          url += `/badges/${this.$route.params.badgeId}`;
        } else if (this.$route.params.subjectId) {
          url += `/subjects/${this.$route.params.subjectId}`;
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
      getUserDisplay(props) {
        return props.row.lastName && props.row.firstName ? `${props.row.firstName} ${props.row.lastName} (${props.row.userIdForDisplay})` : props.row.userIdForDisplay;
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

  .usersTable table {
    width: 100%;
    position: relative;
  }
</style>
