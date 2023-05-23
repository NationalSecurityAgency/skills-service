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
  <div class="text-left text-primary">
    <b-table :items="prerequisites"
             :fields="fields"
             :sort-by="sortBy"
             :per-page="paging.perPage"
             :current-page="paging.currentPage"
             data-cy="prereqTable">
      <template #head(skillName)="data">
        <div class="text-left">
          <i class="fas fa-project-diagram text-success skills-theme-primary-color" :aria-hidden="true" /> {{ data.label }}
        </div>
      </template>
      <template #head(type)="data">
        <div class="text-left">
          <i class="fas fa-atom text-info skills-theme-primary-color" :aria-hidden="true" /> {{ data.label }}
        </div>
      </template>
      <template #head(achieved)="data">
        <div class="text-left">
          <i class="far fa-check-square text-warning skills-theme-primary-color" :aria-hidden="true" /> {{ data.label }}
        </div>
      </template>

      <template v-slot:cell(skillName)="data">
        <div v-if="data.item.isCrossProject"><i>Shared From</i> <b>{{ data.item.projectName }}</b></div>
        <b-link style="text-decoration: underline;"
                class="skills-theme-primary-color"
                :aria-label="`Navigate to prerequisite ${data.item.type} ${data.value}`"
                @click="navigateToSkill(data.item)"
                :data-cy="`skillLink-${data.item.projectId}-${data.item.skillId}`"
                >{{ data.value }}</b-link>
      </template>
      <template v-slot:cell(type)="data">
        <div style="font-size: 1.1rem; width: 2rem;" class="d-inline-block text-center border rounded">
          <i :class="`fas ${getTypeIcon(data.value)}`" :style="`color: ${getTypeIconColor(data.value)}`" aria-hidden="true"/>
        </div>
        <span class="ml-1"
            :aria-label="`Prerequisite's type is ${data.value}`">{{ data.value}}</span>
      </template>
      <template v-slot:cell(achieved)="data">
        <span v-if="data.value" class="font-weight-bold"
              :aria-label="`${data.item.skillName} ${data.item.type} was achieved`"
              :style="`color: ${getAchievedColor()}`">✓Yes</span>
        <div v-if="!data.value" class="d-inline-block"
              :aria-label="`${data.item.skillName} ${data.item.type} is not achieved`">Not Yet...</div>
      </template>
    </b-table>
    <div v-if="paging.shouldPage" class="row justify-content-center">
      <div class="col-auto">
        <b-pagination v-model="paging.currentPage"
                      :total-rows="paging.totalRows"
                      :per-page="paging.perPage"
                      data-cy="prereqTablePaging"/>
      </div>
    </div>
  </div>
</template>

<script>
  import SkillNavigationMixin from '@/userSkills/skill/dependencies/SkillNavigationMixin';
  import PrerequisiteColorsMixin from '@/userSkills/skill/dependencies/PrerequisiteColorsMixin';

  export default {
    name: 'DependenciesDetails',
    mixins: [SkillNavigationMixin, PrerequisiteColorsMixin],
    props: {
      prerequisitesLinks: {
        type: Array,
        required: true,
      },
    },
    mounted() {
      const alreadyAddedIds = [];
      const prerequisites = [];
      this.prerequisitesLinks.forEach((link) => {
        const prereq = link.dependsOn;
        if (!alreadyAddedIds.includes(prereq.id)) {
          prerequisites.push({
            ...prereq,
            achieved: link.achieved,
            isCrossProject: link.crossProject,
          });
        }
      });

      this.prerequisites = prerequisites;
      this.paging.totalRows = this.prerequisites.length;
      this.paging.shouldPage = this.paging.totalRows > 8;
      if (!this.paging.shouldPage) {
        this.paging.perPage = this.paging.totalRows;
      }
    },
    data() {
      return {
        prerequisites: [],
        sortBy: 'skillName',
        fields: [
          {
            key: 'skillName',
            label: 'Prerequisite Name',
            sortable: true,
          },
          {
            key: 'type',
            label: 'Type',
            sortable: true,
          },
          {
            key: 'achieved',
            label: 'Achieved',
            sortable: true,
          },
        ],
        paging: {
          shouldPage: true,
          totalRows: 0,
          currentPage: 1,
          perPage: 5,
        },
      };
    },
    computed: {
    },
    methods: {
      getTypeIcon(type) {
        if (type === 'Badge') {
          return 'fa-award';
        }
        return 'fa-graduation-cap';
      },
      getTypeIconColor(type) {
        if (type === 'Badge') {
          return this.getBadgeColor();
        }
        return this.getSkillColor();
      },
    },
  };
</script>

<style>

</style>
