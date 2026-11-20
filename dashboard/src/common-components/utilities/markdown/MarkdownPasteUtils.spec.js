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
import { afterEach, beforeAll, describe, expect, it } from 'vitest'
import Editor from '@toast-ui/editor'
import { TextSelection } from 'prosemirror-state'
import { normalizeLineEndings } from './MarkdownPasteUtils.js'

describe('normalizeLineEndings', () => {
  it('converts windows and old mac line endings to \\n', () => {
    expect(normalizeLineEndings('a\r\nb\rc\nd')).toBe('a\nb\nc\nd')
    expect(normalizeLineEndings('a\r\n\r\nb\r\n')).toBe('a\n\nb\n')
  })

  it('leaves empty values alone', () => {
    expect(normalizeLineEndings('')).toBe('')
    expect(normalizeLineEndings(null)).toBe(null)
    expect(normalizeLineEndings(undefined)).toBe(undefined)
  })
})

describe('pasting plain text into a table cell (#4116)', () => {
  const table = '| h1 | h2 |\n| --- | --- |\n| a | b |\n| c | d |\n| e | f |'
  const editors = []

  beforeAll(() => {
    // jsdom does not implement layout; the editor only needs these to exist
    document.elementFromPoint = () => null
    Range.prototype.getClientRects = () => []
    Range.prototype.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 })
  })

  afterEach(() => {
    editors.forEach((editor) => editor.destroy())
    editors.length = 0
    document.body.innerHTML = ''
  })

  const createEditor = (initialValue) => {
    const el = document.createElement('div')
    document.body.appendChild(el)
    const editor = new Editor({ el, initialEditType: 'wysiwyg', initialValue, usageStatistics: false })
    editors.push(editor)
    return editor
  }

  const placeCursorAfterText = (editor, text) => {
    const view = editor.wwEditor.view
    let pos = null
    view.state.doc.descendants((node, nodePos) => {
      if (pos === null && node.isText && node.text === text) {
        pos = nodePos + node.text.length
      }
    })
    view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, pos)))
  }

  const countRowsAfterReload = (markdown) => {
    const reloaded = createEditor(markdown)
    return reloaded.wwEditor.view.dom.querySelectorAll('table tr').length
  }

  // same steps MarkdownEditor.handlePasteAsText takes with the clipboard's text/plain value
  const pasteIntoCell = (clipboardText) => {
    const editor = createEditor(table)
    placeCursorAfterText(editor, 'a')
    editor.insertText(normalizeLineEndings(clipboardText))
    return editor.getMarkdown()
  }

  it.each([
    ['one line copied from an Excel cell', 'value\r\n'],
    ['two lines', 'first\r\nsecond'],
    ['two paragraphs', 'first\r\n\r\nsecond'],
    ['Excel rows', 'x\ty\r\nz\tw\r\n'],
  ])('keeps every table row after save and reload: %s', (_, clipboardText) => {
    const markdown = pasteIntoCell(clipboardText)

    expect(markdown).not.toContain('\r')
    expect(countRowsAfterReload(markdown)).toBe(4)
  })
})
