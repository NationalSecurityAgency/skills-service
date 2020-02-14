<template>
  <div>
    <div v-if="!(subjects && subjects.length > 0)" class="card">
      <div class="card-body">
        <no-data-yet class="my-2"
                     title="Subjects have not been added yet." sub-title="Please contact this project's administrator."/>
      </div>
    </div>
    <div v-else class="row pb-3">
      <div v-for="(subject, index) in subjects" :key="`unique-subject-${index}`"
        class="btn user-skill-subject-tile col-md-4"
        @click="openUserSkillSubject(subject, index)" >
        <subject-tile :subject="subject"/>
      </div>
    </div>
  </div>
</template>

<script>
  import SubjectTile from '@/userSkills/subject/SubjectTile.vue';
  import NoDataYet from '@/common/utilities/NoDataYet.vue';

  export default {
    components: {
      NoDataYet,
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
