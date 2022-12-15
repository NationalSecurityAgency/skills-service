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
  <sub-page-header title="Tests And Surveys" action="Test" @add-action="openNewTestModal"/>

  <b-card body-class="p-0">
    <configured-tests />
  </b-card>

  <edit-test v-if="newTest.show" v-model="newTest.show" :test="newTest.test" />
</div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import ConfiguredTests from '@/components/testsAndSurveys/ConfiguredTests';
  import EditTest from '@/components/testsAndSurveys/testCreation/EditTest';

  export default {
    name: 'TestsAndSurveys',
    components: { EditTest, ConfiguredTests, SubPageHeader },
    data() {
      return {
        showNewTestModal: false,
        newTest: {
          show: false,
          isEdit: false,
          test: {},
        },
      };
    },
    methods: {
      openNewTestModal() {
        this.newTest = {
          show: true,
          isEdit: false,
          test: {
            name: '',
            testId: '',
            description: '',
          },
        };
      },
    },
  };
</script>

<style scoped>

</style>
