<template>
  <div
    class="user-skills-categories-container">
    <user-skills-subject-modal
      v-if="showUserSkillsSubjectModal"
      :subject="userSkillsSubjectModalSubject"
      :ribbon-color="'pink'"
      @ok="showUserSkillsSubjectModal = false"
      @cancel="showUserSkillsSubjectModal = false"/>

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
  import UserSkillsSubjectModal from '@/userSkills/modal/UserSkillsSubjectModal.vue';

  export default {
    components: {
      SubjectTile,
      UserSkillsSubjectModal,
    },
    props: {
      subjects: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        userSkillsSubjectModalSubject: null,
        showUserSkillsSubjectModal: false,
      };
    },
    methods: {
      openUserSkillSubject(subject) {
        UserSkillsService.getSubjectSummary(subject.subjectId)
          .then((result) => {
            this.showUserSkillsSubjectModal = true;
            this.userSkillsSubjectModalSubject = result;
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
