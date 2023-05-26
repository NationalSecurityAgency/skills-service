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
    <div class="graph-legend text-primary" data-cy="depsProgress">
      <div class="row">
          <div class="col-8 text-left">
            <b-badge data-cy="numDeps" variant="info"><span style="font-size: 0.9rem">{{ numDependencies }}</span></b-badge> Prerequisites
          </div>
          <div class="col-4">
              <span class="text-muted" data-cy="depsPercentComplete">{{ percentComplete }}%</span>
          </div>
      </div>
      <progress-bar bar-color="lightgreen" :size="5" :val="percentComplete" class="mt-1" />
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
      this.numDependencies = 0;
      let numCompleted = 0;
      const alreadyCountedIds = [];
      this.dependencies.forEach((dependency) => {
        const { dependsOn } = dependency;
        if (dependsOn) {
          const lookup = `${dependsOn.projectId}-${dependsOn.skillId}`;
          if (!alreadyCountedIds.includes(lookup)) {
            this.numDependencies += 1;
            if (dependency.achieved) {
              numCompleted += 1;
            }
            alreadyCountedIds.push(lookup);
          }
        }
      });
      if (this.numDependencies > 0 && numCompleted > 0) {
        this.percentComplete = Math.floor((numCompleted / this.numDependencies) * 100);
      }
    },
  };
</script>

<style scoped>
.card {
  border: none;
  font-size: 0.95rem;
}

</style>
