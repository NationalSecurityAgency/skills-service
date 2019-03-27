<template>
  <div class="subject-skill-row-container">
    <skill-dependency-graph v-if="showSkillDependencyGraph" :skill="skill"
                            @ok="showSkillDependencyGraph=false"></skill-dependency-graph>

    <div class="skill-label-container col-xs-4">
      <span class="skill-label">
        <span class="skill-name">
          {{ skill.skill }}
        </span>
      </span>
    </div>
    <div class="col-xs-8 skill-progress-cell">
      <user-skill-progress :skill="skill" :show-description="showDescription" v-on:progressbar-clicked="skillRowClicked"></user-skill-progress>
    </div>
  </div>
</template>

<script>
  import SkillDependencyGraph from '@/userSkills/subject/SkillDependencyGraph.vue';
  import UserSkillProgress from '@/userSkills/modal/UserSkillProgress.vue';

  export default {
    name: 'UserSkillsSubjectSkillRow',
    components: {
      UserSkillProgress,
      SkillDependencyGraph,
    },
    props: {
      skill: Object,
      showDescription: Boolean,
    },
    data() {
      return {
        showSkillDependencyGraph: false,
      };
    },
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
    },
    methods: {
      skillRowClicked() {
        // only respond to events if the row is locked and we need to display dependency component
        if (this.locked) {
           this.showSkillDependencyGraph = true;
        }
      },
    },
  };
</script>

<style scoped>
  .subject-skill-row-container .skill-progress-cell {
    padding-left: 0px;
    padding-right: 0px;
  }
  .skill-label {
    line-height: 1;
    width: 100%;
    text-align: right;
  }

  .subject-skill-row-container .skill-label-container {
    padding-right: 0px;
  }

  .skill-label .skill-name {
    vertical-align: middle;
    font-weight: bold;
    display: inline-block;
    max-width: 85%;
    min-width: 85%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
