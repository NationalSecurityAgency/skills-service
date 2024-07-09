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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SettingService from '@/components/settings/SettingsService.js'
import LevelService from './LevelService.js'
import Column from 'primevue/column'
import { useConfirm } from 'primevue/useconfirm'
import NewLevel from './NewLevel.vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useFocusState } from '@/stores/UseFocusState.js'

const confirm = useConfirm();
const announcer = useSkillsAnnouncer();
const route = useRoute();
const props = defineProps({
  maxLevels: {
    type: Number,
    default: 25,
  },
});
const focusState = useFocusState()

onMounted(() => {
  SettingService.getSetting(route.params.projectId, 'level.points.enabled')
      .then((data) => {
        const fields = [
          {
            key: 'level',
            label: 'Level',
            sortable: false,
          },
          {
            key: 'percent',
            label: 'Percent %',
            sortable: false,
          },
          {
            key: 'points',
            label: 'Points (>= to <)',
            sortable: false,
          },
          {
            key: 'edit',
            label: 'Modify',
            sortable: false,
            headerTitle: 'Edit Level',
          },
        ];
        table.value.options.fields = fields;
        table.value.options.tableDescription = computedTableDescription;

        const pointsEnabled = data && (data.value === true || data.value === 'true');
        if (pointsEnabled) {
          levelsAsPoints.value = true;
          table.value.options.fields = fields.filter((item) => item.key !== 'percent');
        }
      }).finally(() => {
    loading.value = false;
  });

  loadLevels();
})

let loading = ref(true);
let currentlyFocusedLevelId = ref('');
let displayLevelModal = ref(false);
let isEdit = ref(false);
let levelsAsPoints = ref(false);
let levelToEdit = { iconClass: 'fas fa-user-ninja' };
let levels = ref([]);
let table = ref({
  options: {
    busy: true,
    bordered: false,
    outlined: true,
    stacked: 'md',
    fields: [],
    pagination: {
      remove: true,
    },
    tableDescription: '',
  },
});

// computed
const computedTableDescription = computed(() => {
  if (route.params.subjectId) {
    return `Subject ${subject.name} Levels`;
  }
  return `Project ${project.name} Levels`;
});

const bounds = computed(() => {
  const bounds = {
    previous: null,
    next: null,
  };

  if (levels.value) {
    if (isEdit.value) {
      const existingIdx = levels.value.findIndex((level) => levelToEdit.level === level.level);
      const byIndex = new Map(levels.value.map((level, index) => [index, level]));

      const previous = byIndex.get(existingIdx - 1);
      const next = byIndex.get(existingIdx + 1);

      if (previous) {
        if (levelsAsPoints.value) {
          bounds.previous = previous.pointsTo;
        } else {
          bounds.previous = previous.percent;
        }
      }
      if (next) {
        if (levelsAsPoints.value) {
          bounds.next = next.pointsFrom;
        } else {
          bounds.next = next.percent;
        }
      }
    } else {
      const last = levels.value[levels.value.length - 1];
      if (levelsAsPoints.value) {
        bounds.previous = last?.pointsFrom;
      } else {
        bounds.previous = last?.percent;
      }
    }
  }

  return bounds;
});

const reachedMaxLevels = computed(() => {
  return levels.value.length >= props.maxLevels;
});

const onlyOneLevelLeft = computed(() => {
  return levels.value.length <= 1;
});

// methods
const loadLevels = () => {
  table.value.options.busy = true;
  if (route.params.subjectId) {
    return LevelService.getLevelsForSubject(route.params.projectId, route.params.subjectId)
        .then((response) => {
          levels.value = response;
          table.value.options.busy = false;
        });
  }
  return LevelService.getLevelsForProject(route.params.projectId)
      .then((response) => {
        levels.value = response;
        table.value.options.busy = false;
      });
};

const removeLastItem = () => {
  if (!onlyOneLevelLeft.value) {
    if (!route.params.subjectId && route.params.projectId) {
      const lastLevel = getLastItemLevel();
      LevelService.checkIfProjectLevelBelongsToGlobalBadge(route.params.projectId, lastLevel)
          .then((belongsToGlobalBadge) => {
            if (belongsToGlobalBadge) {
              confirm.require({
                msg: `Cannot remove level: [${lastLevel}].  This project level belongs to one or more global badges. Please contact a Supervisor to remove this dependency.`,
                header: 'Unable to Delete'
              });
            } else {
              confirmAndRemoveLastItem();
            }
          });
    } else {
      confirmAndRemoveLastItem();
    }
  }
};

