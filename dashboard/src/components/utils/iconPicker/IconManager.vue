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
<template xmlns:v-if="http://www.w3.org/1999/xlink">
    <div>
      <input type="text" class="form-control mb-3" :placeholder="searchPlaceholder"
             @keyup="filter($event.target.value)" ref="iconFilterInput" data-cy="icon-search" aria-label="search by icon name">
      <b-card no-body>
      <b-tabs content-class="mt-3" @input="onChange($event)" card>
        <b-tab>
          <template slot="title">
            <i class="fab fa-font-awesome-flag"></i> {{ fontAwesomeIcons.iconPack }}
          </template>
            <span v-if="fontAwesomeIcons.icons.length === 0 && this.activePack === fontAwesomeIcons.iconPack">No icons matched your search</span>
              <virtual-list style="height: 360px; overflow-y: auto;"
                            :keeps="5"
                            :data-key="uniqueIdGenerator"
                            :data-sources="fontAwesomeIcons.icons"
                            :data-component="rowItemComponent"
                            wrap-class="scroll-container"
                            data-cy="fontAwesomeVirtualList"
                            @icon-selected="getIcon($event, fontAwesomeIcons.iconPack)"
                            ref="fontAwesomeVirtualList">
            </virtual-list>
        </b-tab>
        <b-tab>
          <template slot="title">
            <i class="fas fa-file-alt"></i> {{ materialIcons.iconPack }}
          </template>
          <span v-if="materialIcons.icons.length === 0 && this.activePack === materialIcons.iconPack">No icons matched your search</span>
            <virtual-list style="height: 360px; overflow-y: auto;"
                          :keeps="5"
                          :data-key="uniqueIdGenerator"
                          :data-sources="materialIcons.icons"
                          :data-component="rowItemComponent"
                          wrap-class="scroll-container"
                          data-cy="materialVirtualList"
                          @icon-selected="getIcon($event, materialIcons.iconPack)"
                          ref="materialVirtualList">
            </virtual-list>
        </b-tab>
        <b-tab>
          <template slot="title">
            <i class="fas fa-wrench"></i> Custom
          </template>
          <ValidationProvider vid="customIcon" ref="validationProvider" name="Custom Icon" v-slot="{ validate, errors }" rules="image|imageDimensions|duplicateFilename">
            <file-upload
                         :name="'customIcon'"
                         @file-selected="customIconUploadRequest"
                        :disable-input="disableCustomUpload"/>
            <p class="text-muted text-right text-primary font-italic">* custom icons must be between 48px X 48px and 100px X 100px</p>

            <b-alert show variant="danger" v-show="errors[0]" class="text-center">
              <i class="fas fa-exclamation-circle"/> {{ errors[0] }} <i class="fas fa-exclamation-circle"/>
            </b-alert>
          </ValidationProvider>

          <div class="row text-info justify-content-center mt-4">
            <div class="col-4 mb-4" v-for="{cssClassname, filename} in customIconList" :key="cssClassname">
              <div class="icon-item">
                <a
                  href="#"
                  @click.stop.prevent="getIcon({name, cssClassname}, 'Custom Icons')"
                  :class="`item ${selectedCss === cssClassname ? 'selected' : ''}`">
                  <span class="icon is-large text-info">
                    <i :class="cssClassname"></i>
                  </span>
                </a>
                <br/>
                <span class="iconName">
                  <a class="delete-icon" ref="#" @click="deleteIcon(filename, activeProjectId)">
                    <span class="icon is-tiny"><i style="font-size:1rem;height:1rem;width:1rem;" class="fas fa-trash"></i></span>
                  </a>
                  <span>{{ filename }}</span>
                </span>
              </div>
            </div>
          </div>
        </b-tab>
      </b-tabs>
      </b-card>
    </div>
</template>

