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
  nonCSSConfig: ['charts', 'landingPageTitle', 'disableSkillTreeBrand', 'disableBreadcrumb', 'iconColors', 'prerequisites', 'circleProgressInteriorTextColor'],
  bothCssAndThemModule: ['progressIndicators', 'pageTitleTextColor', 'pageTitle', 'skillTreeBrandColor', 'infoCards', 'backgroundColor', 'textPrimaryColor', 'textSecondaryColor', 'tiles'],
  selectorKey: {
    maxWidth: {
      selector: 'body #app',
      styleName: 'max-width'
    },
    backgroundColor: [{
      selector: 'body #app .sd-theme-home, .p-overlaypanel.p-component',
      styleName: 'background-color'
    }, {
      selector: 'body #app .sd-theme-home .skills-theme-bottom-border-with-background-color',
      styleName: 'border-bottom-color'
    }],
    trophyIconColor: {
      selector: 'body #app .sd-theme-home .fa.fa-trophy',
      styleName: 'color'
    },
    subjectTileIconColor: {
      selector: 'body #app .sd-theme-home .sd-theme-subject-tile-icon',
      styleName: 'color'
    },
    pageTitleTextColor: [{
      selector: '.sd-theme-home .skills-theme-page-title,' +
        '.sd-theme-home .skills-theme-page-title .poweredByContainer',
      styleName: 'color'
    }, {
      selector: 'body #app .sd-theme-home .skills-badge .skills-badge-icon, body #app .sd-theme-home .skills-progress-info-card, body #app .sd-theme-home .skills-card-theme-border, body #app .sd-theme-home .card.skills-card-theme-border .card-header',
      styleName: 'border-color'
    }],
    pageTitle: {
      textColor: [{
        selector: '.sd-theme-home .p-card.p-component.skills-theme-page-title,' +
          '.sd-theme-home .p-card.p-component.skills-theme-page-title .poweredByContainer',
        styleName: 'color'
      }],
      borderColor: {
        selector: '.sd-theme-home .skills-theme-page-title',
        styleName: 'border-color'
      },
      borderStyle: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title',
        styleName: 'border-style'
      },
      backgroundColor: {
        selector: '.sd-theme-home .p-card.p-component.skills-theme-page-title,' +
          '.sd-theme-home .p-card.p-component.skills-theme-page-title .p-breadcrumb.p-component',
        styleName: 'background-color'
      },
      textAlign: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-title',
        styleName: 'text-align'
      },
      padding: [{
        selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-title',
        styleName: 'padding'
      }],
      fontSize: {
        selector: '.sd-theme-home .skills-theme-page-title .skills-title',
        styleName: 'font-size'
      },
      margin: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title',
        styleName: 'margin'
      }
    },
    textPrimaryColor: [{
      selector: '.sd-theme-home .p-card,' +
        '.sd-theme-home .sd-theme-primary-color,' +
        '.sd-theme-home .p-card-subtitle,' +
        '.sd-theme-home .text-primary,' +
        '.sd-theme-home .text-color,' +
        '.sd-theme-home .skills-display-test-link a,' +
        'body #app .sd-theme-home .p-datatable .p-datatable-tbody > tr,' +
        'body #app .sd-theme-home .p-datatable .p-datatable-thead > tr > th,' +
        'body #app .sd-theme-home .p-paginator.p-component .p-paginator-element.p-link,' +
        'body #app .sd-theme-home .toastui-editor-contents p,' +
        'body #app .sd-theme-home .p-chip.p-component,' +
        'body #app .sd-theme-home .p-inputtext.p-component,' +
        '.p-autocomplete-panel.p-component .p-autocomplete-item,' +
        '.p-autocomplete-panel.p-component .p-autocomplete-item .text-orange-600,' +
        '.p-overlaypanel-content .p-panelmenu.p-component .p-panelmenu-header-content,' +
        '.p-overlaypanel-content .p-panelmenu .p-panelmenu-content .p-menuitem-link',
      styleName: 'color'
    }, {
      selector: 'todo',
      styleName: 'background-color'
    }, {
      selector: '.sd-theme-home .p-avatar.p-component, ' +
        '.sd-theme-home .badge-catalog-item,' +
        'body #app .sd-theme-home .p-chip.p-component',
      styleName: 'border-color'
    }, {
      selector: 'body #app .sd-theme-home .apexcharts-toolbar svg, body #app .sd-theme-home .vs__open-indicator',
      styleName: 'fill'
    }],
    textPrimaryMutedColor: [{
      selector: 'body #app .sd-theme-home .todo',
      styleName: 'color'
    }, {
      selector: 'body #app .sd-theme-home .skills-theme-menu:hover, body #app .sd-theme-home .apexcharts-menu.apexcharts-menu-open .apexcharts-menu-item:hover',
      styleName: 'background-color'
    }],
    textSecondaryColor: {
      selector: '.sd-theme-home .text-color-secondary,' +
        '.p-autocomplete-panel.p-component i',
      styleName: 'color'
    },
    pageTitleFontSize: {
      selector: 'body #app .sd-theme-home .skills-page-title-text-color .skills-title',
      styleName: 'font-size'
    },
    backButton: {
      padding: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-theme-btn',
        styleName: 'padding'
      },
      fontSize: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-theme-btn',
        styleName: 'font-size'
      },
      lineHeight: {
        selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-theme-btn',
        styleName: 'line-height'
      }
    },
    tiles: {
      backgroundColor: [{
        selector: '.sd-theme-home .p-card, '
          + '.sd-theme-home .p-inputtext,'
          + '.sd-theme-home .p-inputgroup-addon,'
          + '.sd-theme-home .p-breadcrumb.p-component,'
          + 'body #app .sd-theme-home .p-datatable .p-datatable-tbody > tr,'
          + 'body #app .sd-theme-home .p-datatable .p-datatable-thead > tr > th,'
          + 'body #app .sd-theme-home .p-paginator.p-component,'
          + 'body #app .sd-theme-home .p-chip.p-component,'
          + '.p-autocomplete-panel.p-component,'
          + '.p-overlaypanel-content .p-panelmenu.p-component .p-panelmenu-header-content,'
          + '.p-overlaypanel-content .p-panelmenu .p-panelmenu-content,'
          + '.sd-theme-home .apexcharts-menu.apexcharts-menu-open,'
          + '.sd-theme-home .p-avatar.p-component',
        styleName: 'background-color'
      }, {
        selector: '.p-autocomplete-panel.p-component .p-autocomplete-item:hover,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus i,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item:hover i,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus .text-orange-600,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item:hover .text-orange-600,' +
          'body #app .sd-theme-home .p-paginator.p-component .p-paginator-element.p-link.p-highlight,' +
          '.p-overlaypanel-content .p-panelmenu .p-panelmenu-content .p-menuitem-link:hover,' +
          '.p-overlaypanel-content .p-panelmenu .p-panelmenu-content .p-avatar-icon,' +
          'body #app .sd-theme-home .fa-stack .fa-stack-1x.fa-inverse',
        styleName: 'color'
      }],
      borderColor: [{
        selector: '.sd-theme-home .p-card, .sd-theme-home .p-inputtext.p-component, .p-autocomplete-panel.p-component',
        styleName: 'border-color'
      }],
      watermarkIconColor: {
        selector: 'body #app .sd-theme-home .watermark-icon',
        styleName: 'color'
      }
    },
    stars: {
      unearnedColor: {
        selector: 'body #app .sd-theme-home .p-rating .p-rating-item .p-icon.p-rating-icon',
        styleName: 'color'
      },
      earnedColor: {
        selector: 'body #app .sd-theme-home .p-rating .p-rating-item.p-rating-item-active .p-icon.p-rating-icon',
        styleName: 'color'
      }
    },
    graphLegendBorderColor: {
      selector: 'body #app .sd-theme-home .graph-legend .card-header, body #app .sd-theme-home .graph-legend .card-body',
      styleName: 'border'
    },
    buttons: {
      backgroundColor: [{
        selector: '.sd-theme-home .p-button.p-component',
        styleName: 'background-color'
      }, {
        selector: '.sd-theme-home .p-button.p-component:hover,' +
          'body #app .sd-theme-home .p-button.p-component.p-highlight',
        styleName: 'color'
      }, {
        selector: '.sd-theme-home .p-button.p-component:hover',
        styleName: 'border-color'
      }],
      foregroundColor: [{
        selector: '.sd-theme-home .p-button.p-component',
        styleName: 'color'
      }, {
        selector: '.sd-theme-home .p-button.p-component',
        styleName: 'border-color'
      }, {
        selector: '.sd-theme-home .p-button.p-component:hover,' +
          'body #app .sd-theme-home .p-button.p-component.p-highlight',
        styleName: 'background-color'
      }],
      disabledColor: [{
        selector: 'body #app .sd-theme-home .p-button.p-component.p-disabled',
        styleName: 'color'
      }],
      borderColor: [{
        selector: '.sd-theme-home .p-button.p-component',
        styleName: 'border-color'
      }]
    },
    links: {
      foregroundColor: [{
        selector: 'body #app .sd-theme-home .skills-theme-link',
        styleName: 'color'
      }, {
        selector: 'body #app .sd-theme-home .skills-theme-link',
        styleName: 'border-color'
      }, {
        selector: 'body #app .sd-theme-home .skills-theme-link a:hover',
        styleName: 'background-color'
      }],
      disabledColor: [{
        selector: 'body #app .sd-theme-home .skills-theme-link a.disabled',
        styleName: 'color'
      }]
    },
    badges: {
      backgroundColor: [{
        selector: 'body #app .p-tag.p-component',
        styleName: 'background-color'
      }],
      backgroundColorSecondary: [{
        selector: 'body #app .p-tag.p-component.p-tag-secondary',
        styleName: 'background-color'
      }],
      foregroundColor: [{
        selector: 'body #app .sd-theme-home .badge',
        styleName: 'color'
      }]
    },
    progressIndicators: {
      completeColor: [{
        selector: 'body #app .sd-theme-home .p-progressbar.p-component.is-completed',
        styleName: 'background-color'
      }],
      incompleteColor: [{
        selector: 'body #app .sd-theme-home .p-progressbar.p-component.today-progress.is-not-completed',
        styleName: 'background-color'
      }],
      beforeTodayColor: [{
        selector: 'body #app .sd-theme-home .p-progressbar.p-component.total-progress.is-not-completed .p-progressbar-value',
        styleName: 'background-color'
      }],
      earnedTodayColor: [{
        selector: 'body #app .sd-theme-home .p-progressbar.p-component.today-progress.is-not-completed .p-progressbar-value',
        styleName: 'background-color'
      }]
    },
    breadcrumb: {
      linkColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .p-menuitem-link .sd-theme-breadcrumb-item .text-primary,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .p-menuitem-link .sd-theme-breadcrumb-item .text-color-secondary',
        styleName: 'color'
      }],
      linkHoverColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .p-menuitem-link .sd-theme-breadcrumb-item:hover .text-primary,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .p-menuitem-link .sd-theme-breadcrumb-item:hover .text-color-secondary',
        styleName: 'color'
      }],
      currentPageColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .sd-theme-breadcrumb-item .text-color,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .sd-theme-breadcrumb-item .text-color-secondary',
        styleName: 'color'
      }],
      align: [{
        selector: 'body #app .sd-theme-home .skills-theme-breadcrumb-container',
        styleName: '-ms-flex-pack'
      }, {
        selector: 'body #app .sd-theme-home .skills-theme-breadcrumb-container',
        styleName: 'justify-content'
      }]
    },
    infoCards: {
      backgroundColor: [{
        selector: 'body #app .sd-theme-home .sd-theme-summary-cards .p-card',
        styleName: 'background-color'
      }],
      foregroundColor: [{
        selector: 'body #app .sd-theme-home .sd-theme-summary-cards .p-card',
        styleName: 'color'
      }],
      borderColor: [{
        selector: 'body #app .sd-theme-home .sd-theme-summary-cards .p-card',
        styleName: 'border-color'
      }]
    },
    skillTreeBrandColor: {
      selector: '.sd-theme-home .poweredByContainer .skills-theme-brand',
      styleName: 'color'
    },
    quiz: {
      incorrectAnswerColor: {
        selector: 'body #app .sd-theme-home .skills-theme-quiz-incorrect-answer',
        styleName: 'color'
      },
      correctAnswerColor: {
        selector: 'body #app .sd-theme-home .skills-theme-quiz-correct-answer',
        styleName: 'color'
      },
      selectedAnswerColor: [{
        selector: 'body #app .sd-theme-home .skills-theme-quiz-selected-answer',
        styleName: 'color'
      }, {
        selector: 'body #app .sd-theme-home .skills-theme-quiz-selected-answer-row:hover',
        styleName: 'border-color'
      }]
    }
  },

  build(theme) {
    const res = {
      css: '',
      themeModule: new Map()
    }

    const appendCSS = (selectorKeyElement, inputThemeElement) => {
      if (!(selectorKeyElement.selector && selectorKeyElement.styleName)) {
        throw new Error(`Bug in the custom theme code. Both selector and styleName must be present for [${selectorKeyElement}]`)
      }

      // No injection
      const sanitizedValue = inputThemeElement.split(';')[0]
      res.css += `${selectorKeyElement.selector} { ${selectorKeyElement.styleName}: ${sanitizedValue} !important } `
    }

    const validateInputElement = (key, element, keyMsg) => {
      if (!element) {
        throw new Error(`Skills Theme Error! Failed to process provided custom theme due to invalid format! JSON key of [${key}] ${keyMsg}. Theme is ${JSON.stringify(theme)}`)
      }
    }
    const cssBasedOnKeyPathMapping = {
      'tiles.borderColor': '.sd-theme-home .p-card, .p-autocomplete-panel.p-component { border-style: solid !important; border-width: 1px !important; }',
      'tiles.backgroundColor': '.sd-theme-home .sd-theme-summary-cards .p-card.p-component { border-style: solid !important; border-width: 1px !important; } .sd-theme-home .badge-catalog-item .p-card { border-style: solid !important; border-width: 1px !important; }',
      'textPrimaryColor': '.sd-theme-home .p-avatar.p-component { border-style: solid !important; border-width: 1px !important; } body #app .sd-theme-home .p-chip.p-component  { border-style: solid !important; border-width: 1px !important; }  body #app .sd-theme-home .skills-card-theme-border { border-style: solid !important; border-width: 1px !important; }'
    }
    const addCssBasedOnKeyPath = (keyPath) => {
      const cssToAdd = cssBasedOnKeyPathMapping[keyPath]
      if (cssToAdd) {
        res.css += cssToAdd
      }
    }

    const populateResult = (selectorKey, inputTheme, parentPath = '') => {
      Object.keys(inputTheme)
        .forEach((key) => {
          const myPath = parentPath ? parentPath + '.' + key : key
          const isCSSConfig = !this.nonCSSConfig.includes(key)
          const isThemeModule = !isCSSConfig || this.bothCssAndThemModule.includes(key)
          if (isCSSConfig) {
            const selectorKeyElement = selectorKey[key]
            validateInputElement(key, selectorKeyElement, 'is not supported (Is it misspelled?)')
            const inputThemeElement = inputTheme[key]
            validateInputElement(key, inputThemeElement, 'has empty/undefined value')

            const isLeaf = selectorKeyElement.selector || selectorKeyElement.styleName


            if (Array.isArray(selectorKeyElement)) {
              selectorKeyElement.forEach((selectorArrayElement) => {
                // if (myPath === 'tiles.borderColor') {
                //   console.log(`tiles.borderColor!!!!!!!!!!!!!!!!!!!!!!!!!!!! isLeaf=${isLeaf}`)
                //   console.log(selectorArrayElement)
                //   res.css += `.sd-theme-home .p-card { border: solid 1px!important } `;
                // }
                appendCSS(selectorArrayElement, inputThemeElement)
              })
              addCssBasedOnKeyPath(myPath)
            } else if (isLeaf) {
              appendCSS(selectorKeyElement, inputThemeElement)
              addCssBasedOnKeyPath(myPath)
            } else {
              populateResult(selectorKeyElement, inputThemeElement, myPath)
            }
          }

          if (isThemeModule) {
            res.themeModule.set(key, inputTheme[key])
          }
        })
    }

    const setDefaultBackgroundIfNotSet = (themeParam) => {
      // since background color is used on hover when primary color is set we need to default it to white
      const isTilesBackgroundSet = themeParam.tiles && themeParam.tiles.backgroundColor
      const isPrimaryColorSet = themeParam.textPrimaryColor
      if (!isTilesBackgroundSet && isPrimaryColorSet) {
        if (!themeParam.tiles) {
          // eslint-disable-next-line no-param-reassign
          themeParam.tiles = { backgroundColor: '#fff' }
        } else {
          // eslint-disable-next-line no-param-reassign
          themeParam.tiles.backgroundColor = '#fff'
        }
      }
    }

    const applyDefaults = (themeParam) => {

      //  1. if skillTreeBrandColor is provided
      //  2. pageTitle.textColor
      //  3. pageTitleTextColor (backward compat)

      if (!themeParam.skillTreeBrandColor) {
        if (themeParam.pageTitle?.textColor) {
          themeParam.skillTreeBrandColor = themeParam.pageTitle.textColor
        } else if (themeParam.textPrimaryColor) {
          themeParam.skillTreeBrandColor = themeParam.textPrimaryColor
        } else if (themeParam.pageTitleTextColor) {
          themeParam.skillTreeBrandColor = themeParam.pageTitleTextColor
        }
      }
      if (themeParam.pageTitle?.textColor && themeParam.pageTitleTextColor) {
        // pageTitleTextColor is legacy and should be overriden by pageTitle.textColor
        delete themeParam.pageTitleTextColor
      }
    }
    setDefaultBackgroundIfNotSet(theme)
    applyDefaults(theme)
    populateResult(this.selectorKey, theme)

    // Some CSS may mess up some things, fix those here
    // Apex charts context menu
    res.css += 'body #app .sd-theme-home .apexcharts-menu.open { color: black !important; }'
    res.css += ' body #app .sd-theme-home .apexcharts-tooltip { color: black !important; }'

    return res
  }
}
