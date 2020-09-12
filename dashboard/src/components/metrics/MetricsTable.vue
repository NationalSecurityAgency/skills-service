<template>
  <div class="card mb-2">
    <div class="card-header">
      <h5>Achievements Navigator</h5>
    </div>
    <div class="card-body p-0">
      <div class="row p-3">
        <div class="col border-right">
          <b-form-group id="input-group-1" label="User Name Filter:" label-for="input-1" label-class="text-muted">
            <b-form-input id="input-1" v-model="usernameFilter" type="email"/>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group id="input-group-3" label="Level:" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="levels.selected" :options="levels.available" required/>
          </b-form-group>
        </div>
        <div class="col">
          <b-form-group id="badgesGroup" label="Badges:" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="badges.selected" :options="badges.available" required/>
          </b-form-group>
        </div>
      </div>
      <b-table striped :items="items" :busy="isLoading" :sort-by.sync="sortBy" :sort-desc.sync="sortDesc"
               :bordered="true" :outlined="true"
               :fields="fields"  head-variant="light">
        <template v-slot:cell(user_name)="data">
          <b-button-group>
            <b-button variant="outline-info" size="sm" class="text-secondary"><i class="fa fa-eye"/></b-button>
            <b-button variant="outline-info" size="sm" class="text-secondary"><i class="fa fa-chart-bar"/></b-button>
          </b-button-group>
          <span class="ml-2">{{ data.value }}</span>
        </template>
        <template v-slot:cell(achievement)="data">
        <span class="border border-info rounded d-inline-block bg-white" style="width: 2rem; text-align: center">
          <i class="fa fa-trophy text-muted" v-if="data.value.startsWith('Level')"/>
          <i class="fa fa-award text-muted" v-else/>
        </span>
          <span class="ml-2">{{ data.value }}</span>
        </template>
        <template v-slot:cell(timestamp)="data">
          <span class="">{{ data.value | date }}</span>
          <b-badge v-if="isToday(data.value)" variant="info" class="ml-2">Today</b-badge>
        </template>
        <template v-slot:table-busy>
          <div class="text-center text-info my-2">
            <b-spinner class="align-middle"></b-spinner>
            <p>
              <strong>Loading...</strong>
            </p>
          </div>
        </template>
      </b-table>
      <b-pagination v-model="pagination.currentPage" :total-rows="pagination.totalRows" :per-page="pagination.perPage"
                    pills align="center" size="sm" variant="info" class="customPagination">
      </b-pagination>
    </div>
  </div>
</template>

<script>
  import moment from 'moment';

  export default {
    name: 'MetricsTable',
    data() {
      return {
        usernameFilter: '',
        isLoading: false,
        badges: {
          selected: 'All Badges',
          available: ['All Badges', 'Remove Badges', 'Badge 1', 'Badge 2'],
        },
        levels: {
          selected: 'All Levels',
          available: ['All Levels', 'Level 1', 'Level 2', 'Level 3', 'Level 4', 'Level 5'],
        },
        pagination: {
          currentPage: 1,
          totalRows: 76,
          perPage: 5,
        },
        sortBy: 'timestamp',
        sortDesc: true,
        fields: [
          {
            key: 'user_name',
            sortable: true,
          },
          {
            key: 'achievement',
            sortable: true,
          },
          {
            key: 'timestamp',
            label: 'Date',
            sortable: true,
          },
        ],
        items: [
          {
            timestamp: 1599824550435,
            user_name: 'DickersonMacdonald@gmail.com',
            achievement: 'Level 1',
          },
          {
            timestamp: 1599824550435,
            user_name: 'dafeafeafe@gmail.com',
            achievement: 'Level 2',
          },
          {
            timestamp: 313442323,
            user_name: 'aefaefa@gmail.com',
            achievement: 'Level 1',
          },
          {
            timestamp: 1313131313,
            user_name: 'eafeafea@gmail.com',
            achievement: 'Some Badge',
          },
          {
            timestamp: 4422442242,
            user_name: 'eafaefafe@gmail.com',
            achievement: 'Level 1',
          },
        ],
      };
    },
    methods: {
      isToday(timestamp) {
        return moment(timestamp)
          .isSame(new Date(), 'day');
      },
    },
  };
</script>

<style lang="scss" scoped>
@import "~bootstrap/scss/bootstrap";

.customPagination /deep/ button {
  color: $info !important;
  border-color: $secondary !important;
}

.customPagination /deep/ .disabled > .page-link {
  border-color: $secondary !important;
}

.customPagination /deep/ .active > button {
  background-color: $info !important;
  color: $white !important;
}

</style>
