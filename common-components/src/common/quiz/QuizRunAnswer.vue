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
  <div class="answer-row"
       v-on:keydown.space="flipSelected"
       @click="flipSelected"
       tabindex="0"
       :class="{ 'selected-answer': selected }"
       aria-label="Select as the correct answer">
      <div class="row no-gutters">
        <div class="col-auto">
          <span class="checkmark">
            <i :class="{
              'text-success' : selected,
              'far fa-square' : canSelectMoreThanOne && !selected,
              'far fa-check-square' : canSelectMoreThanOne && selected,
              'far fa-circle' : !canSelectMoreThanOne && !selected,
              'far fa-check-circle' : !canSelectMoreThanOne && selected,
            }" />
          </span>
        </div>
        <div class="col ml-2">
          <span class="answerText">{{ a.answerOption }}</span>
        </div>
      </div>
    </div>
</template>

<script>

  export default {
    name: 'QuizRunAnswer',
    props: {
      a: Object,
      value: Boolean,
      readOnly: {
        type: Boolean,
        default: false,
      },
      canSelectMoreThanOne: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        selected: this.value,
      };
    },
    watch: {
      'a.selected': function handleSelected(newValue) {
        this.selected = newValue;
      },
    },
    methods: {
      flipSelected() {
        if (!this.readOnly) {
          this.selected = !this.selected;
          this.$emit('input', this.selected);
          this.$emit('selection-changed', this.a.id);
        }
      },
    },
  };
</script>

<style scoped>
.answer-row {
  padding: 0.2rem 1rem 0rem 1rem  !important;
  margin-bottom: 0.1rem;
  border: 1px dotted transparent;
}

.answer-row:hover {
  border: 1px dotted #007c49;
  border-radius: 5px;
  cursor: pointer;
}

i {
  color: #b6b5b5;
}
.checkmark {
  font-size: 1.2rem;
}

.selected-answer {
  background-color: lightgray;
  border: 1px dotted #007c49;
  border-radius: 5px;
  font-weight: bold;
}

.answerText {
  font-size: 0.8rem;
}
</style>
