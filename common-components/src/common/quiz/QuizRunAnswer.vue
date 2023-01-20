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
       :class="{ 'selected-answer': selected, 'point-cursor answer-row-editable' : !a.isGraded }"
       aria-label="Select as the correct answer">
      <div class="row no-gutters" :data-cy="`selected_${selected}`">
        <div class="col-auto">
          <b-overlay v-if="a.isGraded && a.selected !== a.isCorrect" show variant="transparent"
                     opacity="0">
            <template #overlay>
              <i v-if="a.selected" class="fa fa-ban text-danger" style="font-size: 1.5rem;" data-cy="wrongSelection"></i>
              <i v-else class="fa fa-check text-danger" style="font-size: 1rem;" data-cy="missedSelection"></i>
            </template>
            <span class="checkmark">
               <i :class="selectionIconObject" />
            </span>
          </b-overlay>
          <span v-else class="checkmark">
               <i :class="selectionIconObject" />
            </span>
        </div>
        <div class="col ml-2">
          <span class="answerText" data-cy="answerText">{{ a.answerOption }}</span>
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
      canSelectMoreThanOne: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        selected: this.a.selected ? this.a.selected : this.value,
      };
    },
    watch: {
      'a.selected': function handleSelected(newValue) {
        this.selected = newValue;
      },
    },
    computed: {
      selectionIconObject() {
        return {
          'text-success': this.selected,
          'far fa-square': this.canSelectMoreThanOne && !this.selected,
          'far fa-check-square': this.canSelectMoreThanOne && this.selected,
          'far fa-circle': !this.canSelectMoreThanOne && !this.selected,
          'far fa-check-circle': !this.canSelectMoreThanOne && this.selected,
        };
      },
    },
    methods: {
      flipSelected() {
        if (!this.a.isGraded) {
          this.selected = !this.selected;
          this.$emit('input', this.selected);
          this.$emit('selection-changed', {
            id: this.a.id,
            selected: this.selected,
          });
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

.answer-row-editable:hover {
  border: 1px dotted #007c49;
  border-radius: 5px;
}

.point-cursor {
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
