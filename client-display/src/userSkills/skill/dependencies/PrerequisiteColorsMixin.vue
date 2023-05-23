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
<script>
  export default {
    name: 'PrerequisiteColorsMixin',
    methods: {
      getBadgeColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.badgeColor;
        if (themeColor) {
          return themeColor;
        }
        return 'indigo';
      },
      getSkillColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.skillColor;
        if (themeColor) {
          return themeColor;
        }
        return 'orange';
      },
      getThisSkillColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.thisSkillColor;
        if (themeColor) {
          return themeColor;
        }
        return '#00a4e8';
      },
      getAchievedColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.achievedColor;
        if (themeColor) {
          return themeColor;
        }
        return 'green';
      },
      getThemeNavButtonsColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.navButtonsColor;
        return themeColor;
      },
      getThemeTextPrimaryColor() {
        const themeColor = this.$store.state.themeModule?.textPrimaryColor;
        return themeColor;
      },
    },
  };
</script>
