<script setup>
import { ref, onMounted, watch } from 'vue';
import AutoComplete from 'primevue/autocomplete';
import Dropdown from "primevue/dropdown";

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
      console.log(props.selected);
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
    emit('added', addedItem[addedItem.length - 1].value);
  } else {
    emit('added', addedItem.value);
  }
};

const searchChanged = (query) => {
  currentSearch.value = query.query;
  if(currentSearch.value) {
    optionsInternal.value = props.options.filter((item) => {
      return item.name.toLowerCase().startsWith(currentSearch.value.toLowerCase());
    })
  }
  else {
    optionsInternal.value = props.options.map((entry) => ({  name: entry.name }));
  }
  // emit('search-change', query, loadingFunction);
};
</script>

<!--:disabled="disabled"-->

<template>
  <Dropdown :options="optionsInternal" :placeholder="placeholder" class="st-skills-selector w-full" v-model="selectedInternal" label="name"
            data-cy="subjectSelector" :class="props.class" :disabled="disabled" :loading="isLoading" filter
            @filter="searchChanged" @change="added" optionLabel="name" resetFilterOnHide showClear>
    <template #option="slotProps">
      <div :data-cy="`subjectSelectionItem-${slotProps.option.projectId}-${slotProps.option.subjectId}`">
        <div class="text-xl skills-option-name" data-cy="subjSelector-name">{{ slotProps.option.name }}
        </div>
        <div style="font-size: 0.8rem;">
          <span class="uppercase mr-1 font-italic"># Skills:</span>
          <span class="font-bold" data-cy="skillsSelector-projectId">{{ slotProps.option.numSkills}}</span>
          <span class="mx-2">|</span>
          <span class="uppercase mr-1 font-italic" data-cy="skillsSelectionItem-subjectId">Points:</span>
          <span class="font-weight-bold skills-option-subject-name" data-cy="skillsSelector-subjectName">{{ slotProps.option.totalPoints }}</span>
        </div>
      </div>
    </template>
  </Dropdown>
<!--    -->
<!--  <AutoComplete :suggestions="optionsInternal"-->
<!--                dropdown-->
<!--                :delay="500"-->
<!--                optionLabel="name"-->
<!--                :loading="isLoading"-->
<!--                @complete="searchChanged"-->
<!--                @item-select="added"-->
<!--                @item-unselect="removed"-->
<!--                v-model="selectedInternal"-->
<!--                :placeholder="placeholder"-->
<!--                class="st-skills-selector w-full"-->
<!--                data-cy="subjectSelector">-->
<!--  </AutoComplete>-->
</template>

<style scoped>
</style>