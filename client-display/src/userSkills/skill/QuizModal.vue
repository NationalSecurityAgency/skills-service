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
  <b-modal id="reportSkillModal"
           :hide-header="true"
           :hide-footer="true"
           size="xl"
           body-class="p-0"
           :no-close-on-backdrop="true"
           v-model="show">
    <quiz-run :quiz-id="skill.selfReporting.quizId"
              @testWasTaken="testWasTaken"
      @cancelled="cancel">
      <template slot="splashPageTitle">
        <div class="mb-2">
          <i class="fas fa-glass-cheers text-info" style="font-size: 1.5rem;"></i> You will earn <b-badge variant="success">{{ skill.points}}</b-badge> points for
          <span class="font-weight-bold text-primary" style="font-size: 1.2rem">{{ skill.skill }}</span>
          skill by passing this test.
        </div>
      </template>
    </quiz-run>
  </b-modal>
</template>

<script>
  import QuizRun from '@/common-components/quiz/QuizRun';

  export default {
    name: 'QuizModal',
    components: { QuizRun },
    props: {
      skill: Object,
      value: Boolean,
    },
    data() {
      return {
        show: this.value,
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    mounted() {
      console.log(this.skill);
    },
    methods: {
      cancel() {
        this.show = false;
      },
      testWasTaken(quizResult) {
        this.show = false;
        this.$emit('testWasTaken', quizResult);
      },
    },
  };
</script>

<style scoped>

</style>
