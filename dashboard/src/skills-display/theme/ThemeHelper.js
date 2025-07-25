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
import tinycolor from 'tinycolor2';

export default {
  nonCSSConfig: ['progressIndicators', 'charts', 'landingPageTitle', 'disableSkillTreeBrand', 'disableSearchButton', 'disableBreadcrumb', 'iconColors', 'prerequisites', 'circleProgressInteriorTextColor', 'disableEncouragementsConfetti'],
  bothCssAndThemModule: ['pageTitleTextColor', 'pageTitle', 'skillTreeBrandColor', 'infoCards', 'backgroundColor', 'textPrimaryColor', 'textSecondaryColor', 'tiles', 'breadcrumb'],
  selectorKey: {
    maxWidth: {
      selector: 'body #app .sd-theme-home',
      styleName: 'max-width'
    },
    backgroundColor: [{
      selector: 'body #app .sd-theme-home, .p-overlaypanel.p-component,' +
        'body #app .sd-theme-background-color',
      styleName: 'background-color'
    }, {
      selector: 'body #app .sd-theme-home .skills-theme-bottom-border-with-background-color',
      styleName: 'border-bottom-color'
    }],
    trophyIconColor: {
      selector: 'body #app .sd-theme-home .trophy-icon',
      styleName: 'fill'
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
        selector: 'body #app .sd-theme-home .p-card.p-component.skills-theme-page-title',
        styleName: 'border-color'
      },
      borderStyle: {
        selector: 'body #app .sd-theme-home .p-card.p-component.skills-theme-page-title',
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
        '.p-autocomplete-overlay.p-component .sd-theme-primary-color,' +
        '.sd-theme-home .p-card-subtitle,' +
        '.sd-theme-home .text-primary,' +
        '.sd-theme-home .text-color,' +
        '.sd-theme-home .skills-display-test-link a,' +
        '.sd-theme-home .p-chip-icon,' +
        '.sd-theme-home .p-icon,' +
        '.sd-theme-home .p-avatar-icon,' +
        'body #app .sd-theme-home .p-datatable .p-datatable-tbody > tr,' +
        'body #app .sd-theme-home .p-datatable .p-datatable-thead > tr > th,' +
        'body #app .sd-theme-home .p-paginator.p-component .p-paginator-element.p-link,' +
        'body #app .sd-theme-home .toastui-editor-contents p,' +
        'body #app .sd-theme-home .toastui-editor-contents h1,' +
        'body #app .sd-theme-home .toastui-editor-contents h2,' +
        'body #app .sd-theme-home .toastui-editor-contents h3,' +
        'body #app .sd-theme-home .toastui-editor-contents h4,' +
        'body #app .sd-theme-home .toastui-editor-contents h5,' +
        'body #app .sd-theme-home .toastui-editor-contents h6,' +
        'body #app .sd-theme-home .toastui-editor-tabs .tab-item,' +
        'body #app .sd-theme-home .toastui-editor-popup label,' +
        'body #app .sd-theme-home .p-chip.p-component,' +
        'body #app .sd-theme-home .p-inputtext.p-component,' +
        '.p-listbox-option,' +
        '.p-listbox-empty-message,' +
        'div[data-cy="trainingSearchDialog"],' +
        '.p-autocomplete-panel.p-component .p-autocomplete-item,' +
        '.p-autocomplete-panel.p-component .p-autocomplete-item .text-orange-600,' +
        '.p-autocomplete-panel.p-component .p-autocomplete-item .text-orange-700,' +
        '.p-popover.p-component .p-panelmenu-panel .p-panelmenu-item-link,' +
        '.p-popover.p-component .p-panelmenu-panel .p-panelmenu-header-content,' +
        'body .sd-theme-home a, body .sd-theme-home .skills-theme-skills-progress a,' +
        '.sd-theme-home .editor-help-footer,' +
        '.sd-theme-home .editor-help-footer i',
      styleName: 'color'
    }, {
      selector: '.toastui-editor-popup [data-type="Heading"]:hover,' +
          '.p-popover.p-component .p-panelmenu.p-component .p-panelmenu-header:focus .p-panelmenu-header-content .sd-theme-menu-header',
      styleName: 'background-color'
    }, {
      selector: '.sd-theme-home .p-avatar.p-component, ' +
        '.sd-theme-home .badge-catalog-item,' +
        'body #app .sd-theme-home .p-chip.p-component,' +
        '.sd-theme-home .editor-help-footer',
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
      selector: '.sd-theme-home .text-muted-color,' +
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
  searchButton: {
    padding: {
      selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-search-btn',
      styleName: 'padding'
    },
    fontSize: {
      selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-search-btn',
      styleName: 'font-size'
    },
    lineHeight: {
      selector: 'body #app .sd-theme-home .skills-theme-page-title .skills-search-btn',
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
          + '.p-popover.p-component,'
          + '.p-listbox.p-component,'
          + '.p-autocomplete-overlay.p-component,'
          + '.p-popover.p-component .p-panelmenu-panel,'
          + '.sd-theme-home .apexcharts-menu.apexcharts-menu-open,'
          + '.sd-theme-home .p-avatar.p-component,'
          + '.sd-theme-home .toastui-editor-ww-container,'
          + '.sd-theme-home .toastui-editor-defaultUI-toolbar,'
          + '.sd-theme-home .toastui-editor-popup,'
          + '.sd-theme-home .editor-help-footer,'
          + '.sd-theme-home .sd-theme-tile-background,'
          + '.p-dialog',
        styleName: 'background-color'
      }, {
        selector: '.p-autocomplete-panel.p-component .p-autocomplete-item:hover,' +
          '.p-autocomplete-overlay.p-component .sd-theme-primary-color:hover,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus i,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item:hover i,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus .text-orange-600,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item:hover .text-orange-600,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item.p-focus .text-orange-700,' +
          '.p-autocomplete-panel.p-component .p-autocomplete-item:hover .text-orange-700,' +
          '.p-popover.p-component .p-panelmenu-item.p-focus > .p-panelmenu-item-content .p-panelmenu-item-link,' +
          '.p-popover.p-component .p-panelmenu.p-component .p-panelmenu-header:focus .p-panelmenu-header-content .sd-theme-menu-header,' +
          '.p-listbox-option.p-focus,' +
          '.p-listbox-option:hover,' +
          'body #app .sd-theme-home .sd-theme-tile-background-color,' +
          'body #app .sd-theme-home .p-paginator.p-component .p-paginator-element.p-link.p-highlight,' +
          'body #app .sd-theme-home .fa-stack .fa-stack-1x.fa-inverse,' +
          'body #app .sd-theme-home .toastui-editor-contents pre code,' +
          'body #app .sd-theme-home .toastui-editor-popup [data-type="Heading"]:hover,' +
          'body #app .sd-theme-home .toastui-editor-popup .drop-down .drop-down-item:hover',
        styleName: 'color'
      }],
      borderColor: [{
        selector: '.sd-theme-home .p-card, .sd-theme-home .p-inputtext.p-component, .p-autocomplete-panel.p-component',
        styleName: 'border-color'
      }],
      watermarkIconColor: {
        selector: 'body #app .sd-theme-home .watermark-icon',
        styleName: 'color'
      },
      subTitleOverlayTextColor: {
        selector: 'body #app .sd-theme-home .skills-progress-card .user-rank-text',
        styleName: 'color'
      },
      subTitleOverlayBackgroundColor: {
        selector: 'body #app .sd-theme-home .skills-progress-card .user-rank-text',
        styleName: 'background-color'
      }
    },
    stars: {
      unearnedColor: {
        selector: 'body #app .sd-theme-home .p-rating .p-rating-option .p-icon.p-rating-icon.p-rating-off-icon',
        styleName: 'color'
      },
      earnedColor: {
        selector: 'body #app .sd-theme-home .p-rating .p-rating-option.p-rating-option-active .p-icon.p-rating-icon.p-rating-on-icon',
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
        selector: 'body #app .sd-theme-home .p-tag.p-component',
        styleName: 'background-color'
      }],
      backgroundColorSecondary: [{
        selector: 'body #app .sd-theme-home .p-tag.p-component.p-tag-secondary',
        styleName: 'background-color'
      }],
      foregroundColor: [{
        selector: 'body #app .sd-theme-home .p-tag.p-component',
        styleName: 'color'
      }]
    },
    breadcrumb: {
      linkColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .p-breadcrumb-item-link .sd-theme-breadcrumb-item .text-primary,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .p-breadcrumb-item-link .sd-theme-breadcrumb-item .text-muted-color',
        styleName: 'color'
      }],
      linkHoverColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .p-breadcrumb-item-link .sd-theme-breadcrumb-item:hover .text-primary,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .p-breadcrumb-item-link .sd-theme-breadcrumb-item:hover .text-muted-color',
        styleName: 'color'
      }],
      currentPageColor: [{
        selector: '.sd-theme-home .skills-theme-breadcrumb-container .sd-theme-breadcrumb-item .text-color,' +
          '.sd-theme-home .skills-theme-breadcrumb-container .sd-theme-breadcrumb-item .text-muted-color',
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
      'tiles.backgroundColor': '.sd-theme-home .sd-theme-summary-cards .p-card.p-component { border-style: solid !important; border-width: 1px !important; } ' +
        '.sd-theme-home .badge-catalog-item .p-card { border-style: solid !important; border-width: 1px !important; } ' +
        '.sd-theme-home .toastui-editor-toolbar-group button { background-color: #f5f5f5 !important; color: #454545 !important; } ' +
        '.sd-theme-home .attachment-button.toastui-editor-toolbar-icons { color: #454545 !important; }',
      'textPrimaryColor': '.sd-theme-home .p-avatar.p-component { border-style: solid !important; border-width: 1px !important; } body #app .sd-theme-home .p-chip.p-component  { border-style: solid !important; border-width: 1px !important; }  body #app .sd-theme-home .skills-card-theme-border { border-style: solid !important; border-width: 1px !important; }',
      'pageTitle.borderColor': '.sd-theme-home .skills-theme-page-title.p-card { border-width: 2px !important; }',
      'pageTitle.borderStyle': '.sd-theme-home .skills-theme-page-title.p-card { border-width: 2px !important; }',
    }

    if (theme?.tiles?.backgroundColor) {
      const lighterTilesBackgroundColor = tinycolor(theme.tiles.backgroundColor).lighten(10).toString();
      res.css += `body #app .sd-theme-home .answer-row.surface-200 { background-color: ${lighterTilesBackgroundColor} !important; } `
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

    const handleMenuItemLinkHoverColor = (theme, selectorKey) => {
      if (theme?.tiles?.backgroundColor && theme?.tiles?.backgroundColor !== '#fff') {
        const tilesMenuHoverColor = tinycolor(theme.tiles.backgroundColor).lighten(10).toString();
        theme.tilesMenuLinkHoverCalculatedColor=tilesMenuHoverColor
        selectorKey.tilesMenuLinkHoverCalculatedColor = {
          selector: '.p-popover.p-component .p-panelmenu-panel .p-panelmenu-item-content:hover, .p-popover.p-component .p-panelmenu-panel .p-panelmenu-header-content:hover',
          styleName: 'background-color'
        }
      }
    }
    handleMenuItemLinkHoverColor(theme, this.selectorKey)
    populateResult(this.selectorKey, theme)

    // Some CSS may mess up some things, fix those here
    // Apex charts context menu
    res.css += 'body #app .sd-theme-home .apexcharts-menu.open { color: black !important; }'
    res.css += ' body #app .sd-theme-home .apexcharts-tooltip { color: black !important; }'

    return res
  }
}
