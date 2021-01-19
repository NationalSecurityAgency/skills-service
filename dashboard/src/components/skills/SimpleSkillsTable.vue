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
  <div id="simple-skills-table" v-if="this.skills && this.skills.length">
    <skills-b-table :options="table.options" :items="skills" data-cy="simpleSkillsTable">
      <template #cell(controls)="data">
        <button v-on:click="onDeleteEvent(data.item)" class="btn btn-sm btn-outline-primary"
                :data-cy="`deleteSkill_${data.item.skillId}`"
                :aria-label="`remove dependency on ${data.item.skillId}`">
          <i class="text-warning fas fa-trash" aria-hidden="true"/>
        </button>
        <router-link v-if="data.item.subjectId" :id="data.item.skillId" :to="{ name:'SkillOverview',
                params: { projectId: data.item.projectId, subjectId: data.item.subjectId, skillId: data.item.skillId }}"
                     class="btn btn-sm btn-outline-hc ml-2">
          Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
        </router-link>
      </template>

      <template #cell(totalPoints)="data">
        {{ data.value | number }}
      </template>
    </skills-b-table>

<!--    <v-client-table :data="skills" :columns="columns" :options="options" class="mt-5">-->
<!--      <div slot="edit" slot-scope="props">-->
<!--        <div class="field text-right">-->
<!--          <span class="field">-->
<!--              <button v-on:click="onDeleteEvent(props.row)" class="btn btn-sm btn-outline-primary" data-cy="deleteSkill"-->
<!--                      :aria-label="`remove dependency on ${props.row.skillId}`">-->
<!--                      <i class="text-warning fas fa-trash" aria-hidden="true"/>-->
<!--              </button>-->
<!--                <router-link v-if="props.row.subjectId" :id="props.row.skillId" :to="{ name:'SkillOverview',-->
<!--                params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"-->
<!--                             class="btn btn-sm btn-outline-hc ml-2">-->
<!--                  Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>-->
<!--                </router-link>-->
<!--          </span>-->
<!--        </div>-->
<!--      </div>-->

<!--      <div slot="name" slot-scope="props">-->
<!--        &lt;!&ndash; allow to override how name field is rendered&ndash;&gt;-->
<!--        <slot name="name-cell" v-bind:props="props.row">-->
<!--          {{ props.row.name }}-->
<!--        </slot>-->
<!--      </div>-->
<!--    </v-client-table>-->
  </div>
</template>

<script>
  import SkillsBTable from '../utils/table/SkillsBTable';

  export default {
    name: 'SimpleSkillsTable',
    components: { SkillsBTable },
    props: {
      skills: {
        type: Array,
        required: true,
      },
      showProject: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      let columns;
      let headings;
      // let columnsDisplay;
      let sortable;
      if (this.showProject) {
        columns = ['name', 'skillId', 'projectId', 'edit'];
        headings = {
          name: 'Skill Name',
          skillId: 'Skill ID',
          projectId: 'Project ID',
          edit: '',
        };
        sortable = ['name', 'skillId', 'projectId'];
      } else {
        columns = ['name', 'skillId', 'totalPoints', 'edit'];
        headings = {
          name: 'Skill Name',
          skillId: 'Skill ID',
          totalPoints: 'Total Points',
          edit: '',
        };
        sortable = ['name', 'skillId', 'totalPoints'];
      }

      const fields = [
        {
          key: 'name',
          label: 'Skill Name',
          sortable: true,
        },
        {
          key: 'skillId',
          label: 'Skill ID',
          sortable: true,
        },
        {
          key: 'controls',
          label: 'Delete',
          sortable: false,
        },
      ];

      if (this.showProject) {
        fields.splice(0, 0, {
          key: 'projectId',
          label: 'Project ID',
          sortable: true,
        });
      } else {
        fields.splice(2, 0, {
          key: 'totalPoints',
          label: 'Total Points',
          sortable: true,
        });
      }
      return {
        columns,
        table: {
          options: {
            busy: false,
            bordered: false,
            outlined: true,
            stacked: 'md',
            fields,
            pagination: {
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
        },
        options: {
          headings,
          perPage: 15,
          columnsClasses: {
            edit: 'control-column',
          },
          columnsDisplay: {
            skillId: 'not_mobile',
            pointIncrement: 'not_mobile',
            totalPoints: 'not_mobile',
          },
          pagination: { dropdown: false, edge: false },
          sortable,
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          // highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
          filterable: false,
        },
      };
    },
    methods: {
      onDeleteEvent(skill) {
        this.$emit('skill-removed', skill);
      },
    },
  };
</script>

<style>

</style>
