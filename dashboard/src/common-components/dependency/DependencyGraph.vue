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
import { computed, nextTick, onBeforeUnmount, ref, useTemplateRef, watch } from 'vue'
import { useFullscreen, useStorage } from '@vueuse/core'
import { Network } from 'vis-network'
import { DataSet } from 'vis-data'
import dagre from '@dagrejs/dagre'
import GraphUtils from '@/components/skills/dependencies/GraphUtils.js'
import GraphLegend from '@/components/skills/dependencies/GraphLegend.vue'
import GraphControls from '@/components/skills/dependencies/GraphControls.vue'
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import { useDependencyNavigation } from '@/common-components/dependency/UseDependencyNavigation.js'
import { usePluralize } from '@/components/utils/misc/UsePluralize.js'

const props = defineProps({
  graph: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  isLoading: { type: Boolean, default: false },
  projectId: { type: String, required: true },
  editable: { type: Boolean, default: false },
  mode: { type: String, default: 'admin' },
})
const emit = defineEmits(['dataChanged', 'edgeSelected', 'fullscreenChanged', 'nodeSelected'])

const themeHelper = useThemesHelper()
const themeState = useSkillsDisplayThemeState()
const dependencyNavigation = useDependencyNavigation()
const pluralize = usePluralize()
const graphTemplate = useTemplateRef('fullDepsSkillsGraphContainer')
const dependencyGraph = useTemplateRef('dependencyGraph')
const { isFullscreen, toggle } = useFullscreen(graphTemplate)
const enableZoom = useStorage('learningPath-enableZoom', true)
const enableAnimations = useStorage('learningPath-enableAnimations', true)
const horizontalOrientation = useStorage('learningPath-horizontalOrientation', false)
const dynamicHeight = useStorage('learningPath-dynamicHeight', false)
const showGraph = ref(false)
const data = ref({})
const dataHeight = ref(0)
const dataWidth = ref(0)
const hasGraphData = computed(() => !!props.graph?.nodes?.length)
const nodeSize = 65
const nodes = new DataSet()
const edges = new DataSet()
let network = null
let graphResizeObserver = null
let resizeToFit = false

const isSkillsDisplayMode = computed(() => props.mode === 'skills-display')
const isAdminMode = computed(() => props.mode === 'admin')
const showLegend = computed(() => isAdminMode.value || (isSkillsDisplayMode.value &&
  props.graph?.nodes?.some((node) => node.type === 'Skill') &&
  props.graph?.nodes?.some((node) => node.type === 'Badge')))
const skillColor = computed(() => isSkillsDisplayMode.value ? themeState.graphSkillColor : 'lightgreen')
const badgeColor = computed(() => isSkillsDisplayMode.value ? themeState.graphBadgeColor : '#88a9fc')
const legendItems = computed(() => [
  { label: 'Skill', color: skillColor.value, iconClass: 'fa-graduation-cap' },
  { label: 'Badge', color: badgeColor.value, iconClass: 'fa-award' },
])
const displayOptions = {
  layout: { hierarchical: { enabled: false } },
  interaction: { selectConnectedEdges: false, navigationButtons: true, selectable: true, hover: isSkillsDisplayMode.value },
  physics: { enabled: false },
  edges: { smooth: { enabled: false } },
  nodes: {
    font: { size: 18 },
    color: { border: 'green', background: 'lightgreen' },
  },
}
const showProgress = computed(() => isSkillsDisplayMode.value && (props.graph?.edges?.length || 0) > 0)
const totalItems = computed(() => showProgress.value ? (props.graph?.nodes?.length || 0) : 0)
const completedItems = computed(() => showProgress.value ? (props.graph?.nodes?.filter((node) => node.achieved).length || 0) : 0)
const percentComplete = computed(() => totalItems.value ? Math.round((completedItems.value / totalItems.value) * 100) : 0)
const containerWidth = computed(() => graphTemplate.value?.offsetWidth || 0)
const computedHeight = computed(() => {
  if (containerWidth.value > dataWidth.value) {
    return horizontalOrientation.value
      ? Math.max(dataHeight.value, 700)
      : Math.max(dataHeight.value, 500)
  }
  const scaledHeight = dataWidth.value ? dataHeight.value * (containerWidth.value / dataWidth.value) : 500
  return Math.max(scaledHeight, 500)
})

