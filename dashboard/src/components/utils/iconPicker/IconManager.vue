/*
Copyright 2020 SkillTree

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
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import FileUpload from 'primevue/fileupload';
import VirtualScroller from 'primevue/virtualscroller';
import ScrollPanel from 'primevue/scrollpanel';
import enquire from 'enquire.js';
import FileUploadService from '@/common-components/utilities/FileUploadService';
import IconManagerService from './IconManagerService.js';
import IconRow from './IconRow.vue';
import TabMenu from "primevue/tabmenu";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";

const route = useRoute();
const emit = defineEmits(['selected-icon', 'set-dismissable']);
const dialogMessages = useDialogMessages()

const props = defineProps(
    {
      searchBox: String,
      maxCustomIconDimensions: {
        type: Object,
        default() {
          return {
            width: 100,
            height: 100,
          };
        },
      },
      minCustomIconDimensions: {
        type: Object,
        default() {
          return {
            width: 48,
            height: 48,
          };
        },
      },
    },
)

const minDimensionsString = computed(() => {
  return `${props.minCustomIconDimensions.width}px x ${props.minCustomIconDimensions.width}px`
});

const maxDimensionsString = computed(() => {
  return `${props.maxCustomIconDimensions.width}px x ${props.maxCustomIconDimensions.width}px`
});

const isLoading = ref(true)
onMounted(() => {
  IconManagerService.getIconSetIndexes().then((iconSets) => {
    const fontAwesomeIcons = iconSets.fontAwesome
    const materialIcons = iconSets.material
    console.log(materialIcons)
    iconPacks.value = [
      {
        packName: fontAwesomeIcons.iconPack,
        headerIcon: 'fab fa-font-awesome-flag',
        icons: groupIntoRows(fontAwesomeIcons.icons, rowLength),
        defaultIcons: fontAwesomeIcons.icons.slice(),
      },
      {
        packName: materialIcons.iconPack,
        headerIcon: 'fas fa-file-alt',
        icons: groupIntoRows(materialIcons.icons, rowLength), //materialIcons.icons,
        defaultIcons: materialIcons.icons.slice(),
      },
      {
        packName: 'Custom',
        headerIcon: 'fas fa-wrench',
        icons: [],
        defaultIcons: [],
      }]

    enquire.register(xsAndSmaller, () => {
      rowLength = 1;
      modalWidth.value = "25rem";
    });
    enquire.register(smAndUp, () => {
      rowLength = 3;
      modalWidth.value = "30rem";
    });
    enquire.register(mdAndUp, () => {
      rowLength = 3;
      modalWidth.value = "40rem";
    });
    enquire.register(lgAndUp, () => {
      rowLength = 5;
      modalWidth.value = "60rem";
    });
    enquire.register(xlAndUp, () => {
      rowLength = 6;
      modalWidth.value = "70rem";
    });

    iconPacks.value[0].icons = groupIntoRows(fontAwesomeIcons.icons, rowLength);
    iconPacks.value[1].icons = groupIntoRows(materialIcons.icons, rowLength);

    IconManagerService.getIconIndex(route.params.projectId).then((response) => {
      if (response) {
        iconPacks.value[2].icons = response;
        iconPacks.value[2].defaultIcons = response.slice();
      }

    }).finally(() => {
      isLoading.value = false
    });
  })
});

const xsAndSmaller = '(max-width: 575.98px)';
const smAndUp = '(min-width: 576px) and (max-width: 767.98px)';
const mdAndUp = '(min-width: 768px) and (max-width: 991.98px)';
const lgAndUp = '(min-width: 992px) and (max-width: 1199.98px)';
const xlAndUp = '(min-width: 1200px)';

let modalWidth = ref("70rem");
let rowLength = 6;
let acceptType = 'image/.*';
const mimeTester = new RegExp(acceptType);
let selectedCss = '';
let activePack = ref(0);
let errorMessage = ref('');
const fileInfo = ref(null);
const loadingIcons = ref(false);

let active = ref(0);

const iconPacks = ref([]);

const filterCriteria = ref('');

function groupIntoRows(array, rl) {
  const result = [];
  let row = [];
  for (let i = 0; i < array.length; i += 1) {
    if (i > 0 && i % rl === 0) {
      result.push(row);
      row = [];
    }

    const item = array[i];
    row.push(item);
  }

  if(row.length > 0) {
    result.push(row);
  }

  return result;
}

// computed
const searchPlaceholder = computed(() => {
  return props.searchbox || 'Type to filter icons...';
});

// methods
const getIcon = (event, iconPack) => {
  selectIcon(event.icon, event.cssClass, iconPack);
};

const onChange = () => {
  activePack.value = iconPacks.value[active.value].packName;
  filter();
};

const selectIcon = (icon, iconCss, iconPack) => {
  const result = {
    name: icon,
    css: iconCss,
    pack: iconPack,
  };

  emit('selected-icon', result);
};

const uploadUrl = computed(() => {
  let uploadUrl = `/admin/projects/${encodeURIComponent(route.params.projectId)}/icons/upload`;
  if (!route.params.projectId) {
    uploadUrl = '/supervisor/icons/upload';
  }
  return uploadUrl;
});

const isValidCustomIconDimensions = (width, height) => {
  let isValid = width === height;
  if (isValid) {
    isValid = props.minCustomIconDimensions.width <= width && props.minCustomIconDimensions.height <= height;
    isValid = isValid && props.maxCustomIconDimensions.width >= width && props.maxCustomIconDimensions.height >= height;
  }
  return isValid;
};

const filter = () => {
  const value = filterCriteria.value.trim();
  const regex = new RegExp(value, 'gi');
  const filter = (icon) => {
    return icon.name.match(regex) || (icon.searchTerms && icon.searchTerms.some(searchTerm => searchTerm.match(regex)))
  }

  const currentPack = iconPacks.value[active.value];
  currentPack.icons = value?.length === 0 ? groupIntoRows(currentPack.defaultIcons, rowLength) : groupIntoRows(currentPack.defaultIcons.filter(filter), rowLength);
};

const handleUploadedIcon = (response) => {
  IconManagerService.addCustomIconCSS(response.cssDefinition);
  const newIcon = { name: response.name, cssClass: response.cssClassName };
  iconPacks.value[2].defaultIcons.push(newIcon);
  iconPacks.value[2].icons = iconPacks.value[2].defaultIcons;
  selectIcon(response.name, response.cssClassName, 'custom-icon');
}

const deleteIcon = (file, projectId) => {
  const className = file.cssClassname;
  const iconName = file.filename;
  emit('set-dismissable', false);
  IconManagerService.findUsages(projectId, className).then((resp) => {
    const usages = resp;
    let msg = `Are you sure you want to delete ${iconName}? `
    if(usages.length > 0) {
      msg += ' This icon is currently used by: ';
      const usedBy = usages.join(', ');
      msg += usedBy;
    }

    dialogMessages.msgConfirm({
      message: msg,
      header: 'WARNING: Delete Custom Icon',
      acceptLabel: 'YES, Delete It!',
      rejectLabel: 'Cancel',
      accept: () => {
        IconManagerService.deleteIcon(iconName, projectId).then(() => {
          iconPacks.value[2].defaultIcons = iconPacks.value[2].defaultIcons.filter((element) => element.filename !== iconName);
          if (iconPacks.value[2].defaultIcons && iconPacks.value[2].defaultIcons.length > 0) {
            iconPacks.value[2].icons = [iconPacks.value[2].defaultIcons];
          } else {
            iconPacks.value[2].icons = [];
          }
        });
      },
      reject: () => {
        emit('set-dismissable', true);
      },
      onHideHandler: () => {
        emit('set-dismissable', true);
      }
    })
  })
};

let uploader = ref();

const isValidImageType = (type) => {
  return mimeTester.test(type);
}
const uploadFromInput = (event) => {
  const target = event.target;
  const files = target.files;
  if( files[0] ) {
    files[0].objectURL = URL.createObjectURL(files[0]);
    beforeUpload({files: files});
  }
}
const beforeUpload = (upload) => {
  console.log(upload.files[0]?.type)
  const isImageTypeValid = isValidImageType(upload.files[0]?.type);

  if (!isImageTypeValid) {
    displayError('File is not an image format');
    return;
  }

  const existingIcons = iconPacks.value[2].icons.flat();
  const uploadName = upload.files[0].name;

  if(existingIcons.find(it => it.filename === uploadName)) {
    displayError('A file with this name already exists');
    return;
  }

  const customIcon = new Image();
  customIcon.src = upload.files[0].objectURL;
  customIcon.onload = () => {
    const width = customIcon.naturalWidth;
    const height = customIcon.naturalHeight;
    const isValid = isValidCustomIconDimensions(width, height);
    window.URL.revokeObjectURL(customIcon.src);

    if (isValid) {
      const data = new FormData();
      data.append('customIcon', upload.files[0]);
      FileUploadService.upload(uploadUrl.value, data, (response) => {
        handleUploadedIcon(response.data);
      }, () => {
        displayError('Encountered error when uploading icon');
      });
    } else {
      displayError(`Invalid image dimensions, dimensions must be square and must be between ${minDimensionsString.value} and ${maxDimensionsString.value}`);
    }
  };
}

const displayError = (message) => {
  errorMessage.value = message;
  // uploader.value.clear();
  fileInfo.value = null;
}

const closeError = () => {
  errorMessage.value = null;
}
</script>

<template>
  <div>
    <div v-if="isLoading" class="w-[20rem]">
      <skills-spinner :is-loading="true" class="my-8"/>
    </div>
    <div v-if="!isLoading" class="flex flex-col gap-2" :style="`width: ${modalWidth}`">

      <TabMenu :model="iconPacks" @tab-change="onChange" v-model:activeIndex="active">
        <template #item="{ item, props }">
          <a v-bind="props.action" class="flex items-center gap-2">
            <i :class="item.headerIcon"></i> {{ item.packName }}
          </a>
        </template>
      </TabMenu>

      <InputText v-if="activePack !== 'Custom'"
                 type="text" class="w-full"
                 :placeholder="searchPlaceholder"
                 autofocus
                 v-model="filterCriteria"
                 @keyup="filter" ref="iconFilterInput" data-cy="icon-search" aria-label="search by icon name" />
      <VirtualScroller v-if="activePack !== 'Custom' && iconPacks[active]?.icons?.length > 0" :items="iconPacks[active]?.icons" :itemSize="[100, 100]" orientation="both" style="height: 360px; width: 100%;" data-cy="virtualIconList" :tabindex="-1">
        <template v-slot:item="{ item, options }">
          <IconRow :item="item" :options="options" @icon-selected="getIcon($event, iconPacks[active]?.iconPack)" />
        </template>
      </VirtualScroller>
      <FileUpload ref="uploader"
                  @select="beforeUpload"
                  v-if="activePack === 'Custom'"
                  name="customIcon"
                  :accept="acceptType"
                  :maxFileSize="1000000"
                  customUpload
                  :auto="true"
                  @uploader="beforeUpload">
        <template #header="{ chooseCallback, uploadCallback, clearCallback, files }">
          <div class="flex flex-wrap justify-between items-center flex-1 gap-4 pb-2 border-b-2">
            <div class="">
              <SkillsButton @click="chooseCallback()" icon="fas fa-upload" label="Upload New Icon" severity="info"></SkillsButton>
            </div>
            <div class="flex-1 text-right text-primary">
              <InlineMessage class="text-muted italic" severity="warn">Custom icons must be between {{minDimensionsString}} and {{maxDimensionsString}}</InlineMessage>
            </div>
          </div>
        </template>
        <template #content>
          <Message data-cy="iconErrorMessage" v-if="errorMessage" @close="closeError" severity="error" :closable="true">{{ errorMessage }}</Message>
          <Message severity="info" icon="fas fa-cloud-upload-alt" v-if="iconPacks[2].icons.length > 0 && !loadingIcons" :marginY="0">Drag and drop file here to upload or select an existing icon below</Message>
          <div v-if="iconPacks[2].icons.length > 0 && !loadingIcons">
            <ScrollPanel style="width: 100%; height: 230px">
              <div v-for="(icons, index) of iconPacks[2].icons" v-bind:key="index" class="flex flex-wrap gap-4">
              <div v-for="(file) of icons" :key="file.filename"
                   class="p-3 rounded-border flex flex-col border border-surface items-center gap-4 mb-3 w-40">
                <button
                    class="p-link text-blue-400"
                    :aria-label="`Select icon ${file.filename}`"
                    @click.stop.prevent="selectIcon(file.filename, file.cssClassname, 'Custom Icons')"
                    :class="`item ${selectedCss === file.cssClassname ? 'selected' : ''}`">
                  <div class="icon is-large text-info">
                    <i :class="file.cssClassname" style="background-size: contain;"></i>
                  </div>
                  <div class="font-semibold w-30 overflow-hidden text-overflow-ellipsis break-all">{{
                      file.filename
                    }}
                  </div>
                </button>
                <div class="flex justify-center">
                  <SkillsButton
                      severity="warn"
                      rounded
                      @click="deleteIcon(file, route.params.projectId)"
                      data-cy="deleteIconBtn"
                      :aria-label="`Delete icon ${file.filename}`">
                    <i class="fas fa-trash"></i>
                  </SkillsButton>
                </div>
              </div>
            </div>
            </ScrollPanel>
          </div>
        </template>
        <template #empty v-if="iconPacks[2].icons.length === 0 && !loadingIcons">
          <div class="flex items-center justify-center flex-col py-4">
            <i class="fas fa-cloud-upload-alt border-2! rounded-full! p-8! text-4xl! text-muted-color!" />
            <p class="mt-6 mb-0">Drag and drop files to here to upload.</p>
          </div>
        </template>
      </FileUpload>

      <span v-if="iconPacks[active]?.icons?.length === 0 && activePack === iconPacks[active]?.packName && filterCriteria?.length > 0">No icons matched your search</span>
    </div>
  </div>
</template>

<style>

</style>
