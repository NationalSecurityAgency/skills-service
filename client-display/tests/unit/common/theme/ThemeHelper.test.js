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
    it('test theme generation - colors', () => {
        const theme = {
            backgroundColor: '#626d7d',
            pageTitleTextColor: '#fdfbfb',
            textSecondaryColor: '#fdfdff',
            textPrimaryColor: '#fdf9f9',
        };
        const res = ThemeHelper.build(theme);
        const expected = 'body #app { background-color: #626d7d !important } '
            + 'body #app .skills-theme-bottom-border-with-background-color { border-bottom-color: #626d7d !important } '
            + 'body #app .skills-page-title-text-color { color: #fdfbfb !important } '
            + 'body #app .skills-badge .skills-badge-icon, body #app .skills-progress-info-card, body #app .skills-card-theme-border { border-color: #fdfbfb !important } '
            + 'body #app .text-muted, body #app .text-secondary, body #app .text-secondary a, body #app .skills-theme-secondary-color, body #app .skills-theme-menu-secondary-color, body #app .skills-theme-menu-secondary-color .dropdown-item { color: #fdfdff !important } '
            + 'body #app .text-primary, body #app, body #app .skills-navigable-item, body #app .skills-theme-primary-color, body #app .leaderboardTable tr td, body #app .skills-theme-menu-primary-color, body #app .skills-theme-menu-primary-color .dropdown-item { color: #fdf9f9 !important } '
            + 'body #app .skills-theme-menu-primary-color:hover, body #app .skills-theme-menu-primary-color .dropdown-item:hover { background-color: #fdf9f9 !important } '
            + 'body #app .skills-theme-filter-menu .dropdown-menu { border-color: #fdf9f9 !important } '
            + 'body #app .apexcharts-toolbar svg { fill: #fdf9f9 !important } '
            + 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer, body #app .apexcharts-menu.apexcharts-menu-open, body #app .dropdown-menu { background-color: #fff !important } '
            + 'body #app .skills-no-data-yet .fa-inverse, body #app .apexcharts-menu.apexcharts-menu-open .apexcharts-menu-item:hover, body #app .skills-theme-menu-primary-color, body #app .skills-theme-menu-primary-color .dropdown-item:hover { color: #fff !important } '
            + 'body #app .apexcharts-menu.open { color: black !important; } '
            + 'body #app .apexcharts-tooltip { color: black !important; }';
        expect(res.css).toEqual(expected);
    });

    it('stars', () => {
        const theme = {
                stars: {
                    unearnedColor: '#787886',
                    earnedColor: 'gold',
                },
        };
        const res = ThemeHelper.build(theme);
        const expected = 'body #app .star-empty { color: #787886 !important } '
            + 'body #app .star-filled { color: gold !important } '
            + 'body #app .apexcharts-menu.open { color: black !important; } '
            + 'body #app .apexcharts-tooltip { color: black !important; }';
        expect(res.css).toEqual(expected);
    });

    it('charts should end up in theme modules', () => {
        const theme = {
                charts: {
                    axisLabelColor: '#f9f1f1',
                },
        };
        const res = ThemeHelper.build(theme);
        const charts = res.themeModule.get('charts');
        expect(charts.axisLabelColor).toEqual('#f9f1f1');
    });

    it('test theme generation - pageTitleFontSize prop', () => {
        const theme = {
            pageTitleFontSize: '2rem',
        };
        const res = ThemeHelper.build(theme);
        const expected = 'body #app .skills-page-title-text-color .skills-title { font-size: 2rem !important } '
            + 'body #app .apexcharts-menu.open { color: black !important; } body #app .apexcharts-tooltip { color: black !important; }';
        expect(res.css).toEqual(expected);
    });

    it('test theme generation - backButton prop', () => {
        const theme = {
            backButton: {
                padding: '5px 10px',
                fontSize: '12px',
                lineHeight: '1.5',
            },
        };
        const res = ThemeHelper.build(theme);
        const expected = 'body #app .skills-page-title-text-color .skills-theme-btn { padding: 5px 10px !important } '
            + 'body #app .skills-page-title-text-color .skills-theme-btn { font-size: 12px !important } '
            + 'body #app .skills-page-title-text-color .skills-theme-btn { line-height: 1.5 !important } '
            + 'body #app .apexcharts-menu.open { color: black !important; } body #app .apexcharts-tooltip { color: black !important; }';
        expect(res.css).toEqual(expected);
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
