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
});

onMounted(() => {
  setSelectedInternal();
  if (props.loadImmediately && props.projectId) {
    loadProjectLevels(props.projectId);
  }
})

const isLoading = ref(false);
const projectLevels = ref([]);
const selectedInternal = ref(null);

const loadProjectLevels = (projectId) => {
  isLoading.value = true;
  GlobalBadgeService.getProjectLevels(projectId)
      .then((response) => {
        projectLevels.value = response.map((entry) => entry.level);
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
  if (inputItem != null) {
    selected(inputItem);
  } else {
    removed(null);
  }
};
</script>

<template>
  <div id="level-selector">
    <Dropdown v-model="selectedInternal" :disabled="disabled" :options="projectLevels" :placeholder="placeholder" class="w-full" :loading="isLoading" @change="inputChanged" />
<!--    <v-select :disabled="disabled" v-model="selectedInternal" :options="projectLevels"-->
<!--              :placeholder="placeholder" v-on:input="inputChanged" :loading="isLoading">-->
<!--    </v-select>-->
  </div>
</template>

<style scoped>

</style>