<script>
  import debounce from 'lodash.debounce';
  import VirtualList from 'vue-virtual-scroll-list';
  import enquire from 'enquire.js';
  import { extend } from 'vee-validate';
  import { image } from 'vee-validate/dist/rules';
  import FileUpload from '../upload/FileUpload';
  import FileUploadService from '../upload/FileUploadService';
  import fontAwesomeIconsCanonical from './font-awesome-index';
  import materialIconsCanonical from './material-index';
  import IconManagerService from './IconManagerService';
  import ToastSupport from '../ToastSupport';
  import IconRow from './IconRow';
  import GroupedIcons from './GroupedIcons';

  const faIconList = fontAwesomeIconsCanonical.icons.slice();
  const matIconList = materialIconsCanonical.icons.slice();
  const customIconList = [];

  const xsAndSmaller = '(max-width: 575.98px)';
  const smAndUp = '(min-width: 576px) and (max-width: 767.98px)';
  const mdAndUp = '(min-width: 768px) and (max-width: 991.98px)';
  const lgAndUp = '(min-width: 992px) and (max-width: 1199.98px)';
  const xlAndUp = '(min-width: 1200px)';

  let definitiveCustomIconList = [];

  let rowLength = 5;
  let self = null;

  function groupIntoRows(array, rl) {
    let grouped = new GroupedIcons([]);
    const result = [];
    for (let i = 0; i < array.length; i += 1) {
      if (i > 0 && i % rl === 0) {
        result.push(grouped);
        grouped = new GroupedIcons([]);
      }

      const item = array[i];
      grouped.row.push(item);
    }

    if (grouped.row.length > 0) {
      result.push(grouped);
    }

    return result;
  }

  fontAwesomeIconsCanonical.icons = groupIntoRows(fontAwesomeIconsCanonical.icons, rowLength);
  materialIconsCanonical.icons = groupIntoRows(materialIconsCanonical.icons, rowLength);

  const isValidCustomIconDimensions = (vm, width, height) => {
    let isValid = width / height === 1;
    if (isValid) {
      isValid = vm.minCustomIconDimensions.width <= width && vm.minCustomIconDimensions.height <= height;
      isValid = isValid && vm.maxCustomIconDimensions.width >= width && vm.maxCustomIconDimensions.height >= height;
    }
    return isValid;
  };

  extend('image', {
    ...image,
    message: 'File is not an image format',
  });
  extend('imageDimensions', {
    message: () => `Invalid image dimensions, dimensions must be square and must be between ${self.minCustomIconDimensions.width} x ${self.minCustomIconDimensions.width} and ${self.maxCustomIconDimensions.width} x ${self.maxCustomIconDimensions.width}`,
    validate(value) {
      return new Promise((resolve) => {
        if (value) {
          const file = value.form.get('customIcon');
          const customIcon = new Image();
          customIcon.src = window.URL.createObjectURL(file);
          customIcon.onload = () => {
            const width = customIcon.naturalWidth;
            const height = customIcon.naturalHeight;
            window.URL.revokeObjectURL(customIcon.src);

            if (!isValidCustomIconDimensions(self, width, height)) {
              resolve({
                valid: false,
              });
            } else {
              resolve({
                valid: true,
              });
            }
          };
        } else {
          resolve({
            valid: true,
          });
        }
      });
    },
  });

  extend('duplicateFilename', {
    message: 'Custom Icon with this filename already exists',
    validate(value) {
      return new Promise((resolve) => {
        if (value) {
          const file = value.form.get('customIcon');

          const index = definitiveCustomIconList.findIndex((item) => item.filename === file.name);
          if (index >= 0) {
            resolve({
              valid: false,
            });
            return;
          }
        }
        resolve({ valid: true });
      });
    },
  });

  const validateIconDimensions = (dimensions) => {
    const { width, height } = dimensions;
    let isValid = true;

    if (!width || !height) {
      isValid = false;
    }

    if (isValid) {
      isValid = width / height === 1;
    }

    return isValid;
  };

  export default {
    name: 'IconManager',
    components: { FileUpload, 'virtual-list': VirtualList },
    mixins: [ToastSupport],
    props: {
      searchBox: String,
      maxCustomIconDimensions: {
        type: Object,
        default() {
          return {
            width: 100,
            height: 100,
          };
        },
        validator: validateIconDimensions,
      },
      minCustomIconDimensions: {
        type: Object,
        default() {
          return {
            width: 48,
            height: 48,
          };
        },
        validator: validateIconDimensions,
      },
    },
    data() {
      return {
        acceptType: 'image/*',
        selected: '',
        selectedCss: '',
        selectedIconPack: '',
        activePack: fontAwesomeIconsCanonical.iconPack,
        fontAwesomeIcons: fontAwesomeIconsCanonical,
        materialIcons: materialIconsCanonical,
        customIconList,
        disableCustomUpload: false,
        rowItemComponent: IconRow,
      };
    },
    computed: {
      searchPlaceholder() {
        return this.searchbox || 'Type to filter icons...';
      },
      activeProjectId() {
        return this.$store.state.projectId;
      },
      uploadUrl() {
        let uploadUrl = `/admin/projects/${this.activeProjectId}/icons/upload`;
        if (!this.activeProjectId) {
          uploadUrl = '/supervisor/icons/upload';
        }
        return uploadUrl;
      },
    },
    mounted() {
      self = this;
      IconManagerService.getIconIndex(this.activeProjectId).then((response) => {
        if (response) {
          definitiveCustomIconList = response.slice();
          this.customIconList = response;
        }
      });

      enquire.register(xsAndSmaller, () => {
        rowLength = 2;
        this.groupRows();
      });
      enquire.register(smAndUp, () => {
        rowLength = 3;
        this.groupRows();
      });
      enquire.register(mdAndUp, () => {
        rowLength = 3;
        this.groupRows();
      });
      enquire.register(lgAndUp, () => {
        rowLength = 5;
        this.groupRows();
      });
      enquire.register(xlAndUp, () => {
        rowLength = 6;
        this.groupRows();
      });
    },
    beforeDestroy() {
      enquire.unregister(xsAndSmaller);
      enquire.unregister(smAndUp);
      enquire.unregister(mdAndUp);
      enquire.unregister(lgAndUp);
      enquire.unregister(xlAndUp);
      this.resetIcons();
    },
    methods: {
      uniqueIdGenerator(groupedIcons) {
        return groupedIcons.id;
      },
      getIcon(event, iconPack) {
        this.selected = event.icon;
        this.selectedCss = event.cssClass;
        this.selectedIconPack = iconPack;

        this.selectIcon(event.icon, event.cssClass, iconPack);
      },
      onChange(tabIndex) {
        const { value } = this.$refs.iconFilterInput;
        if (tabIndex === 0) {
          this.$refs.fontAwesomeVirtualList.reset();
          this.activePack = fontAwesomeIconsCanonical.iconPack;
          this.filter(value);
        } else if (tabIndex === 1) {
          this.$refs.materialVirtualList.reset();
          this.activePack = materialIconsCanonical.iconPack;
          this.filter(value);
        } else if (tabIndex === 2) {
          this.activePack = 'Custom Icons';
        }
      },
      filter: debounce(function filterIcons(val) {
        const value = val.trim();
        const iconPack = this.activePack;
        const regex = new RegExp(value, 'gi');
        const filter = (icon) => icon.name.match(regex);

        if (iconPack === fontAwesomeIconsCanonical.iconPack) {
          const filtered = value.length === 0 ? groupIntoRows(faIconList, rowLength) : groupIntoRows(faIconList.filter(filter), rowLength);
          this.fontAwesomeIcons.icons = filtered;
        } else if (iconPack === materialIconsCanonical.iconPack) {
          const filtered = value.length === 0 ? groupIntoRows(matIconList, rowLength) : groupIntoRows(matIconList.filter(filter), rowLength);
          this.materialIcons.icons = filtered;
        } else if (iconPack === 'Custom Icons') {
          const filtered = value.length === 0 ? definitiveCustomIconList : definitiveCustomIconList.filter(filter);
          this.customIconList = filtered;
        }
      }, 250),
      handleUploadedIcon(response) {
        IconManagerService.addCustomIconCSS(response.cssDefinition);
        const newIcon = { name: response.name, cssClass: response.cssClassName };
        definitiveCustomIconList.push(newIcon);
        this.customIconList = definitiveCustomIconList;
        this.selectIcon(response.name, response.cssClassName, 'custom-icon');
      },
      deleteIcon(iconName, projectId) {
        IconManagerService.deleteIcon(iconName, projectId).then(() => {
          definitiveCustomIconList = definitiveCustomIconList.filter((element) => element.filename !== iconName);
          this.customIconList = definitiveCustomIconList;
        });
      },
      selectIcon(icon, iconCss, iconPack) {
        const result = {
          name: icon,
          css: iconCss,
          pack: iconPack,
        };

        this.$emit('selected-icon', result);
      },
      customIconUploadRequest(event) {
        this.$refs.validationProvider.validate(event).then((res) => {
          if (res) {
            this.disableCustomUpload = true;
            FileUploadService.upload(this.uploadUrl, event.form, (response) => {
              self.handleUploadedIcon(response.data);
              self.successToast('Success!', 'File successfully uploaded');
              this.disableCustomUpload = false;
            }, (err) => {
              self.errorToast('Error!', 'Encountered error when uploading icon');
              this.disableCustomUpload = false;
              throw err;
            });
          }
        });
      },
      groupRows() {
        this.fontAwesomeIcons.icons = groupIntoRows([].concat(...this.fontAwesomeIcons.icons), rowLength);
        this.materialIcons.icons = groupIntoRows([].concat(...this.materialIcons.icons), rowLength);
      },
      resetIcons() {
        if (this.$refs.iconFilterInput.value.length > 0) {
          setTimeout(() => {
            this.fontAwesomeIcons.icons = groupIntoRows(faIconList, rowLength);
            this.materialIcons.icons = groupIntoRows(matIconList, rowLength);
            this.customIconList = definitiveCustomIconList;
            if (this.$refs.iconFilterInput) {
              this.$refs.iconFilterInput.value = '';
            }
          }, 100);
        }
      },
    },
  };

</script>

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
