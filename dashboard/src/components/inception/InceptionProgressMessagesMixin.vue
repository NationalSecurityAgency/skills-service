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
  import { SkillsReporter } from '@skilltree/skills-client-vue';

  export default {
    name: 'InceptionProgressMessagesMixin',
    methods: {
      registerToDisplayProgress() {
        const myGlobalSuccessHandler = (details) => {
          if (details.completed) {
            details.completed.forEach(this.handleEvent);
          }
        };
        SkillsReporter.addSuccessHandler(myGlobalSuccessHandler);
      },
      handleEvent(completedItem) {
        let title = '';
        let msg = '';
        switch (completedItem.type) {
        case 'Overall':
          title = `Level ${completedItem.level}!!!!`;
          msg = `Wow! Congratulations on the Overall Level ${completedItem.level}!`;
          break;
        case 'Subject':
          title = 'Subject Level Achieved!!';
          msg = `Impressive!! Level ${completedItem.level} in ${completedItem.name} subject!`;
          break;
        case 'Skill':
          title = 'Skill Completed!!';
          msg = `Way to complete ${completedItem.name} skill!!!`;
          break;
        case 'Badge':
          title = `${completedItem.name}!!!`;
          msg = `You are now a proud owner of ${completedItem.name} badge!!!`;
          break;
        default:
          title = 'Completed!!';
          msg = `Way to finish ${completedItem.name}!`;
        }

        this.displayToast(msg, title);
      },
      displayToast(msg, title) {
        this.$bvToast.toast(msg, {
          title,
          autoHideDelay: 8000,
          toaster: 'b-toaster-top-center',
          solid: true,
          appendToast: true,
          variant: 'primary',
        });
      },
    },
  };
</script>

<style scoped>

</style>
