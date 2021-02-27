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
<template>

  <div class="bg-white header">
    <div class="container-fluid py-3">
      <div class="row">
        <div class="col-sm pl-0">
          <div class="text-center text-sm-left">
            <router-link class="h2 text-primary ml-2" to="/">
              <img ref="skillTreeLogo" src="/static/img/skilltree_logo_v1.png" alt="skilltree logo"/>
            </router-link>
            <span v-if="isAdminPage" ref="adminStamp" class="skills-stamp">ADMIN</span>
          </div>
        </div>

        <hr class="w-75 mb-0 d-sm-none"/>

        <div class="col-sm-auto text-center text-sm-right pt-sm-2 mt-3 mt-sm-0">
          <inception-button v-if="isAdminPage" class="mr-2" data-cy="inception-button"></inception-button>
          <settings-button data-cy="settings-button" class="mr-2"/>
          <help-button class=""/>
        </div>
      </div>
    </div>
    <breadcrumb></breadcrumb>
  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';
  import InceptionButton from '../inception/InceptionButton';
  import HelpButton from './HelpButton';

  export default {
    name: 'Header',
    components: {
      HelpButton,
      InceptionButton,
      Breadcrumb,
      SettingsButton,
    },
    mounted() {
      window.addEventListener('resize', this.updateAdminStampSize);
      this.$nextTick(() => {
        this.updateAdminStampSize();
      });
    },
    updated() {
      this.$nextTick(() => {
        this.updateAdminStampSize();
      });
    },
    methods: {
      updateAdminStampSize() {
        const windowWidth = window.innerWidth;
        if (windowWidth && windowWidth > 0) {
          const { adminStamp, skillTreeLogo } = this.$refs;
          if (skillTreeLogo && adminStamp) {
            const dimensions = skillTreeLogo.getBoundingClientRect();
            if (dimensions && dimensions.width && dimensions.width > 0) {
              const left = (dimensions.x + dimensions.width) + 5;
              adminStamp.style.left = `${left}px`;
            }
            if (windowWidth < 675) {
              adminStamp.style['line-height'] = '12px';
              adminStamp.style['font-size'] = '14px';
              adminStamp.style.width = '85px';
            } else {
              adminStamp.style['line-height'] = '22px';
              adminStamp.style['font-size'] = '24px';
              adminStamp.style.width = '155px';
            }
          }
        }
      },
    },
    computed: {
      isAdminPage() {
        return this.$route && this.$route.meta && this.$route.meta.requiresAuth && !this.$route.meta.nonAdmin;
      },
    },
    beforeDestroy() {
      window.removeEventListener('resize', this.updateAdminStampSize);
    },
  };
</script>

<style scoped>
/* black-ops-one-regular - latin-ext_latin */
@font-face {
  font-family: 'Black Ops One';
  font-style: normal;
  font-weight: 400;
  src: url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.eot'); /* IE9 Compat Modes */
  src: local(''),
  url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.eot?#iefix') format('embedded-opentype'), /* IE6-IE8 */
  url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.woff2') format('woff2'), /* Super Modern Browsers */
  url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.woff') format('woff'), /* Modern Browsers */
  url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.ttf') format('truetype'), /* Safari, Android, iOS */
  url('../../fonts/black-ops-one-v12-latin-ext_latin-regular.svg#BlackOpsOne') format('svg'); /* Legacy iOS */
}

.skills-stamp {
  /* Abs positioning makes it not take up vert space */
  position: absolute;
  bottom:8px;
  left: 203px;

  box-shadow: 0 0 0 3px red, 0 0 0 2px red inset;
  border: 2px solid transparent;
  border-radius: 4px;
  display: inline-block;
  padding: 5px 2px;
  line-height: 22px;
  color: red;
  font-size: 24px;
  font-family: 'Black Ops One', cursive;
  text-transform: uppercase;
  text-align: center;
  opacity: 0.4;
  width: 155px;
  transform: rotate(-17deg);
}
</style>
