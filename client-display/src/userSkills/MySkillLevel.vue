/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
    <div>
        <label class="skill-tile-label">My Level</label>

        <span slot="reference" class="fa-stack skills-icon trophy-stack">
            <i class="fa fa-trophy fa-stack-2x"/>
            <i class="fa fa-star fa-stack-1x trophy-star"/>
            <strong class="fa-stack-1x trophy-text">{{ skillLevel }}</strong>
        </span>

        <div>
            Level <strong>{{ skillLevel }}</strong> out of <strong>{{ totalNumLevels }}</strong>
        </div>

        <star-progress v-if="totalNumLevels <= 6" :number-complete="skillLevel" :total-num-levels="totalNumLevels" star-style="circle"/>
        <div v-else class="row justify-content-center">
            <div class="col-6 col-md-9 col-lg-7">
                <progress-bar class="mt-2 mx-3 text-center" bar-color="rgb(89, 173, 82)" size="large" :val="progressPercent"></progress-bar>
            </div>
        </div>
    </div>
</template>

<script>
    import ProgressBar from 'vue-simple-progress';
    import StarProgress from '@/common/progress/StarProgress.vue';

    export default {
        components: {
            StarProgress,
            ProgressBar,
        },
        props: {
            skillLevel: Number,
            totalNumLevels: {
                type: Number,
                default: 5,
            },
        },
        computed: {
            progressPercent() {
                return Math.floor((this.skillLevel / this.totalNumLevels) * 100);
            },
        },
    };
</script>

<style scoped>
    .skill-tile-label {
        font-size: 1.2rem;
        width: 100%;
    }

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
