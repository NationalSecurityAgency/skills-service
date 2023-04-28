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
  <b-modal id="dependencyModification" size="lg"
           :no-close-on-esc="true"
           :title="`${node.label}`"
           v-model="show"
           @hide="hideModal"
           :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog">
    <b-container fluid>
      <div>
        Edit Prerequisites:
        <skills-selector2 v-if="!isReadOnlyProj" :options="allSkills" :selected="skills" v-on:added="addSkill"
                          v-on:removed="removeSkill">
          <template #dropdown-item="{ option }">
            <div class="media" data-cy="skillsSelector">
              <div class="d-inline-block mt-1 mr-3">
                <i v-if="option.otherProjectId" class="fas fa-w-16 fa-handshake text-hc"></i>
                <i v-else class="fas fa-w-16 fa-list-alt text-info"></i>
              </div>
              <div class="media-body">
                <strong class="mb-2"><span v-if="option.otherProjectId"
                                           class="">{{ option.otherProjectName }} : </span>
                  {{ option.name }}</strong>
                <div style="font-size: 0.95rem;" class="row text-secondary skills-option-id">
                  <div class="col-md">
                    <span class="font-italic">ID:</span> <span class="ml-1" data-cy="skillsSelector-skillId">{{option.skillId}}</span>
                  </div>
                  <div class="col-md">
                    <span v-if="option.otherProjectId" class="text-warning ml-3">** Shared Skill **</span>
                    <span v-else class="ml-2">
                        <span class="font-italic">Version:</span>
                        <span class="ml-1">{{option.version}}</span>
                        <span v-if="option.version > skill.version" class="text-danger ml-3"><br class="d-lg-none"/>** Not Eligible due to later version**</span>
                      </span>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template slot="selected-item" slot-scope="{ props }">
              <span class="mt-2 mr-2 border-hc rounded px-1" style="padding-top: 2px; padding-bottom: 2px;"
                    v-bind:style="{'background-color': props.option.isFromAnotherProject ? '#ffb87f' : 'lightblue'}">
                <span class="skills-handle-overflow" style="width: 15rem;"
                      :title="props.option.isFromAnotherProject ? props.option.projectId + ' : ' + props.option.name : props.option.name">
                  <span v-if="props.option.isFromAnotherProject">{{ props.option.projectId | truncate(10)}} : </span>
                  {{ props.option.name }}
                </span>
                <button class="btn btn-sm btn-outline-secondary p-0 border-0 ml-1"
                        v-on:click="props.remove(props.option)"><i class="fas fa-times"/></button>
              </span>
          </template>
        </skills-selector2>
      </div>
    </b-container>

    <div slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right" @click="hideModal"
                data-cy="allDoneBtn">
        Done
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';

  export default {
    name: 'DependencyModificationModal',
    components: { SkillsSelector2 },
    props: {
      value: Boolean,
      node: {
        type: Object,
        required: true,
      },
    },
    mounted() {
      this.projectId = this.node.details.projectId;
      this.skillId = this.node.details.skillId;
      this.skill = this.node.details;
      this.loadDataForSkill();
      this.loadAllSkills();
    },
    data() {
      return {
        projectId: null,
        skillId: null,
        show: this.value,
        skills: [],
        allSkills: [],
        isReadOnlyProj: false,
        skill: {},
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      loadDataForSkill() {
        SkillsService.getDependentSkillsGraphForSkill(this.projectId, this.skillId).then((data) => {
          const mySkill = data.nodes.find((entry) => entry.skillId === this.skillId && entry.projectId === this.projectId);
          const myEdges = data.edges.filter((entry) => entry.fromId === mySkill.id);
          const myChildren = data.nodes.filter((item) => myEdges.find((item1) => item1.toId === item.id));
          this.skills = myChildren.map((entry) => {
            const externalProject = entry.projectId !== this.projectId;
            const disableInfo = {
              manageBtn: {
                disable: externalProject,
                msg: 'Cannot manage skills from external projects.',
              },
            };
            return Object.assign(entry, {
              disabledStatus: disableInfo,
              isFromAnotherProject: externalProject,
            });
          });
        });
      },
      removeSkill(dependentSkill) {
        SkillsService.removeDependency(this.projectId, this.skillId, dependentSkill.skillId, dependentSkill.projectId).then(() => {
          this.loadDataForSkill();
        });
      },
      addSkill(newSkill) {
        SkillsService.assignDependency(this.projectId, this.skillId, newSkill.skillId, newSkill.projectId).then(() => {
          this.loadDataForSkill();
        });
      },
      hideModal(e) {
        this.show = false;
        this.$emit('hidden', e);
      },
      loadAllSkills() {
        SkillsService.getSkillsForDependency(this.projectId)
          .then((skills) => {
            this.allSkills = skills.filter((item) => (item.skillId !== this.skillId || item.otherProjectId));
          });
      },
    },
  };
</script>

<style scoped>

</style>
