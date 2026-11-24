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

// Clipboards on Windows (Word, Outlook, Excel, etc.) hand over text/plain with \r\n line endings.
// Toast UI's insertText only splits on \n, so a stray \r ends up inside the editor's text nodes.
// The \r is invisible in the editor, but it is written into the saved markdown, where it acts
// as a line break and cuts any table it lands in (see issue #4116).
export const normalizeLineEndings = (text) => {
  if (!text) {
    return text
  }
  return text.replace(/\r\n?/g, '\n')
}
