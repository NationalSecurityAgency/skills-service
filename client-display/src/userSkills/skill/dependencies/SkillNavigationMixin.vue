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
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';

  export default {
    name: 'SkillNavigationMixin',
    mixins: [NavigationErrorMixin],
    methods: {
      navigateToSkill(skillItem) {
        if (skillItem && skillItem.skillId && !skillItem.isThisSkill) {
          if (skillItem.isCrossProject) {
            if (this.$route.params.badgeId) {
              const params = {
                badgeId: this.$route.params.badgeId,
                crossProjectId: skillItem.projectId,
                dependentSkillId: skillItem.skillId,
              };
              this.handlePush({ name: 'crossProjectSkillDetailsUnderBadge', params });
            } else {
              this.handlePush({
                name: 'crossProjectSkillDetails',
                params: {
                  subjectId: this.$route.params.subjectId,
                  crossProjectId: skillItem.projectId,
                  skillId: this.$route.params.skillId,
                  dependentSkillId: skillItem.skillId,
                },
              });
            }
          } else if (skillItem.type !== 'Badge') {
            this.handlePush({
              name: 'skillDetails',
              params: {
                subjectId: skillItem.subjectId,
                skillId: skillItem.skillId,
              },
            });
          } else {
            this.handlePush(`/badges/${skillItem.skillId}/`);
          }
        }
      },
    },
  };
</script>

<style scoped>

</style>
