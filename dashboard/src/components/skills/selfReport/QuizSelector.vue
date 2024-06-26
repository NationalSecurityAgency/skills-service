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
  <ValidationProvider rules="required" v-slot="{errors}" name="Test">
    <v-select :options="availableQuizzes"
              v-model="selectedInternal"
              placeholder="Search available quizzes and surveys..."
              :multiple="false"
              :filterable="true"
              label="name"
              :loading="isLoading"
              @input="quizSelected"
              class="ml-2"
              data-cy="quizSelector">
      <template #option="{ name, type, quizId }">
        <div :data-cy="`availableQuizSelection-${quizId}`">
          <span class="text-secondary">{{ type }}:</span><span class="h6 ml-2">{{ name }}</span>
        </div>
      </template>
      <template #selected-option-container="{ option }">
        <div class="p-1" :data-cy="`quizSelected-${option.quizId}`">
          <span class="text-secondary">{{ option.type }}:</span><span class="ml-1">{{ option.name }}</span>
        </div>
      </template>
      <template #no-options>
        <div class="text-left pl-3 pb-2" data-cy="quizSelectHasNoValues">
          <div v-if="noQuizzes">
            <div>You currently do not administer any quizzes or surveys.</div>
            <div>Please navigate to the Quizzes and Surveys page to start creating.</div>
          </div>
          <div v-else>
            No results. Please refine your search string.
          </div>
        </div>
      </template>
    </v-select>
    <small role="alert" class="form-text text-danger" data-cy="skillDescriptionError">{{ errors[0] }}</small>
  </ValidationProvider>
</div>
</template>

<script>
  import vSelect from 'vue-select';
  import QuizService from '@/components/quiz/QuizService';

  export default {
    name: 'QuizSelector',
    components: { vSelect },
    props: {
      initiallySelectedQuizId: {
        type: String,
        default: null,
      },
    },
    data() {
      return {
        isLoading: true,
        selectedInternal: null,
        availableQuizzes: [],
        currentSearch: '',
      };
    },
    mounted() {
      this.selectedInternal = this.value;
      this.loadData();
    },
    computed: {
      noQuizzes() {
        return this.availableQuizzes && this.availableQuizzes.length === 0;
      },
    },
    methods: {
      loadData() {
        this.isLoading = true;
        QuizService.getQuizDefs()
          .then((res) => {
            this.availableQuizzes = res;
            if (this.initiallySelectedQuizId) {
              const found = this.availableQuizzes.find((q) => q.quizId === this.initiallySelectedQuizId);
              if (found) {
                this.selectedInternal = found;
              }
            }
          }).finally(() => {
            this.isLoading = false;
          });
      },
      quizSelected(quiz) {
        this.$emit('changed', quiz ? quiz.quizId : null);
      },
    },
  };
</script>

<style scoped>

</style>
