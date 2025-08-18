package skills.storage.model

class MonthlyCount implements MonthlyCountItem {
    Date month
    Long count
    String projectId // optional, used when counting items for multiple projects in the same query

    MonthlyCount(Date month, Long count) {
        this.month = month
        this.count = count
    }

    MonthlyCount(String projectId, Date month, Long count) {
        this.projectId = projectId
        this.month = month
        this.count = count
    }
}
