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
// import TimeDurationFilter from '@/common-components/filter/TimeDurationFilter';
//
// describe('TimeDurationFilter', () => {
//
//   const oneSecond = 1000;
//   const oneMin = oneSecond * 60;
//   const oneHour = oneMin * 60;
//   const oneDay = oneHour * 24;
//   const oneYear = oneDay * 365;
//   it('format ms', () => {
//     expect(TimeDurationFilter(0)).toEqual('0 ms');
//     expect(TimeDurationFilter(5)).toEqual('5 ms');
//     expect(TimeDurationFilter(999)).toEqual('999 ms');
//     expect(TimeDurationFilter(1000)).toEqual('1 second');
//     expect(TimeDurationFilter(1000 * 23)).toEqual('23 seconds');
//     expect(TimeDurationFilter(oneMin - 1)).toEqual('59 seconds');
//     expect(TimeDurationFilter(oneMin)).toEqual('1 minute and 0 seconds');
//     expect(TimeDurationFilter(oneMin + (1333))).toEqual('1 minute and 1 second');
//     expect(TimeDurationFilter(oneMin + (oneSecond * 33))).toEqual('1 minute and 33 seconds');
//     expect(TimeDurationFilter(oneMin + (oneMin - 1))).toEqual('1 minute and 59 seconds');
//     expect(TimeDurationFilter((oneMin * 2) + oneSecond)).toEqual('2 minutes and 1 second');
//     expect(TimeDurationFilter((oneMin * 3) - oneSecond)).toEqual('2 minutes and 59 seconds');
//     expect(TimeDurationFilter((oneMin * 10) - oneSecond)).toEqual('9 minutes and 59 seconds');
//     expect(TimeDurationFilter((oneMin * 10))).toEqual('10 minutes');
//     expect(TimeDurationFilter(oneHour - oneSecond)).toEqual('59 minutes');
//     expect(TimeDurationFilter(oneHour)).toEqual('1 hour and 0 minutes');
//     expect(TimeDurationFilter(oneHour + oneMin + (oneSecond * 59))).toEqual('1 hour and 1 minute');
//     expect(TimeDurationFilter(oneHour + (oneMin * 2) + oneSecond)).toEqual('1 hour and 2 minutes');
//     expect(TimeDurationFilter(oneHour + (oneMin * 33) + oneSecond)).toEqual('1 hour and 33 minutes');
//     expect(TimeDurationFilter((oneHour * 2) - oneSecond)).toEqual('1 hour and 59 minutes');
//     expect(TimeDurationFilter((oneHour * 2))).toEqual('2 hours and 0 minutes');
//     expect(TimeDurationFilter((oneHour * 2) + oneMin + (oneSecond * 59))).toEqual('2 hours and 1 minute');
//     expect(TimeDurationFilter((oneHour * 10) - oneSecond)).toEqual('9 hours and 59 minutes');
//     expect(TimeDurationFilter((oneHour * 10))).toEqual('10 hours');
//     expect(TimeDurationFilter((oneHour * 10) + (oneMin * 30))).toEqual('10 hours');
//     expect(TimeDurationFilter((oneDay) - oneSecond)).toEqual('23 hours');
//     expect(TimeDurationFilter(oneDay)).toEqual('1 day');
//     expect(TimeDurationFilter(oneDay + (oneHour * 12))).toEqual('1 day');
//     expect(TimeDurationFilter((oneYear) - oneSecond)).toEqual('364 days');
//     expect(TimeDurationFilter((oneYear))).toEqual('1 year');
//     expect(TimeDurationFilter((oneYear) + oneDay)).toEqual('1 year');
//     expect(TimeDurationFilter((oneYear * 2) - oneSecond)).toEqual('1 year');
//     expect(TimeDurationFilter((oneYear * 2))).toEqual('2 years');
//   });
// });
