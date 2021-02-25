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

  </div>
</template>

<script>
  export default {
    name: 'TimeWindowMixin',
    methods: {
      timeWindowHasLength(skill) {
        return skill.timeWindowEnabled && skill.numPerformToCompletion > 1;
      },
      timeWindowTitle(skill) {
        let title = '';
        if (!skill.timeWindowEnabled) {
          title = 'Time Window Disabled';
        } else if (skill.numPerformToCompletion === 1) {
          title = 'Time Window N/A';
        } else {
          title = `${skill.pointIncrementIntervalHrs} Hour`;
          if (skill.pointIncrementIntervalHrs === 0 || skill.pointIncrementIntervalHrs > 1) {
            title = `${title}s`;
          }
          if (skill.pointIncrementIntervalMins > 0) {
            title = `${title} ${skill.pointIncrementIntervalMins} Minute`;
            if (skill.pointIncrementIntervalMins > 1) {
              title = `${title}s`;
            }
          }
        }
        return title;
      },
      timeWindowDescription(skill) {
        const numOccur = skill.numPointIncrementMaxOccurrences;
        let desc = 'Minimum Time Window between occurrences to receive points';
        if (!skill.timeWindowEnabled) {
          desc = 'Each occurrence will receive points immediately';
        } else if (numOccur > 1) {
          desc = `Up to ${numOccur} occurrences within this time window to receive points`;
        } else if (skill.numPerformToCompletion === 1) {
          desc = 'Only one event is required to complete this skill.';
        }
        return desc;
      },
    },
  };
</script>

<style scoped>

</style>
