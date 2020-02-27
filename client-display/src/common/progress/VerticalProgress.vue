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
  <div class="user-skill-progress-layers" :class="{ 'cursor-pointer': isClickable, 'locked-background': isLocked }">
    <div class="locked-border"></div>
    <span v-if="isLocked" class="locked-icon">
      <i class="fas fa-lock"></i>
    </span>
    <progress-bar
      v-if="totalProgress === 100"
      :val="totalProgress"
      :size="barSize"
      :bar-color="completeColor"
      class="complete-total"/>
    <progress-bar
      v-if="totalProgress !== 100"
      :val="totalProgress"
      :size="barSize"
      :bar-color="earnedTodayColor"
      :bg-color="incompleteColor"
      class="complete-total"/>
    <progress-bar
      v-if="totalProgress !== 100"
      :val="totalProgressBeforeToday"
      :size="barSize"
      :bar-color="beforeTodayColor"
      bg-color="transparent"
      class="complete-before-today" />
  </div>
</template>

<script>
  import ProgressBar from 'vue-simple-progress';

  export default {
    components: {
      ProgressBar,
    },
    props: {
      isLocked: Boolean,
      isClickable: Boolean,
      totalProgress: Number,
      totalProgressBeforeToday: Number,
      beforeTodayBarColor: {
        type: String,
        default: '#337ab7',
      },
      totalProgressBarColor: {
        type: String,
        default: '#97c9f5',
      },
      barSize: {
        type: Number,
        default: 22,
      },
    },
    computed: {
      completeColor() {
        return this.$store.state.themeModule.progressIndicators.completeColor;
      },
      incompleteColor() {
        let res = this.$store.state.themeModule.progressIndicators.incompleteColor;
        if (!res) {
          res = this.isLocked ? 'E6E6E6' : 'aeaeae';
        }
        return res;
      },
      beforeTodayColor() {
        let res = this.$store.state.themeModule.progressIndicators.beforeTodayColor;
        if (!res) {
          res = this.beforeTodayBarColor;
        }
        return res;
      },
      earnedTodayColor() {
        let res = this.$store.state.themeModule.progressIndicators.earnedTodayColor;
        if (!res) {
          res = this.beforeTodayBarColor;
        }
        return res;
      },
    },
  };
</script>

<style scoped>
  .complete-before-today {
    position: absolute !important;
    top: 0;
    left: 0;
    width: 100%;
  }

  .user-skill-progress-layers {
    position: relative;
    background-color: #e8e8e8;
  }

  .locked-background {
    background-color: #e8e8e8;
  }

  .locked-border {
    position: absolute;
    width: 100%;
    height: 100%;
    border: lightgrey solid 2px;
    z-index: 10000;
  }

  .locked-icon {
    position: absolute;
    /*top: -5px;*/
    left: 50%;
    color: #5d5d5d;
  }

  .cursor-pointer {
    cursor: pointer;
  }

</style>
