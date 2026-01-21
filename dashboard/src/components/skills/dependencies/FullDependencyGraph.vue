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
import { ref, onMounted, computed, onBeforeUnmount, useTemplateRef } from 'vue';
import {useFullscreen, useStorage} from '@vueuse/core';
import { useRoute } from 'vue-router';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import PrerequisiteSelector from '@/components/skills/dependencies/PrerequisiteSelector.vue';
import SkillsService from '@/components/skills/SkillsService';
import GraphUtils from '@/components/skills/dependencies/GraphUtils';
import GraphLegend from '@/components/skills/dependencies/GraphLegend.vue';
import NoContent2 from "@/components/utils/NoContent2.vue";
import DependencyTable from "@/components/skills/dependencies/DependencyTable.vue";
import ShareSkillsWithOtherProjects from "@/components/skills/crossProjects/ShareSkillsWithOtherProjects.vue";
import SharedSkillsFromOtherProjects from "@/components/skills/crossProjects/SharedSkillsFromOtherProjects.vue";
import { useProjConfig } from '@/stores/UseProjConfig.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import Accordion from 'primevue/accordion';
import AccordionPanel from 'primevue/accordionpanel';
import AccordionHeader from 'primevue/accordionheader';
import AccordionContent from 'primevue/accordioncontent';
import dagre from "@dagrejs/dagre"
import GraphControls from "@/components/skills/dependencies/GraphControls.vue";

const dialogMessages = useDialogMessages()
const projConfig = useProjConfig();
const route = useRoute();
const themeHelper = useThemesHelper()
const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);
const graphTemplate = useTemplateRef('fullDepsSkillsGraphContainer')
const { isFullscreen, enter, exit, toggle } = useFullscreen(graphTemplate)

