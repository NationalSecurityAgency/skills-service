package skills.controller.result.model

import groovy.transform.Canonical

@Canonical
class TableResultWithTotalPointsAndLevel extends TableResultWithTotalPoints {
    Integer totalLevels = 0

    final static TableResultWithTotalPointsAndLevel EMPTY = new TableResultWithTotalPointsAndLevel()

    TableResultWithTotalPointsAndLevel() {
        super()
    }

    TableResultWithTotalPointsAndLevel(List<ProjectUser> users, Integer count, Integer totalPoints, Integer totalLevels) {
        this.totalPoints = totalPoints
        this.count = count
        this.totalCount =  count
        this.data = users
        this.totalLevels = totalLevels
    }
}
