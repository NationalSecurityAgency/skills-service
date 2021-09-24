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
    name: 'ThemePropsMixin',
    methods: {
      getThemeModule() {
        return this.$store.state.themeModule;
      },
      chartLabels() {
        const chartsModule = this.getThemeModule()?.charts;
        return {
          borderColor: chartsModule?.labelBorderColor ? chartsModule.labelBorderColor : '#143740',
          backgroundColor: chartsModule?.labelBackgroundColor ? chartsModule.labelBackgroundColor : '#007c49',
          foregroundColor: chartsModule?.labelForegroundColor ? chartsModule.labelForegroundColor : '#fff',
        };
      },
      pointHistoryChart() {
        const chartsModule = this.getThemeModule()?.charts?.pointHistory;
        return {
          lineColor: chartsModule?.lineColor ? chartsModule.lineColor : '#146c75',
          gradientStartColor: chartsModule?.gradientStartColor ? [chartsModule.gradientStartColor] : ['#00a4e8'],
          gradientStopColor: chartsModule?.gradientStopColor ? [chartsModule.gradientStopColor] : ['#fff'],
        };
      },
      infoCards() {
        const iconColors = this.getThemeModule()?.infoCards?.iconColors;
        return {
          iconColors: [
            iconColors && iconColors.length > 0 ? iconColors[0] : '#007c49',
            iconColors && iconColors.length > 1 ? iconColors[1] : '#ffc42b',
            iconColors && iconColors.length > 2 ? iconColors[2] : '#146c75',
            iconColors && iconColors.length > 3 ? iconColors[3] : '#290000',
          ],
        };
      },
    },
  };
</script>

<style scoped>

</style>
