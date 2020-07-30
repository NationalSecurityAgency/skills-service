/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import ThemeHelper from '@/common/theme/ThemeHelper';

describe('ThemeHelper', () => {
    it('test theme generation', () => {
        const theme = {
            backgroundColor: '#626d7d',
            pageTitleTextColor: '#fdfbfb',
            textSecondaryColor: '#fdfdff',
            textPrimaryColor: '#fdf9f9',
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
                axisLabelColor: '#f9f1f1',
            },
            tiles: {
                backgroundColor: '#152E4d',
                watermarkIconColor: '#a6c5f7',
            },
            graphLegendBorderColor: '1px solid grey',
        };
        const res = ThemeHelper.build(theme);
        const expected = 'body #app { background-color: #626d7d !important } '
            + 'body #app .skills-page-title-text-color, body #app .skills-page-title-text-color button { color: #fdfbfb !important } '
            + 'body #app .skills-page-title-text-color button, body #app .skills-badge .skills-badge-icon, body #app .skills-progress-info-card, body #app .skills-card-theme-border { border-color: #fdfbfb !important } '
            + 'body #app .skills-page-title-text-color button:hover { background-color: #fdfbfb !important } '
            + 'body #app .text-muted, body #app .text-secondary { color: #fdfdff !important } '
            + 'body #app .text-primary, body #app, body #app .skills-navigable-item { color: #fdf9f9 !important } '
            + 'body #app .star-empty { color: #787886 !important } '
            + 'body #app .star-filled { color: gold !important } '
            + 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer { background-color: #152E4d !important } '
            + 'body #app .skills-page-title-text-color button:hover, body #app .skills-no-data-yet .fa-inverse { color: #152E4d !important } '
            + 'body #app .card-body .watermark-icon { color: #a6c5f7 !important } '
            + 'body #app .graph-legend .card-header, '
            + 'body #app .graph-legend .card-body { border: 1px solid grey !important } '
            + 'body #app .apexcharts-menu.open { color: black !important; } '
            + 'body #app .apexcharts-tooltip { color: black !important; }';
        expect(res.css).toEqual(expected);

        const progressIndicators = res.themeModule.get('progressIndicators');
        expect(progressIndicators.beforeTodayColor).toEqual('#3e4d44');
        expect(progressIndicators.earnedTodayColor).toEqual('#667da4');
        expect(progressIndicators.completeColor).toEqual('#59ad52');
        expect(progressIndicators.incompleteColor).toEqual('#cdcdcd');

        const charts = res.themeModule.get('charts');
        expect(charts.axisLabelColor).toEqual('#f9f1f1');
    });

    it('bad theme - misspelled key', () => {
        const theme = {
            backgroundColor: '#626d7d',
            pageTitleTextColor3: 'white',
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
            graphLegendBorderColor: '1px solid grey',
        };
        expect(() => {
            ThemeHelper.build(theme);
        }).toThrow(new Error(`Skills Theme Error! Failed to process provided custom theme due to invalid format! JSON key of [pageTitleTextColor3] is not supported (Is it misspelled?). Theme is ${JSON.stringify(theme)}`));
    });

    it('bad theme - missing value', () => {
        const theme = {
            backgroundColor: '#626d7d',
            pageTitleTextColor: 'white',
            textSecondaryColor: 'white',
            textPrimaryColor: 'white',
            stars: {
                unearnedColor: '#787886',
                earnedColor: '',
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
            graphLegendBorderColor: '1px solid grey',
        };
        expect(() => {
            ThemeHelper.build(theme);
        }).toThrow(new Error(`Skills Theme Error! Failed to process provided custom theme due to invalid format! JSON key of [earnedColor] has empty/undefined value. Theme is ${JSON.stringify(theme)}`));
    });
});
