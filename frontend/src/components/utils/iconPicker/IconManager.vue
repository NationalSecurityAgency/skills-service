<template xmlns:v-if="http://www.w3.org/1999/xlink">
    <div>
      <div class="field">
        <div class="control">
          <input type="text" class="input" :placeholder="searchPlaceholder" @keyup="filter($event.target.value)" ref="iconFilterInput">
        </div>
      </div>
      <b-tabs content-class="mt-3" justified @input="onChange($event)">
        <b-tab>
          <template slot="title">
            <i class="fab fa-font-awesome-flag"></i> <span>{{ fontAwesomeIcons.iconPack }}</span>
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
            <i class="mi mi-description"></i> {{ materialIcons.iconPack }}
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
          <file-upload :name="'customIcon'" :url="uploadUrl" :accept="acceptType"
                       @upload-success="handleUploadedIcon($event)" validate-images="true"
                       :image-height="customIconHeight" :image-width="customIconWidth"/>
          <span>* custom icons must be 48px X 48px</span><br />
          <div class="columns is-multiline">
            <div class="column" v-for="{cssClassname, filename} in customIconList" :key="cssClassname">
              <div class="icon-item">
                <a
                  href="#"
                  @click.stop.prevent="getIcon(name, cssClassname, 'Custom Icons')"
                  :class="`item ${selectedCss === cssClassname ? 'selected' : ''}`"
                >
                    <span class="icon is-large">
                      <i :class="cssClassname"></i>
                    </span>
                </a><br/>
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
    </div>
</template>

<script>
  import debounce from 'lodash.debounce';
  import VirtualList from 'vue-virtual-scroll-list';

  import FileUpload from '../upload/FileUpload';

  import fontAwesomeIconsCanonical from './font-awesome-index';
  import materialIconsCanonical from './material-index';
  import IconManagerService from './IconManagerService';

  const faIconList = fontAwesomeIconsCanonical.icons.slice();
  const matIconList = materialIconsCanonical.icons.slice();
  const customIconList = [];
  let definitiveCustomIconList = [];

  function groupIntoRows(array, rowLength) {
    let subArr = [];
    const result = [];
    for (let i = 0; i < array.length; i += 1) {
      if (i > 0 && i % rowLength === 0) {
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

  fontAwesomeIconsCanonical.icons = groupIntoRows(fontAwesomeIconsCanonical.icons, 5);
  materialIconsCanonical.icons = groupIntoRows(materialIconsCanonical.icons, 5);

  export default {
    name: 'IconManager',
    components: { FileUpload, 'virtual-list': VirtualList },
    props: {
      searchBox: String,
      customIconHeight: {
        type: Number,
        default: 48,
      },
      customIconWidth: {
        type: Number,
        default: 48,
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
      IconManagerService.getIconIndex(this.activeProjectId).then((response) => {
        if (response) {
          definitiveCustomIconList = response.slice();
          this.customIconList = response;
        }
      });
      this.$root.$on('bv::modal::hide', (bvEvent, modalId) => {
        if (modalId === 'icons') {
          this.resetIcons();
        }
      });
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
          const filtered = value.length === 0 ? groupIntoRows(faIconList, 5) : groupIntoRows(faIconList.filter(filter), 5);
          this.fontAwesomeIcons.icons = filtered;
        } else if (iconPack === materialIconsCanonical.iconPack) {
          const filtered = value.length === 0 ? groupIntoRows(matIconList, 5) : groupIntoRows(matIconList.filter(filter), 5);
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
      resetIcons() {
        if (this.$refs.iconFilterInput.value.length > 0) {
          setTimeout(() => {
            this.fontAwesomeIcons.icons = groupIntoRows(faIconList, 5);
            this.materialIcons.icons = groupIntoRows(matIconList, 5);
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