const confirmAndRemoveLastItem = () => {
  const msg = 'Are you absolutely sure you want to delete the highest Level?';
  confirm.require({
    message: msg,
    header: 'WARNING: Delete Highest Level',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
          table.value.options.busy = true;
          doRemoveLastItem().then(() => {
            loadLevels().then(() => {
              focusState.focusOnLastElement()
              announcer.polite('Level has been removed');
            });
          }).catch((error) => {
            if (error?.response?.data) {
              confirm.require({
                message: error.response.data.explanation,
                header: 'Unable to delete',
                rejectClass: 'hidden',
                acceptLabel: 'OK',
              });
              // msgOk(error.response.data.explanation, 'Unable to delete');
            } else {
              // eslint-disable-next-line
              console.error(error);
            }
            table.value.options.busy = false;
          })
    }
  });
};

const doRemoveLastItem = () => {
  if (route.params.subjectId) {
    return LevelService.deleteLastLevelForSubject(route.params.projectId, route.params.subjectId);
  }
  return LevelService.deleteLastLevelForProject(route.params.projectId);
};

const getLastItemLevel = () => {
  return [...levels.value].sort((a, b) => {
    if (a.level > b.level) {
      return 1;
    }
    if (b.level > a.level) {
      return -1;
    }
    return 0;
  })[levels.value.length - 1].level;
};

const editLevel = (existingLevel) => {
  isEdit.value = !!existingLevel;

  if (existingLevel) {
    levelToEdit = { ...existingLevel };
    if (levels.value[levels.value.length - 1].level === existingLevel.level) {
      levelToEdit.isLast = true;
    }
    currentlyFocusedLevelId.value = existingLevel.level;
  } else if (!reachedMaxLevels.value) {
    levelToEdit = { iconClass: 'fas fa-user-ninja' };
  }

  displayLevelModal.value = true;
};

const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.sm.value)
</script>

<template>
  <div id="level-def-panel" ref="mainFocus">
    <sub-page-header title="Levels">
      <div class="row">
        <div class="col">
          <span id="remove-button" class="mr-2">
            <SkillsButton
              id="removeHighestBtn"
              ref="removeNextLevel"
              @click="removeLastItem"
              :disabled="onlyOneLevelLeft"
              size="small"
              :track-for-focus="true"
              data-cy="removeLevel"
              icon="fas fa-trash-alt" label="Remove Highest">
            </SkillsButton>
          </span>
          <span id="add-button">
            <SkillsButton @click="editLevel()" ref="addLevel" :disabled="reachedMaxLevels" :track-for-focus="true" id="addLevel"
                      size="small" data-cy="addLevel" icon="fas fa-plus-circle" label="Add Next">
            </SkillsButton>
          </span>
        </div>
      </div>
    </sub-page-header>

    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>
        <SkillsDataTable tableStoredStateId="levels" v-if="!loading" :options="table.options" :loading="loading" :value="levels"
                   data-cy="levelsTable" striped-rows>
          <Column field="level" header="Level" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <div>
              {{ slotProps.data.level }}
              </div>
              <InlineMessage v-if="slotProps.data.achievable === false" class="text-sm" severity="warn">
                Level is unachievable. Insufficient available points in project.
              </InlineMessage>
            </template>
          </Column>
          <Column field="percent" header="Percent" :class="{'flex': isFlex }">
            {{ slotProps.data.percent }}
          </Column>
          <Column field="points" header="Points" :class="{'flex': isFlex }">
            <template #body="slotProps">
                <span v-if="slotProps.data.pointsFrom !== null && slotProps.data.pointsFrom !== undefined">
                  {{ slotProps.data.pointsFrom }} to
                  <span v-if="slotProps.data.pointsTo">{{ slotProps.data.pointsTo }}</span>
                  <span v-else><i class="fas fa-infinity"/></span>
                </span>
              <div v-else aria-label="Points cannot be calculated. Please create more skills first." class="flex align-items-center">N/A
                  <InlineMessage severity="warn" class="ml-1"> Please create more skills first</InlineMessage>
              </div>
            </template>
          </Column>
          <Column field="edit" header="Edit" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <SkillsButton :ref="`edit_${ slotProps.data.level}`" @click="editLevel(slotProps.data)" size="small" data-cy="editLevelButton" icon="fas fa-edit" label="Edit" :track-for-focus="true" :id="`editLevelButton_${slotProps.data.level}`" />
            </template>
          </Column>
        </SkillsDataTable>
      </template>
    </Card>
    <new-level v-if="displayLevelModal && levels"
               v-model="displayLevelModal"
               @load-levels="loadLevels"
               :boundaries="bounds"
               :level="levelToEdit"
               :level-as-points="levelsAsPoints"
               :is-edit="isEdit"
               :all-levels="levels"></new-level>
  </div>
</template>

<style scoped></style>
