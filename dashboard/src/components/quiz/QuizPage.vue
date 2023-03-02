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
    <page-header :loading="loadingQuizSummary || isLoadingQuizConfig" :options="headerOptions">
      <div slot="subSubTitle">
        <b-button-group v-if="!isReadOnlyQuiz" class="mt-1" size="sm">
          <b-button ref="editQuizButton"
                    class="btn btn-outline-primary"
                    size="sm"
                    variant="outline-primary"
                    data-cy="editQuizButton"
                    @click="editQuizInfo.showDialog = true"
                    :aria-label="`edit Quiz ${quizId}`">
            <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
<!--          <b-button ref="shareQuizButton"-->
<!--                    data-cy="shareQuizBtn"-->
<!--                    variant="outline-primary"-->
<!--                    :aria-label="`Share ${quizId} quiz with users`">-->
<!--            <span>Share</span> <i class="fas fa-share-alt" style="font-size:1rem;" aria-hidden="true"/>-->
<!--          </b-button>-->
          <b-button target="_blank" :to="{ name:'QuizRun', params: { quizId: quizId } }"
                    data-cy="quizPreview"
                    variant="outline-primary" :aria-label="`Preview Quiz ${quizId}`">
            <span>Preview</span> <i class="fas fa-eye" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
        </b-button-group>
        <div class="mt-2" v-if="!isLoadingQuizConfig">
          <i class="fas fa-user-shield text-success header-status-icon" aria-hidden="true" /> <span class="text-secondary font-italic small">Role:</span> <span class="small text-primary" data-cy="userRole">{{ userQuizRole | userRole }}</span>
        </div>
      </div>
    </page-header>

    <edit-quiz v-if="editQuizInfo.showDialog" v-model="editQuizInfo.showDialog"
               :quiz="editQuizInfo.quizDef"
               :is-edit="editQuizInfo.isEdit"
               @quiz-saved="updateQuizDef"
               @hidden="handleHideQuizEdit"/>

    <navigation v-if="!this.isLoadingQuizConfig" :nav-items="navItems">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Navigation from '@/components/utils/Navigation';
  import PageHeader from '@/components/utils/pages/PageHeader';
  import EditQuiz from '@/components/quiz/testCreation/EditQuiz';
  import QuizService from '@/components/quiz/QuizService';
  import QuizConfigMixin from '@/components/quiz/QuizConfigMixin';

  const { mapActions, mapGetters } = createNamespacedHelpers('quiz');

  export default {
    name: 'QuizPage',
    mixins: [QuizConfigMixin],
    components: {
      EditQuiz,
      PageHeader,
      Navigation,
    },
    data() {
      return {
        quizId: this.$route.params.quizId,
        editQuizInfo: {
          showDialog: false,
          isEdit: true,
          quizDef: {
            quizId: this.$route.params.quizId,
          },
        },
      };
    },
    computed: {
      ...mapGetters([
        'quizSummary',
        'loadingQuizSummary',
      ]),
      navItems() {
        const res = [
          { name: 'Questions', iconClass: 'fa-graduation-cap skills-color-skills', page: 'Questions' },
          { name: 'Results', iconClass: 'fa-users skills-color-users', page: 'QuizRunsHistoryPage' },
          // { name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'QuizMetrics' },
        ];

        if (!this.isReadOnlyQuiz) {
          res.push({ name: 'Access', iconClass: 'fas fa-shield-alt skills-color-access', page: 'QuizAccessPage' });
          res.push({ name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'QuizSettings' });
        }

        return res;
      },
      headerOptions() {
        if (!this.quizSummary) {
          return {};
        }
        const isSurvey = this.quizSummary.type === 'Survey';
        const typeDesc = isSurvey ? 'Collect Info' : 'Graded Questions';
        const typeIcon = isSurvey ? 'fas fa-chart-pie' : 'fas fa-tasks';
        return {
          icon: 'fas fa-spell-check skills-color-subjects',
          title: `${this.quizSummary.name}`,
          stats: [{
            label: 'Type',
            preformatted: `<div class="h5 font-weight-bold mb-0">${this.quizSummary.type}</div>`,
            secondaryPreformatted: `<div class="text-secondary text-uppercase text-truncate" style="font-size:0.8rem;margin-top:0.1em;">${typeDesc}</div>`,
            icon: `${typeIcon} skills-color-points`,
          }, {
            label: 'Questions',
            count: this.quizSummary.numQuestions,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }],
        };
      },
    },
    mounted() {
      this.loadQuizSummary({ quizId: this.$route.params.quizId });
    },
    methods: {
      ...mapActions([
        'loadQuizSummary',
      ]),
      updateQuizDef(quizDef) {
        QuizService.updateQuizDef(quizDef)
          .then(() => {
          const origId = this.quizId;
          if (quizDef.quizId !== origId) {
            this.quizId = quizDef.quizId;
            this.editQuizInfo.quizDef.quizId = quizDef.quizId;
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, quizId: quizDef.quizId } })
              .then(() => {
                this.loadQuizSummary({ quizId: quizDef.quizId }).then(() => this.handleHideQuizEdit());
              });
          } else {
            this.loadQuizSummary({ quizId: this.$route.params.quizId }).then(() => this.handleHideQuizEdit());
          }
          this.$nextTick(() => {
            this.$announcer.polite(`${quizDef.type} ${quizDef.name} has been edited`);
          });
        });
      },
      handleHideQuizEdit() {
        this.editQuizInfo.showDialog = false;
        this.$nextTick(() => {
          this.$nextTick(() => {
            const ref = this.$refs?.editQuizButton;
            if (ref) {
              ref.focus();
            }
          });
        });
      },
    },
  };
</script>

<style scoped>

</style>
