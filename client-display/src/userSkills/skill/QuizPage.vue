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
  <skills-spinner :loading="isLoading"/>
  <div v-if="!isLoading">
    <skills-title>{{ quizInfo.quizType }}</skills-title>
    <div class="text-left">
      <quiz-run :quiz-id="quizId"
                :quiz="quizInfo"
                @testWasTaken="done"
                @cancelled="done">
        <template slot="splashPageTitle">
          <div class="mb-4">
            <i class="fas fa-glass-cheers text-info" style="font-size: 1.5rem;"></i> You will earn <b-badge variant="success">
            <animated-number :num="skillInternal.pointIncrement"></animated-number></b-badge> points for
            <span class="font-weight-bold text-primary" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
            skill by <span v-if="isSurveySkill">completing this survey</span><span v-else>passing this quiz</span>.
          </div>
        </template>
        <template slot="completeAboveTitle">
          <div class="mb-4">
            <i class="fas fa-glass-cheers text-info" style="font-size: 1.5rem;"></i> Congrats!! You just earned <b-badge variant="success">
            <animated-number :num="skillInternal.pointIncrement"></animated-number></b-badge> points for
            <span class="font-weight-bold text-primary" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
            skill by <span v-if="isSurveySkill">completing the survey</span><span v-else>passing the test</span>.
          </div>
        </template>
      </quiz-run>
    </div>
  </div>
</div>
</template>

<script>
  import QuizRun from '@/common-components/quiz/QuizRun';
  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import AnimatedNumber from '@/userSkills/skill/progress/AnimatedNumber';
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';

  export default {
    name: 'QuizPage',
    mixins: [NavigationErrorMixin],
    components: {
      QuizRun, SkillsTitle, SkillsSpinner, AnimatedNumber,
    },
    props: {
      skill: {
        type: Object,
        default: null,
      },
    },
    data() {
      return {
        quizId: this.$route.params.quizId,
        skillId: this.$route.params.skillId,
        skillInternal: {},
        loadingSkillInfo: true,
        quizInfo: {},
        loadingQuizInfo: true,
      };
    },
    mounted() {
      if (this.$route.params.skill) {
        this.skillInternal = ({ ...this.$route.params.skill });
        this.loadingSkillInfo = false;
        this.loadQuizInfo();
      } else {
        this.loadSkillInfo().then(() => {
          this.loadQuizInfo();
        });
      }
    },
    computed: {
      isSurveySkill() {
        return this.skillInternal.selfReporting.type === 'Survey';
      },
      isLoading() {
        return this.loadingQuizInfo || this.loadingSkillInfo;
      },
    },
    methods: {
      done() {
        this.handlePush({
          name: 'skillDetails',
          params: {
            quizId: this.quizId,
            skillId: this.skillId,
          },
        });
      },
      loadSkillInfo() {
        this.loadingSkillInfo = true;
        return UserSkillsService.getSkillSummary(this.skillId, null, null)
          .then((res) => {
            this.skillInternal = res;
          }).finally(() => {
            this.loadingSkillInfo = false;
          });
      },
      loadQuizInfo() {
        this.loadingQuizInfo = true;
        QuizRunService.getQuizInfo(this.quizId)
          .then((quizInfo) => {
            this.quizInfo = quizInfo;
          }).finally(() => {
            this.loadingQuizInfo = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
