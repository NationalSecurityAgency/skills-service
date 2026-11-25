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

export const hasOnlyImportedEventsRemaining = async (loadPage) => {
  // Inspect unfiltered pages, not just the currently visible rows.
  const limit = 50;
  let page = 1;
  let inspectedCount = 0;
  while (true) {
    const res = await loadPage({
      query: '',
      limit,
      ascending: true,
      page,
      byColumn: 0,
      orderBy: 'performedOn'
    });
    if (!res.data?.length || res.data.some((event) => event.importedSkill !== true)) {
      return false;
    }
    inspectedCount += res.data.length;
    if (inspectedCount >= res.count) {
      return true;
    }
    page += 1;
  }
};
