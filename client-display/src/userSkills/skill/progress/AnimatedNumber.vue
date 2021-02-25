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
  <span>{{ displayNumber | number }}</span>
</template>

<script>
  export default {
    name: 'AnimatedNumber',
    props: {
      num: Number,
    },
    data() {
      return {
        displayNumber: 0,
        interval: false,
      };
    },
    mounted() {
      this.doAnimate(this.num);
    },
    watch: {
      num(val) {
        this.doAnimate(val);
      },
    },
    methods: {
      doAnimate(val) {
        clearInterval(this.interval);

        if (this.num !== this.displayNumber) {
          this.interval = window.setInterval(() => {
            if (this.displayNumber !== val) {
              let change = (val - this.displayNumber) / 10;
              change = change >= 0 ? Math.ceil(change) : Math.floor(change);
              this.displayNumber += change;
            }
          }, 20);
        }
      },
    },
  };
</script>

<style scoped>

</style>
