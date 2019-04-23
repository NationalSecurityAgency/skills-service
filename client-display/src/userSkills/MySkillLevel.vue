<template>
    <div>
        <label class="skill-tile-label level-label">My Level</label>

        <span slot="reference" class="fa-stack skills-icon trophy-stack">
            <i class="fa fa-trophy fa-stack-2x"/>
            <i class="fa fa-star fa-stack-1x trophy-star"/>
            <strong class="fa-stack-1x trophy-text">{{ skillLevel }}</strong>
        </span>

        <div class="level-subtitle">
            Level <strong>{{ skillLevel }}</strong> out of <strong>{{ totalNumLevels }}</strong>
        </div>

        <star-progress :number-complete="skillLevel" star-style="circle"/>
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
            totalNumLevels: {
                type: Number,
                default: 5,
            },
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

  .level-subtitle {

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
