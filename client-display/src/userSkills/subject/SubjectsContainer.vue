<template>
  <div
    class="user-skills-categories-container">
    <div
      v-for="(subject, index) in subjects"
      :key="`unique-subject-${index}`"
      class="btn user-skill-subject-tile user-skills-panel"
      @click="openUserSkillSubject(subject, index)" >
      <subject-tile :subject="subject" />
    </div>
  </div>
</template>

<script>
  import SubjectTile from '@/userSkills/subject/SubjectTile.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  export default {
    components: {
      SubjectTile,
    },
    props: {
      subjects: {
        type: Array,
        required: true,
      },
    },
    methods: {
      openUserSkillSubject(subject) {
        UserSkillsService.getSubjectSummary(subject.subjectId)
          .then((result) => {
            this.$router.push({
              name: 'subjectDetails',
              params: {
                subjectId: subject.subjectId,
                subject: result,
              },
            });
          });
      },
    },
  };
</script>

<style scoped>
  .user-skills-categories-container {
    background-color: #f0f0f0;
    padding: 10px;
  }

  .user-skill-subject-tile {
    padding: 10px 0;
    cursor: pointer;
    width: 305px;
    margin: 10px;
  }

  .user-skill-subject-tile:hover {
    background-color: #f8f8f8;
  }
</style>
