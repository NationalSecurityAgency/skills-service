<template>
  <section>
    <div
      class="graph-container">
      <skill-dependency-details
        v-if="nodeDetailsView.show"
        :node-details-view="nodeDetailsView" @close="closeNodeDetails"></skill-dependency-details>
      <graph-legend class="graph-legend" :items="[
        {label: 'This Skill', color: 'lightblue'},
        {label: 'Dependencies', color: 'lightgray'},
        {label: 'Achieved Dependencies', color: 'lightgreen'}
        ]">
      </graph-legend>

      <skill-dependency-summary
          v-if="dependencies && dependencies.length > 0"
          class="summary-widget" :dependencies="dependencies"></skill-dependency-summary>
      <div id="dependent-skills-network" style="height: 500px"></div>
    </div>
  </section>
</template>

<script>
  import vis from 'vis';
  import 'vis/dist/vis.css';
  import Modal from '@/common/modal/Modal.vue';
  import ModalHeader from '@/common/modal/ModalHeader.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import GraphLegend from '@/userSkills/subject/GraphLegend.vue';
  import SkillDependencySummary from '@/userSkills/subject/SkillDependencySummary.vue';
  import SkillDependencyDetails from '@/userSkills/subject/SkillDependencyDetails.vue';

  export default {
    name: 'SkillDependencyGraph',
    components: {
      SkillDependencyDetails,
      SkillDependencySummary,
      GraphLegend,
      Modal,
      ModalHeader,
    },
    props: {
      skill: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        nodeDetailsView: {
          show: false,
          skill: {},
          pointer: {
            x: 0,
            y: 0,
          },
        },
        clickParams: '',
        dependencies: [],
        network: null,
        displayOptions: {
          layout: {
            randomSeed: 419465,
            hierarchical: {
              enabled: true,
              sortMethod: 'directed',
              nodeSpacing: 350,
            },
          },
          interaction: {
            selectConnectedEdges: false,
            navigationButtons: true,
            hover: true,
          },
          physics: {
            enabled: false,
          },
          nodes: {
            font: {
              size: 18,
            },
            color: {
              border: '#868686',
              background: '#e4e4e4',
            },
            mass: 20,
          },
        },
      };
    },
    mounted() {
      UserSkillsService.getSkillDependencies(this.skill.skillId)
        .then((res) => {
          this.dependencies = res.dependencies;
          this.createGraph();
        });
    },
    beforeDestroy() {
      if (this.network) {
        this.network.destroy();
      }
    },
    methods: {
      handleClose() {
        this.$emit('ok');
      },
      createGraph() {
        const data = this.buildData();
        const container = document.getElementById('dependent-skills-network');
        this.network = new vis.Network(container, data, this.displayOptions);
        // const self = this;
        this.network.on('click', (params) => {
          if (this.nodeDetailsView.show) {
            this.nodeDetailsView.show = false;
          } else if (params.nodes && params.nodes.length > 0) {
            const skillItem = this.locateSelectedSkill(params);
            if (skillItem) {
              this.nodeDetailsView.pointer.x = params.pointer.DOM.x;
              this.nodeDetailsView.pointer.y = params.pointer.DOM.y;
              UserSkillsService.getSkillSummary(skillItem.projectId, skillItem.skillId)
                .then((res) => {
                  this.nodeDetailsView.skill = res;
                  this.nodeDetailsView.show = true;
                });
            }
          }
        });
        const networkCanvas = container.getElementsByTagName('canvas')[0];
        this.network.on('hoverNode', () => {
          networkCanvas.style.cursor = 'pointer';
        });
        this.network.on('blurNode', () => {
          networkCanvas.style.cursor = 'default';
        });
      },
      closeNodeDetails() {
        this.nodeDetailsView.show = false;
      },
      locateSelectedSkill(params) {
        const skillId = params.nodes[0];
        let skillItem = null;
        const depItem = this.dependencies.find(item => this.getNodeId(item.dependsOn) === skillId);
        if (depItem) {
          skillItem = depItem.dependsOn;
        } else {
          const found = this.dependencies.find(item => this.getNodeId(item.skill) === skillId);
          if (found) {
            skillItem = found.skill;
          }
        }

        return skillItem;
      },
      buildData() {
        const nodes = new vis.DataSet();
        const edges = new vis.DataSet();
        const createdSkillIds = [];

        // color: { border: '#3273dc', background: 'lightblue' },
        // nodes.add(this.buildNode(this.skill, { color: { border: 'green', background: 'lightgreen' } }));
        this.dependencies.forEach((item) => {
          const isThisSkill = this.skill.projectId === item.skill.projectId && this.skill.skillId === item.skill.skillId;
          const extraParentProps = isThisSkill ? { color: { border: '#3273dc', background: 'lightblue' } } : {};
          this.buildNode(item.skill, createdSkillIds, nodes, extraParentProps);

          const extraChildProps = item.achieved ? { color: { border: 'green', background: 'lightgreen' } } : {};
          this.buildNode(item.dependsOn, createdSkillIds, nodes, extraChildProps);
          edges.add({
            from: this.getNodeId(item.skill),
            to: this.getNodeId(item.dependsOn),
            arrows: 'to',
          });
        });

        const data = { nodes, edges };
        return data;
      },
      buildNode(skill, createdSkillIds, nodes, extraProps = {}) {
        if (!createdSkillIds.includes(skill.skillId)) {
          createdSkillIds.push(skill.skillId);

          const node = {
            id: this.getNodeId(skill),
            label: this.getLabel(skill),
            margin: 10,
            shape: 'box',
            chosen: false,
            font: { multi: 'html', size: 20 },
          };
          const res = Object.assign(node, extraProps);
          nodes.add(res);
        }
      },
      getNodeId(skill) {
        return `${skill.projectName}_${skill.skillId}`;
      },
      getLabel(skill) {
        const isCrossProj = skill.projectId !== this.skill.projectId;
        const label = isCrossProj ? `<b>${skill.projectName}</b>\n${skill.skillName}` : skill.skillName;
        return label;
      },
    },
  };
</script>

<style scoped>
  .graph-container {
    max-width: 1100px;
    margin: 0 auto;
    position: relative;
  }

  .graph-legend {
    position: absolute;
    z-index: 10;
  }
  .summary-widget {
    position: absolute;
    z-index: 10;
    right: 40px;
  }
</style>

<style>
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-up,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-down,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-left,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-right,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomIn,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomOut,
  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomExtends {
    background-image: none !important;
  }

  #dependent-skills-network div.vis-network div.vis-navigation div.vis-button:hover {
    box-shadow: none !important;
  }

  #dependent-skills-network .vis-button:after {
    font-size: 2.7em;
    color: gray;
    font-family: "Font Awesome 5 Free";
  }

  #dependent-skills-network .vis-button:hover:after {
    font-size: 2em;
    color: #3273dc;
  }

  #dependent-skills-network .vis-button.vis-up:after {
    content: '\f35b';
  }

  #dependent-skills-network .vis-button.vis-down:after {
    content: '\f358';
  }

  #dependent-skills-network .vis-button.vis-left:after {
    content: '\f359';
  }

  #dependent-skills-network .vis-button.vis-right:after {
    content: '\f35a';
  }

  #dependent-skills-network .vis-button.vis-zoomIn:after {
    content: '\f0fe';
  }

  #dependent-skills-network .vis-button.vis-zoomOut:after {
    content: '\f146';
  }

  #dependent-skills-network .vis-button.vis-zoomExtends:after {
    content: "\f78c";
    font-weight: 900;
    font-size: 30px;
  }
</style>
