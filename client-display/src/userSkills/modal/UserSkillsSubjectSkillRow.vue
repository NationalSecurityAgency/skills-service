<template>
  <div class="subject-skill-row-container">
    <skill-dependency-graph v-if="showSkillDependencyGraph" :skill="skill"
                            @ok="showSkillDependencyGraph=false"></skill-dependency-graph>

    <div class="skill-label-container col-xs-3">
      <span
        class="skill-label">
        <span
          class="skill-name">
          {{ skill.skill }}
        </span>
      </span>
    </div>
    <div
      class="col-xs-7 skill-progress-cell">
      <popper
        trigger="hover"
        :options="{ placement: 'top' }">
        <div slot="reference" v-on:click="skillRowClicked">
          <vertical-progress
            v-if="progress.total === 100"
            total-progress-bar-color="#59ad52"
            before-today-bar-color="#59ad52"
            :total-progress="progress.total"
            :total-progress-before-today="progress.totalBeforeToday"
          />
          <vertical-progress
            v-if="skill.points !== skill.totalPoints && progress.total !== 100"
            :total-progress="progress.total"
            :total-progress-before-today="progress.totalBeforeToday"
            :is-locked="locked"
          />
        </div>
        <div class="popper">
          <my-progress-summary v-if="!locked"
            :user-skills="skill"
            summary-type="skill" />
          <div v-else>
            <skill-is-locked-message :user-skill="skill"></skill-is-locked-message>
          </div>
        </div>
      </popper>
      <div
        v-if="showDescription && skill.description"
        class="user-skill-subject-description text-muted">
        <p
          v-if="skill.description.description"
          v-html="parseMarkdown(skill.description.description)"/>
        <ul v-if="skill.description.examples">
          Examples:
          <li
            v-for="(example, index) in skill.description.examples"
            :key="`unique-example-${index}`"
            v-html="example"/>
        </ul>
        <div
          v-if="skill.description.href"
          class="user-skill-description-href">
          <strong>Need help?</strong>
          <a
            :href="skill.description.href"
            target="_blank">
            Click here!
          </a>
        </div>
      </div>
    </div>
    <div
      class="col-xs-2">
      <div class="col-xs-9">
        <popper
          trigger="hover"
          :options="{ placement: 'left' }">
          <div
            slot="reference"
            class="skill-label text-left">{{ skill.points | number }} / {{ skill.totalPoints |
            number }}
          </div>
          <div class="popper">
            <div>{{ skill.pointIncrement | number }} points maximum earned per day
            </div>
          </div>
        </popper>
      </div>
      <div
        v-if="skill.points === skill.totalPoints"
        class="col-xs-3">
        <popper
          trigger="hover"
          :options="{ placement: 'left' }">
          <div
            slot="reference"
            class="fa fa-check item-complete-icon"/>
          <div class="popper">
            <div>Skill complete</div>
          </div>
        </popper>
      </div>
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import VerticalProgress from '@/common/progress/VerticalProgress.vue';
  import SkillIsLockedMessage from '@/userSkills/SkillIsLockedMessage.vue';
  import SkillDependencyGraph from '@/userSkills/subject/SkillDependencyGraph.vue';

  import ProgressBar from 'vue-simple-progress';
  import Popper from 'vue-popperjs';
  import marked from 'marked';

  import 'vue-popperjs/dist/css/vue-popper.css';

  export default {
    name: 'UserSkillsSubjectSkillRow',
    components: {
      SkillDependencyGraph,
      SkillIsLockedMessage,
      MyProgressSummary,
      VerticalProgress,
      ProgressBar,
      Popper,
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
      progress() {
        return {
          total: (this.skill.points / this.skill.totalPoints) * 100,
          totalBeforeToday: ((this.skill.points - this.skill.todaysPoints) / this.skill.totalPoints) * 100,
        };
      },
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
    },
    methods: {
      parseMarkdown(markdown) {
        return marked(markdown);
      },
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
  .user-skill-subject-description {
    text-align: left;
    font-style: italic;
    padding: 10px;
  }

  .subject-skill-row-container .skill-progress-cell {
    padding-left: 0px;
    padding-right: 0px;
  }

  .expand-icon {
    vertical-align: bottom;
    margin-left: 10px;
    width: 5%;
    cursor: pointer;
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

  .child-skill-row-container {
    padding-top: 5px;
    text-align: left;
  }

  .child-skill-row-container .skill-label {
    text-align: left;
  }

  .child-skill-row-container .skill-label-container {
    padding: 0px;
  }

  .user-skill-description-href {
    margin-top: 8.5px;
  }
</style>
