<script>
  import { SkillsReporter } from '@skills/skills-client-vue';

  export default {
    name: 'ReportSkillOnMount',
    props: ['skillId'],
    mounted() {
      SkillsReporter.reportSkill(this.skillId);
    },
  };
</script>

<style scoped>

</style>
