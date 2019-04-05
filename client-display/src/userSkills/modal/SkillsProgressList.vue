<template>
  <div class="pt-3">
    <div
      v-for="(skill, index) in subject.skills"
      :key="`unique-skill-${index}`"
      class="row">
      <skills-subject-skill-row
        :skill="skill"
        :show-description="showDescriptions" />
    </div>
  </div>
</template>

<script>
  import SkillsSubjectSkillRow from '@/userSkills/modal/SkillsSubjectSkillRow.vue';

  export default {
    components: {
      SkillsSubjectSkillRow,
    },
    props: {
      subject: {
        type: Object,
        required: true,
      },
      showDescriptions: {
        type: Boolean,
        default: false,
      },
    },
  };
</script>

<style scoped>
</style>
