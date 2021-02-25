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
export default {
  nonCSSConfig: ['progressIndicators', 'charts'],
  selectorKey: {
    backgroundColor: [{
      selector: 'body #app',
      styleName: 'background-color',
    }, {
      selector: 'body #app .skills-theme-bottom-border-with-background-color',
      styleName: 'border-bottom-color',
    }],
    trophyIconColor: {
      selector: 'body #app .fa.fa-trophy',
      styleName: 'color',
    },
    subjectTileIconColor: {
      selector: 'body #app .subject-tile-icon',
      styleName: 'color',
    },
    pageTitleTextColor: [{
      selector: 'body #app .skills-page-title-text-color',
      styleName: 'color',
    }, {
      selector: 'body #app .skills-badge .skills-badge-icon, body #app .skills-progress-info-card, body #app .skills-card-theme-border',
      styleName: 'border-color',
    }],
    circleProgressInteriorTextColor: {
      selector: 'body #app .circle-number span',
      styleName: 'color',
    },
    textPrimaryColor: {
      selector: 'body #app .text-primary, body #app, body #app .skills-navigable-item',
      styleName: 'color',
    },
    textPrimaryMutedColor: {
      selector: 'body #app .text-primary .text-muted, body #app .text-primary.text-muted',
      styleName: 'color',
    },
    textSecondaryColor: {
      selector: 'body #app .text-muted, body #app .text-secondary, body #app .text-secondary a',
      styleName: 'color',
    },
    tiles: {
      backgroundColor: [{
        selector: 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer',
        styleName: 'background-color',
      }, {
        selector: 'body #app .skills-no-data-yet .fa-inverse',
        styleName: 'color',
      }],
      watermarkIconColor: {
        selector: 'body #app .card-body .watermark-icon',
        styleName: 'color',
      },
    },
    stars: {
      unearnedColor: {
        selector: 'body #app .star-empty',
        styleName: 'color',
      },
      earnedColor: {
        selector: 'body #app .star-filled',
        styleName: 'color',
      },
    },
    graphLegendBorderColor: {
      selector: 'body #app .graph-legend .card-header, body #app .graph-legend .card-body',
      styleName: 'border',
    },
    buttons: {
      backgroundColor: [{
        selector: 'body #app .skills-theme-btn',
        styleName: 'background-color',
      }, {
        selector: 'body #app .skills-theme-btn:hover',
        styleName: 'color',
      }, {
        selector: 'body #app .skills-theme-btn:hover',
        styleName: 'border-color',
      }],
      foregroundColor: [{
        selector: 'body #app .skills-theme-btn',
        styleName: 'color',
      }, {
        selector: 'body #app .skills-theme-btn',
        styleName: 'border-color',
      }, {
        selector: 'body #app .skills-theme-btn:hover',
        styleName: 'background-color',
      }],
    },
  },

  build(theme) {
    const res = {
      css: '',
      themeModule: new Map(),
    };

    const appendCSS = (selectorKeyElement, inputThemeElement) => {
      if (!(selectorKeyElement.selector && selectorKeyElement.styleName)) {
        throw new Error(`Bug in the custom theme code. Both selector and styleName must be present for [${selectorKeyElement}]`);
      }

      // No injection
      const sanitizedValue = inputThemeElement.split(';')[0];
      res.css += `${selectorKeyElement.selector} { ${selectorKeyElement.styleName}: ${sanitizedValue} !important } `;
    };

    const validateInputElement = (key, element, keyMsg) => {
      if (!element) {
        throw new Error(`Skills Theme Error! Failed to process provided custom theme due to invalid format! JSON key of [${key}] ${keyMsg}. Theme is ${JSON.stringify(theme)}`);
      }
    };

    const populateResult = (selectorKey, inputTheme) => {
      Object.keys(inputTheme)
        .forEach((key) => {
          const isCSSConfig = !this.nonCSSConfig.includes(key);
          if (isCSSConfig) {
            const selectorKeyElement = selectorKey[key];
            validateInputElement(key, selectorKeyElement, 'is not supported (Is it misspelled?)');
            const inputThemeElement = inputTheme[key];
            validateInputElement(key, inputThemeElement, 'has empty/undefined value');

            const isLeaf = selectorKeyElement.selector || selectorKeyElement.styleName;
            if (Array.isArray(selectorKeyElement)) {
              selectorKeyElement.forEach((selectorArrayElement) => {
                appendCSS(selectorArrayElement, inputThemeElement);
              });
            } else if (isLeaf) {
              appendCSS(selectorKeyElement, inputThemeElement);
            } else {
              populateResult(selectorKeyElement, inputThemeElement);
            }
          } else {
            res.themeModule.set(key, inputTheme[key]);
          }
        });
    };
    populateResult(this.selectorKey, theme);

    // Some CSS may mess up some things, fix those here
    // Apex charts context menu
    res.css += 'body #app .apexcharts-menu.open { color: black !important; }';
    res.css += ' body #app .apexcharts-tooltip { color: black !important; }';

    return res;
  },
};
