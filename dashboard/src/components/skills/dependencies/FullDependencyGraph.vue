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

    <simple-card>
      <loading-container :is-loading="!isLoading">
        <div v-if="!this.graph.nodes || this.graph.nodes.length === 0" class="mt-5">
            <no-content2 icon="fa fa-vector-square" title="No Dependencies Yet..."
                         message="You can manage and visualize skill's dependencies. Please add dependencies to get started."></no-content2>
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
      <div id="dependency-graph" style="height: 800px"></div>
    </simple-card>
  </div>
</template>

<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import SkillsService from '../SkillsService';
  import LoadingContainer from '../../utils/LoadingContainer';
  import GraphNodeSortMethodSelector from './GraphNodeSortMethodSelector';
  import NoContent2 from '../../utils/NoContent2';
  import GraphUtils from './GraphUtils';
  import GraphLegend from './GraphLegend';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import SimpleCard from '../../utils/cards/SimpleCard';

  export default {
    name: 'FullDependencyGraph',
    components: {
      SimpleCard,
      SubPageHeader,
      GraphLegend,
      NoContent2,
      GraphNodeSortMethodSelector,
      LoadingContainer,
    },
    data() {
      return {
        isLoading: false,
        graph: {},
        network: null,
        nodes: new vis.DataSet(),
        edges: new vis.DataSet(),
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
            this.isLoading = true;
            this.createGraph();
          })
          .finally(() => {
            this.isLoading = true;
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
          this.nodes.clear();
          this.edges.clear();
        }

        const data = this.buildData();
        const container = document.getElementById('dependency-graph');
        this.network = new vis.Network(container, data, this.displayOptions);
      },
      buildData() {
        this.graph.nodes.forEach((node) => {
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
          this.nodes.add(newNode);
        });
        this.graph.edges.forEach((edge) => {
          this.edges.add({
            from: edge.fromId,
            to: edge.toId,
            arrows: 'to',
          });
        });

        const data = { nodes: this.nodes, edges: this.edges };
        return data;
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
