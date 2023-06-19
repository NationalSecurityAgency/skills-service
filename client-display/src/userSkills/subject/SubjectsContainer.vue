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
<template>
  <div>
    <div v-if="!hasData" class="card">
      <div class="card-body">
        <no-data-yet class="my-2"
                     :title="`${this.subjectDisplayName}s have not been added yet.`"
                     :sub-title="`Please contact this ${this.projectDisplayName.toLowerCase()}'s administrator.`"/>
      </div>
    </div>
    <search-all-project-skills v-if="hasData" />
    <div v-if="hasData" class="row">
      <div v-for="(subject, index) in subjects" :key="`unique-subject-${index}`"
           class="btn user-skill-subject-tile col-md-4"
           @click="openUserSkillSubject(subject, index)"
           @keydown.enter="openUserSkillSubject(subject, index)">
        <subject-tile :subject="subject"/>
      </div>
    </div>
  </div>
</template>

<script>
  import NoDataYet from '@/common-components/utilities/NoDataYet';
  import SubjectTile from '@/userSkills/subject/SubjectTile';
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';
  import SearchAllProjectSkills from '@/userSkills/searchSkills/SearchAllProjectSkills';

  export default {
    mixins: [NavigationErrorMixin],
    components: {
      SearchAllProjectSkills,
      NoDataYet,
      SubjectTile,
    },
    props: {
      subjects: {
        type: Array,
        required: true,
      },
    },
    computed: {
      hasData() {
        return this.subjects && this.subjects.length > 0;
      },
    },
    methods: {
      openUserSkillSubject(subject) {
        this.handlePush({
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
