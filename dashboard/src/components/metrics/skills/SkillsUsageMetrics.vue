<template>
  <div class="card">
    <div class="card-header">
      <h5>Skills Usage Navigator</h5>
    </div>
    <div class="card-body mx-0 px-0">

      <div class="row p-3">
        <div class="col border-right">
          <b-form-group id="input-group-1" label="From Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="example-datepicker" v-model="value" class="mb-2"></b-form-datepicker>
          </b-form-group>
        </div>
        <div class="col border-right">
          <b-form-group label="To Date:" label-for="input-1" label-class="text-muted">
            <b-form-datepicker id="example-datepicker" v-model="value" class="mb-2"></b-form-datepicker>
          </b-form-group>

        </div>
        <div class="col">
          <b-form-group id="input-group-3" label="Only Users With Min Level Achievement:" label-for="input-3" label-class="text-muted">
            <b-form-select id="input-3" v-model="levels.selected" :options="levels.available" required/>
          </b-form-group>
        </div>
      </div>

      <skills-b-table :items="items" :options="tableOptions">
        <template v-slot:cell(skill)="data">
          <b-button size="sm" @click="data.toggleDetails" class="mr-2">
            <i v-if="data.detailsShowing" class="fa fa-minus-square" />
            <i v-else class="fa fa-plus-square" />
          </b-button>
          <span class="ml-2">{{ data.value }}</span>

          <b-button-group class="float-right">
            <b-button :to="{ name: 'ClientDisplayPreview', params: { projectId: projectId, userId: data.value } }"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View Skill's Configuration"><i class="fa fa-wrench"/></b-button>
            <b-button variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover title="View User's Metrics"><i class="fa fa-chart-bar"/></b-button>
          </b-button-group>
        </template>

        <template v-slot:cell(num_users_achieved_skill)="data">
          <span class="ml-2">{{ data.value }}</span>
          <b-badge v-if="data.value == 0" variant="danger" class="ml-2">Overlooked Skill</b-badge>
          <b-badge v-if="data.value > 100" variant="info" class="ml-2">Top Skill</b-badge>
        </template>

        <template v-slot:cell(num_users_started_but_not_achieved)="data">
          <span class="ml-2">{{ data.value }}</span>
          <b-badge v-if="data.value > 600" variant="success" class="ml-2">High Activity</b-badge>
          <!--          <span v-if="data.value > 100" class="border border-info rounded d-inline-block bg-white" style="width: 2rem; text-align: center">-->
          <!--            <i class="fa fa-trophy text-muted"/>-->
          <!--          </span>-->
        </template>

        <template v-slot:cell(last_skill_achieved)="data">
          <b-badge v-if="!data.value" variant="warning" class="ml-2">Never</b-badge>
          <div v-else>
            <div>
              <span>{{ relativeTime(data.value) }}</span>
            </div>
            <div class="text-muted" style="font-size: 0.8rem;">
              {{ data.value | date }}
            </div>
          </div>
        </template>

        <template v-slot:cell(last_event_applied)="data">
          <b-badge v-if="!data.value" variant="warning" class="ml-2">Never</b-badge>
          <div v-else>
            <div>
              <span>{{ relativeTime(data.value) }}</span>
            </div>
            <div class="text-muted" style="font-size: 0.8rem;">
              {{ data.value | date }}
            </div>
          </div>
        </template>

        <template v-slot:row-details="row">
          <b-card>
            {{ row.item }}
          </b-card>
        </template>

      </skills-b-table>
    </div>
  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import moment from 'moment';

  export default {
    name: 'SkillsUsageMetrics',
    components: { SkillsBTable },
    data() {
      return {
        projectId: this.$route.params.projectId,
        levels: {
          selected: null,
          available: ['Level 1', 'Level 2', 'Level 3', 'Level 4', 'Level 5'],
        },
        tableOptions: {
          busy: false,
          sortBy: 'timestamp',
          sortDesc: true,
          fields: [
            {
              key: 'skill',
              sortable: true,
            },
            {
              key: 'num_users_achieved_skill',
              sortable: true,
              label: '# Users Achieved',
            },
            {
              key: 'num_users_started_but_not_achieved',
              sortable: true,
              label: '# Users In Progress',
            },
            {
              key: 'last_event_applied',
              sortable: true,
              label: 'Last Applied',
            },
            {
              key: 'last_skill_achieved',
              sortable: true,
              label: 'Last Achieved',
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: 76,
            perPage: 5,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
        },
        items: [
          {
            skill: 'How to drive',
            last_event_applied: 1599824550435,
            last_skill_achieved: 1599824550435,
            num_users_achieved_skill: 520,
            num_users_started_but_not_achieved: 40,
            num_events_applied: 344,
          },
          {
            skill: 'How to drive',
            last_event_applied: 1599824550435,
            last_skill_achieved: 1599824550435,
            num_users_achieved_skill: 150,
            num_users_started_but_not_achieved: 40,
            num_events_applied: 344,
          },
          {
            skill: 'How to own a boat',
            last_event_applied: 1599824550435,
            last_skill_achieved: null,
            num_users_achieved_skill: 0,
            num_users_started_but_not_achieved: 2,
            num_events_applied: 0,
          },
          {
            skill: 'How to say NO to everything',
            last_event_applied: null,
            last_skill_achieved: null,
            num_users_achieved_skill: 0,
            num_users_started_but_not_achieved: 0,
            num_events_applied: 0,
          },
          {
            skill: 'How to fight',
            last_event_applied: 1600113998292,
            last_skill_achieved: 1600113963711,
            num_users_achieved_skill: 40,
            num_users_started_but_not_achieved: 625,
            num_events_applied: 344,
          },
        ],
      };
    },
    methods: {
      relativeTime(timestamp) {
        return moment(timestamp)
          .startOf('hour')
          .fromNow();
      },
    },
  };
</script>

<style scoped>

</style>
