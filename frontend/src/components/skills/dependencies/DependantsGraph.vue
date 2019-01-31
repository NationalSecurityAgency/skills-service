<template>
  <div class="skills-bordered-component">
    <div v-if="!this.dependentSkills || this.dependentSkills.length === 0" class="columns is-centered">
      <div class="column is-half has-text-centered">
        <no-dependenies message="You can manage and visualize skill's dependencies on this page. Please use the dropdown above to start adding
      dependent skills."></no-dependenies>
      </div>
    </div>
    <div id="dependent-skills-network" style="height: 500px"></div>
  </div>
</template>

<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import NoDependenies from './NoDependenies';

  export default {
    name: 'DependantsGraph',
    components: { NoDependenies },
    props: ['skill', 'dependentSkills', 'graph'],
    data() {
      return {
        network: null,
        nodes: new vis.DataSet(),
        edges: new vis.DataSet(),
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

        if (this.nodes.get().length <= 1) {
          if (this.network) {
            this.network.destroy();
            this.network = null;
          }

          this.nodes.clear();
          this.edges.clear();
        }
      },
      createGraph() {
        const data = this.buildData();
        const container = document.getElementById('dependent-skills-network');

        const options = {
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
        };

        this.network = new vis.Network(container, data, options);
        // const self = this;
        // this.network.on('selectEdge', (params) => {
        //   self.selected = JSON.stringify(params.edges);
        // });
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
            newNode.color = 'lightgreen';
          } else if (!this.dependentSkills.find(elem => elem.id === newNode.id)) {
            newNode.color = 'lightgray';
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

        // this.nodes.add({
        //   id: this.skill.id,
        //   label: this.skill.name,
        //   color: 'lightgreen',
        //   shape: 'box',
        //   margin: 10,
        //   chosen: false,
        //   title: this.getTitle(this.skill),
        // });
        // this.dependentSkills.forEach((skillItem) => {
        //   const nodeEdgeDate = this.buildNodeEdgeData(skillItem);
        //   this.nodes.add(nodeEdgeDate.node);
        //   this.edges.add(nodeEdgeDate.edge);
        // });
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
</style>
