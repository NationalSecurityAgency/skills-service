/*
 * Copyright 2024 SkillTree
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
import DOMPurify from 'dompurify'

export const restrictedMarkdownImagesKey = Symbol('restrictedMarkdownImages')

// SVG is deliberately excluded: only inline raster images may load automatically.
const inlineImage = /^data:image\/(?:png|jpe?g|gif|webp|bmp|avif|x-icon);base64,[a-z\d+/=]+$/i
const resourceTags = 'iframe,embed,object,video,audio,source,track,link,style,script,svg,math,template'

// Keep text formatting without admitting CSS resource loads. Validate the raw
// syntax before letting the browser normalize values: CSS escapes, comments,
// custom properties, and functions other than numeric colors are not supported.
const colorValue = /^(?:#[a-f\d]{3,8}|[a-z]+|(?:rgb|rgba|hsl|hsla)\([\d\s.,%+\-/]+\))$/i
const lengthValue = /^(?:\d+(?:\.\d+)?|\.\d+)(?:px|pt|pc|in|cm|mm|em|rem|%|vh|vw)$/i
const globalStyleValue = /^(?:inherit|initial|unset|revert|revert-layer)$/
const safeStyleValues = {
  'font-size': (value) => value === '0' || lengthValue.test(value) || /^(?:xx-small|x-small|small|medium|large|x-large|xx-large|xxx-large|smaller|larger)$/.test(value),
  'font-weight': (value) => /^(?:normal|bold|bolder|lighter|[1-9]\d{0,2}|1000)$/.test(value),
  'font-style': (value) => /^(?:normal|italic|oblique)$/.test(value),
  'color': (value) => colorValue.test(value),
  'background-color': (value) => colorValue.test(value),
  'text-align': (value) => /^(?:left|right|center|justify|start|end)$/.test(value),
  'text-decoration': (value) => /^(?:none|(?:underline|overline|line-through|solid|double|dotted|dashed|wavy)(?:\s+(?:underline|overline|line-through|solid|double|dotted|dashed|wavy))*)$/.test(value),
  'text-decoration-color': (value) => colorValue.test(value),
  'line-height': (value) => value === 'normal' || /^(?:\d+(?:\.\d+)?|\.\d+)$/.test(value) || lengthValue.test(value),
}

const sanitizeTextStyle = (style) => {
  const safeStyle = document.createElement('span').style
  let blocked = false
  for (const declaration of style.split(';')) {
    if (!declaration.trim()) continue
    const separator = declaration.indexOf(':')
    const property = declaration.substring(0, separator).trim().toLowerCase()
    const value = declaration.substring(separator + 1).trim().toLowerCase()
    const validateValue = Object.hasOwn(safeStyleValues, property) && safeStyleValues[property]
    if (separator < 0 || !validateValue || (!globalStyleValue.test(value) && !validateValue(value))) {
      blocked = true
      continue
    }
    // Check CSS validity as well as the resource-free syntax (e.g. valid color
    // names and hex lengths). Reconstruct rather than retain untrusted CSS text.
    const parsedStyle = document.createElement('span').style
    parsedStyle.setProperty(property, value)
    const normalizedValue = parsedStyle.getPropertyValue(property)
    if (!normalizedValue) {
      blocked = true
      continue
    }
    safeStyle.setProperty(property, normalizedValue)
  }
  return { style: safeStyle.cssText, blocked }
}

const applyImagePolicy = (html) => {
  // Template contents are inert. Never parse untrusted resources into a live or
  // detached image element, which can start a request before its src is removed.
  const template = document.createElement('template')
  template.innerHTML = html
  let blocked = false
  template.content.querySelectorAll(resourceTags).forEach((element) => {
    blocked = true
    element.remove()
  })
  template.content.querySelectorAll('*').forEach((element) => {
    if (element.localName === 'img' && !inlineImage.test(element.getAttribute('src') || '')) {
      blocked = true
      element.replaceWith(document.createTextNode(`[Image blocked: ${element.getAttribute('alt') || 'external or unsupported image'}]`))
      return
    }
    if (element.hasAttribute('style')) {
      const result = sanitizeTextStyle(element.getAttribute('style'))
      blocked = blocked || result.blocked
      if (result.style) {
        element.setAttribute('style', result.style)
      } else {
        element.removeAttribute('style')
      }
    }
    // srcset may override an otherwise safe inline src. Also exclude legacy
    // resource-loading attributes rather than relying on URL deny-lists.
    for (const attribute of ['srcset', 'background', 'poster']) {
      if (element.hasAttribute(attribute)) {
        blocked = true
        element.removeAttribute(attribute)
      }
    }
    if (element.localName !== 'img' && element.hasAttribute('src')) {
      blocked = true
      element.removeAttribute('src')
    }
  })
  return { template, blocked }
}

export const hasBlockedMarkdownImages = (html) => applyImagePolicy(html).blocked

export const sanitizeRestrictedMarkdown = (html) => {
  const { template } = applyImagePolicy(html)
  // Keep XSS sanitization when replacing Toast UI's default sanitizer. Resource
  // URLs have already been removed and styles reconstructed from the formatting
  // allowlist in the inert template before DOMPurify runs.
  return DOMPurify.sanitize(template.content, {
    USE_PROFILES: { html: true },
    ADD_ATTR: ['rel', 'target', 'hreflang', 'type'],
    FORBID_TAGS: ['input', 'textarea', 'form', 'button', 'select', 'meta', 'base', 'title'],
    FORBID_ATTR: ['srcset', 'background', 'poster'],
  })
}
