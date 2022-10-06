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
  <div id="full-dependent-skills-graph">
    <sub-page-header title="Skill Dependencies"/>

    <simple-card data-cy="fullDepsSkillsGraph">
      <loading-container :is-loading="isLoading">
        <div v-if="!hasGraphData" class="my-5">
            <no-content2 icon="fa fa-project-diagram" title="No Dependencies Yet..."
                         message="Here you can visualize skill's dependencies for the entire project. However, please navigate to a single skill to add dependencies."></no-content2>
        </div>
        <div v-else class="row">
          <div class="col-12 col-sm">
            <graph-legend class="graph-legend" :items="legendItems"/>
          </div>
          <div class="col text-left text-sm-right mt-2">
            <graph-node-sort-method-selector @value-changed="onSortNodeStrategyChange"/>
          </div>
        </div>
      </loading-container>
      <div v-if="showGraph" id="dependency-graph" style="height: 500px"></div>
    </simple-card>

    <share-skills-with-other-projects v-if="!isReadOnlyProj" :project-id="this.$route.params.projectId" class="mt-4"/>

    <shared-skills-from-other-projects v-if="!isReadOnlyProj" :project-id="this.$route.params.projectId" class="my-4"/>
  </div>
</template>

<script>
  import 'vis-network/styles/vis-network.css';
  import { Network } from 'vis-network';
  import ShareSkillsWithOtherProjects
    from '@/components/skills/crossProjects/ShareSkillsWithOtherProjects';
  import SharedSkillsFromOtherProjects
    from '@/components/skills/crossProjects/SharedSkillsFromOtherProjects';
  import SkillsService from '@/components/skills/SkillsService';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import GraphNodeSortMethodSelector from './GraphNodeSortMethodSelector';
  import NoContent2 from '@/components/utils/NoContent2';
  import GraphUtils from '@/components/skills/dependencies/GraphUtils';
  import GraphLegend from '@/components/skills/dependencies/GraphLegend';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SimpleCard from '@/components/utils/cards/SimpleCard';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';

  export default {
    name: 'FullDependencyGraph',
    mixins: [ProjConfigMixin],
    components: {
      SharedSkillsFromOtherProjects,
      ShareSkillsWithOtherProjects,
      SimpleCard,
      SubPageHeader,
      GraphLegend,
      NoContent2,
      GraphNodeSortMethodSelector,
      LoadingContainer,
    },
    data() {
      return {
        isLoading: true,
        showGraph: true,
        graph: {},
        network: null,
        nodes: {},
        edges: {},
        legendItems: [
          { label: 'Skill Dependencies', color: 'lightgreen' },
          { label: 'Cross Project Skill Dependencies', color: '#ffb87f' },
        ],
        displayOptions: {
          layout: {
            hierarchical: {
              enabled: true,
              sortMethod: 'directed',
              nodeSpacing: 350,
              treeSpacing: 370,
            },
          },
          interaction: {
            selectConnectedEdges: false,
            navigationButtons: true,
          },
          physics: {
            enabled: false,
          },
          nodes: {
            font: {
              size: 18,
            },
            color: {
              border: 'green',
              background: 'lightgreen',
            },
          },
        },
      };
    },
    mounted() {
      this.nodes = [];
      this.edges = [];
      this.loadGraphDataAndCreateGraph();
    },
    beforeDestroy() {
      if (this.network) {
        this.network.destroy();
      }
    },
    methods: {
      loadGraphDataAndCreateGraph() {
        SkillsService.getDependentSkillsGraphForProject(this.$route.params.projectId)
          .then((response) => {
            this.graph = response;
            this.isLoading = false;
            this.createGraph();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },

      onSortNodeStrategyChange(newStrategy) {
        this.displayOptions.layout.hierarchical.sortMethod = newStrategy;
        this.createGraph();
      },
      createGraph() {
        if (this.network) {
          this.network.destroy();
          this.network = null;
          this.nodes = [];
          this.edges = [];
        }

        const data = this.buildData();
        if (this.hasGraphData) {
          const container = document.getElementById('dependency-graph');
          this.network = new Network(container, data, this.displayOptions);
        } else {
          this.showGraph = false;
        }
      },
      buildData() {
        const sortedNodes = this.graph.nodes.sort((a, b) => a.id - b.id);
        sortedNodes.forEach((node) => {
          const isCrossProject = node.projectId !== this.$route.params.projectId;
          const newNode = {
            id: node.id,
            label: GraphUtils.getLabel(node, isCrossProject),
            margin: 10,
            shape: 'box',
            chosen: false,
            title: GraphUtils.getTitle(node, isCrossProject),
          };
          if (isCrossProject) {
            newNode.color = {
              border: 'orange',
              background: '#ffb87f',
            };
          }
          this.nodes.push(newNode);
        });
        const sortedEdges = this.graph.edges.sort((a, b) => a.toId - b.toId);
        sortedEdges.forEach((edge) => {
          this.edges.push({
            from: edge.fromId,
            to: edge.toId,
            arrows: 'to',
          });
        });

        const data = { nodes: this.nodes, edges: this.edges };
        return data;
      },
    },
    computed: {
      hasGraphData() {
        return this.graph && this.graph.nodes && this.graph.nodes.length > 0;
      },
    },
  };
</script>
<style>
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-up,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-down,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-left,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-right,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomIn,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomOut,
  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomExtends {
    background-image: none !important;
  }

  #full-dependent-skills-graph div.vis-network div.vis-navigation div.vis-button:hover {
    box-shadow: none !important;
  }

  #full-dependent-skills-graph .vis-button:after {
    font-size: 2em;
    color: gray;
    font-family: "Font Awesome 5 Free";
  }

  #full-dependent-skills-graph .vis-button:hover:after {
    font-size: 2em;
    color: #3273dc;
  }

  #full-dependent-skills-graph .vis-button.vis-up:after {
    content: '\f35b';
  }

  #full-dependent-skills-graph .vis-button.vis-down:after {
    content: '\f358';
  }

  #full-dependent-skills-graph .vis-button.vis-left:after {
    content: '\f359';
  }

  #full-dependent-skills-graph .vis-button.vis-right:after {
    content: '\f35a';
  }

  #full-dependent-skills-graph .vis-button.vis-zoomIn:after {
    content: '\f0fe';
  }

  #full-dependent-skills-graph .vis-button.vis-zoomOut:after {
    content: '\f146';
  }

  #full-dependent-skills-graph .vis-button.vis-zoomExtends:after {
    content: "\f78c";
    font-weight: 900;
    font-size: 30px;
  }
</style>

<style scoped>

</style>
