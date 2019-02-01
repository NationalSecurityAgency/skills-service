<template>
  <div id="dependent-skills-graph" class="skills-bordered-component">
    <div v-if="!this.dependentSkills || this.dependentSkills.length === 0" class="columns is-centered">
      <div class="column is-half has-text-centered">
        <no-dependenies message="You can manage and visualize skill's dependencies on this page. Please use the dropdown above to start adding
      dependent skills."></no-dependenies>
      </div>

    </div>
    <div v-else>
      <graph-node-sort-method-selector class="sort-method-selector" v-on:value-changed="onSortNodeStrategyChange"></graph-node-sort-method-selector>
      <graph-legend class="graph-legend"></graph-legend>
    </div>
    <div id="dependent-skills-network" style="height: 500px"></div>
  </div>
</template>

<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import NoDependenies from './NoDependenies';
  import GraphLegend from './GraphLegend';
  import GraphNodeSortMethodSelector from './GraphNodeSortMethodSelector';

  export default {
    name: 'DependantsGraph',
    components: { GraphNodeSortMethodSelector, GraphLegend, NoDependenies },
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
        const newItems = this.dependentSkills.filter(item => !this.nodes.get().find(item1 => item1.id === item.id));
        newItems.forEach((newItem) => {
          const nodeEdgeData = this.buildNodeEdgeData(newItem);
          this.edges.add(nodeEdgeData.edge);
          this.nodes.add(nodeEdgeData.node);
        });

        const removeItems = this.nodes.get().filter(item => !this.dependentSkills.find(item1 => item1.id === item.id) && item.id !== this.skill.id);
        removeItems.forEach((item) => {
          this.nodes.remove(item.id);
          const edgeToRemove = this.edges.get().find(edgeItem => edgeItem.to === item.id);
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
          const newNode = {
            id: node.id,
            label: node.name,
            margin: 10,
            shape: 'box',
            chosen: false,
            title: this.getTitle(node),
          };
          if (newNode.id === this.skill.id) {
            newNode.color = {
              border: 'green',
              background: 'lightgreen',
            };
            newNode.shape = 'circle';
          } else if (!this.dependentSkills.find(elem => elem.id === newNode.id)) {
            newNode.color = {
              border: 'darkgray',
              background: 'lightgray',
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
          title: this.getTitle(skillItem),
        };
        const edge = {
          from: this.skill.id,
          to: skillItem.id,
          arrows: 'to',
        };
        return { node, edge };
      },
      getTitle(skillItem) {
        return `<span style="font-style: italic; color: #444444">ID:</span> ${skillItem.skillId}<br/>
                <span style="font-style: italic; color: #444444">Point Increment:</span> ${skillItem.pointIncrement}<br/>
                <span style="font-style: italic; color: #444444">Total Points:</span> ${skillItem.totalPoints}`;
      },
    },
  };
</script>

<style scoped>
  .sort-method-selector {
    position: absolute;
    z-index: 10;
    right: 40px;
  }
  .graph-legend {
    position: absolute;
    z-index: 10;
  }
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
