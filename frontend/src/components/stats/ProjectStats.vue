<template>
  <div>
    <div class="columns">
      <div class="column is-full">
        <span class="title is-3">Stats</span>
      </div>
    </div>

    <div class="columns">
      <div class="column is-full">
        <system-usage-chart :project-id="projectId"></system-usage-chart>
      </div>
    </div>
    <div class="columns">
      <div class="column">
        <users-per-level-chart :project-id="projectId"></users-per-level-chart>
      </div>
      <div class="column">
        <achieved-skills-chart :project-id="projectId"></achieved-skills-chart>
      </div>
    </div>
    <div class="columns">
      <div class="column is-full">
        <available-stats></available-stats>
      </div>
    </div>
  </div>
</template>

<script>
  import SystemUsageChart from './SystemUsageChart';
  import AchievedSkillsChart from './AchievedSkillsChart';
  import UsersPerLevelChart from './UsersPerLevelChart';
  import AvailableStats from './AvailableStats';

  export default {
    name: 'ProjectStats',
    components: { AvailableStats, UsersPerLevelChart, AchievedSkillsChart, SystemUsageChart },
    props: ['projectId'],
  };
</script>

<style scoped>

</style>
