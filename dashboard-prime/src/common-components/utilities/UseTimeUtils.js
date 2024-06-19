/*
 * Copyright 2024 SkillTree
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
import dayjs from '@/common-components/DayJsCustomizer';

export const useTimeUtils = () => {

  const second = 1000;
  const minute = second * 60;
  const hour = minute * 60;
  const day = hour * 24;
  const year = day * 365;

  const timeFromNow = (timestamp, fromStartOfDay = false) => {
    if (fromStartOfDay) {
      return dayjs().startOf('day').to(dayjs(timestamp));
    }
    return dayjs(timestamp).startOf('seconds').fromNow();
  }

  const isToday = (timestamp) => {
    return dayjs().utc().isSame(dayjs(timestamp), 'day');
  }

  const formatDate = (timestamp, formatter = 'YYYY-MM-DD HH:mm') => {
    return dayjs(timestamp).format(formatter);
  }

  const simpleClockFilter = (valueInMs) => {
    if (valueInMs === null || valueInMs === undefined) {
      return 'N/A';
    }
    if (valueInMs < second) {
      return '00:00:00';
    }
    if (valueInMs < minute) {
      const seconds = Math.trunc(valueInMs / second);
      return `00:00:${seconds.toString().padStart(2, '0')}`;
    }
    if (valueInMs < hour) {
      const totalSeconds = Math.trunc(valueInMs / second);
      const minutes = Math.trunc(totalSeconds / 60);
      const seconds = totalSeconds - (minutes * 60);
      return `00:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
    const totalMinutes = Math.trunc(valueInMs / minute);
    const hours = Math.trunc(totalMinutes / 60);
    const minutes = totalMinutes - (hours * 60);
    const totalSeconds = Math.trunc(valueInMs / second);
    const seconds = totalSeconds - (hours * 60 * 60) - (minutes * 60);
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  const formatDurationDiff = (startDate, completedDate, detailedDays = false, asClock = false) => {
    const start = dayjs(startDate);
    const end = completedDate ? dayjs(completedDate) : dayjs();
    const valueInMs = end.diff(start);
    return asClock ? simpleClockFilter(valueInMs) : formatDuration(valueInMs, false, detailedDays);
  }

  const formatDuration = (valueInMs, alwaysIncludeSecondsWithMinutes = false, detailedDays = false) => {
    if (valueInMs === null || valueInMs === undefined) {
      return 'N/A';
    }
    if (valueInMs < second) {
      return `${valueInMs} ms`;
    }
    if (valueInMs < minute) {
      const seconds = Math.trunc(valueInMs / second);
      return `${seconds} second${seconds > 1 ? 's' : ''}`;
    }
    if (valueInMs < hour) {
      const totalSeconds = Math.trunc(valueInMs / second);
      const minutes = Math.trunc(totalSeconds / 60);
      const seconds = totalSeconds - (minutes * 60);
      let res = `${minutes} minute${minutes > 1 ? 's' : ''}`;
      if (alwaysIncludeSecondsWithMinutes || minutes < 10) {
        res = `${res} and ${seconds} second${seconds === 1 ? '' : 's'}`;
      }
      return res;
    }
    if (valueInMs < day) {
      const totalMinutes = Math.trunc(valueInMs / minute);
      const hours = Math.trunc(totalMinutes / 60);
      const minutes = totalMinutes - (hours * 60);
      let res = `${hours} hour${hours > 1 ? 's' : ''}`;
      if (detailedDays || hours < 10) {
        if (minutes > 0) {
          res = `${res} and ${minutes} minute${minutes === 1 ? '' : 's'}`;
        }
      }
      return res;
    }
    if (valueInMs < year) {
      const totalHours = Math.trunc(valueInMs / hour);
      const days = Math.trunc(totalHours / 24);
      const hours = totalHours - (days * 24);
      let res = `${days} day${days > 1 ? 's' : ''}`;
      if (detailedDays && days <= 7 && hours > 0) {
        res = `${res} and ${hours} hour${hours === 1 ? '' : 's'}`;
      }
      return res;
    }

    const totalDays = Math.trunc(valueInMs / day);
    const years = Math.trunc(totalDays / 365);
    return `${years} year${years > 1 ? 's' : ''}`;
  };

  const relativeTime = (date) => dayjs(date).fromNow()

  return {
    timeFromNow,
    isToday,
    formatDate,
    formatDuration,
    formatDurationDiff,
    simpleClockFilter,
    relativeTime
  }
}