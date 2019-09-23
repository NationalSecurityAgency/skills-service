<script>
  import { SkillsReporter } from '@skills/skills-client-vue';

  export default {
    name: 'InceptionProgressMessagesMixin',
    methods: {
      registerToDisplayProgress() {
        const myGlobalSuccessHandler = (event) => {
          const det = event.detail;
          if (det.completed) {
            det.completed.forEach((completedItem) => {
              this.handleEvent(completedItem);
            });
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
          toaster: 'b-toaster-top-right',
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
