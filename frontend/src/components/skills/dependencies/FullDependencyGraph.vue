<template>
  <div class="skills-bordered-component">
    <loading-container :is-loading="!isLoading">
      <div v-if="!this.graph.nodes || this.graph.nodes.length === 0" class="columns is-centered skills-pad-top-1-rem">
        <div class="column is-half">
          <no-dependenies message="You can manage and visualize skill's dependencies. Please add dependencies to get started."></no-dependenies>
        </div>
      </div>
    </loading-container>
    <div id="dependency-graph" style="height: 500px"></div>
  </div>
</template>


<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import SkillsService from '../SkillsService';
  import LoadingContainer from '../../utils/LoadingContainer';
  import NoDependenies from './NoDependenies';

  export default {
    name: 'FullDependencyGraph',
    props: ['projectId'],
    components: { NoDependenies, LoadingContainer },
    data() {
      return {
        isLoading: false,
        graph: {},
        network: null,
        nodes: new vis.DataSet(),
        edges: new vis.DataSet(),
        serverErrors: [],
      };
    },
    mounted() {
      this.loadGraphDataAndCreateGraph();
    },
    // watch: {
    //   dependentSkills: function watchSkills() {
    //     if (!this.network && this.dependentSkills && this.dependentSkills.length > 0) {
    //       this.createGraph();
    //     } else {
    //       this.updateNodes();
    //     }
    //   },
    // },
    methods: {
      loadGraphDataAndCreateGraph() {
        SkillsService.getDependentSkillsGraphForProject(this.projectId)
          .then((response) => {
            this.graph = response;
            this.isLoading = true;
            this.createGraph();
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.isLoading = true;
            throw e;
        });
      },

      // updateNodes() {
      //   const newItems = this.dependentSkills.filter(item => !this.nodes.get().find(item1 => item1.id === item.id));
      //   newItems.forEach((newItem) => {
      //     const nodeEdgeData = this.buildNodeEdgeData(newItem);
      //     this.edges.add(nodeEdgeData.edge);
      //     this.nodes.add(nodeEdgeData.node);
      //   });
      //
      //   const removeItems = this.nodes.get().filter(item => !this.dependentSkills.find(item1 => item1.id === item.id) && item.id !== this.skill.id);
      //   removeItems.forEach((item) => {
      //     this.nodes.remove(item.id);
      //     const edgeToRemove = this.edges.get().find(edgeItem => edgeItem.to === item.id);
      //     this.edges.remove(edgeToRemove);
      //   });
      //
      //   if (this.nodes.get().length <= 1) {
      //     if (this.network) {
      //       this.network.destroy();
      //       this.network = null;
      //     }
      //
      //     this.nodes.clear();
      //     this.edges.clear();
      //   }
      // },
      createGraph() {
        const data = this.buildData();
        const container = document.getElementById('dependency-graph');

        const options = {
          layout: {
            // randomSeed: 419465,
            hierarchical: {
              enabled: true,
              // sortMethod: 'directed',
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
          },
          physics: {
            enabled: false,
            //   barnesHut: {
            //     gravitationalConstant: -30000,
            //   },
            //   stabilization: {
            //     iterations: 2500,
            //   },
          },
        };

        this.network = new vis.Network(container, data, options);
        // const self = this;
        // this.network.on('selectEdge', (params) => {
        //   self.selected = JSON.stringify(params.edges);
        // });
      },
      buildData() {
        this.graph.nodes.forEach((node) => {
          this.nodes.add({
            id: node.id,
            label: node.name,
            margin: 10,
            shape: 'box',
            chosen: false,
            title: this.getTitle(node),
          });
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
      getTitle(skillItem) {
        return `<span style="font-style: italic; color: #444444">ID:</span> ${skillItem.skillId}<br/>
                <span style="font-style: italic; color: #444444">Point Increment:</span> ${skillItem.pointIncrement}<br/>
                <span style="font-style: italic; color: #444444">Total Points:</span> ${skillItem.totalPoints}`;
      },
    },
  };
</script>
<style scoped>
</style>
