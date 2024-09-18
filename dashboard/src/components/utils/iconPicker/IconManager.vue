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
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import FileUpload from 'primevue/fileupload';
import VirtualScroller from 'primevue/virtualscroller';
import Message from 'primevue/message';
import enquire from 'enquire.js';
import FileUploadService from '@/common-components/utilities/FileUploadService';
import fontAwesomeIconsCanonical from './font-awesome-index';
import materialIconsCanonical from './material-index';
import IconManagerService from './IconManagerService.js';
import IconRow from './IconRow.vue';
import TabMenu from "primevue/tabmenu";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

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

onMounted(() => {
  IconManagerService.getIconIndex(route.params.projectId).then((response) => {
    if (response) {
      iconPacks.value[2].icons = response;
      iconPacks.value[2].defaultIcons = response.slice();
    }
  });

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

  iconPacks.value[0].icons = groupIntoRows(fontAwesomeIconsCanonical.icons, rowLength);
  iconPacks.value[1].icons = groupIntoRows(materialIconsCanonical.icons, rowLength);
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
let fontAwesomeIcons = fontAwesomeIconsCanonical;
let materialIcons = materialIconsCanonical;
let errorMessage = ref('');
const fileInfo = ref(null);
const loadingIcons = ref(false);

let active = ref(0);

const iconPacks = ref([
  {
    packName: fontAwesomeIcons.iconPack,
    headerIcon: 'fab fa-font-awesome-flag',
    icons: groupIntoRows(fontAwesomeIconsCanonical.icons, rowLength),
    defaultIcons: fontAwesomeIconsCanonical.icons.slice(),
  },
  {
    packName: materialIcons.iconPack,
    headerIcon: 'fas fa-file-alt',
    icons: groupIntoRows(materialIconsCanonical.icons, rowLength), //materialIcons.icons,
    defaultIcons: materialIconsCanonical.icons.slice(),
  },
  {
    packName: 'Custom',
    headerIcon: 'fas fa-wrench',
    icons: [],
    defaultIcons: [],
  }
]);

let filterCriteria = ref('');

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
  const filter = (icon) => icon.name.match(regex);

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
          iconPacks.value[2].icons = [iconPacks.value[2].defaultIcons];
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
  const isImageTypeValid = isValidImageType(upload.files[0].type);

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
  uploader.value.clear();
  fileInfo.value = null;
}

const closeError = () => {
  errorMessage.value = null;
}
</script>

<template xmlns:v-if="http://www.w3.org/1999/xlink">
    <div class="flex flex-column gap-3" :style="`width: ${modalWidth}`">
        <InputText type="text" class="w-full" :placeholder="searchPlaceholder" autofocus v-model="filterCriteria"
               @keyup="filter" ref="iconFilterInput" data-cy="icon-search" aria-label="search by icon name" />

      <TabMenu :model="iconPacks" @tab-change="onChange" v-model:activeIndex="active">
        <template #item="{ item, props }">
          <a v-bind="props.action" class="flex align-items-center gap-2">
            <i :class="item.headerIcon"></i> {{ item.packName }}
          </a>
        </template>
      </TabMenu>

      <VirtualScroller v-if="activePack !== 'Custom' && iconPacks[active]?.icons?.length > 0" :items="iconPacks[active]?.icons" :itemSize="[100, 100]" orientation="both" style="height: 360px; width: 100%;" data-cy="virtualIconList" :tabindex="-1">
        <template v-slot:item="{ item, options }">
          <IconRow :item="item" :options="options" @icon-selected="getIcon($event, iconPacks[active]?.iconPack)" />
        </template>
      </VirtualScroller>
      <FileUpload ref="uploader" @select="beforeUpload" v-if="activePack === 'Custom'" name="customIcon" :accept="acceptType" :maxFileSize="1000000" customUpload @uploader="beforeUpload">
        <template #header>
          <div class="w-full">
            <InputText class="w-full" data-cy="fileInput" placeholder="Browse..." type="file" @change="uploadFromInput($event)" v-model="fileInfo" />
            <p class="text-muted text-right text-primary font-italic">* custom icons must be between {{minDimensionsString}} and {{maxDimensionsString}}</p>
          </div>
        </template>
        <template #content>
          <Message data-cy="iconErrorMessage" v-if="errorMessage" @close="closeError" severity="error">{{ errorMessage }}</Message>
          <p>Drag and drop file here to upload.</p>
          <div v-if="iconPacks[2].icons.length > 0 && !loadingIcons">
            <div v-for="(icons, index) of iconPacks[2].icons" v-bind:key="index" class="flex">
              <div v-for="(file) of icons" :key="file.filename" class="card m-0 px-6 flex flex-wrap border-1 surface-border align-items-center gap-3">
                <div class="icon-item" style="max-width: 100px;">
                  <button class="p-link text-blue-400"
                     :aria-label="`Select icon ${file.filename}`"
                     @click.stop.prevent="selectIcon(file.filename, file.cssClassname, 'Custom Icons')"
                     :class="`item ${selectedCss === file.cssClassname ? 'selected' : ''}`">
                            <span class="icon is-large text-info">
                              <i :class="file.cssClassname" style="background-size: contain;"></i>
                            </span>
                  </button>
                  <br/>
                  <span class="iconName">
                    <button class="p-link text-blue-400 delete-icon" @click="deleteIcon(file, route.params.projectId)" :aria-label="`Delete icon ${file.filename}`">
                      <span class="icon is-tiny"><i style="font-size:1rem;height:1rem;width:1rem;" class="fas fa-trash"></i></span>
                    </button>
                    <span>{{ file.filename }}</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </FileUpload>

      <span v-if="iconPacks[active]?.icons?.length === 0 && activePack === iconPacks[active]?.packName && filterCriteria?.length > 0">No icons matched your search</span>
    </div>
</template>

<style>
  .p-fileupload-content {
    height: 300px;
    overflow: auto;
  }

  .icon-row {
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;
    padding-top: 12px;
    padding-bottom: 12px;
  }

  .iconManager_header {
    padding: 1em;
    border-radius: 8px 8px 0 0;
    border: 1px solid #ccc;
  }

  .icon-item {
    border-radius: 3px;
    color: inherit;
    flex: auto;
    text-align: center;
  }

  .icon-item i {
    box-sizing: content-box;
    text-align: center;
    border-radius: 3px;
    font-size: 3rem;
    width: 48px;
    height: 48px;
    display: inline-block;
  }

  .tab-content div {
    width: 100%;
  }

  .delete-icon {
    left: 0;
  }

  .is-tiny {
    height: .5rem;
    width: .5rem;
    font-size:.5rem;
    padding-right: .5rem;
  }
</style>
