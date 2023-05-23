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
        <div class="card-body p-0">
          <div class="pt-3 px-3">
            <div class="row legend-row">
                <div class="col-12 mb-2 m-sm-0 col-sm-6">
                    <graph-legend :items="legendItems" class="legend-component deps-overlay"/>
                </div>

                <div class="col-12 col-sm-6">
                    <skill-dependency-summary
                        v-if="dependencies && dependencies.length > 0"
                        :dependencies="dependencies"
                        class="legend-component float-md-right deps-overlay"/>
                </div>
            </div>
            <div id="dependent-skills-network" style="height: 500px" aria-hidden="true"></div>
          </div>

          <dependencies-details :prerequisites-links="dependencies" />
        </div>
    </div>
</template>

<script>
  import 'vis-network/styles/vis-network.css';
  import { Network } from 'vis-network';
  import GraphLegend from '@/userSkills/skill/dependencies/GraphLegend';
  import SkillDependencySummary from '@/userSkills/skill/dependencies/SkillDependencySummary';
  import DependenciesDetails from '@/userSkills/skill/dependencies/DependenciesDetails';
  import SkillNavigationMixin from '@/userSkills/skill/dependencies/SkillNavigationMixin';
  import PrerequisiteColorsMixin from '@/userSkills/skill/dependencies/PrerequisiteColorsMixin';

  export default {
    name: 'SkillDependencies',
    mixins: [SkillNavigationMixin, PrerequisiteColorsMixin],
    components: {
      DependenciesDetails,
      SkillDependencySummary,
      GraphLegend,
    },
    props: {
      dependencies: Array,
      skillId: String,
      subjectId: String,
    },
    data() {
      return {
        thisSkill: {},
        dependenciesInternal: [],
        network: null,
        legendItems: [
          { label: 'Skill', color: this.getSkillColor(), iconClass: 'fa-graduation-cap' },
          { label: 'Badge', color: this.getBadgeColor(), iconClass: 'fa-award' },
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
      this.initInternalDeps();
      this.createGraph();
    },
    beforeDestroy() {
      this.cleanUp();
    },
    methods: {
      setVisNetworkTabIndex() {
        setTimeout(() => {
          const elements = document.getElementsByClassName('vis-network');
          if (elements && elements.length === 1) {
            const element = elements[0];
            if (element) {
              element.setAttribute('tabindex', -1);
            }
          }
        }, 500);
      },
      applyThemeToNavigationControls() {
        setTimeout(() => {
          const themePrimaryColor = this.getThemeTextPrimaryColor();
          const themeNavButtonsColor = this.getThemeNavButtonsColor();
          if (themePrimaryColor || themeNavButtonsColor) {
            const networkElement = document.getElementById('dependent-skills-network');
            if (networkElement) {
              const buttons = document.getElementsByClassName('vis-button');
              if (buttons && buttons.length > 0) {
                for (let i = 0; i < buttons.length; i += 1) {
                  const button = buttons[i];
                  button.style.color = themeNavButtonsColor || themePrimaryColor;
                }
              }
            }
          }
        }, 500);
      },
      initInternalDeps() {
        const idForThisSkill = this.appendForId(this.$store.state.projectId, this.$route.params.skillId);
        this.dependenciesInternal = this.dependencies.map((item) => {
          const copy = { ...item };
          copy.dependsOn.id = this.getNodeId(copy.dependsOn);
          copy.dependsOn.isThisSkill = false;

          copy.skill.id = this.getNodeId(copy.skill);
          copy.skill.isThisSkill = idForThisSkill === copy.skill.id;

          if (copy.skill.isThisSkill) {
            this.thisSkill = copy.skill;
          }

          return copy;
        });
      },
      cleanUp() {
        this.clearNetwork();
        this.thisSkill = {};
      },
      clearNetwork() {
        if (this.network) {
          this.network.destroy();
        }
      },
      isSmallScreen() {
        const width = window.innerWidth;
        return width <= 768;
      },
      createGraph() {
        this.clearNetwork();

        const data = this.buildData();
        const container = document.getElementById('dependent-skills-network');
        this.network = new Network(container, data, this.displayOptions);
        this.network.on('click', (params) => {
          const skillItem = this.locateSelectedSkill(params);
          this.navigateToSkill(skillItem);
        });
        const networkCanvas = container.getElementsByTagName('canvas')[0];
        this.network.on('hoverNode', () => {
          networkCanvas.style.cursor = 'pointer';
        });
        this.network.on('blurNode', () => {
          networkCanvas.style.cursor = 'default';
        });

        this.focusOnParentNode();
        this.setVisNetworkTabIndex();
        this.applyThemeToNavigationControls();
      },
      focusOnParentNode() {
        if (this.isSmallScreen() || this.dependencies.length > 5) {
          const options = {
            scale: 0.7,
            offset: { x: 0, y: 180 },
            animation: {
              duration: 1200,
              easingFunction: 'easeInOutQuad',
            },
          };
          const nodeIdToFocus = this.getNodeId(this.thisSkill);
          this.network.focus(nodeIdToFocus, options);
        }
      },
      locateSelectedSkill(params) {
        const nodeId = params.nodes[0];
        let skillItem = null;
        let crossProj = false;
        const depItem = this.dependenciesInternal.find((item) => item.dependsOn && item.dependsOn.id === nodeId);

        if (depItem) {
          skillItem = depItem.dependsOn;
          crossProj = depItem.crossProject;
        } else {
          const found = this.dependenciesInternal.find((item) => item.skill.id === nodeId);
          if (found) {
            skillItem = found.skill;
            crossProj = found.crossProject;
          }
        }

        return { ...skillItem, ...{ isCrossProject: crossProj } };
      },
      buildData() {
        const nodes = [];
        const edges = [];
        const createdSkillIds = [];
        const onlyUnique = (value, index, array) => array.indexOf(value) === index;
        const achievedIds = this.dependenciesInternal.filter((dep) => dep.achieved).map((dep) => dep.dependsOn.id).filter(onlyUnique);
        this.dependenciesInternal.forEach((item) => {
          const extraParentProps = item.skill.isThisSkill ? {
            color: {
              border: '#3273dc',
              background: 'lightblue',
            },
          } : {};
          this.buildNode(item.skill, false, createdSkillIds, nodes, achievedIds, extraParentProps);

          const extraChildProps = item.achieved ? {
            color: {
              border: this.getAchievedColor(),
            },
          } : {};
          if (item.dependsOn) {
            this.buildNode(item.dependsOn, item.crossProject, createdSkillIds, nodes, achievedIds, extraChildProps);
            edges.push({
              from: this.getNodeId(item.dependsOn),
              to: this.getNodeId(item.skill),
              arrows: 'to',
            });
          }
        });

        const data = { nodes, edges };
        return data;
      },
      buildNode(skill, isCrossProject, createdSkillIds, nodes, achievedIds, extraProps = {}) {
        if (!createdSkillIds.includes(skill.id)) {
          createdSkillIds.push(skill.id);
          const skillColor = skill.isThisSkill ? this.getThisSkillColor() : this.getSkillColor();
          const isAchieved = achievedIds.includes(skill.id);
          let label = isCrossProject ? `Shared from\n<b>${skill.projectName}</b>\n${skill.skillName}` : skill.skillName;
          if (skill.isThisSkill) {
            label = `<b>This Skill</b>\n${label}`;
          }
          const node = {
            id: skill.id,
            label,
            margin: 10,
            shape: 'icon',
            icon: {
              face: '"Font Awesome 5 Free"',
              code: '\uf19d',
              weight: '900',
              size: 50,
              color: skillColor,
            },
            chosen: !skill.isThisSkill,
            font: { multi: 'html', size: 20 },
          };

          const themePrimaryColor = this.getThemeTextPrimaryColor();
          if (themePrimaryColor) {
            node.font.color = themePrimaryColor;
          }

          if (isAchieved) {
            node.font.color = this.getAchievedColor();
            node.label = `${node.label} <b>✓</b>`;
          }

          if (skill.type === 'Badge') {
            node.shape = 'icon';
            node.icon.code = '\uf559';
            node.icon.color = this.getBadgeColor();
          }
          if (skill.isThisSkill) {
            node.margin = { top: 25 };
          }
          if (isCrossProject) {
            node.margin = { top: 40 };
          }
          const res = Object.assign(node, extraProps);
          nodes.push(res);
        }
      },
      getNodeId(skill) {
        return this.appendForId(skill.projectId, skill.skillId);
      },
      appendForId(projectId, skillId) {
        return `${projectId}_${skillId}`;
      },
      isDependency() {
        const routeName = this.$route.name;
        return routeName === 'crossProjectSkillDetails';
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
            width: 100%;
        }
        .deps-overlay {
            z-index: 99;
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

    #dependent-skills-network .vis-button {
        font-size: 2em;
        color: #8c8c8c;
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
