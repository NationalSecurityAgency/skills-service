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
  <b-card body-class="m-0 p-0">
    <skills-b-table :options="options" :items="skillsWithOutOfBoundsPoints"
                    data-cy="skillsWithOutOfBoundsPoints">
      <template #head(totalPoints)="data">
          <span class="text-danger"><i
            class="fas fa-exclamation-circle"/> {{ data.label }}</span>
      </template>
      <template v-slot:cell(totalPoints)="data">
        <span class="text-danger font-weight-bold">{{ data.value | number }}</span>
        <span v-if="data.value > projectSkillMaxPoints" class="text-info"> ( <span class="font-italic">more than</span> {{ projectSkillMaxPoints | number }} )</span>
        <span v-if="data.value < projectSkillMinPoints" class="text-info"> ( <span class="font-italic">less than</span> {{ projectSkillMinPoints | number }} )</span>
      </template>
    </skills-b-table>
  </b-card>
</template>

<script>
  import SkillsBTable from '../../utils/table/SkillsBTable';

  export default {
    name: 'FinalizeWarningSkillsPointsTable',
    components: { SkillsBTable },
    props: {
      skillsWithOutOfBoundsPoints: {
        type: Array,
        required: true,
      },
      projectSkillMinPoints: {
        type: Number,
        required: true,
      },
      projectSkillMaxPoints: {
        type: Number,
        required: true,
      },
    },
    data() {
      return {
        options: {
          busy: false,
          bordered: false,
          outlined: true,
          stacked: 'md',
          sortBy: 'skillName',
          tableDescription: 'Skills',
          fields: [
            {
              key: 'skillName',
              label: 'Skill Name',
              sortable: true,
            },
            {
              key: 'totalPoints',
              label: 'Points',
              sortable: true,
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: this.skillsWithOutOfBoundsPoints.length,
            pageSize: 3,
            possiblePageSizes: [3, 5],
          },
        },
      };
    },
  };
</script>

<style scoped>

</style>
