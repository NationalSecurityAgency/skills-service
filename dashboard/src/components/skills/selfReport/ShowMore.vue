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
  <div data-cy="showMoreText" class="text-break">
    <span>
      <span v-if="containsHtml" v-html="toDisplay">html RMM</span><span v-else data-cy="smtText">{{toDisplay}}</span>
      <b-link v-if="truncate" size="xs" variant="outline-info"
                class=""
                @click="displayFullText = !displayFullText"
                aria-label="Show/Hide truncated text"
                data-cy="showMoreOrLessBtn">
        <small v-if="displayFullText" data-cy="showLess"> &lt;&lt; less</small>
        <small v-else data-cy="showMore"><em>... &gt;&gt; more</em></small>
      </b-link>
    </span>
  </div>

</template>

<script>
  export default {
    name: 'ShowMore',
    props: {
      text: {
        type: String,
        required: true,
      },
      limit: {
        type: Number,
        required: false,
        default: 50,
      },
      containsHtml: {
        type: Boolean,
        required: false,
      },
    },
    data() {
      return {
        slop: 15,
        displayFullText: false,
      };
    },
    mounted() {
      this.displayFullText = this.text.length < this.limit + this.slop;
    },
    computed: {
      truncate() {
        return this.text.length >= this.limit + this.slop;
      },
      toDisplay() {
        if (this.displayFullText) {
          return `${this.text}`;
        }
        return `${this.text.substring(0, 50)}`;
      },
    },
  };
</script>

<style scoped>
  .btn-group-xs > .btn,
  .btn-xs {
    padding : .25rem .4rem;
    font-size : .875rem;
    line-height : .5;
    border-radius: .2rem;
  }
</style>
