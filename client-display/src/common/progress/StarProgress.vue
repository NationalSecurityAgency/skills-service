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
  <div class="star-container">
    <div
      v-for="index in totalNumLevels"
      :key="`unique-star-${index}`"
      :class="[ (index <= numberComplete) ? 'star-filled' : 'star-empty', starStyle === 'circle' ? 'circle-star-wrapper' : 'star-wrapper' ]">
        <span
          v-if="starStyle === 'circle'"
          class="fa-stack star-stack">
          <i class="far fa-circle fa-stack-2x"/>
          <i class="fa fa-star fa-stack-1x"/>
        </span>
      <span
        v-else
        class="fa fa-star"/>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      numberComplete: {
        type: Number,
        required: true,
      },
      totalNumLevels: {
        type: Number,
        default: 5,
      },
      starStyle: {
        type: String,
        default: '',
        validator(value) {
          return ['', 'circle'].includes(value);
        },
      },
    },
  };
</script>

<style scoped>
  .star-container {
    font-size: 1.5rem;
  }

  .star-stack {
    font-size: 1rem;
  }

  .circle-star-wrapper {
    display: inline-block;
  }

  .star-wrapper {
    display: inline-block;
    margin: 0 3px;
  }

  .star-wrapper {
    display: inline-block;
    margin: 0 3px;
  }

  .star-empty {
    color: #eaeaea;
  }

  .star-filled {
    color: gold;
  }
</style>
