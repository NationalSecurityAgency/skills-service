<script setup>
import { ref, onMounted, watch } from 'vue';

const emit = defineEmits(['added', 'removed', 'search-change'])
const props = defineProps({
  options: {
    type: Array,
    required: true,
  },
  selected: {
    type: Object,
  },
  onlySingleSelectedValue: {
    type: Boolean,
    default: false,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  internalSearch: {
    type: Boolean,
    default: true,
  },
  emptyWithoutSearch: {
    type: Boolean,
    default: false,
  },
  afterListSlotText: {
    type: String,
    default: '',
  },
  showProject: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: 'Select subject',
  },
  placeholderIcon: {
    type: String,
    default: null,
  },
  selectLabel: {
    type: String,
    default: 'Press enter to select',
  },
  warnBeforeRemoving: {
    type: Boolean,
    default: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const selectedInternal = ref(null);
const optionsInternal = ref([]);
const multipleSelection = ref(true);
const currentSearch = ref('');

onMounted(() => {
  setSelectedInternal();
  setOptionsInternal();
  if (props.onlySingleSelectedValue) {
    multipleSelection.value = false;
  }
});
const setSelectedInternal = () => {
  if (props.selected) {
    selectedInternal.value = ({ ...props.selected });
  } else {
    selectedInternal.value = null;
  }
};
const setOptionsInternal = () => {
  if (props.options) {
    optionsInternal.value = props.options.map((entry) => ({ entryId: `${entry.projectId}_${entry.subjectId}`, ...entry }));
    if (props.selected) {
      // removed already selected items
      optionsInternal.value = optionsInternal.value.filter((el) => !props.selected.some((sel) => `${sel.projectId}_${sel.subjectId}` === el.entryId));
    }
  }
};

const removed = (removedItem) => {
  selectedInternal.value = null;
  emit('removed', removedItem);
};

const added = (addedItem) => {
  if (multipleSelection.value) {
    emit('added', addedItem[addedItem.length - 1]);
  } else {
    emit('added', addedItem);
  }
};

const searchChanged = (query, loadingFunction) => {
  currentSearch.value = query;
  emit('search-change', query, loadingFunction);
};
</script>

<template>
  test
</template>

<style scoped>
</style>