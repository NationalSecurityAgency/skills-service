<template>
  <div class="row pb-3">
    <div v-for="(subject, index) in subjects" :key="`unique-subject-${index}`"
      class="btn user-skill-subject-tile col-md-4"
      @click="openUserSkillSubject(subject, index)" >
      <subject-tile :subject="subject"/>
    </div>
  </div>
</template>

<script>
  import SubjectTile from '@/userSkills/subject/SubjectTile.vue';

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
        this.$router.push({
          name: 'subjectDetails',
          params: {
            subjectId: subject.subjectId,
          },
        });
      },
    },
  };
</script>

<style scoped>
  .user-skill-subject-tile {
    cursor: pointer;
  }
</style>
