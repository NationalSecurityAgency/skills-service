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
    <sub-page-header ref="subPageHeader"
                     title="Questions"
                     action="Question"
                     @add-action="openNewAnswerModal"
                     aria-label="new question"/>

    <b-card body-class="p-0">
      <div class="p-2 text-right">
        <b-button variant="outline-info"><i class="far fa-minus-square"></i> Collapse All</b-button>
      </div>
      <div v-for="(q, index) in questions" :key="q.questionId">
        <question-card :question="q" :question-num="index+1"></question-card>
        <!--        <hr v-if="index + 1 < questions.length"/>-->
      </div>
    </b-card>

    <edit-question v-if="editQuestionInfo.showDialog" v-model="editQuestionInfo.showDialog"
                   :is-edit="editQuestionInfo.isEdit"
                   @question-saved="questionDefSaved"
                   :question-def="editQuestionInfo.questionDef"/>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import QuestionCard from '@/components/quiz/testCreation/QuestionCard';
  import EditQuestion from '@/components/quiz/testCreation/EditQuestion';

  export default {
    name: 'Questions',
    components: { EditQuestion, QuestionCard, SubPageHeader },
    data() {
      return {
        isLoading: false,
        questions: [{
          questionId: 'question1',
          ask: 'How many legs does a spider have?',
          answers: ['Seven', 'Eight', 'Two', 'Four'],
        }, {
          questionId: 'question2',
          ask: 'What fruit do kids traditionally give to teachers?',
          answers: ['Banana', 'Pineapple', 'Apple', 'Pear'],
        }],
        editQuestionInfo: {
          showDialog: false,
          isEdit: false,
          questionDef: {},
        },
      };
    },
    methods: {
      questionDefSaved(questionDef) {
        console.log(JSON.stringify(questionDef, null, 2));
      },
      openNewAnswerModal() {
        this.editQuestionInfo.questionDef = {
          id: null,
          question: '',
          type: 'MultipleChoice',
          graded: true,
          answers: [{
            id: null,
            answer: '',
            isCorrect: false,
          }, {
            id: null,
            answer: '',
            isCorrect: false,
          }],
        };
        this.editQuestionInfo.showDialog = true;
      },
    },
  };
</script>

<style scoped>

</style>
