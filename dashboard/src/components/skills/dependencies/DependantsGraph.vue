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
  <simple-card id="dependent-skills-graph">
    <div v-if="!this.dependentSkills || this.dependentSkills.length === 0">
      <div class="column is-half has-text-centered">
        <no-content2 icon="fa fa-project-diagram" title="No Dependencies Yet..."
                     message="You can manage and visualize skill's dependencies on this page. Please use the dropdown above to start adding dependent skills."></no-content2>
      </div>

    </div>
    <div v-else>
      <div class="row">
        <div class="col-12 col-sm">
          <graph-legend :items="[
            {label: 'This Skill', color: 'lightgreen'},
            {label: 'My Dependencies', color: 'lightblue'},
            {label: 'Cross Project Skill Dependencies', color: '#ffb87f'},
            {label: 'Transitive Dependencies', color: 'lightgray'}
            ]"></graph-legend>
        </div>
        <div class="col text-left text-sm-right mt-2">
          <graph-node-sort-method-selector @value-changed="onSortNodeStrategyChange"></graph-node-sort-method-selector>
        </div>
      </div>
    </div>
    <div id="dependent-skills-network" style="height: 500px" aria-label="skills dependency graph"></div>
  </simple-card>
</template>

<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import GraphLegend from './GraphLegend';
  import GraphNodeSortMethodSelector from './GraphNodeSortMethodSelector';
  import NoContent2 from '../../utils/NoContent2';
  import GraphUtils from './GraphUtils';
  import SimpleCard from '../../utils/cards/SimpleCard';

  export default {
    name: 'DependantsGraph',
    components: {
      SimpleCard,
      NoContent2,
      GraphNodeSortMethodSelector,
      GraphLegend,
    },
    props: ['skill', 'dependentSkills', 'graph'],
    data() {
      return {
        network: null,
        nodes: new vis.DataSet(),
        edges: new vis.DataSet(),
        displayOptions: {
          layout: {
            randomSeed: 419465,
            hierarchical: {
              enabled: true,
              sortMethod: 'directed',
              nodeSpacing: 350,
              // treeSpacing: 1000,
              // blockShifting: false,
              // edgeMinimization: false,
              // parentCentralization: false,
              // levelSeparation: 1000,
              // direction: 'UP',
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
              border: '#3273dc',
              background: 'lightblue',
            },
            mass: 20,
          },
        },
      };
    },
    mounted() {
      if (this.graph && this.graph.nodes && this.graph.nodes.length > 0) {
        this.createGraph();
      }
    },
    beforeDestroy() {
      if (this.network) {
        this.network.destroy();
      }
    },
    watch: {
      graph: function watchGraph() {
        if (this.network) {
          this.network.destroy();
          this.network = null;
        }

        this.nodes.clear();
        this.edges.clear();

        this.createGraph();
      },
    },
    methods: {
      updateNodes() {
        const newItems = this.dependentSkills.filter((item) => !this.nodes.get().find((item1) => item1.id === item.id));
        newItems.forEach((newItem) => {
          const nodeEdgeData = this.buildNodeEdgeData(newItem);
          this.edges.add(nodeEdgeData.edge);
          this.nodes.add(nodeEdgeData.node);
        });

        const removeItems = this.nodes.get().filter((item) => !this.dependentSkills.find((item1) => item1.id === item.id) && item.id !== this.skill.id);
        removeItems.forEach((item) => {
          this.nodes.remove(item.id);
          const edgeToRemove = this.edges.get().find((edgeItem) => edgeItem.to === item.id);
          this.edges.remove(edgeToRemove);
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
        const container = document.getElementById('dependent-skills-network');
        this.network = new vis.Network(container, data, this.displayOptions);
      },
      buildData() {
        this.graph.nodes.forEach((node) => {
          const isCrossProject = node.projectId !== this.skill.projectId;
          const newNode = {
            id: node.id,
            label: GraphUtils.getLabel(node, isCrossProject),
            margin: 10,
            shape: 'box',
            chosen: false,
            title: GraphUtils.getTitle(node, isCrossProject),
          };
          if (newNode.id === this.skill.id) {
            newNode.color = {
              border: 'green',
              background: 'lightgreen',
            };
            // newNode.shape = 'circle';
          } else if (!this.dependentSkills.find((elem) => elem.id === newNode.id)) {
            newNode.color = {
              border: 'darkgray',
              background: 'lightgray',
            };
          } else if (isCrossProject) {
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
      buildNodeEdgeData(skillItem) {
        const node = {
          id: skillItem.id,
          label: skillItem.name,
          margin: 10,
          shape: 'box',
          chosen: false,
          title: GraphUtils.getTitle(skillItem, skillItem.projectId !== this.skill.projectId),
        };
        const edge = {
          from: this.skill.id,
          to: skillItem.id,
          arrows: 'to',
        };
        return { node, edge };
      },
    },
  };
</script>

<style scoped>
</style>

<style>
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-up,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-down,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-left,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-right,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomIn,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomOut,
  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomExtends {
    background-image: none !important;
  }

  #dependent-skills-graph div.vis-network div.vis-navigation div.vis-button:hover {
    box-shadow: none !important;
  }

  #dependent-skills-graph .vis-button:after {
    font-size: 2em;
    color: gray;
    font-family: "Font Awesome 5 Free";
  }

  #dependent-skills-graph .vis-button:hover:after {
    font-size: 2em;
    color: #3273dc;
  }

  #dependent-skills-graph .vis-button.vis-up:after {
    content: '\f35b';
  }

  #dependent-skills-graph .vis-button.vis-down:after {
    content: '\f358';
  }

  #dependent-skills-graph .vis-button.vis-left:after {
    content: '\f359';
  }

  #dependent-skills-graph .vis-button.vis-right:after {
    content: '\f35a';
  }

  #dependent-skills-graph .vis-button.vis-zoomIn:after {
    content: '\f0fe';
  }

  #dependent-skills-graph .vis-button.vis-zoomOut:after {
    content: '\f146';
  }

  #dependent-skills-graph .vis-button.vis-zoomExtends:after {
    content: "\f78c";
    font-weight: 900;
    font-size: 30px;
  }

</style>
