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
<div>
  <skills-display
    :options="options"
    :version="skillsVersion"
    :theme="themeObj"
    ref="skillsDisplayRef"
    @route-changed="skillsDisplayRouteChanged"/>
</div>
</template>

<script>
  import { SkillsDisplay } from '@skilltree/skills-client-vue';
  import SkillsDisplayOptionsMixin from './SkillsDisplayOptionsMixin';
  import MyProgressService from '@/components/myProgress/MyProgressService';

  export default {
    name: 'MyProjectSkillsPage',
    mixins: [SkillsDisplayOptionsMixin],
    components: {
      SkillsDisplay,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        skillsVersion: 2147483647, // max int
        theme: {
          disableSkillTreeBrand: true,
          disableBreadcrumb: true,
          landingPageTitle: `PROJECT: ${this.$route.params.name ? this.$route.params.name : this.$route.params.projectId}`,
          maxWidth: '100%',
          backgroundColor: '#f8f9fe',
          pageTitle: {
            textColor: '#212529',
            fontSize: '1.5rem',
            borderColor: '#dee2e6',
            borderStyle: 'none none solid none',
            backgroundColor: '#fff',
            textAlign: 'left',
            padding: '1.6rem 1rem 1.1rem 1rem',
            margin: '-10px -15px 1.6rem -15px',
          },
          backButton: {
            padding: '5px 10px',
            fontSize: '12px',
            lineHeight: '1.5',
          },
        },
        darkTheme: {
          disableSkillTreeBrand: false,
          disableBreadcrumb: false,
          maxWidth: '100%',
          backgroundColor: '#626d7d',
          pageTitle: {
            textColor: '#FFF',
            fontSize: '1.5rem',
          },
          textSecondaryColor: 'white',
          textPrimaryColor: 'white',
          stars: {
            unearnedColor: '#787886',
            earnedColor: 'gold',
          },
          progressIndicators: {
            beforeTodayColor: '#3e4d44',
            earnedTodayColor: '#667da4',
            completeColor: '#59ad52',
            incompleteColor: '#cdcdcd',
          },
          charts: {
            axisLabelColor: 'white',
          },
          tiles: {
            backgroundColor: '#152E4d',
            watermarkIconColor: '#a6c5f7',
          },
          buttons: {
            backgroundColor: '#152E4d',
            foregroundColor: '#59ad52',
          },
          graphLegendBorderColor: '1px solid grey',
        },
      };
    },
    mounted() {
      if (!this.$route.params.name) {
        MyProgressService.findProjectName(this.projectId).then((res) => {
          if (res) {
            this.$set(this.theme, 'landingPageTitle', `PROJECT: ${res.name}`);
          }
        });
      }
    },
    computed: {
      themeObj() {
        if (this.$route.query.classicSkillsDisplay && this.$route.query.classicSkillsDisplay.toLowerCase() === 'true') {
          const res = { ...this.theme };
          return Object.assign(res, {
            disableSkillTreeBrand: false,
            disableBreadcrumb: false,
            pageTitle: {
              textColor: '#212529',
              fontSize: '1.5rem',
            },
          });
        }

        if (this.$route.query.enableTheme && this.$route.query.enableTheme.toLowerCase() === 'true') {
          const res = { ...this.theme };
          return Object.assign(res, this.darkTheme);
        }

        return this.theme;
      },
    },
  };
</script>

<style scoped>

</style>
