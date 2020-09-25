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
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  export default {
    name: 'SkillDisplayDataLoadingMixin',
    data() {
      return {
        loading: {
          userSkills: true,
          userSkillsRanking: true,
        },
        displayData: {
          userSkills: null,
          userSkillsRanking: null,
        },
      };
    },
    methods: {
      loadUserSkills() {
        UserSkillsService.getUserSkills()
          .then((response) => {
            this.displayData.userSkills = response;
            this.loading.userSkills = false;
          });
      },
      loadSubject() {
        UserSkillsService.getSubjectSummary(this.$route.params.subjectId)
          .then((result) => {
            this.displayData.userSkills = result;
            this.loading.userSkills = false;
          });
      },
      loadUserSkillsRanking() {
        UserSkillsService.getUserSkillsRanking(this.$route.params.subjectId)
          .then((response) => {
            this.displayData.userSkillsRanking = response;
            this.loading.userSkillsRanking = false;
          });
      },

      resetLoading() {
        this.loading.userSkills = true;
        this.loading.pointsHistory = true;
        this.loading.userSkillsRanking = true;
      },
    },
  };
</script>

<style scoped>

</style>
