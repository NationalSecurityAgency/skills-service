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
import TabPanel from 'primevue/tabpanel';
import TabView from 'primevue/tabview';
import Card from 'primevue/card';
import VirtualScroller from 'primevue/virtualscroller';
import FileUploadService from '@/common-components/utilities/FileUploadService';
import fontAwesomeIconsCanonical from './font-awesome-index';
import materialIconsCanonical from './material-index';
import IconManagerService from './IconManagerService.js';
import IconRow from './IconRow.vue';
import GroupedIcons from './GroupedIcons.js';

const route = useRoute();
const emit = defineEmits(['selected-icon']);

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
        // validator: validateIconDimensions,
      },
      minCustomIconDimensions: {
        type: Object,
        default() {
          return {
            width: 48,
            height: 48,
          };
        },
        // validator: validateIconDimensions,
      },
    },
)

onMounted(() => {
  IconManagerService.getIconIndex(route.params.projectId).then((response) => {
    if (response) {
      definitiveCustomIconList = response.slice();
      customIconList = response;
    }
  });

  // enquire.register(xsAndSmaller, () => {
  //   rowLength = 2;
  //   groupRows();
  // });
  // enquire.register(smAndUp, () => {
  //   rowLength = 3;
  //   groupRows();
  // });
  // enquire.register(mdAndUp, () => {
  //   rowLength = 3;
  //   groupRows();
  // });
  // enquire.register(lgAndUp, () => {
  //   rowLength = 5;
  //   groupRows();
  // });
  // enquire.register(xlAndUp, () => {
  //   rowLength = 6;
  //   groupRows();
  // });
});

const faIconList = fontAwesomeIconsCanonical.icons.slice();
const matIconList = materialIconsCanonical.icons.slice();
let customIconList = [];

const xsAndSmaller = '(max-width: 575.98px)';
const smAndUp = '(min-width: 576px) and (max-width: 767.98px)';
const mdAndUp = '(min-width: 768px) and (max-width: 991.98px)';
const lgAndUp = '(min-width: 992px) and (max-width: 1199.98px)';
const xlAndUp = '(min-width: 1200px)';

let definitiveCustomIconList = [];

let rowLength = 6;
let acceptType = 'image/*';
let selected = '';
let selectedCss = '';
let selectedIconPack = '';
let activePack = ref(fontAwesomeIconsCanonical.iconPack);
let fontAwesomeIcons = fontAwesomeIconsCanonical;
let materialIcons = materialIconsCanonical;
// let customIconList;
let disableCustomUpload = false;
// let rowItemComponent = IconRow;
let currentCustomIconFile = null;

let active = ref(0);
const iconPacks = [
  {
    packName: fontAwesomeIcons.iconPack,
    headerIcon: 'fab fa-font-awesome-flag',
    icons: groupIntoRows(fontAwesomeIconsCanonical.icons, rowLength),

  },
  {
    packName: materialIcons.iconPack,
    headerIcon: 'fas fa-file-alt',
    icons: groupIntoRows(materialIconsCanonical.icons, rowLength), //materialIcons.icons,
  },
  // {
  //   packName: 'Custom',
  //   headerIcon: 'fas fa-wrench',
  //   icons: []
  // }
];

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
  result.push([]);

  return result;
}

// computed
const searchPlaceholder = computed(() => {
  return props.searchbox || 'Type to filter icons...';
});

// methods
const getIcon = (event, iconPack) => {
  // selected = event.icon;
  // selectedCss = event.cssClass;
  // selectedIconPack = iconPack;

  selectIcon(event.icon, event.cssClass, iconPack);
};

const onChange = (tabIndex) => {
  activePack.value = iconPacks[active.value].packName;
  // filter(value);
};

const selectIcon = (icon, iconCss, iconPack) => {
  const result = {
    name: icon,
    css: iconCss,
    pack: iconPack,
  };

  emit('selected-icon', result);
};

// const uploadUrl = computed(() => {
//   let uploadUrl = `/admin/projects/${encodeURIComponent(route.params.projectId)}/icons/upload`;
//   if (!route.params.projectId) {
//     uploadUrl = '/supervisor/icons/upload';
//   }
//   return uploadUrl;
// });
//
// const validateIconDimensions = (dimensions) => {
//   const { width, height } = dimensions;
//   let isValid = true;
//
//   if (!width || !height) {
//     isValid = false;
//   }
//
//   if (isValid) {
//     isValid = width / height === 1;
//   }
//
//   return isValid;
// };
//
// const isValidCustomIconDimensions = (vm, width, height) => {
//   let isValid = width / height === 1;
//   if (isValid) {
//     isValid = vm.minCustomIconDimensions.width <= width && vm.minCustomIconDimensions.height <= height;
//     isValid = isValid && vm.maxCustomIconDimensions.width >= width && vm.maxCustomIconDimensions.height >= height;
//   }
//   return isValid;
// };

