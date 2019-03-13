<template>
  <div>
    <label class="skill-tile-label level-label">Skill Level</label>
    <popper
      trigger="hover"
      :append-to-body="true">
        <span
          slot="reference"
          class="fa-stack skills-icon trophy-stack">
            <i class="fa fa-trophy fa-stack-2x"/>
            <i class="fa fa-star fa-stack-1x trophy-star"/>
            <strong class="fa-stack-1x trophy-text">{{ skillLevel }}</strong>
        </span>
      <div class="popper">
        <div>Level {{ skillLevel }} out of 5</div>
      </div>
    </popper>

    <h2 class="skill-tile-label">Level {{ skillLevel }}</h2>

    <star-progress
      :number-complete="skillLevel"
      star-style="circle" />
  </div>
</template>

<script>
  import StarProgress from '@/common/progress/StarProgress.vue';

  import Popper from 'vue-popperjs';

  export default {
    components: {
      StarProgress,
      Popper,
    },
    props: {
      skillLevel: Number,
    },
  };
</script>

<style scoped>
  /* Font awesome gives a 2em width which doesnt fit the full trophy at font-size:60px. A bug? */
  .trophy-stack.fa-stack {
    width: 3em;
    font-size: 60px;
  }

  .trophy-text {
    margin-top: -0.65em;
    font-size: 0.5em;
    color: #333;
  }

  .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .trophy-star {
    color: #ffffff;
    margin-top: -0.35em;
    font-size: 0.9em;
  }

  .trophy-text {
    margin-top: -0.65em;
    font-size: 0.5em;
    color: #333;
  }
</style>
