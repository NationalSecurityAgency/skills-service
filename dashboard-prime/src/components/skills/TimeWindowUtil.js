
export default class TimeWindowUtil {
    static timeWindowHasLength(skill)
    {
        return skill.timeWindowEnabled && skill.numPerformToCompletion > 1;
    }

    static timeWindowTitle(skill)
    {
        let title = '';
        if (!skill.timeWindowEnabled) {
            title = 'Time Window Disabled';
        } else if (skill.numPerformToCompletion === 1) {
            title = 'Time Window N/A';
        } else {
            title = `${skill.pointIncrementIntervalHrs} Hour`;
            if (skill.pointIncrementIntervalHrs === 0 || skill.pointIncrementIntervalHrs > 1) {
                title = `${title}s`;
            }
            if (skill.pointIncrementIntervalMins > 0) {
                title = `${title} ${skill.pointIncrementIntervalMins} Minute`;
                if (skill.pointIncrementIntervalMins > 1) {
                    title = `${title}s`;
                }
            }
        }
        return title;
    }

    static timeWindowDescription(skill)
    {
        const numOccur = skill.numPointIncrementMaxOccurrences;
        let desc = 'Minimum Time Window between occurrences to receive points';
        if (!skill.timeWindowEnabled) {
            desc = 'Each occurrence will receive points immediately';
        } else if (numOccur > 1) {
            desc = `Up to ${numOccur} occurrences within this time window to receive points`;
        } else if (skill.numPerformToCompletion === 1) {
            desc = 'Only one event is required to complete this skill.';
        }
        return desc;
    }
}