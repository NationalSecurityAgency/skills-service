/*
Copyright 2024 SkillTree

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
<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Network } from 'vis-network'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import GraphLegend from '@/skills-display/components/skill/prerequisites/GraphLegend.vue'
import GraphUtils from '@/components/skills/dependencies/GraphUtils';
import UserPrerequisitesProgress from '@/skills-display/components/skill/prerequisites/UserPrerequisitesProgress.vue'
import PrerequisitesTable from '@/skills-display/components/skill/prerequisites/PrerequisitesTable.vue'
import { useNavToSkillUtil } from '@/skills-display/components/skill/prerequisites/UseNavToSkillUtil.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const props = defineProps({
  dependencies: Array
})

const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const navHelper = useNavToSkillUtil()

const loadingData = ref(true)
const thisSkill = ref({})
const dependenciesInternal = ref([])
const network = ref(null)
const displayOptions = {
  layout: {
    randomSeed: 419465,
    hierarchical: {
      enabled: true,
      sortMethod: 'directed',
      nodeSpacing: 350
    }
  },
  interaction: {
    navigationButtons: true,
    hover: true
  },
  physics: {
    enabled: false
  },
  nodes: {
    font: {
      size: 18
    },
    color: {
      border: '#868686',
      background: '#e4e4e4'
    },
    mass: 20
  }
}

onMounted(() => {
  loadAllDataForPrereq()
})

watch( () => route.params.skillId, () => {
  loadAllDataForPrereq()
});

const loadAllDataForPrereq  = () => {
  loadData().then(() => {
    if (dependenciesInternal.value?.length > 0) {
      initInternalDeps()
      createGraph()
    }
  })
}


const isDependency = () => {
  const routeName = route.name
  return routeName === 'crossProjectSkillDetails' || routeName === 'crossProjectSkillDetailsUnderBadge'
}

const loadData = () => {
  loadingData.value = true
  if (!route.params.crossProjectId) {
    let lookupId = isDependency() ? route.params.dependentSkillId : route.params.skillId
    if (!route.params.skillId && route.params.badgeId) {
      lookupId = route.params.badgeId
    }
    return skillsDisplayService.getSkillDependencies(lookupId)
      .then((res) => {
        dependenciesInternal.value = res.dependencies
        loadingData.value = false
      })
  } else {
    loadingData.value = false
    return Promise.resolve()
  }
}


const getNodeId = (skill) => {
  return appendForId(skill.projectId, skill.skillId)
}
const appendForId = (projectId, skillId) => {
  return `${projectId}_${skillId}`
}

const initInternalDeps = () => {
  const idForThisSkill = route.params.skillId ? appendForId(attributes.projectId, route.params.skillId) : null
  const idForThisBadge = route.params.badgeId ? appendForId(attributes.projectId, route.params.badgeId) : null
  dependenciesInternal.value = dependenciesInternal.value.map((item) => {
    const copy = { ...item }
    copy.dependsOn.id = getNodeId(copy.dependsOn)
    copy.dependsOn.isThisSkill = false

    copy.skill.id = getNodeId(copy.skill)
    copy.skill.isThisSkill = idForThisSkill === copy.skill.id
    copy.skill.isThisBadge = idForThisBadge === copy.skill.id

    if (copy.skill.isThisSkill || copy.skill.isThisBadge) {
      thisSkill.value = copy.skill
    }

    return copy
  })
}


const clearNetwork = () => {
  if (network.value) {
    return network.value.destroy()
  }
  return true
}
const isSmallScreen = () => {
  const width = window.innerWidth
  return width <= 768
}
const createGraph = () => {
  clearNetwork()

  const data = buildData()
  const container = document.getElementById('dependent-skills-network')
  network.value = new Network(container, data, displayOptions)
  network.value.on('click', (params) => {
    const skillItem = locateSelectedSkill(params)
    navHelper.navigateToSkill(skillItem);
  })
  const networkCanvas = container.getElementsByTagName('canvas')[0]
  network.value.on('hoverNode', () => {
    networkCanvas.style.cursor = 'pointer'
  })
  network.value.on('blurNode', () => {
    networkCanvas.style.cursor = 'default'
  })

  focusOnParentNode()
  setVisNetworkTabIndex();
  applyThemeToNavigationControls();
}

const setVisNetworkTabIndex = () => {
  setTimeout(() => {
    const elements = document.getElementsByClassName('vis-network');
    if (elements && elements.length === 1) {
      const element = elements[0];
      if (element) {
        element.setAttribute('tabindex', -1);
      }
    }
  }, 500);
}
const applyThemeToNavigationControls = () => {
  setTimeout(() => {
    const themePrimaryColor = themeState.graphTextPrimaryColor
    const themeNavButtonsColor = themeState.graphNavButtonsColor
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
}
const focusOnParentNode = () => {
  if (isSmallScreen() || dependenciesInternal.value.length > 5) {
    const options = {
      scale: 0.7,
      offset: { x: 0, y: 180 },
      animation: {
        duration: 1200,
        easingFunction: 'easeInOutQuad'
      }
    }
    const nodeIdToFocus = getNodeId(thisSkill.value)
    network.value.focus(nodeIdToFocus, options)
  }
}
const locateSelectedSkill = (params) => {
  const nodeId = params.nodes[0]
  let skillItem = null
  let crossProj = false
  const depItem = dependenciesInternal.value.find((item) => item.dependsOn && item.dependsOn.id === nodeId)

  if (depItem) {
    skillItem = depItem.dependsOn
    crossProj = depItem.crossProject
  } else {
    const found = dependenciesInternal.value.find((item) => item.skill.id === nodeId)
    if (found) {
      skillItem = found.skill
      crossProj = found.crossProject
    }
  }

  return { ...skillItem, ...{ isCrossProject: crossProj } }
}
const buildData = () => {
  const nodes = []
  const edges = []
  const createdSkillIds = []
  const onlyUnique = (value, index, array) => array.indexOf(value) === index
  const achievedIds = dependenciesInternal.value.filter((dep) => dep.achieved).map((dep) => dep.dependsOn.id).filter(onlyUnique)
  dependenciesInternal.value.forEach((item) => {
    const extraParentProps = item.skill.isThisSkill ? {
      color: {
        border: '#3273dc',
        background: 'lightblue'
      }
    } : {}
    buildNode(item.skill, false, createdSkillIds, nodes, achievedIds, extraParentProps)

    const extraChildProps = item.achieved ? {
      color: {
        border: themeState.graphAchievedColor
      }
    } : {}
    if (item.dependsOn) {
      buildNode(item.dependsOn, item.crossProject, createdSkillIds, nodes, achievedIds, extraChildProps)
      edges.push({
        from: getNodeId(item.dependsOn),
        to: getNodeId(item.skill),
        arrows: 'to'
      })
    }
  })

  const data = { nodes, edges }
  return data
}
const buildNode = (skill, isCrossProject, createdSkillIds, nodes, achievedIds, extraProps = {}) => {
  if (!createdSkillIds.includes(skill.id)) {
    createdSkillIds.push(skill.id)
    const skillColor = skill.isThisSkill ? themeState.graphThisSkillColor : themeState.graphSkillColor
    const isAchieved = achievedIds.includes(skill.id)
    let label = isCrossProject ? `Shared from\n<b>${skill.projectName}</b>\n${GraphUtils.truncate(skill.skillName)}` : GraphUtils.truncate(skill.skillName)
    if (skill.isThisSkill) {
      label = `<b>This Skill</b>\n${label}`
    } else if (skill.isThisBadge) {
      label = `<b>This Badge</b>\n${label}`
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
        color: skillColor
      },
      chosen: !skill.isThisSkill,
      font: { multi: 'html', size: 20 }
    }

    const themePrimaryColor = themeState.graphTextPrimaryColor
    if (themePrimaryColor) {
      node.font.color = themePrimaryColor
    } else if(themeHelper.isDarkTheme) {
      node.font.color = '#f5f9ff'
    }

    if (isAchieved) {
      node.font.color = themeState.graphAchievedColor
      node.label = `${node.label} <b>✓</b>`
    }

    if (skill.type === 'Badge') {
      node.shape = 'icon'
      node.icon.code = '\uf559'
      node.icon.color = themeState.graphBadgeColor
    }
    if (skill.isThisSkill || skill.isThisBadge) {
      node.margin = { top: 25 }
    }
    if (isCrossProject) {
      node.margin = { top: 40 }
    }
    const res = Object.assign(node, extraProps)
    nodes.push(res)
  }
}


</script>

<template>
  <Card v-if="!loadingData && dependenciesInternal?.length > 0"
        :pt="{ content: { class: 'p-0' }, body: {class: 'p-0'} }"
        data-cy="prerequisitesCard" class="mt-4">
    <template #content>
      <div class="pt-4 px-4">
        <div class="flex flex-wrap gap-4">
          <div class="flex-1 w-min-16rem">
            <graph-legend />
          </div>

          <div class="">
            <user-prerequisites-progress :dependencies="dependenciesInternal"/>
          </div>
        </div>
        <div id="dependent-skills-network" style="height: 500px" aria-hidden="true"></div>
      </div>

      <prerequisites-table :items="dependenciesInternal" />
    </template>
  </Card>
</template>

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