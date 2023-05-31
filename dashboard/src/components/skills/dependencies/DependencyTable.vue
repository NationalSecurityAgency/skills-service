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
  <metrics-card id="learning-path-table" title="Learning Path Routes"
                :no-padding="true" data-cy="dependencyTable">
    <loading-container :is-loading="isLoading">
        <div v-if="!isLoading && !isProcessing && learningPaths.length > 0" class="my-4">
          <skills-b-table v-if="!isProcessing" :options="table.options" :items="learningPaths" data-cy="learningPathTable" tableStoredStateId="learningPathTable">
            <template v-slot:cell(fromItem)="data">
              <a :href="getUrl(data.item.fromNode)">{{ data.item.fromItem }}</a>
            </template>
            <template v-slot:cell(toItem)="data">
              <a :href="getUrl(data.item.toNode)">{{ data.item.toItem }}</a>
            </template>
            <template v-slot:cell(edit)="data">
              <b-button @click="removeLearningPath(data)"
                        variant="outline-info" size="sm" class="text-info"
                        :aria-label="`Remove learning path route of ${data.item.fromItem} to ${data.item.toItem}`"
                        data-cy="sharedSkillsTable-removeBtn"><i class="fa fa-trash"/></b-button>
            </template>
          </skills-b-table>
        </div>
        <div v-else>
          <no-content2 title="No Learning Paths Yet..." icon="fas fa-share-alt" class="my-5"
                       message="Add a path between a Skill/Badge and another Skill/Badge"/>
        </div>
    </loading-container>
  </metrics-card>
</template>

<script>
  import SkillsService from '@/components/skills/SkillsService';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';
  import LoadingContainer from '../../utils/LoadingContainer';
  import NoContent2 from '../../utils/NoContent2';
  import MetricsCard from '../../metrics/utils/MetricsCard';
  import SkillsBTable from '../../utils/table/SkillsBTable';

  export default {
    name: 'DependencyTable',
    mixins: [MsgBoxMixin, ProjConfigMixin],
    props: ['isLoading', 'data'],
    components: {
      MetricsCard,
      NoContent2,
      LoadingContainer,
      SkillsBTable,
    },
    data() {
      const fields = [
        {
          key: 'fromItem',
          label: 'From',
          sortable: true,
        },
        {
          key: 'toItem',
          label: 'To',
          sortable: true,
        },
      ];
      if (!this.isReadOnlyProjMethod()) {
        fields.push({
          key: 'edit',
          label: 'Remove',
          sortable: false,
        });
      }

      return {
        learningPaths: [],
        isProcessing: true,
        table: {
          options: {
            busy: false,
            bordered: false,
            outlined: true,
            stacked: 'md',
            fields,
            pagination: {
              server: false,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
        },
      };
    },
    mounted() {
      if (this.data && this.data.edges && this.data.edges.length > 0) {
        const { nodes, edges } = this.data;

        if (edges && edges.length > 0) {
          edges.forEach((edge) => {
            const fromNode = nodes.find((node) => node.id === edge.from);
            const toNode = nodes.find((node) => node.id === edge.to);

            this.learningPaths.push({
              fromItem: fromNode.details.name,
              fromNode: fromNode.details,
              toItem: toNode.details.name,
              toNode: toNode.details,
            });
          });
        }
        this.isProcessing = false;
      }
    },
    methods: {
      removeLearningPath(data) {
        const message = `Do you want to remove the path from ${data.item.fromItem} to ${data.item.toItem}?`;
        this.msgConfirm(message, 'Remove Learning Path?', 'Remove')
          .then((ok) => {
            if (ok) {
              SkillsService.removeDependency(data.item.toNode.projectId, data.item.toNode.skillId, data.item.fromNode.skillId, data.item.fromNode.projectId).then(() => {
                this.$emit('update');
              }).finally(() => {
                this.$nextTick(() => this.$announcer.assertive(`Successfully removed Learning Path route of ${data.item.fromItem} to ${data.item.toItem}`));
              });
            }
          });
      },
      getUrl(item) {
        let url = `/administrator/projects/${encodeURIComponent(item.projectId)}`;
        if (item.type === 'Skill') {
          url += `/subjects/${encodeURIComponent(item.subjectId)}/skills/${encodeURIComponent(item.skillId)}/`;
        } else if (item.type === 'Badge') {
          url += `/badges/${encodeURIComponent(item.skillId)}/`;
        }

        return url;
      },
    },
  };
</script>

<style>

</style>
