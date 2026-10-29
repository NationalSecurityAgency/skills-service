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

import { decodeHTML } from 'entities/decode';

/**
 * Decodes one layer of HTML entities into plain text for validation, comparison,
 * or safe text rendering through Vue interpolation or textContent.
 * Uses HTML text-context entity decoding, without parsing HTML markup or requiring a DOM.
 *
 * WARNING: THIS FUNCTION DOES NOT SANITIZE HTML.
 * Decoding can turn escaped text into literal HTML tags. NEVER pass its result
 * to v-html, innerHTML, or another HTML-rendering sink without sanitizing it.
 */
export function decodeHtmlEntities(input) {
  if (!input) {
    return input;
  }
  return decodeHTML(input);
}
