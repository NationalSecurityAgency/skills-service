<script setup>
import { ref, onMounted, watch } from 'vue';
import Dropdown from 'primevue/dropdown';
import Badge from 'primevue/badge';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil';

const announcer = useSkillsAnnouncer();
const emit = defineEmits(['removed', 'added', 'search-change', 'selection-removed']);
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
    default: 'Select skill(s)...',
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
  showType: {
    type: Boolean,
    default: false,
  }
});

let selectedInternal = ref([]);
let optionsInternal = ref([]);
let multipleSelection = ref(true);
let currentSearch = ref('');
let isMounted = ref(false);

watch(() => props.selected, async () => {
  setSelectedInternal();
})
// methods
const removeReuseTag = (val) => {
  return SkillReuseIdUtil.removeTag(val);
};

const setSelectedInternal = () => {
  if (props.selected) {
    selectedInternal.value = { entryId: `${props.selected.projectId}_${props.selected.skillId}`, ...props.selected }; //props.selected.map((entry) => ({ entryId: `${entry.projectId}_${entry.skillId}`, ...entry }));
  }
};

const setOptionsInternal = () => {
  if (props.options) {
    optionsInternal.value = props.options.map((entry) => ({ entryId: `${entry.projectId}_${entry.skillId}`, ...entry }));
    if (props.selected) {
      // removed already selected items
      optionsInternal.value = optionsInternal.value.filter((el) => !props.selected.some((sel) => `${sel.projectId}_${sel.skillId}` === el.entryId));
    }
    if (optionsInternal.value && optionsInternal.value.length === 0) {
      announcer.polite('No elements found. Consider changing the search query');
    }
  }
};

const considerRemoval = (removedItem) => {
  if (props.warnBeforeRemoving) {
    const msg = `Are you sure you want to remove "${removedItem.name}"?`;
    // this.msgConfirm(msg, 'WARNING', 'Yes, Please!')
    //     .then((res) => {
    //       if (res) {
    //         removed(removedItem);
    //       }
    //     });
  } else {
    removed(removedItem);
  }
};

const removed = (removedItem) => {
  emit('removed', removedItem);
};

const added = (addedItem) => {
  // if(addedItem.originalEvent.type === 'click') {
    if (multipleSelection.value) {
      emit('added', addedItem[addedItem.length - 1].value);
    } else {
      emit('added', addedItem.value);
    }
  // }
};

const searchChanged = (query) => {
  currentSearch.value = query.value;
  emit('search-change', query.value);
};

const valChanged = (val) => {
  if (isMounted.value && val === null) {
    emit('selection-removed');
  }
};

const clearValue = () => {
  selectedInternal.value = [];
}

onMounted(() => {
  setSelectedInternal();
  setOptionsInternal();
  if (props.onlySingleSelectedValue) {
    multipleSelection.value = false;
  }
  isMounted.value = true;
})

defineExpose({
  clearValue,
})
</script>

<template>
  <Dropdown :options="options" :placeholder="placeholder" class="st-skills-selector" v-model="selectedInternal" label="name"
            data-cy="skillsSelector" :class="props.class" style="min-width: 100%;" :disabled="disabled" :loading="isLoading" filter
            @filter="searchChanged" @change="added" optionLabel="name" resetFilterOnHide showClear>

    <template #option="slotProps">
      <slot name="dropdown-item" :option="slotProps">
        <div :data-cy="`skillsSelectionItem-${slotProps.option.projectId}-${slotProps.option.skillId}`">
          <div class="text-xl text-info skills-option-name" data-cy="skillsSelector-skillName"><span v-if="showType">{{ slotProps.option.type }}:</span> {{ slotProps.option.name }}
            <Tag v-if="slotProps.option.isReused" variant="success" size="sm" class="uppercase"
                     data-cy="reusedBadge"
                     style="font-size: 0.85rem !important;"><i class="fas fa-recycle"></i> reused
            </Tag>
          </div>
          <div style="font-size: 0.8rem;">
              <span class="skills-option-id">
                <span v-if="showProject" data-cy="skillsSelectionItem-projectId"><span
                    class="uppercase mr-1 font-italic">Project ID:</span><span
                    class="font-bold"
                    data-cy="skillsSelector-projectId">{{ slotProps.option.projectId }}</span></span>
                <span v-if="!showProject" data-cy="skillsSelectionItem-skillId">
                  <span class="uppercase mr-1 font-italic">ID:</span>
                  <span class="font-bold" data-cy="skillsSelector-skillId">
                  {{ removeReuseTag(slotProps.option.skillId) }}
                  </span>
                </span>
              </span>
            <span class="mx-2" v-if="slotProps.option.type !== 'Badge'">|</span>
            <span v-if="slotProps.option.type === 'Skill'" class="uppercase mr-1 font-italic" data-cy="skillsSelectionItem-subjectId">Subject:</span>
            <span v-if="slotProps.option.type === 'Skill'"
                  class="font-bold skills-option-subject-name"
                  data-cy="skillsSelector-subjectName">{{ slotProps.option.subjectName }}</span>
            <span v-if="slotProps.option.type === 'Shared Skill'" class="uppercase mr-1 font-italic" data-cy="skillsSelectionItem-projectName">Project:</span>
            <span v-if="slotProps.option.type === 'Shared Skill'"
                  class="font-bold skills-option-subject-name"
                  data-cy="skillsSelector-projectName">{{ slotProps.option.projectName }}</span>
            <span v-if="slotProps.option.groupName">
                <span class="mx-2">|</span>
                <span class="uppercase mr-1 font-italic skills-option-group-name" data-cy="skillsSelectionItem-group">Group:</span><span
                class="font-bold skills-id"
                data-cy="skillsSelector-groupName">{{ slotProps.option.groupName }}</span>
              </span>
          </div>
        </div>
      </slot>
    </template>
    <template #value="slotProps">
      <div v-if="slotProps.value && slotProps.value.name">
        {{ slotProps.value.name }}
      </div>
      <div v-else>
        {{ slotProps.placeholder }}
      </div>
    </template>
    <template #footer v-if="afterListSlotText">
      <li>
        <div class="h6 ml-1"> {{ afterListSlotText }}</div>
      </li>
    </template>
    <template #empty>
      <span v-if="emptyWithoutSearch && !internalSearch && !currentSearch"><i class="fas fa-search"/> Type to <span class="font-bold">search</span> for skills...</span>
      <span v-else>No elements found. Consider changing the search query</span>
    </template>
  </Dropdown>
</template>

<style scoped></style>