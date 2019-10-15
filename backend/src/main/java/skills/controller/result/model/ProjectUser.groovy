package skills.controller.result.model

interface ProjectUser {
    String getUserId()
    Date getLastUpdated()
    Integer getTotalPoints()
    String getDn()
    String getFirstName()
    String getLastName()
    String getEmail()
    String getUserIdForDisplay()
}
