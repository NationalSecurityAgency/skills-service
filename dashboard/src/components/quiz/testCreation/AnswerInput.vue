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
  <div class="row no-gutters">
    <div class="col-auto">
      <select-correct-answer class="mr-2" />
    </div>
    <div class="col">
      <input class="form-control" type="text" v-model="answer.answerText"
             placeholder="Enter an answer"
             @keydown.enter="$emit('keydown-enter', answer)"
             v-focus
             data-cy="testName"
             id="testNameInput"
             aria-errormessage="testNameError"
             aria-describedby="testNameError"/>
    </div>
    <b-button-group class="ml-2">
      <b-button variant="outline-info"><i class="fas fa-plus"></i></b-button>
      <b-button variant="outline-info"><i class="fas fa-minus"></i></b-button>
    </b-button-group>
  </div>
</template>

<script>
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';

  export default {
    name: 'AnswerInput',
    components: { SelectCorrectAnswer },
    props: {
      answerDef: Object,
    },
    data() {
      return {
        answer: {
          id: null,
          answerText: '',
          isCorrect: false,
        },
      };
    },
  };
</script>

<style scoped>

</style>
