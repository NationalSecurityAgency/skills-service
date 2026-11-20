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
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { useTimeUtils } from './UseTimeUtils.js'

describe('UseTimeUtils.isToday', () => {
  const originalTz = process.env.TZ
  const { isToday } = useTimeUtils()

  beforeAll(() => {
    process.env.TZ = 'America/New_York'
  })
  afterAll(() => {
    if (originalTz === undefined) {
      delete process.env.TZ
    } else {
      process.env.TZ = originalTz
    }
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('is true for an earlier time on the same local day, even after UTC midnight has passed', () => {
    // 2026-10-15 20:30 in New York (EDT) is already 2026-10-16 00:30 UTC
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-16T00:30:00Z'))
    // 2026-10-15 10:00 in New York
    expect(isToday('2026-10-15T14:00:00Z')).toBe(true)
  })

  it('is false for a late evening time on the previous local day, even though it is the same UTC day', () => {
    // 2026-10-15 09:00 in New York is 2026-10-15 13:00 UTC
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-15T13:00:00Z'))
    // 2026-10-14 22:00 in New York is 2026-10-15 02:00 UTC
    expect(isToday('2026-10-15T02:00:00Z')).toBe(false)
  })

  it('is false for the next local day', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-15T13:00:00Z'))
    expect(isToday('2026-10-16T13:00:00Z')).toBe(false)
  })

  it('is true for the current moment', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-15T13:00:00Z'))
    expect(isToday('2026-10-15T13:00:00Z')).toBe(true)
  })
})
