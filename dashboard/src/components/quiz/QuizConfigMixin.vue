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
<script>
  import UserRolesUtil from '@/components/utils/UserRolesUtil';

  export default {
    name: 'QuizConfigMixin',
    methods: {
      loadQuizConfig() {
        return this.$store.dispatch('afterQuizConfigStateLoaded').then((quizConfig) => quizConfig);
      },
    },
    computed: {
      isLoadingQuizConfig() {
        return this.$store.getters.loadingQuizConfig;
      },
      quizConfig() {
        return this.$store.getters.quizConfig;
      },
      isReadOnlyQuiz() {
        return this.quizConfig && UserRolesUtil.isQuizReadOnlyRole(this.quizConfig.quizUserRole);
      },
      userQuizRole() {
        return this.quizConfig && this.quizConfig.quizUserRole;
      },
    },
  };
</script>

<style scoped>

</style>
