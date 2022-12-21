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
  <div class="border pb-2">
    <div class="row">
      <div class="col">

        <b-row :no-gutters="true" class="mb-3">
          <b-col cols="auto">
            <div class="sort-control mr-3"><i class="fas fa-arrows-alt"/></div>
          </b-col>
<!--          <b-col cols="auto">-->
<!--            <div class="h5 mt-2 d-inline-block"><b-badge pill variant="info">{{questionNum}}</b-badge></div>-->
<!--          </b-col>-->
          <b-col class="">
            <div class="mt-2">Question <span class="font-weight-bold">#{{questionNum}}</span></div>

            <div class="px-2 pb-1">
              <markdown-text :text="question.question"/>

              <div class="mt-1 pl-1">
                <div v-for="(a) in question.answers" :key="a.id" class="row no-gutters">
                  <div class="col-auto">
                    <select-correct-answer :value="a.isCorrect" :read-only="true"
                                           font-size="1.5rem"/>
                  </div>
                  <div class="col pt-1 ml-2"><span class="mt-1 answerText">{{ a.answer }}</span>
                  </div>
                </div>
              </div>
            </div>
          </b-col>
        </b-row>

      </div>
      <div class="col-auto">
        <b-button-group size="sm" class="ml-1 mt-2 mr-3">
          <b-button variant="outline-primary" :data-cy="`editSkillButton_${question.questionId}`"
                    :aria-label="'edit Skill '+question.questionId" :ref="'edit_'+question.questionId"
                    title="Edit Skill">
            Edit <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button @click="deleteSkill(question.questionId)" variant="outline-primary"
                    :data-cy="`deleteSkillButton_${question.questionId}`"
                    :aria-label="'delete Skill '+question.questionId"
                    title="Delete Skill">
            Delete <i class="text-warning fas fa-trash" aria-hidden="true"/>
          </b-button>
        </b-button-group>

<!--        <span class="expand-collapse-control" v-b-toggle="`collapse-${question.id}`"><i class="far fa-minus-square"></i></span>-->
      </div>
    </div>
  </div>
</template>

<script>
  import SelectCorrectAnswer from '@/components/quiz/testCreation/SelectCorrectAnswer';
  import MarkdownText from '@/common-components/utilities/MarkdownText';

  export default {
    name: 'QuestionCard',
    components: { MarkdownText, SelectCorrectAnswer },
    props: {
      question: Object,
      questionNum: Number,
    },
  };
</script>

<style lang="scss" scoped>
@import "@/assets/custom";

.sort-control i {
  padding: 0.4rem;
  font-size: 1.2rem;
  color: #b3b3b3 !important;
  top: 0rem;
  left: 0rem;
  border-bottom: 1px solid #e8e8e8;
  border-right: 1px solid #e8e8e8;
  background-color: #fbfbfb !important;
  border-bottom-right-radius: .25rem !important
}

//.expand-collapse-control i {
//  padding: 0.5rem;
//  font-size: 1.3rem;
//  color: map_get($theme-colors, info) !important;;
//  top: 0rem;
//  left: 0rem;
//  border-bottom: 1px solid #e8e8e8;
//  border-left: 1px solid #e8e8e8;
//  background-color: #fbfbfb !important;
//  border-bottom-left-radius: .25rem !important
//}

.answerText {
  font-size: 0.8rem;
}

</style>
