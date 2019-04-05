<template>
  <div class="pt-3">
    <div
      v-for="(skill, index) in subject.skills"
      :key="`unique-skill-${index}`"
      class="row">
      <user-skills-subject-skill-row
        :skill="skill"
        :show-description="showDescriptions" />
    </div>
  </div>
</template>

<script>
  import UserSkillsSubjectSkillRow from '@/userSkills/modal/UserSkillsSubjectSkillRow.vue';

  export default {
    components: {
      UserSkillsSubjectSkillRow,
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
