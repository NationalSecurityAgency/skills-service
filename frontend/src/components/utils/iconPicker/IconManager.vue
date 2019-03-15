<template xmlns:v-if="http://www.w3.org/1999/xlink">
  <div class="modal-card" style="width: 800px; height: 600px;">
    <header class="modal-card-head">
      <p class="modal-card-title">Select Icon</p>
      <button class="delete" aria-label="close" v-on:click="close()"></button>
    </header>

    <section class="modal-card-body">
      <div class="field">
        <div class="control">
          <input type="text" class="input" :placeholder="searchPlaceholder" @keyup="filter($event.target.value)" ref="iconFilterInput">
        </div>
      </div>
      <b-tabs type="is-boxed" @change="onChange($event)" class="skills-pad-top-1-rem" size="is-medium">
        <span v-if="fontAwesomeIcons.icons.length === 0 && this.activePack == fontAwesomeIcons.iconPack">No icons matched your search</span>
        <b-tab-item>
          <template slot="header">
            <i class="fab fa-font-awesome-flag"></i> <span>{{ fontAwesomeIcons.iconPack }}</span>
          </template>

            <virtual-list :size="60" :remain="5" :bench="10" wclass="scroll-container">
              <div class="icon-row" v-for="(row, index) in fontAwesomeIcons.icons" :key="index">
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
        </b-tab-item>
        <b-tab-item :label="materialIcons.iconPack">
          <template slot="header">
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
        </b-tab-item>
        <b-tab-item>
          <template slot="header">
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
        </b-tab-item>
      </b-tabs>
    </section>
  </div>
</template>

<script>
  import debounce from 'lodash.debounce';
  import VirtualList from 'vue-virtual-scroll-list';

  import FileUpload from '../upload/FileUpload';

  import fontAwesomeIcons from './font-awesome-index';
  import materialIcons from './material-index';
  import IconManagerService from './IconManagerService';

  const faIconList = fontAwesomeIcons.icons.slice();
  const matIconList = materialIcons.icons.slice();
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

  fontAwesomeIcons.icons = groupIntoRows(fontAwesomeIcons.icons, 5);
  materialIcons.icons = groupIntoRows(materialIcons.icons, 5);

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
        activePack: fontAwesomeIcons.iconPack,
        fontAwesomeIcons,
        materialIcons,
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
    },
    methods: {
      getIcon(icon, iconCss, iconPack) {
        this.selected = icon;
        this.selectedCss = iconCss;
        this.selectedIconPack = iconPack;

        this.selectIcon(icon, iconCss, iconPack);
      },
      onChange(tabIndex) {
        const value = this.$refs.iconFilterInput.value;
        if (tabIndex === 0) {
          this.activePack = fontAwesomeIcons.iconPack;
          this.filter(value);
        } else if (tabIndex === 1) {
          this.activePack = materialIcons.iconPack;
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

        if (iconPack === fontAwesomeIcons.iconPack) {
          this.fontAwesomeIcons.icons = value.length === 0 ? groupIntoRows(faIconList, 5) : groupIntoRows(faIconList.filter(filter), 5);
        } else if (iconPack === materialIcons.iconPack) {
          this.materialIcons.icons = value.length === 0 ? groupIntoRows(matIconList, 5) : groupIntoRows(matIconList.filter(filter), 5);
        } else if (iconPack === 'Custom Icons') {
          this.customIconList = value.length === 0 ? definitiveCustomIconList : definitiveCustomIconList.filter(filter);
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
        this.close();
      },
      close() {
        this.resetIcons();
        this.$parent.close();
      },
      resetIcons() {
        if (this.$refs.iconFilterInput.value.length > 0) {
          setTimeout(() => {
            this.fontAwesomeIcons.icons = groupIntoRows(faIconList, 5);
            this.materialIcons.icons = groupIntoRows(matIconList, 5);
            this.customIconList = definitiveCustomIconList;
          }, 50);
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
