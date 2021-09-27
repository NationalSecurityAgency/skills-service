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
  const colors = {
    info: '#146c75',
    primary: '#143740',
    secondary: '#60737b',
    warning: '#ffc42b',
    success: '#007c49',
    danger: '#290000',
    white: '#fff',
    pointHistoryGradientStartColor: '#00a4e8',
  };
  export default {
    name: 'ThemePropsMixin',
    methods: {
      getThemeModule() {
        return this.$store.state.themeModule;
      },
      chartLabels() {
        const chartsModule = this.getThemeModule()?.charts;
        return {
          borderColor: chartsModule?.labelBorderColor ? chartsModule.labelBorderColor : colors.primary,
          backgroundColor: chartsModule?.labelBackgroundColor ? chartsModule.labelBackgroundColor : colors.success,
          foregroundColor: chartsModule?.labelForegroundColor ? chartsModule.labelForegroundColor : colors.white,
        };
      },
      pointHistoryChart() {
        const chartsModule = this.getThemeModule()?.charts?.pointHistory;
        return {
          lineColor: chartsModule?.lineColor ? chartsModule.lineColor : colors.info,
          gradientStartColor: chartsModule?.gradientStartColor ? [chartsModule.gradientStartColor] : [colors.pointHistoryGradientStartColor],
          gradientStopColor: chartsModule?.gradientStopColor ? [chartsModule.gradientStopColor] : [colors.white],
        };
      },
      infoCards() {
        const iconColors = this.getThemeModule()?.infoCards?.iconColors;
        return {
          iconColors: [
            iconColors && iconColors.length > 0 ? iconColors[0] : colors.success,
            iconColors && iconColors.length > 1 ? iconColors[1] : colors.warning,
            iconColors && iconColors.length > 2 ? iconColors[2] : colors.info,
            iconColors && iconColors.length > 3 ? iconColors[3] : colors.danger,
          ],
        };
      },
    },
  };
</script>

<style scoped>

</style>
