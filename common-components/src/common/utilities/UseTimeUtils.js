import dayjs from '@/common-components/DayJsCustomizer';

export const useTimeUtils = () => {

  const timeFromNow = (timestamp, fromStartOfDay = false) => {
    if (fromStartOfDay) {
      return dayjs().startOf('day').to(dayjs(timestamp));
    }
    return dayjs(timestamp).startOf('seconds').fromNow();
  }

  const isToday = (timestamp) => {
    return dayjs().utc().isSame(dayjs(timestamp), 'day');
  }

  const formatDate = (timestamp) => {
    return dayjs(timestamp).format('YYYY-MM-DD HH:mm');
  }

  return {
    timeFromNow,
    isToday,
    formatDate
  }
}