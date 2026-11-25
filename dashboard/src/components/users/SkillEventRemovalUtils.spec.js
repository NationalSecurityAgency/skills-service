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
import { describe, expect, it, vi } from 'vitest';
import { hasOnlyImportedEventsRemaining } from './SkillEventRemovalUtils.js';

const importedEvents = (count) => Array.from({ length: count }, () => ({ importedSkill: true }));

describe('remaining imported skill events', () => {
  it.each([
    ['empty results', { count: 0, data: [] }],
    ['missing data', { count: 0 }],
    ['missing imported flag', { data: [{}] }],
    ['truthy non-boolean imported flag', { data: [{ importedSkill: 'true' }] }],
    ['regular-only events', { count: 1, data: [{ importedSkill: false }] }],
    ['mixed events', { count: 2, data: [{ importedSkill: true }, { importedSkill: false }] }]
  ])('does not warn for %s', async (_name, response) => {
    const loadPage = vi.fn().mockResolvedValue(response);
    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(false);
    expect(loadPage).toHaveBeenCalledTimes(1);
    expect(loadPage).toHaveBeenCalledWith({ query: '', limit: 5, ascending: true, page: 1, byColumn: 0, orderBy: 'performedOn' });
  });

  it('warns when the first unfiltered page contains only imported events, without checking later pages', async () => {
    const loadPage = vi.fn()
      .mockResolvedValueOnce({ count: 6, data: importedEvents(5) })
      .mockResolvedValueOnce({ count: 6, data: [{ importedSkill: false }] });

    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(true);
    expect(loadPage).toHaveBeenCalledTimes(1);
    expect(loadPage).toHaveBeenCalledWith({ query: '', limit: 5, ascending: true, page: 1, byColumn: 0, orderBy: 'performedOn' });
  });

  it('warns for a nonempty partial page without a total count', async () => {
    const loadPage = vi.fn().mockResolvedValue({ data: importedEvents(1) });

    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(true);
    expect(loadPage).toHaveBeenCalledTimes(1);
  });
});
