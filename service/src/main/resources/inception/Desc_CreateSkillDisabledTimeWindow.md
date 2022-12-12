When `Time Window` is disabled skill events are applied immediately.

Time Window is a powerful feature that limits awarding of points to a maximum number of occurrences within the configured time span. This feature provides a balance between requiring repetition of an action and spacing out that repetition.

> “Repetition is the mother of learning, the father of action, which makes it the architect of accomplishment.” - Zig Ziglar

When designing a gamification profile, the Time Window must be considered in conjunction with the `Occurrences to Completion` property. For example, you may want to require 30 occurrences to complete a skill but only up to 5 occurrences within a 24 hour window. This means that it will take a user at a minimum, 6 days to complete this skill.

Here are the properties for this hypothetical example:

* <em>Occurrences to Completion</em>: 30
* <em>Time Window</em>: 24 hours 0 minutes
* <em>Max Occurrences Within Window</em>: 5

This is just a fictitious example and values will depend on your gamification needs.

You can also disable the Time Window property of a Skill, which will force each event to be applied immediately (up to `Occurrences to Completion`). To disable, uncheck the checkbox next to the Time Window property.