To manage and view project-wide settings navigate to `Project -> Settings`.

There are many project-level settings available including:

* **Visibility** \- defines how this project is visible to the users consuming the training \(covered below in a great detail\)
* **Project Description** \- determines where description is rendered \(covered below in a great detail\)
* **Rank Opt-Out for ALL Admins** \- when enabled\, all project admins will be excluded from the Leaderboard and will not be assigned a rank within the embedded Skills Display component
* **Always Show Group Descriptions** \- toggle this setting to always show the group's descriptions in this project embedded Skills Display component and Progress and Ranking pages\.
  <br>

### Visibility Setting

There are three possible values for the Project Visibility setting:

1. Public Not Discoverable (default value)
2. Private Invite Only
3. Discoverable on Progress And Ranking

`Public Not Discoverable` projects can be accessed by users who have a direct link to the project's client display or by applications that have integrated the SkillTree client libraries. The project will not be available in Manage My Projects if the Progress and Ranking views have been enabled.

`Private Invite Only` projects can only be accessed by users who have been invited to join the project and who have accepted the invite, any other user attempting to access the project will receive an Access Denied error. Users who have been designated as Project Administrators will continue to have access to the project. Users can be invited to join the project using the Project Access page.

`Discoverable on Progress And Ranking` projects can be discoverd by users in the Manage My Projects view. This option will only be displayed if the instance of SkillTree has been configured to enable the Progress and Ranking views.

### Project Description Setting

There are two possible values for the Project Description setting:

1. Only show Project Description in Manager My Projects (default value)
2. Show Project Description everywhere

`Only show Project Description in Manager My Projects` is the default value for a Project. With this setting, any project description that has been configured will only be displayed in the Manage My Projects view - in the future the description may be visible to other Project Administrators in the Import Skills From the Catalog dialog. `Show Project Description everywhere` will cause any configured project description to be displayed anywhere that the training profile is displayed, including in the Manage My Proejcts view. This setting may be most applicable for SkillTree users whose training profile is viewed primarily through the Progress and Ranking view in the SkillTree dashboard.