// extend('image', {
//   ...image,
//   message: 'File is not an image format',
// });
// extend('imageDimensions', {
//   message: () => `Invalid image dimensions, dimensions must be square and must be between ${minCustomIconDimensions.width} x ${minCustomIconDimensions.width} and ${maxCustomIconDimensions.width} x ${maxCustomIconDimensions.width}`,
//   validate(value) {
//     return new Promise((resolve) => {
//       if (value) {
//         const file = value;
//         const customIcon = new Image();
//         customIcon.src = window.URL.createObjectURL(file);
//         customIcon.onload = () => {
//           const width = customIcon.naturalWidth;
//           const height = customIcon.naturalHeight;
//           window.URL.revokeObjectURL(customIcon.src);
//
//           if (!isValidCustomIconDimensions(self, width, height)) {
//             resolve({
//               valid: false,
//             });
//           } else {
//             resolve({
//               valid: true,
//             });
//           }
//         };
//       } else {
//         resolve({
//           valid: true,
//         });
//       }
//     });
//   },
// });

// extend('duplicateFilename', {
//   message: 'Custom Icon with this filename already exists',
//   validate(value) {
//     return new Promise((resolve) => {
//       if (value) {
//         const file = value;
//
//         const index = definitiveCustomIconList.findIndex((item) => item.filename === file.name);
//         if (index >= 0) {
//           resolve({
//             valid: false,
//           });
//           return;
//         }
//       }
//       resolve({ valid: true });
//     });
//   },
// });

// beforeDestroy() {
//   enquire.unregister(xsAndSmaller);
//   enquire.unregister(smAndUp);
//   enquire.unregister(mdAndUp);
//   enquire.unregister(lgAndUp);
//   enquire.unregister(xlAndUp);
//   resetIcons();
// },

// const filter: debounce(function filterIcons(val) {
//   const value = val.trim();
//   const iconPack = activePack;
//   const regex = new RegExp(value, 'gi');
//   const filter = (icon) => icon.name.match(regex);
//
//   if (iconPack === fontAwesomeIconsCanonical.iconPack) {
//     const filtered = value.length === 0 ? groupIntoRows(faIconList, rowLength) : groupIntoRows(faIconList.filter(filter), rowLength);
//     fontAwesomeIcons.icons = filtered;
//   } else if (iconPack === materialIconsCanonical.iconPack) {
//     const filtered = value.length === 0 ? groupIntoRows(matIconList, rowLength) : groupIntoRows(matIconList.filter(filter), rowLength);
//     materialIcons.icons = filtered;
//   } else if (iconPack === 'Custom Icons') {
//     const filtered = value.length === 0 ? definitiveCustomIconList : definitiveCustomIconList.filter(filter);
//     customIconList = filtered;
//   }
// }, 250),

// const handleUploadedIcon = (response) => {
//   IconManagerService.addCustomIconCSS(response.cssDefinition);
//   const newIcon = { name: response.name, cssClass: response.cssClassName };
//   definitiveCustomIconList.push(newIcon);
//   customIconList = definitiveCustomIconList;
//   selectIcon(response.name, response.cssClassName, 'custom-icon');
// }
//
// const deleteIcon = (iconName, projectId) => {
//   IconManagerService.deleteIcon(iconName, projectId).then(() => {
//     definitiveCustomIconList = definitiveCustomIconList.filter((element) => element.filename !== iconName);
//     customIconList = definitiveCustomIconList;
//   });
// };



// const customIconUploadRequest = (customIcon) => {
//   $refs.validationProvider.validate(customIcon).then((res) => {
//     if (res && res.valid) {
//       disableCustomUpload = true;
//       const data = new FormData();
//       data.append('customIcon', customIcon);
//       FileUploadService.upload(uploadUrl, data, (response) => {
//         handleUploadedIcon(response.data);
//         // successToast('Success!', 'File successfully uploaded');
//         disableCustomUpload = false;
//       }, () => {
//         // errorToast('Error!', 'Encountered error when uploading icon');
//         disableCustomUpload = false;
//       });
//     }
//   });
// };

