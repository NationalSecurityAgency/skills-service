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
  nonCSSConfig: ['charts', 'landingPageTitle', 'earnedTodayColor', 'beforeTodayColor', 'disableSkillTreeBrand', 'disableBreadcrumb', 'iconColors'],
  bothCssAndThemModule: ['progressIndicators', 'pageTitleTextColor', 'pageTitle', 'skillTreeBrandColor', 'infoCards'],
  selectorKey: {
    maxWidth: {
      selector: 'body #app.skills-display-container',
      styleName: 'max-width',
    },
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
    pageTitle: {
      textColor: [{
        selector: 'body #app .skills-page-title-text-color',
        styleName: 'color',
      }],
      borderColor: {
        selector: 'body #app .skills-theme-page-title',
        styleName: 'border-color',
      },
      borderStyle: {
        selector: 'body #app .skills-theme-page-title',
        styleName: 'border-style',
      },
      backgroundColor: {
        selector: 'body #app .skills-theme-page-title .titleBody',
        styleName: 'background',
      },
      textAlign: {
        selector: 'body #app .skills-theme-page-title .titleBody',
        styleName: 'text-align',
      },
      padding: [{
        selector: 'body #app .skills-theme-page-title .titleBody',
        styleName: 'padding',
      }],
      fontSize: {
        selector: 'body #app .skills-page-title-text-color .skills-title',
        styleName: 'font-size',
      },
      margin: {
        selector: 'body #app .skills-theme-page-title',
        styleName: 'margin',
      },
    },
    circleProgressInteriorTextColor: {
      selector: 'body #app .circle-number span',
      styleName: 'color',
    },
    textPrimaryColor: [{
      selector: 'body #app .text-primary, body #app, body #app .skills-navigable-item, body #app .skills-theme-primary-color, body #app .leaderboardTable tr td, body #app .skills-theme-menu-primary-color, body #app .skills-theme-menu-primary-color .dropdown-item',
      styleName: 'color',
    }, {
      selector: 'body #app .skills-theme-menu-primary-color:hover, body #app .skills-theme-menu-primary-color .dropdown-item:hover',
      styleName: 'background-color',
    }, {
      selector: 'body #app .skills-theme-filter-menu .dropdown-menu',
      styleName: 'border-color',
    }, {
      selector: 'body #app .apexcharts-toolbar svg',
      styleName: 'fill',
    }],
    textPrimaryMutedColor: [{
      selector: 'body #app .text-primary .text-muted, body #app .text-primary.text-muted',
      styleName: 'color',
    }, {
      selector: 'body #app .skills-theme-menu:hover, body #app .apexcharts-menu.apexcharts-menu-open .apexcharts-menu-item:hover',
      styleName: 'background-color',
    }],
    textSecondaryColor: {
      selector: 'body #app .text-muted, body #app .text-secondary, body #app .text-secondary a, body #app .skills-theme-secondary-color, body #app .skills-theme-menu-secondary-color, body #app .skills-theme-menu-secondary-color .dropdown-item',
      styleName: 'color',
    },
    pageTitleFontSize: {
      selector: 'body #app .skills-page-title-text-color .skills-title',
      styleName: 'font-size',
    },
    backButton: {
      padding: {
        selector: 'body #app .skills-page-title-text-color .skills-theme-btn',
        styleName: 'padding',
      },
      fontSize: {
        selector: 'body #app .skills-page-title-text-color .skills-theme-btn',
        styleName: 'font-size',
      },
      lineHeight: {
        selector: 'body #app .skills-page-title-text-color .skills-theme-btn',
        styleName: 'line-height',
      },
    },
    tiles: {
      backgroundColor: [{
        selector: 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer, body #app .apexcharts-menu.apexcharts-menu-open, body #app .dropdown-menu',
        styleName: 'background-color',
      }, {
        selector: 'body #app .skills-no-data-yet .fa-inverse, body #app .apexcharts-menu.apexcharts-menu-open .apexcharts-menu-item:hover, body #app .skills-theme-menu-primary-color, body #app .skills-theme-menu-primary-color .dropdown-item:hover',
        styleName: 'color',
      }],
      borderColor: [{
        selector: 'body #app .card, body #app .card-header, body #app .card-body, body #app .card-footer, body #app .apexcharts-menu.apexcharts-menu-open, body #app .dropdown-menu',
        styleName: 'border-color',
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
        selector: 'body #app .skills-theme-btn:hover, body #app .skills-theme-link a:hover',
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
        selector: 'body #app .skills-theme-btn:hover, body #app .skills-theme-link a:hover',
        styleName: 'background-color',
      }],
      disabledColor: [{
        selector: 'body #app .skills-theme-btn .disabled',
        styleName: 'color',
      }],
      borderColor: [{
        selector: 'body #app .skills-theme-btn',
        styleName: 'border-color',
      }],
    },
    links: {
      foregroundColor: [{
        selector: 'body #app .skills-theme-link',
        styleName: 'color',
      }, {
        selector: 'body #app .skills-theme-link',
        styleName: 'border-color',
      }, {
        selector: 'body #app .skills-theme-link a:hover',
        styleName: 'background-color',
      }],
      disabledColor: [{
        selector: 'body #app .skills-theme-link a.disabled',
        styleName: 'color',
      }],
    },
    badges: {
      backgroundColor: [{
        selector: 'body #app .badge',
        styleName: 'background-color',
      }],
      backgroundColorSecondary: [{
        selector: 'body #app .badge.badge-secondary',
        styleName: 'background-color',
      }],
      foregroundColor: [{
        selector: 'body #app .badge',
        styleName: 'color',
      }],
    },
    progressIndicators: {
      completeColor: [{
        selector: 'body #app .leaderboard .progress .progress-bar',
        styleName: 'background-color',
      }],
      incompleteColor: [{
        selector: 'body #app .leaderboard .progress',
        styleName: 'background-color',
      }],
    },
    breadcrumb: {
      linkColor: [{
        selector: 'body #app .skills-theme-breadcrumb a',
        styleName: 'color',
      }],
      linkHoverColor: [{
        selector: 'body #app .skills-theme-breadcrumb a:hover',
        styleName: 'color',
      }],
      currentPageColor: [{
        selector: 'body #app .skills-theme-breadcrumb .skills-theme-breadcrumb-current-page',
        styleName: 'color',
      }],
      align: [{
        selector: 'body #app .skills-theme-breadcrumb-container',
        styleName: '-ms-flex-pack',
      }, {
        selector: 'body #app .skills-theme-breadcrumb-container',
        styleName: 'justify-content',
      }],
    },
    infoCards: {
      backgroundColor: [{
        selector: 'body #app .skills-theme-info-card, body #app .skills-theme-info-card .card-body',
        styleName: 'background-color',
      }],
      foregroundColor: [{
        selector: 'body #app .skills-theme-info-card .card-body, body #app .skills-theme-info-card .card-body .text-primary',
        styleName: 'color',
      }],
      borderColor: [{
        selector: 'body #app .skills-theme-info-card',
        styleName: 'border-color',
      }],
    },
    skillTreeBrandColor: {
      selector: 'body #app .poweredByContainer .skills-theme-brand',
      styleName: 'color',
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
          const isThemeModule = !isCSSConfig || this.bothCssAndThemModule.includes(key);
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
          }

          if (isThemeModule) {
            res.themeModule.set(key, inputTheme[key]);
          }
        });
    };

    const setDefaultBackgroundIfNotSet = (themeParam) => {
      // since background color is used on hover when primary color is set we need to default it to white
      const isTilesBackgroundSet = themeParam.tiles && themeParam.tiles.backgroundColor;
      const isPrimaryColorSet = themeParam.textPrimaryColor;
      if (!isTilesBackgroundSet && isPrimaryColorSet) {
        if (!themeParam.tiles) {
          // eslint-disable-next-line no-param-reassign
          themeParam.tiles = { backgroundColor: '#fff' };
        } else {
          // eslint-disable-next-line no-param-reassign
          themeParam.tiles.backgroundColor = '#fff';
        }
      }
    };

    setDefaultBackgroundIfNotSet(theme);
    populateResult(this.selectorKey, theme);

    // Some CSS may mess up some things, fix those here
    // Apex charts context menu
    res.css += 'body #app .apexcharts-menu.open { color: black !important; }';
    res.css += ' body #app .apexcharts-tooltip { color: black !important; }';

    return res;
  },
};
