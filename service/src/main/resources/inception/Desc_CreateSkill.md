Projects are composed of Subjects which are made of Skills (or Skill Groups) and a single skill defines a training unit within the gamification framework. To complete a skill, users may need to perform the same action multiple times - repetition is important for retention after all. A Skill definition specifies how many times a skill has to be performed. Each occurrence is called a Skill Event.

To create a skill, and earn points please navigate to a subject and then click the `Skill +` button.

The following skill properties can be specified:

| Property | Explanation |
| -------- | ----------- |
| Skill Name | Display name of the skill |
| Skill ID | Skill ID that will be used to report skill events |
| Point Increment | Number of points added for each skill event; used in conjunction with the 'Occurrences to Completion' property |
| Occurrences to Completion | Number of successful occurrences to fully accomplish this skill; used in conjunction with the 'Point Increment' property |
| Time Window | Used in conjunction with the 'Max Occurrences Within Window' property; once this Max Occurrences is reached, points will not be incremented until outside of the configured Time Window. When 'Time Window' is disabled skill events are applied immediately." |
| Max Occurrences Within Window | Used in conjunction with the Time Window property; Once this Max Occurrences is reached, points will not be incremented until outside of the configured Time Window. |
| Self Reporting | (Optional) When checked Self Reporting is enabled for this skill. The type of `Approval Queue` or `Honor System` can then be selected. When choosing Approval Queue, you may also choose to require users to submit a justification when self-reporting this skill by selecting the 'Justification Required' check box. Please visit Self Reporting to learn more. |
| Version | *(Optional)* Utilize Skills Versioning to support running multiple versions of client software |
| Description | *(Optional)* Description of how to perform this skill. The Description property supports markdown. |
| Help URL/Path | *(Optional)* URL pointing to a help article providing further information about this skill or capability. Please note that this property works in conjunction with the Root Help Url project setting |