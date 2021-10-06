<template>
<div>
  <b-card body-class="p-0" class="ml-4 mb-3" style="background-color: rgba(0,124,73,0.04)">
    <div class="row px-3 my-2" style="height: 3rem;">
      <div class="col">

        <div class="row align-items-center">
          <div class="col-auto border-right">
            <div v-if="enabled">
              <span class="text-secondary">Status: </span> <span class="text-uppercase"><b-badge variant="success">Live <span class="far fa-check-circle" aria-hidden="true"/></b-badge></span>
            </div>
            <div v-if="!enabled" data-cy="skillGroupStatus" style="">
              <span class="text-secondary">Status: </span>
              <span class="text-uppercase mr-1"><b-badge variant="warning">Disabled</b-badge></span> <b-button variant="outline-info" size="sm"
                                                                                             data-cy="selectPageOfApprovalsBtn"><i class="fas fa-glass-cheers"></i> Go Live</b-button>
            </div>
          </div>
          <div class="col">
            <b-form inline>
              <span class="mr-1 text-secondary">Required: </span>
              <b-form-select size="sm" v-model="numSkillsRequired.selected" :options="numSkillsRequired.options" />
              <span class="ml-1">out <b-badge>2</b-badge> skills</span>

              <b-button variant="outline-info" size="sm"
                        data-cy="selectPageOfApprovalsBtn" class="ml-2"><i class="fas fa-sync"></i> Sync Points</b-button>
            </b-form>

          </div>
        </div>
      </div>
      <div class="col text-right">
        <b-button id="newProjectBtn" ref="newProjButton" variant="outline-info" size="sm"
                data-cy="newProjectButton" class="ml-1">
          <span class="">Add Skill to Group</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
        </b-button>
      </div>
    </div>
    <skills-table :table-id="`groupSkills_${parentSkillId}`"
      :skills-prop="skills" :is-top-level="true"
                  :project-id="this.$route.params.projectId"
                  :subject-id="this.$route.params.subjectId"
      :show-search="false" :show-header="false" :show-paging="false"/>
  </b-card>
</div>
</template>

<script>
  export default {
    name: 'ChildRowSkillGroupDisplay',
    components: {
      SkillsTable: () => import('./SkillsTable'),
    },
    props: {
      parentSkillId: String,
      enabled: Boolean,
    },
    data() {
      return {
        isLoading: false,
        numSkillsRequired: {
          options: [1, 2],
          selected: 2,
        },
        skills: [
          {
            created: '2021-10-04T13:41:19.444+00:00',
            displayOrder: 1,
            name: 'Very Great Skill 1',
            numMaxOccurrencesIncrementInterval: 1,
            numPerformToCompletion: 2,
            numUsers: 0,
            pointIncrement: 100,
            pointIncrementInterval: 480,
            projectId: 'proj1',
            selfReportingType: 'Approval',
            skillId: 'skill1',
            totalPoints: 200,
            type: 'Skill',
            updated: '2021-10-04T13:41:19.444+00:00',
            version: 0,
          },
          {
            created: '2021-10-04T13:41:19.444+00:00',
            displayOrder: 1,
            name: 'Very Great Skill 1',
            numMaxOccurrencesIncrementInterval: 1,
            numPerformToCompletion: 2,
            numUsers: 0,
            pointIncrement: 100,
            pointIncrementInterval: 480,
            projectId: 'proj1',
            selfReportingType: 'Approval',
            skillId: 'skill1',
            totalPoints: 200,
            type: 'Skill',
            updated: '2021-10-04T13:41:19.444+00:00',
            version: 0,
          },
        ],
      };
    },
  };
</script>

<style scoped>

</style>
