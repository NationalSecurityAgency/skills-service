/*
 * Copyright 2026 SkillTree
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
import { describe, expect, it } from 'vitest'
import Editor from '@toast-ui/editor'
import InputSanitizer from './InputSanitizer.js'

describe('HTML export sanitization', () => {
  it('removes unsafe links retained by the WYSIWYG editor', () => {
    const el = document.createElement('div')
    document.body.appendChild(el)
    const editor = new Editor({
      el,
      initialEditType: 'wysiwyg',
      initialValue: '[unsafe](javascript:alert(1)) and [safe](https://example.com)',
      usageStatistics: false,
      autofocus: false,
    })
    try {
      const exportedHtml = editor.getHTML()
      expect(exportedHtml).toContain('javascript:alert(1)')
      const result = document.createElement('div')
      result.innerHTML = InputSanitizer.sanitize(exportedHtml)
      expect(result.querySelector('a[href^="javascript:"]')).toBeNull()
      expect(result.querySelector('a[href="https://example.com"]')).not.toBeNull()
      expect(result.textContent).toContain('unsafe and safe')
    } finally {
      editor.destroy()
      el.remove()
    }
  })

  it.each([
    'javascript:alert(1)',
    'JaVaScRiPt:alert(1)',
    'java&#x09;script:alert(1)',
    'jav&#x61;script:alert(1)',
    'vbscript:msgbox(1)',
    'data:text/html,<script>alert(1)</script>',
  ])('removes unsafe URL scheme: %s', (url) => {
    const result = document.createElement('div')
    result.innerHTML = InputSanitizer.sanitize(`<a href="${url}" onclick="alert(1)">link text</a>`)
    expect(result.querySelector('a').hasAttribute('href')).toBe(false)
    expect(result.querySelector('[onclick]')).toBeNull()
    expect(result.textContent).toBe('link text')
  })

  it('preserves normal editor formatting while removing executable markup', () => {
    const html = '<h1>Title</h1><p><strong>bold</strong><em>italic</em><del>deleted</del>'
      + '<span style="font-size: 24px;">large</span></p>'
      + '<ul><li>item</li></ul><table><tbody><tr><td>cell</td></tr></tbody></table>'
      + '<pre><code>&lt;script&gt;literal example&lt;/script&gt;</code></pre>'
      + '<img src="https://example.com/image.png" onerror="alert(1)">'
      + '<script>alert(1)</script>'
    const result = document.createElement('div')
    result.innerHTML = InputSanitizer.sanitize(html)
    expect(result.querySelector('script, [onerror]')).toBeNull()
    expect(result.querySelector('span').style.fontSize).toBe('24px')
    expect(result.querySelector('code').textContent).toBe('<script>literal example</script>')
    expect(result.querySelector('img').getAttribute('src')).toBe('https://example.com/image.png')
    for (const tag of ['h1', 'strong', 'em', 'del', 'li', 'td']) {
      expect(result.querySelector(tag)).not.toBeNull()
    }
  })
})
