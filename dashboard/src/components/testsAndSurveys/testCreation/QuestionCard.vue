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

        <b-row :no-gutters="true">
          <b-col cols="auto">
            <span class="sort-control mr-3"><i class="fas fa-arrows-alt"/></span>
            <b-badge>{{questionNum}}.</b-badge>
          </b-col>
          <b-col class="ml-2">
            <div class="mt-2">
              <span class="h5 font-weight-bold text-secondary">{{ question.ask }}</span>
            </div>

            <div class="mt-1">
              <div v-for="(a, index) in question.answers" :key="a" class=" my-1">
                <span class="border rounded p-1 mr-2">{{ String.fromCharCode(97 + index).toUpperCase() }}.</span> {{ a }}
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

        <span class="expand-collapse-control"><i class="far fa-minus-square"></i></span>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'QuestionCard',
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

.expand-collapse-control i {
  padding: 0.5rem;
  font-size: 1.3rem;
  color: map_get($theme-colors, info) !important;;
  top: 0rem;
  left: 0rem;
  border-bottom: 1px solid #e8e8e8;
  border-left: 1px solid #e8e8e8;
  background-color: #fbfbfb !important;
  border-bottom-left-radius: .25rem !important
}
</style>
