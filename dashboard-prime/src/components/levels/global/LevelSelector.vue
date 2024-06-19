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
import { ref, watch, onMounted } from 'vue';
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";

const emit = defineEmits(['input', 'removed'])
const props = defineProps({
  value: {
    type: Number,
  },
  projectId: {
    type: String,
  },
  disabled: {
    type: Boolean,
  },
  placeholder: {
    type: String,
  },
  loadImmediately: {
    type: Boolean,
    required: false,
    default: false,
  },
  selectedLevel: {
    required: false,
    type: Number,
  }
});

onMounted(() => {
  setSelectedInternal();
  if (props.loadImmediately && props.projectId) {
    loadProjectLevels(props.projectId);
  }
})

watch(() => props.projectId, (newProjectId) => {
  selectedInternal.value = null;
  if (!newProjectId) {
    projectLevels.value = [];
  } else {
    loadProjectLevels(newProjectId);
  }
})

const isLoading = ref(false);
const projectLevels = ref([]);
const selectedInternal = ref(null);

const loadProjectLevels = (projectId) => {
  isLoading.value = true;
  GlobalBadgeService.getProjectLevels(projectId)
      .then((response) => {
        response.forEach((entry) => {
          projectLevels.value.push({ level: entry.level, disabled: props.selectedLevel && props.selectedLevel === entry.level });
        })
      }).finally(() => {
    isLoading.value = false;
  });
};

const setSelectedInternal = () => {
  if (props.value) {
    selectedInternal.value = props.value;
  }
};

const removed = (removedItem) => {
  emit('removed', removedItem);
};

const selected = (selectedItem) => {
  emit('input', selectedItem);
};

const inputChanged = (inputItem) => {
  if (inputItem.value != null) {
    selected(inputItem.value);
  } else {
    removed(null);
  }
};
</script>

<template>
  <div id="level-selector">
    <Dropdown v-model="selectedInternal" :disabled="disabled" optionLabel="level" optionValue="level"
              optionDisabled="disabled" :options="projectLevels" :placeholder="placeholder" filter
              class="w-full" :loading="isLoading" @change="inputChanged">
    </Dropdown>
  </div>
</template>

<style scoped>

</style>