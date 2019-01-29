<template>
  <div class="skills-bordered-component">
    <div class="columns">
      <div class="column is-full">
        <span class="title is-5">Available Stats</span>
      </div>
    </div>

    <div class="columns is-multiline">
      <div class="column is-one-third">
        <stat-card title="Top Achieved" subtitle="The Olympians of Skills" icon="fa fa-trophy"
                   description="This stat depicts most achieved skills. It will display top skills and number of users that achieved those skills."></stat-card>
      </div>
      <div class="column is-one-third">
        <stat-card title="Overlooked skills" subtitle="Users seem to ignore these" icon="fas fa-sad-tear"
                   description="These skills have low to no usage at all. Explore these skills and see if they need to be improved or removed."></stat-card>
      </div>
      <div class="column is-one-third">
        <stat-card title="Time to Achieve" subtitle="How long did it take them?" icon="fa fa-calendar-alt"
                   description="Provides a breakdown of how long it takes users to achieve each level."></stat-card>
      </div>
      <div class="column is-one-third">
        <stat-card title="Continued Usage" subtitle="Do they stick around?" icon="fa fa-suitcase"
                   description="Once a particular skill is achieve, do users continue using that skill?"></stat-card>
      </div>
      <div class="column is-one-third">
        <stat-card title="Badges" subtitle="Number of users per badge" icon="fa fa-award"
        description="Breakdown of number of users per badge."></stat-card>
      </div>
    </div>


  </div>
</template>

<script>
  import StatCard from './StatCard';

  export default {
    name: 'AvailableStats',
    components: { StatCard },
  };
</script>

<style scoped>

</style>
