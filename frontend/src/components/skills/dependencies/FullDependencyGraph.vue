<template>
  <div>
    <div class="columns">
      <div class="column is-full skills-underline-container">
        <span class="title is-3">Skill Dependencies</span>
      </div>
    </div>
    <div id="full-dependent-skills-graph" class="skills-bordered-component">
      <loading-container :is-loading="!isLoading">
        <div v-if="!this.graph.nodes || this.graph.nodes.length === 0" class="columns is-centered skills-pad-top-1-rem">
          <div class="column is-half">
            <no-content2 icon="fa fa-vector-square" title="No Dependencies Yet..."
              message="You can manage and visualize skill's dependencies. Please add dependencies to get started."></no-content2>
          </div>
        </div>
        <div v-else>
          <graph-legend class="graph-legend" :items="[
            {label: 'Skill Dependencies', color: 'lightgreen'},
            {label: 'Cross Project Skill Dependencies', color: '#ffb87f'}
            ]"></graph-legend>
          <graph-node-sort-method-selector class="sort-method-selector"
                                           v-on:value-changed="onSortNodeStrategyChange"></graph-node-sort-method-selector>
        </div>
      </loading-container>
      <div id="dependency-graph" style="height: 800px"></div>
    </div>
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

  export default {
    name: 'FullDependencyGraph',
    props: ['projectId'],
    components: {
      GraphLegend, NoContent2, GraphNodeSortMethodSelector, LoadingContainer,
    },
    data() {
      return {
        isLoading: false,
        graph: {},
        network: null,
        nodes: new vis.DataSet(),
        edges: new vis.DataSet(),
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
        SkillsService.getDependentSkillsGraphForProject(this.projectId)
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
          const isCrossProject = node.projectId !== this.projectId;
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
  .sort-method-selector {
    position: absolute;
    z-index: 10;
    right: 40px;
  }
</style>
