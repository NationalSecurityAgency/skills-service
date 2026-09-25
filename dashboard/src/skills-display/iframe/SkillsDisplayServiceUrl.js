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

export const validateSkillsDisplayServiceUrl = (serviceUrl) => {
  if (typeof serviceUrl !== 'string' || !serviceUrl.trim()) {
    throw new Error('A SkillTree service URL is required.')
  }
  const url = new URL(serviceUrl, window.location.href)
  // The embedding application may be cross-origin; the API must belong to the iframe's origin.
  if (!['http:', 'https:'].includes(url.protocol) || url.origin !== window.location.origin ||
    url.username || url.password || url.search || url.hash) {
    throw new Error('The SkillTree service URL must use the iframe origin without credentials, query parameters, or a fragment.')
  }
  return url.href.replace(/\/+$/, '')
}