// const resetIcons = () => {
//   if ($refs.iconFilterInput.value.length > 0) {
//     setTimeout(() => {
//       fontAwesomeIcons.icons = groupIntoRows(faIconList, rowLength);
//       materialIcons.icons = groupIntoRows(matIconList, rowLength);
//       customIconList = definitiveCustomIconList;
//       if ($refs.iconFilterInput) {
//         $refs.iconFilterInput.value = '';
//       }
//     }, 100);
//   }
// };

</script>

<template xmlns:v-if="http://www.w3.org/1999/xlink">
    <div style="width: 50rem;">
      <input type="text" class="form-control mb-3" :placeholder="searchPlaceholder"
             @keyup="filter($event.target.value)" ref="iconFilterInput" data-cy="icon-search" aria-label="search by icon name">
      <Card>
        <template #content>
          <TabView content-class="mt-3" @tab-change="onChange" v-model:activeIndex="active">
            <TabPanel v-for="pack in iconPacks" :key="pack.packName">
              <template #header>
                <i :class="pack.headerIcon"></i> {{ pack.packName }}
              </template>
              <span v-if="pack.icons.length === 0 && activePack === pack.packName">No icons matched your search</span>
              <VirtualScroller v-if="activePack === pack.packName" :items="pack.icons" :itemSize="[100, 100]" orientation="both" style="height: 360px;" data-cy="fontAwesomeVirtualList">
                <template v-slot:item="{ item, options }">
                  <IconRow :item="item" :options="options" @icon-selected="getIcon($event, fontAwesomeIcons.iconPack)" />
                </template>
              </VirtualScroller>
            </TabPanel>

<!--            <TabPanel>-->
<!--              <template #header>-->
<!--                <i class="fas fa-wrench"></i> Custom-->
<!--              </template>-->
<!--              <div data-cy="customIconUpload">-->
<!--    &lt;!&ndash;            <ValidationProvider vid="customIcon" ref="validationProvider" name="Custom Icon" v-slot="{ errors }" rules="image|imageDimensions|duplicateFilename">&ndash;&gt;-->
<!--    &lt;!&ndash;              <b-form-file&ndash;&gt;-->
<!--    &lt;!&ndash;                  v-model="currentCustomIconFile"&ndash;&gt;-->
<!--    &lt;!&ndash;                  placeholder="Drag your file here to upload"&ndash;&gt;-->
<!--    &lt;!&ndash;                  @input="customIconUploadRequest" />&ndash;&gt;-->
<!--                <p class="text-muted text-right text-primary font-italic">* custom icons must be between 48px X 48px and 100px X 100px</p>-->

<!--    &lt;!&ndash;            <b-alert show variant="danger" v-show="errors[0]" class="text-center" data-cy="customIconErr">&ndash;&gt;-->
<!--    &lt;!&ndash;              <i class="fas fa-exclamation-circle"/> {{ errors[0] }} <i class="fas fa-exclamation-circle"/>&ndash;&gt;-->
<!--    &lt;!&ndash;            </b-alert>&ndash;&gt;-->
<!--    &lt;!&ndash;          </ValidationProvider>&ndash;&gt;-->

<!--                <div class="row text-info justify-content-center mt-4">-->
<!--                <div class="col-4 mb-4" v-for="{cssClassname, filename} in customIconList" :key="cssClassname">-->
<!--                  <div class="icon-item">-->
<!--                    <a-->
<!--                      href="#"-->
<!--                      @click.stop.prevent="getIcon({name, cssClassname}, 'Custom Icons')"-->
<!--                      :class="`item ${selectedCss === cssClassname ? 'selected' : ''}`">-->
<!--                      <span class="icon is-large text-info">-->
<!--                        <i :class="cssClassname"></i>-->
<!--                      </span>-->
<!--                    </a>-->
<!--                    <br/>-->
<!--                    <span class="iconName">-->
<!--                      <a class="delete-icon" ref="#" @click="deleteIcon(filename, route.params.projectId)">-->
<!--                        <span class="icon is-tiny"><i style="font-size:1rem;height:1rem;width:1rem;" class="fas fa-trash"></i></span>-->
<!--                      </a>-->
<!--                      <span>{{ filename }}</span>-->
<!--                    </span>-->
<!--                  </div>-->
<!--                </div>-->
<!--              </div>-->
<!--              </div>-->
<!--            </TabPanel>-->
          </TabView>
        </template>
      </Card>
    </div>
</template>

<style>
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
    visibility: hidden;
  }

  .is-tiny {
    height: .5rem;
    width: .5rem;
    font-size:.5rem;
    padding-right: .5rem;
  }

  .icon-item:hover .delete-icon {
    z-index: 1000;
    display: inline-block;
    visibility: visible;
  }

  .virtual-container {
    height: 320px;
    overflow-y: auto;
  }
</style>