const getNodeById = (id) => nodes.get(id)
const getNodeBySkillId = (skillId) => nodes.get({ filter: (node) => node.details.skillId === skillId })[0]

const navigate = (item) => dependencyNavigation.navigate(item, props.mode === 'skills-display')
const panToNode = (nodeId, scrollIntoView = false) => {
  if (!network) return
  network.focus(nodeId, { scale: 1, animation: enableAnimations.value })
  if (scrollIntoView) dependencyGraph.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const buildNode = (node) => {
  const isCrossProject = node.projectId !== props.projectId
  const newNode = {
    id: node.id,
    label: GraphUtils.getLabel(node, isCrossProject),
    margin: { top: isCrossProject ? 40 : 25 },
    shape: 'icon',
    icon: {
      face: '"Font Awesome 5 Free"',
      code: '\uf19d',
      weight: '900',
      size: 50,
      color: skillColor.value,
    },
    chosen: false,
    details: node,
    font: { multi: 'html', size: 20 },
    title: GraphUtils.getTitle(node, isCrossProject),
  }
  if (themeHelper.isDarkTheme) newNode.font.color = '#f5f9ff'
  if (node.achieved) {
    newNode.font.color = themeState.graphAchievedColor
    newNode.label = `${newNode.label} <b>✓</b>`
  }
  if (node.type === 'Badge') {
    newNode.icon.code = '\uf559'
    newNode.icon.color = badgeColor.value
  }
  if (node.belongsToBadge) newNode.icon.color = badgeColor.value
  return newNode
}

const layout = () => {
  if (nodes.length <= 1 || edges.length === 0) return
  const graphLayout = new dagre.graphlib.Graph()
  graphLayout.setGraph({
    rankdir: horizontalOrientation.value ? 'LR' : 'TB',
    nodesep: nodeSize * 3,
    edgesep: nodeSize,
    ranksep: nodeSize * (horizontalOrientation.value ? 4 : 2),
  })
  graphLayout.setDefaultEdgeLabel(() => ({}))
  nodes.forEach((node) => graphLayout.setNode(node.id, { label: node.label, width: nodeSize, height: nodeSize }))
  edges.forEach((edge) => graphLayout.setEdge(edge.from, edge.to))
  dagre.layout(graphLayout)
  graphLayout.nodes().forEach((nodeId) => {
    const node = nodes.get(Number(nodeId))
    if (node) nodes.update({ ...node, x: graphLayout.node(nodeId).x, y: graphLayout.node(nodeId).y })
  })
  dataHeight.value = graphLayout?._label?.height || 0
  dataWidth.value = graphLayout?._label?.width || 0
}

const buildData = () => {
  nodes.clear()
  edges.clear()
  ;[...(props.graph?.nodes || [])].sort((a, b) => a.id - b.id).forEach((node) => nodes.add(buildNode(node)))
  ;[...(props.graph?.edges || [])].sort((a, b) => a.toId - b.toId).forEach((edge) => {
    edges.add({
      from: edge.toId,
      to: edge.fromId,
      arrows: 'to',
      title: props.editable ? 'Click to remove this path' : undefined,
    })
  })
  layout()
  data.value = { nodes, edges }
  emit('dataChanged', data.value)
}

const setVisNetworkTabIndex = () => {
  nextTick(() => dependencyGraph.value?.querySelector('.vis-network')?.setAttribute('tabindex', -1))
}

const createGraph = () => {
  graphResizeObserver?.disconnect()
  resizeToFit = false
  network?.destroy()
  network = null
  buildData()
  showGraph.value = hasGraphData.value
  if (!hasGraphData.value || !dependencyGraph.value || !dependencyGraph.value.isConnected) return
  network = new Network(dependencyGraph.value, data.value, displayOptions)
  graphResizeObserver = new ResizeObserver(() => {
    if (resizeToFit && dependencyGraph.value?.clientWidth && dependencyGraph.value?.clientHeight) {
      resizeToFit = false
      requestAnimationFrame(fitNetworkToScreen)
    }
  })
  graphResizeObserver.observe(dependencyGraph.value)

  if (isSkillsDisplayMode.value) {
    const networkCanvas = dependencyGraph.value.querySelector('canvas')
    network.on('hoverNode', () => {
      networkCanvas.style.cursor = 'pointer'
    })
    network.on('blurNode', () => {
      networkCanvas.style.cursor = 'default'
    })
    network.on('click', ({ nodes: selectedNodes }) => {
      const selectedNode = selectedNodes?.length ? getNodeById(selectedNodes[0]) : null
      if (!selectedNode) return
      const skillItem = selectedNode.details
      navigate(skillItem)
    })
  }

  if (isAdminMode.value) {
    network.on('selectNode', ({ nodes: selectedNodes }) => {
      const selectedNode = getNodeById(selectedNodes[0])
      if (!selectedNode) return
      if (enableZoom.value) panToNode(selectedNode.id, dynamicHeight.value)
      emit('nodeSelected', selectedNode.details)
    })
  }
  if (props.editable) {
    network.on('selectEdge', ({ edges: selectedEdges }) => {
      const connectedNodes = network.getConnectedNodes(selectedEdges[0])
      const fromNode = getNodeById(connectedNodes[0])
      const toNode = getNodeById(connectedNodes[1])
      if (fromNode && toNode && !fromNode.details.belongsToBadge && !toNode.details.belongsToBadge) {
        emit('edgeSelected', { fromNode: fromNode.details, toNode: toNode.details })
      }
    })
  }
  network.fit()
  setVisNetworkTabIndex()
}
const updateGraph = async () => {
  // The initial graph replaces the empty-state markup; wait for its container to mount.
  await nextTick()
  createGraph()
}

const refresh = (skillId = null) => {
  createGraph()
  if (skillId && enableZoom.value) {
    const node = getNodeBySkillId(skillId)
    if (node) panToNode(node.id)
  }
}
const fitNetworkToScreen = () => {
  if (!network || !dependencyGraph.value) return
  network.setSize('100%', '100%')
  network.fit({ animation: false })
}
const toggleFullscreen = async () => {
  resizeToFit = !!network
  try {
    await toggle()
  } catch (error) {
    resizeToFit = false
    throw error
  }
}
const toggleOrientation = () => {
  horizontalOrientation.value = !horizontalOrientation.value
  buildData()
  fitNetworkToScreen()
}

watch(() => props.graph, updateGraph, { immediate: true })
watch([skillColor, badgeColor], () => nextTick(createGraph))
watch(isFullscreen, (fullscreen) => emit('fullscreenChanged', fullscreen))
onBeforeUnmount(() => {
  graphResizeObserver?.disconnect()
  network?.destroy()
})
defineExpose({ fitNetworkToScreen, panToNode, refresh })
</script>

<template>
  <SkillsOverlay :show="isLoading && !isFullscreen">
    <Card data-cy="fullDepsSkillsGraph" class="mb-6">
      <template #content>
        <div
          id="fullDepsSkillsGraphContainer"
          ref="fullDepsSkillsGraphContainer"
          class="flex flex-col"
          :style="!isFullscreen && dynamicHeight ? { height: `${computedHeight}px` } : undefined">
          <slot name="fullscreen-header" :is-fullscreen="isFullscreen" />
          <SkillsOverlay :show="isLoading && isFullscreen" class="flex flex-col flex-1 min-h-0">
            <div v-if="!hasGraphData && !isLoading" class="my-8">
              <NoContent2
                icon="fa fa-project-diagram"
                title="No Learning Path Yet..."
                :message="isAdminMode ? `Here you can create and manage the project's Learning Path.` : `Here you can view the project's Learning Path, which may consist of skills and badges.`" />
            </div>
            <div class="w-full px-2" :class="isFullscreen ? 'pt-4' : ''">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div v-if="showProgress" class="mb-4 w-full max-w-xs rounded-lg border border-surface-200 dark:border-surface-700 bg-surface-0 dark:bg-surface-900 px-3 py-2 shadow-sm" data-cy="learningPathProgressSummary">
                    <div class="flex items-center justify-between gap-4 text-sm">
                      <span class="text-surface-600 dark:text-surface-300" data-cy="learningPathProgressCount">{{ completedItems }} of {{ totalItems }} {{ pluralize.plural('item', totalItems) }} achieved</span>
                      <span class="shrink-0 font-semibold sd-theme-primary-color" data-cy="learningPathProgressPercent">{{ percentComplete }}%</span>
                    </div>
                    <VerticalProgressBar class="mt-2" :total-progress="percentComplete" :bar-size="6" :disable-daily-color="true" :aria-label="`${completedItems} of ${totalItems} ${pluralize.plural('item', totalItems)} achieved`" data-cy="learningPathProgressBar" />
                  </div>
                  <GraphLegend v-if="showLegend" class="graph-legend deps-overlay" :items="legendItems" />
                </div>
                <div id="additionalControls" class="flex shrink-0 items-center gap-2">
                  <GraphControls
                    :is-fullscreen="isFullscreen"
                    :is-admin-mode="isAdminMode"
                    :enable-zoom="enableZoom"
                    :enable-animations="enableAnimations"
                    :horizontal-orientation="horizontalOrientation"
                    :enable-dynamic-height="dynamicHeight"
                    :disabled="isLoading"
                    @toggle-zoom="enableZoom = !enableZoom"
                    @toggle-orientation="toggleOrientation"
                    @toggle-animations="enableAnimations = !enableAnimations"
                    @toggle-dynamic-height="dynamicHeight = !dynamicHeight"
                    @toggle-fullscreen="toggleFullscreen" />
                </div>
              </div>
            </div>
            <div id="dependency-graph" ref="dependencyGraph" :style="{ visibility: showGraph ? 'visible' : 'hidden' }" :class="{ fullscreen: isFullscreen }" />
          </SkillsOverlay>
        </div>
      </template>
    </Card>
  </SkillsOverlay>
</template>

<style>
#dependency-graph div.vis-network div.vis-navigation div.vis-button {
  background-image: none !important;
}
#dependency-graph div.vis-network div.vis-navigation div.vis-button:hover { box-shadow: none !important; }
#dependency-graph .vis-button { font-size: 2em; color: #8c8c8c; font-family: "Font Awesome 5 Free"; }
@media screen and (max-width: 720px) { #dependency-graph .vis-button { display: none; } }
#dependency-graph .vis-button:hover:after { color: #3273dc; }
#dependency-graph .vis-button.vis-up:after { content: '\f35b'; }
#dependency-graph .vis-button.vis-down:after { content: '\f358'; }
#dependency-graph .vis-button.vis-left:after { content: '\f359'; }
#dependency-graph .vis-button.vis-right:after { content: '\f35a'; }
#dependency-graph .vis-button.vis-zoomIn:after { content: '\f0fe'; }
#dependency-graph .vis-button.vis-zoomOut:after { content: '\f146'; }
#dependency-graph .vis-button.vis-zoomExtends:after { content: '\f78c'; font-weight: 900; font-size: 30px; }
.deps-overlay { z-index: 99; }
:fullscreen, ::backdrop { background-color: rgba(255, 255, 255, 1); }
#dependency-graph { flex: 1 1 0; min-height: 0; overflow: hidden; }
.vis-navigation { background-color: white; position: absolute; top: 30px; right: 0; }
.fullscreen > .vis-network > .vis-navigation { right: 15px !important; }
#fullDepsSkillsGraphContainer { height: 31.25rem; min-height: 31.25rem; }
#fullDepsSkillsGraphContainer:fullscreen { height: 100vh; }
#additionalControls { z-index: 999; }
</style>
