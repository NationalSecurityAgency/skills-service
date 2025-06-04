package skills.utils

import java.text.SimpleDateFormat

class TimeRangeFormatterUtil {

    public static List<Date> formatTimeRange(String start, String end) {
        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        Date startDate = format.parse(start)
        Date endDate = format.parse(end)

        return [startDate, endDate]
    }
}
