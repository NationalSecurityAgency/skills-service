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
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import 'vis-network/styles/vis-network.css';
import { Network } from 'vis-network';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import PrerequisiteSelector from '@/components/skills/dependencies/PrerequisiteSelector.vue';
import SkillsService from '@/components/skills/SkillsService';
import GraphUtils from '@/components/skills/dependencies/GraphUtils';
import GraphLegend from '@/components/skills/dependencies/GraphLegend.vue';
import GraphNodeSortMethodSelector from '@/components/skills/dependencies/GraphNodeSortMethodSelector.vue';
import NoContent2 from "@/components/utils/NoContent2.vue";
import DependencyTable from "@/components/skills/dependencies/DependencyTable.vue";
import ShareSkillsWithOtherProjects from "@/components/skills/crossProjects/ShareSkillsWithOtherProjects.vue";
import SharedSkillsFromOtherProjects from "@/components/skills/crossProjects/SharedSkillsFromOtherProjects.vue";
import { useProjConfig } from '@/stores/UseProjConfig.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const dialogMessages = useDialogMessages()
const projConfig = useProjConfig();
const route = useRoute();
const themeHelper = useThemesHelper()
const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

const isLoading = ref(true);
const showGraph = ref(true);
const selectedFromSkills = ref({});
const data = ref([]);
const graph = ref({});
let network = null;
let nodes = [];
let edges = [];
const legendItems = [
  { label: 'Skill', color: 'lightgreen', iconClass: 'fa-graduation-cap' },
  { label: 'Badge', color: '#88a9fc', iconClass: 'fa-award' },
];
const displayOptions = ref({
  layout: {
    hierarchical: {
      enabled: true,
          sortMethod: 'directed',
          nodeSpacing: 350,
          treeSpacing: 370,
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
  nodes = [];
  edges = [];
  loadGraphDataAndCreateGraph();
})

onBeforeUnmount(() => {
  if (network) {
    network.destroy();
  }
})

const hasGraphData = computed(() => {
  return graph.value && graph.value.nodes && graph.value.nodes.length > 0;
});

const updateSelectedFromSkills = (item) => {
  if(item) {
    selectedFromSkills.value = item;
  }
  else {
    clearSelectedFromSkills();
  }
};

const clearSelectedFromSkills = () => {
  selectedFromSkills.value = {};
};

const handleUpdate = () => {
  clearSelectedFromSkills();
  graph.value = [];
  network = null;
  nodes = [];
  edges = [];
  isLoading.value = true;

  loadGraphDataAndCreateGraph();
};

const loadGraphDataAndCreateGraph = () => {
  SkillsService.getDependentSkillsGraphForProject(route.params.projectId)
      .then((response) => {
        graph.value = response;
        isLoading.value = false;
        createGraph();
      })
      .finally(() => {
        isLoading.value = false;
      });
};

const onSortNodeStrategyChange = (newStrategy) => {
  displayOptions.value.layout.hierarchical.sortMethod = newStrategy;
  createGraph();
};

const selectNode = (params) => {
  const selectedNode = params.nodes[0];
  const nodeValue = nodes.find((node) => node.id === selectedNode);
  updateSelectedFromSkills(nodeValue.details);
}

const selectEdge = (params) => {
  const allNodes = graph.value.nodes;
  const selectedEdge = params.edges[0];
  const connectedNodes = network.getConnectedNodes(selectedEdge);

  const fromNode = allNodes.find((node) => node.id === connectedNodes[0]);
  const toNode = allNodes.find((node) => node.id === connectedNodes[1]);

  if(fromNode.belongsToBadge || toNode.belongsToBadge) {
    return;
  }

  const message = `Do you want to remove the path from ${fromNode.name} to ${toNode.name}?`;
  dialogMessages.msgConfirm({
    message: message,
    header: 'Remove Learning Path?',
    acceptLabel: 'Remove',
    rejectLabel: 'Cancel',
    accept: () => {
      SkillsService.removeDependency(toNode.projectId, toNode.skillId, fromNode.skillId, fromNode.projectId).then(() => {
        handleUpdate();
      });
    }
  });
}

const createGraph = () => {
  if (network) {
    network.destroy();
    network = null;
    nodes = [];
    edges = [];
  }

  data.value = buildData();
  if (hasGraphData.value) {
    showGraph.value = true;
    const container = document.getElementById('dependency-graph');
    network = new Network(container, data.value, displayOptions.value);

    network.on('selectNode', selectNode);
    network.on('selectEdge', selectEdge);

    setVisNetworkTabIndex();
  } else {
    showGraph.value = false;
  }
};

const buildData = () => {
  const sortedNodes = graph.value.nodes.sort((a, b) => a.id - b.id);
  sortedNodes.forEach((node) => {
    const isCrossProject = node.projectId !== route.params.projectId;
    if(node.type === 'Badge') {
      if(node.containedSkills && node.containedSkills.length > 0) {
        const skillIds = node.containedSkills.map((it) => it.name);
        const childNode = {
          id: node.id + '-skills',
          label: skillIds.join('\n'),
          shape: 'box',
          type: 'Badge-Skills',
        };
        nodes.push(childNode);
        edges.push({
          to: node.id,
          from: node.id + '-skills',
          dashes: true,
          label: 'contains',
        });
      }
    }
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
    nodes.push(newNode);
  });
  const sortedEdges = graph.value.edges.sort((a, b) => a.toId - b.toId);
  sortedEdges.forEach((edge) => {
    edges.push({
      from: edge.toId,
      to: edge.fromId,
      arrows: 'to',
      title: 'Click to remove this path',
    });
  });

  return { nodes: nodes, edges: edges };
};

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
</script>

<template>
  <div id="full-dependent-skills-graph">
    <sub-page-header title="Learning Path"/>

    <prerequisite-selector v-if="!isReadOnlyProj" :project-id="route.params.projectId" @update="handleUpdate" :selected-from-skills="selectedFromSkills"
                           @updateSelectedFromSkills="updateSelectedFromSkills" @clearSelectedFromSkills="clearSelectedFromSkills"></prerequisite-selector>
    <Card data-cy="fullDepsSkillsGraph" style="margin-bottom: 25px;">
      <template #content>
<!--      <loading-container :is-loading="isLoading">-->
        <div v-if="!hasGraphData && !isLoading" class="my-5">
          <no-content2 icon="fa fa-project-diagram" title="No Learning Path Yet..."
                       message="Here you can create and manage the project's Learning Path which may consist of skills and badges. You can get started by adding a path above."></no-content2>
        </div>
        <div v-else class="relative w-full">
          <div class="left-0">
            <graph-legend class="graph-legend deps-overlay" :items="legendItems"/>
          </div>
          <div class="absolute right-0" style="top: -20px;">
            <graph-node-sort-method-selector class="deps-overlay" @value-changed="onSortNodeStrategyChange"/>
          </div>
        </div>
<!--      </loading-container>-->
        <div id="dependency-graph" v-bind:style="{'visibility': showGraph ? 'visible' : 'hidden', 'height': '500px'}"></div>
      </template>
    </Card>

    <dependency-table v-if="hasGraphData" :is-loading="isLoading" :data="data" @update="handleUpdate" />
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
</style>
