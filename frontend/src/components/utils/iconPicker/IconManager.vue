<template xmlns:v-if="http://www.w3.org/1999/xlink">
    <div>
      <input type="text" class="form-control mb-3" :placeholder="searchPlaceholder" @keyup="filter($event.target.value)" ref="iconFilterInput">
      <b-card no-body>
      <b-tabs content-class="mt-3" @input="onChange($event)" card>
        <b-tab>
          <template slot="title">
            <i class="fab fa-font-awesome-flag"></i> {{ fontAwesomeIcons.iconPack }}
          </template>
            <span v-if="fontAwesomeIcons.icons.length === 0 && this.activePack === fontAwesomeIcons.iconPack">No icons matched your search</span>
            <virtual-list :size="60" :remain="5" :bench="10" wclass="scroll-container">
              <div class="icon-row" v-for="(row, index) in fontAwesomeIcons.icons" :key="`${row[0].cssClass}-${index}`">
                  <div class="icon-item" v-for="item in row" :key="item.cssClass">
                    <a
                      href="#"
                      @click.stop.prevent="getIcon(item.name, item.cssClass, fontAwesomeIcons.iconPack)"
                      :class="`item ${selectedCss === item.cssClass ? 'selected' : ''}`"
                    >
                      <span class="icon is-large">
                        <i :class="item.cssClass"></i>
                      </span>
                    </a><br/>
                    <span class="iconName">{{ item.name }}</span>
                  </div>
              </div>
            </virtual-list>
        </b-tab>
        <b-tab>
          <template slot="title">
            <i class="fas fa-file-alt"></i> {{ materialIcons.iconPack }}
          </template>
          <span v-if="materialIcons.icons.length === 0 && this.activePack === materialIcons.iconPack">No icons matched your search</span>
            <virtual-list :size="60" :remain="5" :bench="10" wclass="scroll-container">
              <div class="icon-row" v-for="(row, index) in materialIcons.icons" :key="index">
                <div class="icon-item" v-for="item in row" :key="item.cssClass">
                  <a
                    href="#"
                    @click.stop.prevent="getIcon(item.name, item.cssClass, materialIcons.iconPack)"
                    :class="`item ${selectedCss === item.cssClass ? 'selected' : ''}`"
                  >
                      <span class="icon is-large">
                        <i :class="item.cssClass"></i>
                      </span>
                  </a><br/>
                  <span class="iconName">{{ item.name }}</span>
                </div>
              </div>
            </virtual-list>
        </b-tab>
        <b-tab>
          <template slot="title">
            <i class="fas fa-wrench"></i> Custom
          </template>
          <file-upload data-vv-name="customIcon" v-validate.disable="'imageDimensions|duplicateFilename'"
                       :name="'customIcon'"
                       @file-selected="customIconUploadRequest"
                      :disable-input="disableCustomUpload"/>
          <p class="text-muted text-right text-primary font-italic">* custom icons must be between 48px X 48px and 100px X 100px</p>

          <b-alert show variant="danger" v-show="errors.has('customIcon')" class="text-center">
            <i class="fas fa-exclamation-circle"/> {{ errors.first('customIcon') }} <i class="fas fa-exclamation-circle"/>
          </b-alert>

          <div class="row text-info justify-content-center mt-4">
            <div class="col-4 mb-4" v-for="{cssClassname, filename} in customIconList" :key="cssClassname">
              <div class="icon-item">
                <a
                  href="#"
                  @click.stop.prevent="getIcon(name, cssClassname, 'Custom Icons')"
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
  import { Validator } from 'vee-validate';
  import FileUpload from '../upload/FileUpload';
  import FileUploadService from '../upload/FileUploadService';
  import fontAwesomeIconsCanonical from './font-awesome-index';
  import materialIconsCanonical from './material-index';
  import IconManagerService from './IconManagerService';
  import ToastSupport from '../ToastSupport';


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
    let subArr = [];
    const result = [];
    for (let i = 0; i < array.length; i += 1) {
      if (i > 0 && i % rl === 0) {
        result.push(subArr);
        subArr = [];
      }

      const item = array[i];
      subArr.push(item);
    }

    if (subArr.length > 0) {
      result.push(subArr);
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

  Validator.extend('imageDimensions', {
    getMessage(field, params, data) {
      return (data && data.message) || `Custom Icon must be ${self.customIconHeight} X ${self.customIconWidth}`;
    },
    validate(value) {
      return new Promise((resolve) => {
        if (value) {
          const file = value.get('customIcon');
          const isImageType = file.type.startsWith('image/');
          if (!isImageType) {
            resolve({
              valid: false,
              data: { message: 'File is not an image format' },
            });
          } else {
            const image = new Image();
            image.src = window.URL.createObjectURL(file);
            image.onload = () => {
              const width = image.naturalWidth;
              const height = image.naturalHeight;
              window.URL.revokeObjectURL(image.src);

              if (!isValidCustomIconDimensions(self, width, height)) {
                const dimensionRange = { min: self.minCustomIconDimensions.width, max: self.maxCustomIconDimensions.width };
                resolve({
                  valid: false,
                  data: {
                    message: `Invalid image dimensions, dimensions must be square and must be between ${dimensionRange.min} x ${dimensionRange.min} and ${dimensionRange.max} x ${dimensionRange.max} for ${file.name} `,
                  },
                });
              } else {
                resolve({
                  valid: true,
                });
              }
            };
          }
        } else {
          resolve({
            valid: true,
          });
        }
      });
    },
  }, {
    immediate: false,
  });

  Validator.extend('duplicateFilename', {
    getMessage(field, params, data) {
      return (data && data.message) || 'Custom Icon with this filename already exists';
    },
    validate(value) {
      return new Promise((resolve) => {
        if (value) {
          const file = value.get('customIcon');

          const index = definitiveCustomIconList.findIndex(item => item.filename === file.name);
          if (index >= 0) {
            resolve({
              valid: false,
              data: { message: `Custom Icon with filename ${file.name} already exists` },
            });
            return;
          }
        }
        resolve({ valid: true });
      });
    },
  }, {
    immediate: false,
  });

  const validateIconDimensions = (dimensions) => {
    const { width, height } = dimensions;
    let isValid = true;

    if (!width || !height) {
      console.error('width and height are required dimensions');
      isValid = false;
    }

    if (isValid) {
      isValid = width / height === 1;
      if (!isValid) {
        console.error('Icon dimensions must be square');
      }
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
        return `/admin/projects/${this.activeProjectId}/icons/upload`;
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
      getIcon(icon, iconCss, iconPack) {
        this.selected = icon;
        this.selectedCss = iconCss;
        this.selectedIconPack = iconPack;

        this.selectIcon(icon, iconCss, iconPack);
      },
      onChange(tabIndex) {
        const { value } = this.$refs.iconFilterInput;
        if (tabIndex === 0) {
          this.activePack = fontAwesomeIconsCanonical.iconPack;
          this.filter(value);
        } else if (tabIndex === 1) {
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
        const filter = icon => icon.name.match(regex);

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
          definitiveCustomIconList = definitiveCustomIconList.filter(element => element.filename !== iconName);
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
        this.$validator.validate().then((res) => {
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
            this.$refs.iconFilterInput.value = '';
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
</style>