const nodeSize = 65
const isLoading = ref(true);
const showGraph = ref(true);
const selectedFromSkills = ref({});
const data = ref([]);
const graph = ref({});
let network = null;
const enableZoom = useStorage('learningPath-enableZoom', true);
const enableAnimations = useStorage('learningPath-enableAnimations', true);
const horizontalOrientation = useStorage('learningPath-horizontalOrientation', false);
const dynamicHeight = useStorage('learningPath-dynamicHeight', false);
const fullDepsSkillsGraph = ref()
let nodes = new DataSet();
let edges = new DataSet();
const legendItems = [
  { label: 'Skill', color: 'lightgreen', iconClass: 'fa-graduation-cap' },
  { label: 'Badge', color: '#88a9fc', iconClass: 'fa-award' },
];
const dependencyGraph = ref()
const displayOptions = ref({
  layout: {
    hierarchical: {
      enabled: false,
    },
  },
  interaction: {
    selectConnectedEdges: false,
    navigationButtons: true,
    selectable: true,
  },
  physics: {
    enabled: false,
  },
  edges: {
    smooth: {
      enabled: false
    }
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
});

onMounted(() => {
  nodes.clear();
  edges.clear();
  loadGraphDataAndCreateGraph();
})

onBeforeUnmount(() => {
  if (network) {
    network.destroy();
  }
})


const getNodeBySkillId = (skillId) => {
  const foundNode = nodes.get({
    filter: (node) => {
      return node.details.skillId === skillId
    }
  })
  if(foundNode && foundNode.length > 0) {
    return foundNode[0];
  } else {
    return null;
  }
}

const getNodeById = (id) => {
  const foundNode = nodes.get({
    filter: (node) => {
      return node.id === id
    }
  })
  if(foundNode && foundNode.length > 0) {
    return foundNode[0];
  } else {
    return null;
  }
}

const hasGraphData = computed(() => {
  return graph.value && graph.value.nodes && graph.value.nodes.length > 0;
});

const updateSelectedFromSkills = (item, zoomTo = true) => {
  if(item) {
    selectedFromSkills.value = item;
    const foundNode = getNodeBySkillId(item.skillId)
    if(foundNode && zoomTo && enableZoom.value) {
      panToNode(foundNode.id)
    }
  }
  else {
    clearSelectedFromSkills();
  }
};

const clearSelectedFromSkills = () => {
  selectedFromSkills.value = {};
  if(network) {
    network.unselectAll()
  }
};

const handleUpdate = (addedNode) => {
  graph.value = null;
  edges.clear()
  clearSelectedFromSkills();
  isLoading.value = true;

  loadGraphDataAndUpdateGraph(addedNode);
};

const loadGraphDataAndUpdateGraph = (addedNode = null) => {
  SkillsService.getDependentSkillsGraphForProject(route.params.projectId).then((response) => {
    graph.value = response;
    if(graph.value.nodes.length > 0) {
      if (!network) {
        data.value = [];
        createGraph()
      } else {
        buildData()
      }
    } else {
      if(network) {
        network.destroy()
      }
      network = null;
    }
  }).finally(() => {
    isLoading.value = false;
    if(graph.value.nodes.length === 0) {
      network = null;
    } else if(addedNode && enableZoom.value) {
      const foundNode = getNodeBySkillId(addedNode)
      if(foundNode && foundNode.id) {
        panToNode(foundNode.id);
      }
    } else {
      fitNetworkToScreen()
    }
  });
}

const loadGraphDataAndCreateGraph = (addedNode = null) => {
  SkillsService.getDependentSkillsGraphForProject(route.params.projectId).then((response) => {
    graph.value = response;
    data.value = [];
    createGraph(addedNode);
  }).finally(() => {
    isLoading.value = false;
  });
};

const panToNode = (node, scrollIntoView = false) => {
  network.focus(node, {scale: 1, animation: enableAnimations.value})
  if(scrollIntoView) {
    dependencyGraph.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

const selectNode = (params) => {
  const selectedNode = params.nodes[0];
  const nodeValue = getNodeById(selectedNode);
  if(enableZoom.value) {
    panToNode(selectedNode, dynamicHeight.value)
  }
  updateSelectedFromSkills(nodeValue.details, false);
}

const selectEdge = (params) => {
  const selectedEdge = params.edges[0];
  const connectedNodes = network.getConnectedNodes(selectedEdge);

  const fromNode = getNodeById(connectedNodes[0])
  const toNode = getNodeById(connectedNodes[1])

  if(fromNode.details.belongsToBadge || toNode.details.belongsToBadge) {
    return;
  }

  const message = `Do you want to remove the path from ${fromNode.details.name} to ${toNode.details.name}?`;
  dialogMessages.msgConfirm({
    message: message,
    header: 'Remove Learning Path?',
    acceptLabel: 'Remove',
    rejectLabel: 'Cancel',
    appendTo: '#dependency-graph',
    accept: () => {
      SkillsService.removeDependency(toNode.details.projectId, toNode.details.skillId, fromNode.details.skillId, fromNode.details.projectId).then(() => {
        nodes.clear()
        handleUpdate();
      });
    }
  });
}

const createGraph = (nodeToPanTo = null) => {
  data.value = buildData();
  if (hasGraphData.value) {
    showGraph.value = true;
    network = new Network(dependencyGraph.value, data.value, displayOptions.value);

    network.on('selectNode', selectNode);
    network.on('selectEdge', selectEdge);

    if(nodeToPanTo && enableZoom.value) {
      const foundNode = getNodeBySkillId(nodeToPanTo)
      if(foundNode && foundNode.id) {
        panToNode(foundNode.id);
      }
    } else {
      fitNetworkToScreen()
    }
    setVisNetworkTabIndex();
  } else {
    showGraph.value = false;
  }
};

const buildData = () => {
  const sortedNodes = graph.value.nodes.sort((a, b) => a.id - b.id);
  sortedNodes.forEach((node) => {
    const newNode = buildNode(node)
    nodes.update(newNode);
  });
  const sortedEdges = graph.value.edges.sort((a, b) => a.toId - b.toId);
  sortedEdges.forEach((edge) => {
    edges.add({
      from: edge.toId,
      to: edge.fromId,
      arrows: 'to',
      title: 'Click to remove this path',
    });
  });

  layout();

  return { nodes: nodes, edges: edges };
};

const buildNode = (node) => {
  const isCrossProject = node.projectId !== route.params.projectId;
  const newNode = {
    id: node.id,
    label: GraphUtils.getLabel(node, isCrossProject),
    margin: {
      top: 25,
    },
    shape: 'icon',
    icon: {
      face: '"Font Awesome 5 Free"',
      code: '\uf19d',
      weight: '900',
      size: 50,
      color: 'lightgreen',
    },
    chosen: false,
    details: node,
    font: { multi: 'html', size: 20 },
    title: GraphUtils.getTitle(node, isCrossProject),
  };
  if(themeHelper.isDarkTheme) {
    newNode.font.color = '#f5f9ff'
  }

  if (isCrossProject) {
    newNode.margin = { top: 40 };
  }
  if (node.type === 'Badge') {
    newNode.icon.code = '\uf559';
    newNode.icon.color = '#88a9fc';
  }
  if (node.belongsToBadge) {
    newNode.icon.color = '#88a9fc';
  }

  return newNode;
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
};

const layout = () => {
  if (nodes.length <= 1 || edges.length === 0) {
    return
  }
  // convert graph
  // ref: https://github.com/dagrejs/dagre/wiki
  const g = new dagre.graphlib.Graph()
  const separationAmount = nodeSize * (horizontalOrientation.value ? 4 : 2)
  // Set an object for the graph label
  g.setGraph({
    rankdir: horizontalOrientation.value ? 'LR' : 'TB',
    nodesep: nodeSize * 3,
    edgesep: nodeSize,
    ranksep: separationAmount,
  })
  // Default to assigning a new object as a label for each new edge.
  g.setDefaultEdgeLabel(() => ({}))

  // Add nodes to the graph. The first argument is the node id. The second is
  // metadata about the node. In this case we're going to add labels to each of
  // our nodes.
  nodes.getDataSet().forEach((node) => {
    g.setNode(node.id, { label: node.label, width: nodeSize, height: nodeSize })
  })

  // Add edges to the graph.
  edges.getDataSet().forEach(edge => {
    g.setEdge(edge.from, edge.to)
  })

  dagre.layout(g)

  g.nodes().forEach((nodeId) => {
    // update node position
    const x = g.node(nodeId).x
    const y = g.node(nodeId).y

    const node = nodes.get(parseInt(nodeId));
    if(node) {
      nodes.update({...node, x: x, y: y})
    }
  })

  dataHeight.value = g?._label?.height;
  dataWidth.value = g?._label?.width;
}

const fitNetworkToScreen = () => {
  if(network) {
    network.fit()
  }
}

const toggleFullscreen = () => {
  toggle().then(() => {
    clearSelectedFromSkills()
    fitNetworkToScreen()
  })
}

const toggleZoom = () => {
  enableZoom.value = !enableZoom.value
}

const toggleAnimations = () => {
  enableAnimations.value = !enableAnimations.value
}

const toggleDynamicHeight = () => {
  dynamicHeight.value = !dynamicHeight.value
  network.fit()
}

const toggleOrientation = () => {
  horizontalOrientation.value = !horizontalOrientation.value;
  buildData()
  fitNetworkToScreen()
}

const active = ref(null);
const isVisible = computed(() => {
  return active.value === "0";
})

const dataHeight = ref(0);
const dataWidth = ref(0);
const containerWidth = computed(() => {
  return graphTemplate.value?.offsetWidth
})
const computedHeight = computed(() => {
  if(containerWidth.value > dataWidth.value) {
    if(horizontalOrientation.value) {
      return dataHeight.value >= 700 ? dataHeight.value: 700;
    } else {
      return dataHeight.value >= 500 ? dataHeight.value : 500;
    }
  } else {
    return dataHeight.value * (containerWidth.value / dataWidth.value)
  }
})

</script>

<template>
  <div id="full-dependent-skills-graph" ref="fullDepsSkillsGraph">
    <sub-page-header title="Learning Path"/>

    <prerequisite-selector v-if="!isReadOnlyProj && !isFullscreen" :project-id="route.params.projectId" @update="handleUpdate" :selected-from-skills="selectedFromSkills"
                           @updateSelectedFromSkills="updateSelectedFromSkills" @clearSelectedFromSkills="clearSelectedFromSkills" :showHeader="true"></prerequisite-selector>
    <BlockUI :blocked="isLoading">
      <Card data-cy="fullDepsSkillsGraph" style="margin-bottom: 25px;">
        <template #content>
          <skills-spinner :is-loading="isLoading" />
          <div ref="fullDepsSkillsGraphContainer" id="fullDepsSkillsGraphContainer" :style="!isFullscreen && dynamicHeight ? {'height': computedHeight + 'px !important'} : ''">
            <Accordion v-model:value="active" v-if="isFullscreen" id="prerequisiteContent">
              <AccordionPanel value="0">
                <AccordionHeader>Add a new item to the learning path</AccordionHeader>
                <AccordionContent>
                  <prerequisite-selector :showHeader="false" v-if="!isReadOnlyProj" :project-id="route.params.projectId" @update="handleUpdate" :selected-from-skills="selectedFromSkills"
                                         @updateSelectedFromSkills="updateSelectedFromSkills" @clearSelectedFromSkills="clearSelectedFromSkills" appendTo="self"></prerequisite-selector>
                </AccordionContent>
              </AccordionPanel>
            </Accordion>
            <div v-if="!hasGraphData && !isLoading" class="my-8">
              <no-content2 icon="fa fa-project-diagram" title="No Learning Path Yet..."
                           message="Here you can create and manage the project's Learning Path which may consist of skills and badges. You can get started by adding a path above."></no-content2>
            </div>
            <div v-else class="relative w-full mt-4 ml-1">
              <div class="left-0" :class="isFullscreen ? 'pl-2' : ''">
                <graph-legend class="graph-legend deps-overlay" :items="legendItems"/>
              </div>
              <div class="absolute right-0 mr-1 gap-2 flex items-center" id="additionalControls" :class="isFullscreen ? 'pr-2 mt-1' : ''">
                <graph-controls @toggleZoom="toggleZoom"
                                :isFullscreen="isFullscreen"
                                :enableZoom="enableZoom"
                                :enableAnimations="enableAnimations"
                                :horizontalOrientation="horizontalOrientation"
                                :enableDynamicHeight="dynamicHeight"
                                @toggleOrientation="toggleOrientation"
                                @toggleAnimations="toggleAnimations"
                                @toggleDynamicHeight="toggleDynamicHeight"
                                @toggleFullscreen="toggleFullscreen" />
              </div>
            </div>
            <div id="dependency-graph" ref="dependencyGraph" v-bind:style="{'visibility': showGraph ? 'visible' : 'hidden'}" :class="`${isFullscreen ? 'fullscreen' : ''} ${isVisible ? 'accordion' : ''}`"></div>
          </div>
        </template>
      </Card>
    </BlockUI>

    <dependency-table v-if="hasGraphData" :is-loading="isLoading" :data="data" @update="handleUpdate" @panToNode="panToNode" />
    <share-skills-with-other-projects v-if="!isReadOnlyProj" :project-id="route.params.projectId" />
    <shared-skills-from-other-projects v-if="!isReadOnlyProj" :project-id="route.params.projectId" />
  </div>
</template>

<style>
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-up,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-down,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-left,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-right,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomIn,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomOut,
#dependency-graph div.vis-network div.vis-navigation div.vis-button.vis-zoomExtends {
  background-image: none !important;
}

#dependency-graph div.vis-network div.vis-navigation div.vis-button:hover {
  box-shadow: none !important;
}

#dependency-graph .vis-button {
  font-size: 2em;
  color: #8c8c8c;
  font-family: "Font Awesome 5 Free";
}

