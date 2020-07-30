/*
 * Copyright 2020 SkillTree
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
import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';

describe('UniqueIdGenerator', () => {
  describe('uniqueId', () => {
    it('takes a optional prefix', () => {
      const mockPrefix = Math.random();

      expect(UniqueIdGenerator.uniqueId(mockPrefix)).toBe(`${mockPrefix}1`);
    });

    it('prefix is optional', () => {
      expect(UniqueIdGenerator.uniqueId()).toBe('2');
    });
  });
});
