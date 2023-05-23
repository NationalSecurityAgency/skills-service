<script>
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';

  export default {
    name: 'SkillNavigationMixin',
    mixins: [NavigationErrorMixin],
    methods: {
      navigateToSkill(skillItem) {
        console.log(skillItem);
        if (skillItem && skillItem.skillId && !skillItem.isThisSkill) {
          console.log('inside first if');
          if (skillItem.isCrossProject) {
            console.log('inside cross project');
            this.handlePush({
              name: 'crossProjectSkillDetails',
              params: {
                subjectId: this.$route.params.subjectId,
                crossProjectId: skillItem.projectId,
                skillId: this.$route.params.skillId,
                dependentSkillId: skillItem.skillId,
              },
            });
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
