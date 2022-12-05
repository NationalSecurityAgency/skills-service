Badges add another facet to the overall gamificaiton profile and provide a mechanism to further reward your users by awarding these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned. To create a badge navigate to `Project -> Badges` and then click `Badge +` button.

By default, when badges are created, they are in a disabled state. Disabled badges will not show up in the client display, nor can they be achieved by users. This is to allow all dependencies to be added to the badge before a user can trigger achievement. When a badge is published, all users with existing achievements that meet the Badge criteria will be immediately awarded that badge.

> A Badge can only be published one time. Once a Badge has Gone Live, it can no longer be placed into a disabled state

Creating badges is simple:

1. Navigate to `Project -> Badges` and click `Badge +`
    * You can (and should) assign an Icon to your badge.
2. Once a badge is created you can assign existing skills to that badge under `Project -> Badge -> Skills`
    * When initially created, a badge is in a Disabled state. This is to allow dependencies to be fully added to the badge before it can be achieved by users.
3. After assigning skill dependencies to the badge, locate the badge in the `Badges` view and click the `Go Live` link on the bottom right of the Badge overview.
    * When the badge is published, any users with existing achievements that meet the badge requirements will be awarded the badge at that time.

| Property | Explanation |
| -------- | ----------- |
| Badge Name | Display name of the badge |
| Badge ID | The badge ID |
| Description | *(Optional)* Description, can be used to describe how to achieve the badge or what it's significance is. The Description property supports markdown. |
| Help URL/Path | *(Optional)* URL pointing to a help article further documenting information about this badge. Please note that this property works in conjunction with the Root Help Url project setting |
| Enable Gem Feature | *(Optional)* Enables the Gem feature, allowing badges to be only available within a specific time window |