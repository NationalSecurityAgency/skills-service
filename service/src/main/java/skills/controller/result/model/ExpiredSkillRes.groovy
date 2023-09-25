package skills.controller.result.model

interface ExpiredSkillRes {
    String getUserId()
    String getSkillId()
    Date getExpiredOn()
    String getSkillName()
}