@media screen and (max-width: 720px) {
  #dependency-graph .vis-button {
    display: none;
  }
}

#dependency-graph .vis-button:hover:after {
  color: #3273dc;
}

#dependency-graph .vis-button.vis-up:after {
  content: '\f35b';
}

#dependency-graph .vis-button.vis-down:after {
  content: '\f358';
}

#dependency-graph .vis-button.vis-left:after {
  content: '\f359';
}

#dependency-graph .vis-button.vis-right:after {
  content: '\f35a';
}

#dependency-graph .vis-button.vis-zoomIn:after {
  content: '\f0fe';
}

#dependency-graph .vis-button.vis-zoomOut:after {
  content: '\f146';
}

#dependency-graph .vis-button.vis-zoomExtends:after {
  content: "\f78c";
  font-weight: 900;
  font-size: 30px;
}

.legend-row {
  position: absolute;
  width: 100%;
}
.deps-overlay {
  z-index: 99;
}

:fullscreen, ::backdrop {
  background-color: rgba(255,255,255,1);
}

#dependency-graph {
  height: 98%;
}

#dependency-graph.fullscreen {
  height: 90% !important;
}

#dependency-graph.fullscreen.accordion {
  height: 80% !important;
}

.vis-navigation {
  background-color: white;
  position: absolute;
  top: 30px;
  right: 0;
}

.fullscreen > .vis-network > .vis-navigation {
  right: 15px !important;
}

#fullDepsSkillsGraphContainer {
  height: 500px;
}

#additionalControls {
  top: -15px;
  z-index:999;
}
</style>
