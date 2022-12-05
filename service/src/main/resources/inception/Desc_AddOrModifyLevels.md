Levels are users' achievement path - the overall goal of the gameified training profile is to encourage users to achieve the highest level. Levels are tracked for the entire project as well as for each subject which provides users many ways to progress forward.

The Skills dashboard supports two flexible ways to manage levels:

* **Percentage Based** <em>(default)</em>: Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage.
* <strong>Point Based</strong>: Level's from and to points are configured explicitly.

> There can be only one level strategy selected and it will apply to the **entire** project including project levels and subject levels.
> Please visit `Project -> Settings` to configure a level management strategy.

## Best practices

* Consider starting with the Percentage Based strategy as each level's points are generated from a percentage so it's quick to get started. Once the majority of skills are created and the overall points are stable then the switch to the Point based strategy may be considered.
* Initially, play around with both strategies but then select one strategy and stick with it. Both strategies work very well so it's a matter of preference.
* See the Percentage Based vs. Points Based section to determine which option works best for you.

## Percentage Based Levels

Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage. By default, projects and subjects are created with **5** levels:

| Level | Percentage |
| ----- | ---------- |
| 1 | 10% |
| 2 | 25% |
| 3 | 45% |
| 4 | 67% |
| 5 | 92% |

This allows levels to be fluid as Skills are defined and **overall** points change.

## Point Based Levels

Using `Project -> Settings`, levels can be changed to a points based strategy, where each level requires the project administrator to define an explicit point range. From and to points are defined with `from` being exclusive and `to` being inclusive.
Please Note

> A project must have at least 100 total points defined before this setting can be enabled.

**Empty Project and Subject** \- In the event that a project is switched to points based levels\, any **empty** subjects (subjects with no skills) will have levels defined based on a theoretical points maximum of 1000 e.g., "Level 1" at 100-250 points, "Level 2" at 250-450 points, "Level 3" at 450-670, etc. These values can be easily edited after the configuration change if desired.

## Percentage Based vs. Points Based

So which strategy is right for your application? As always the answer is... it depends 😃!

The percentage based approach is the easiest to manage - the points are always calculated (and re-calculated) based on the defined percentages. As skills are added, and therefore the overall amount of points goes up, the point requirements for levels are re-calculated.

But what about users that already achieved a particular level based on the previously defined points (as calculated based on the percentages)? The system's overall approach is to never take away achievements therefore that achieved level will persist. Users will simply need to earn those missing points in addition to the next level's point requirements in order to progress to the next level.

> Please note: Our overall methodology is to **never** take away achievements

If you don't like the idea that the point requirements to achieve a particular level will vary with time (as skills are added) then the points based management strategy is for you. Once you switch to the Point based strategy each level will have an explicit *from* and *to* points defined.

As new skills are added, the extra points will *not* affect existing levels and without further actions will *not* influence what it takes to achieve those levels. You really have two options to address this issue:

1. change *from* and *to* points of each level *OR*
2. create additional levels that encapsulate the newly added points.

Approach #1 has the same issues as the percentage based strategy. Approach #2 requires careful planning so that when new points are added a new level is created to accommodate those points.

***

To achieve this skill simply study the available percentage based and point-based strategies and then report completion via `I did it` button below! Thanks for reading!