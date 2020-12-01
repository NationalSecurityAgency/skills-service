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
    <div class="card">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">Dependencies</h6>
        </div>
        <div class="card-body">
            <div class="row legend-row">
                <div class="col-12 mb-2 m-sm-0 col-sm-6">
                    <graph-legend :items="legendItems" class="legend-component"/>
                </div>

                <div class="col-12 col-sm-6">
                    <skill-dependency-summary
                        v-if="dependencies && dependencies.length > 0"
                        :dependencies="dependencies"
                        class="legend-component float-md-right"/>
                </div>
            </div>
            <div id="dependent-skills-network" style="height: 500px"></div>
        </div>
    </div>
</template>

<script>
  import 'vis/dist/vis.css';
  import GraphLegend from '@/userSkills/skill/dependencies/GraphLegend';
  import SkillDependencySummary from '@/userSkills/skill/dependencies/SkillDependencySummary';

  const getVis = () => import(
  /* webpackChunkName: "vis" */
  'vis'
  );

  export default {
    name: 'SkillDependencies',
    components: {
      SkillDependencySummary,
      GraphLegend,
    },
    props: {
      dependencies: Array,
      skillId: String,
    },
    data() {
      return {
        thisSkill: {},
        network: null,
        legendItems: [
          { label: 'This Skill', color: 'lightblue' },
          { label: 'Dependencies', color: 'lightgray' },
          { label: 'Achieved Dependencies', color: 'lightgreen' },
        ],
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
      getVis().then((vis) => {
        this.createGraph(vis);
      });
    },
    beforeDestroy() {
      this.cleanUp();
    },
    methods: {
      cleanUp() {
        if (this.network) {
          this.network.destroy();
        }
        this.thisSkill = {};
      },
      isSmallScreen() {
        const width = window.innerWidth;
        return width <= 768;
      },
      createGraph(vis) {
        this.cleanUp();

        const data = this.buildData(vis);
        const container = document.getElementById('dependent-skills-network');
        this.network = new vis.Network(container, data, this.displayOptions);
        // const self = this;
        this.network.on('click', (params) => {
          const skillItem = this.locateSelectedSkill(params);
          if (skillItem) {
            if (skillItem.isCrossProject) {
              this.$router.push({
                name: 'crossProjectSkillDetails',
                params: {
                  crossProjectId: skillItem.projectId,
                  skillId: skillItem.skillId,
                },
              });
            } else {
              this.$router.push({
                name: 'skillDetails',
                params: {
                  skillId: skillItem.skillId,
                },
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

        this.focusOnParentNode();
      },
      focusOnParentNode() {
        if (this.isSmallScreen()) {
          const options = {
            scale: 0.7,
            offset: { x: 0, y: 0 },
            animation: {
              duration: 1500,
              easingFunction: 'easeInOutQuad',
            },
          };
          this.network.focus(this.getNodeId(this.thisSkill), options);
        }
      },
      locateSelectedSkill(params) {
        const skillId = params.nodes[0];
        let skillItem = null;
        let crossProj = false;
        const depItem = this.dependencies.find((item) => this.getNodeId(item.dependsOn) === skillId);
        if (depItem) {
          skillItem = depItem.dependsOn;
          crossProj = depItem.crossProject;
        } else {
          const found = this.dependencies.find((item) => this.getNodeId(item.skill) === skillId);
          if (found) {
            skillItem = found.skill;
            crossProj = found.crossProject;
          }
        }

        return { ...skillItem, ...{ isCrossProject: crossProj } };
      },
      buildData(vis) {
        const nodes = new vis.DataSet();
        const edges = new vis.DataSet();
        const createdSkillIds = [];

        this.dependencies.forEach((item) => {
          const isThisSkill = !item.crossProject && this.$route.params.skillId === item.skill.skillId;

          if (isThisSkill) {
            this.thisSkill = item.skill;
          }

          const extraParentProps = isThisSkill ? {
            color: {
              border: '#3273dc',
              background: 'lightblue',
            },
          } : {};
          this.buildNode(item.skill, false, createdSkillIds, nodes, extraParentProps);

          const extraChildProps = item.achieved ? {
            color: {
              border: 'green',
              background: 'lightgreen',
            },
          } : {};
          this.buildNode(item.dependsOn, item.crossProject, createdSkillIds, nodes, extraChildProps);
          edges.add({
            from: this.getNodeId(item.skill),
            to: this.getNodeId(item.dependsOn),
            arrows: 'to',
          });
        });

        const data = { nodes, edges };
        return data;
      },
      buildNode(skill, isCrossProject, createdSkillIds, nodes, extraProps = {}) {
        if (!createdSkillIds.includes(skill.skillId)) {
          createdSkillIds.push(skill.skillId);

          const node = {
            id: this.getNodeId(skill),
            label: this.getLabel(skill, isCrossProject),
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
      getLabel(skill, isCrossProject) {
        const label = isCrossProject ? `CROSS-PROJECT SKILL\n<b>${skill.projectName}</b>\n${skill.skillName}` : skill.skillName;
        return label;
      },
    },
  };
</script>

<style scoped>
    #dependent-skills-network {
        margin-bottom: 2rem;
    }

    @media (min-width: 721px) {
        .legend-component {
            max-width: 18rem;
            min-width: 14rem;
        }
        .legend-row {
            position: absolute;
            z-index: 99;
            width: 100%;
        }
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
        font-size: 2em;
        color: gray;
        font-family: "Font Awesome 5 Free";
    }

    @media screen and (max-width: 720px) {
        #dependent-skills-network .vis-button {
            display: none;
        }
    }

    #dependent-skills-network .vis-button:hover:after {
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
