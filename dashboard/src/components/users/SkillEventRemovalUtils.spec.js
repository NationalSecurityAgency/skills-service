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
    ['regular-only events', { count: 1, data: [{ importedSkill: false }] }],
    ['mixed events', { count: 2, data: [{ importedSkill: true }, { importedSkill: false }] }]
  ])('does not warn for %s', async (_name, response) => {
    const loadPage = vi.fn().mockResolvedValue(response);
    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(false);
    expect(loadPage).toHaveBeenCalledTimes(1);
  });

  it('checks every unfiltered page before warning for more than 50 imported events', async () => {
    const loadPage = vi.fn()
      .mockResolvedValueOnce({ count: 51, data: importedEvents(50) })
      .mockResolvedValueOnce({ count: 51, data: importedEvents(1) });

    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(true);
    expect(loadPage).toHaveBeenCalledTimes(2);
    const params = { query: '', limit: 50, ascending: true, byColumn: 0, orderBy: 'performedOn' };
    expect(loadPage).toHaveBeenNthCalledWith(1, { ...params, page: 1 });
    expect(loadPage).toHaveBeenNthCalledWith(2, { ...params, page: 2 });
  });

  it('does not warn when a regular event remains on a later page', async () => {
    const loadPage = vi.fn()
      .mockResolvedValueOnce({ count: 51, data: importedEvents(50) })
      .mockResolvedValueOnce({ count: 51, data: [{ importedSkill: false }] });

    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(false);
    expect(loadPage).toHaveBeenCalledTimes(2);
  });

  it('does not warn or continue paging when a later page is unexpectedly empty', async () => {
    const loadPage = vi.fn()
      .mockResolvedValueOnce({ count: 51, data: importedEvents(50) })
      .mockResolvedValueOnce({ count: 51, data: [] });

    expect(await hasOnlyImportedEventsRemaining(loadPage)).toBe(false);
    expect(loadPage).toHaveBeenCalledTimes(2);
  });
});
