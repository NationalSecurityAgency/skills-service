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
  <b-progress :max="max" :height="height" :variant="variant" :animated="animated">
    <b-progress-bar :value="current" :aria-label="`${name} Progress`"></b-progress-bar>
  </b-progress>
</template>

<script>
  export default {
    name: 'LengthyOperationProgressBar',
    props: {
      name: String,
      timeout: {
        type: Number,
        default: 600,
      },
      increment: {
        type: Number,
        default: 4,
      },
      variant: {
        type: String,
        default: 'success',
      },
      height: {
        type: String,
        default: '6px',
      },
      animated: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        max: 100,
        current: 5,
        timer: null,
      };
    },
    mounted() {
      this.timer = setInterval(() => {
        if (this.current >= this.max) {
          this.current = this.increment;
        } else {
          this.current += this.increment;
        }
      }, this.timeout);
    },
    beforeDestroy() {
      clearInterval(this.timer);
      this.timer = null;
    },
  };
</script>

<style scoped>

</style>
