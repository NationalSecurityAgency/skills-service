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
    <div class="card skills-card-theme-border" data-cy="depsProgress">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">Progress</h6>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-8 text-left">
                    <strong data-cy="numDeps">{{ numDependencies }}</strong> Dependencies
                </div>
                <div class="col-4">
                    <span class="text-muted" data-cy="depsPercentComplete">{{ percentComplete }}%</span>
                </div>
            </div>
            <progress-bar bar-color="lightgreen" :val="percentComplete"></progress-bar>
        </div>

    </div>
</template>

<script>
  import ProgressBar from 'vue-simple-progress';

  export default {
    components: {
      ProgressBar,
    },
    name: 'SkillDependencySummary',
    props: {
      dependencies: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        numDependencies: 0,
        percentComplete: 0,
      };
    },
    mounted() {
      this.numDependencies = this.dependencies.length;
      const numCompleted = this.dependencies.filter((item) => item.achieved).length;
      if (this.numDependencies > 0 && numCompleted > 0) {
        this.percentComplete = Math.floor((numCompleted / this.numDependencies) * 100);
      }
    },
  };
</script>

<style scoped>
</style>
