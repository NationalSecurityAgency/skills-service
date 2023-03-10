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
  <div v-if="answersInternal && answersInternal.length > 0">
    <div v-for="(answer, index) in answersInternal" :key="index">
      <div class="row no-gutters mt-2" :data-cy="`answer-${index}`">
        <div class="col-auto">
          <select-correct-answer v-if="isQuizType"
                                 :id="`answer=${index}`"
                                 :answer-number="index+1"
                                 v-model="answer.isCorrect"
                                 class="mr-2"/>
        </div>
        <div class="col">
          <input class="form-control" type="text" v-model="answer.answer"
                 placeholder="Enter an answer"
                 data-cy="answerText"
                 :id="`answer${index}TextInput`"
                 :aria-label="`Enter answer number ${index+1}`"
                 aria-errormessage="testNameError"
                 aria-describedby="testNameError"/>
        </div>
        <b-button-group class="ml-2">
          <b-button variant="outline-info"
                    :disabled="noMoreAnswers"
                    :aria-label="`Add New Answer at index ${index}`"
                    data-cy="addNewAnswer"
                    @click="addNewAnswer(index)">
            <i class="fas fa-plus"></i>
          </b-button>
          <b-button variant="outline-info"
                    :disabled="twoOrLessQuestions"
                    :aria-label="`Delete Answer at index ${index}`"
                    data-cy="removeAnswer"
                    @click="removeAnswer(index)">
            <i class="fas fa-minus"></i>
          </b-button>
        </b-button-group>
      </div>
    </div>
  </div>
</template>

<script>
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';

  export default {
    name: 'ConfigureAnswers',
    components: { SelectCorrectAnswer },
    props: {
      value: Array,
      quizType: String,
    },
    data() {
      return {
        answersInternal: this.value.map((a) => ({ ...a })),
      };
    },
    computed: {
      isQuizType() {
        return this.quizType === 'Quiz';
      },
      maxAnswersAllowed() {
        return this.$store.getters.config.maxAnswersPerQuizQuestion;
      },
      noMoreAnswers() {
        return this.answersInternal && this.answersInternal?.length >= this.maxAnswersAllowed;
      },
      twoOrLessQuestions() {
        return !this.answersInternal || this.answersInternal?.length <= 2;
      },
    },
    watch: {
      answersInternal: {
        handler: function emitUpdate() {
          this.$emit('input', this.answersInternal);
        },
        deep: true,
      },
    },
    methods: {
      addNewAnswer(index) {
        const newQuestion = {
          id: null,
          answer: '',
          isCorrect: false,
        };
        this.answersInternal.splice(index + 1, 0, newQuestion);
        this.answersInternal = this.answersInternal.map((a) => ({ ...a }));
      },
      removeAnswer(index) {
        this.answersInternal = this.answersInternal.filter((item, arrIndex) => arrIndex !== index);
      },
    },
  };
</script>

<style scoped>

</style>
