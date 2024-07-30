To use an existing Project as a template you can easily copy its training profile (subjects, skills, badges, etc..) into a brand-new project.
To copy a project please use the
![image.png](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFEAAAA1CAIAAAC2iJAtAAAAA3NCSVQICAjb4U/gAAAB6UlEQVRoQ2P8//8/wwgDTCPMvyDvjvp5ZET6aDyPxvNwDYGRmLYZ2fNmEYxPO1WpQ7efEVQ2GBQQ41RGYtokHPmzgf75MTF1MPgKjxuIdOdITNujfsaTboaR1Gg8D6PIxOOV0XjGEzjDSGo0nodRZOLxykiMZxY84UFTqX///3/79QduBQcrCxMjAxMjI00thRg+YH6Omr93w8X7cB8uindacPxmjaexlZI4rb09YGn75advaH57//2n7/Rtx+69pLWf6RrP77/9rNty+vvvP4mWGmge+/nnb6ih0qP3X848fEXrqKarny8/ezf76HWgb+1UpND8XL7+hLakEFDw609EJqdRhNPVz3imD959+3n47nOgJ998/UEjr8KNHbD8TGuP4TF/1M94AmcYSY3G8zCKTDxeGZh4/vX3Lz8nG1ZnCeAQx6qYPEG61lVwJwKbmYsTnCdga1yL8XCS5xPidQ2Mn888eq3ZtALZlcD2dpiRMvHupkQlXf2sLi7gpinz/ddfNBezMjMZyIhQ4g2S9NLVzxJ8nJsyPElyHy0UD0wZRgufEG/mqJ+JD6uhrHI0nody7BHv9tF4Jj6shrLK0XgeyrFHvNtH45n4sBrKKkfXSg3l2CPe7aP5mfiwGsoqR+N5KMce8W4HAPTtcC8eTPy0AAAAAElFTkSuQmCC)
copy button available on a project card on the Project page. The system will prompt you to enter a new project name and optionally modify the project id.
The following training profile elements are copied into the new project:

* Subjects and their attributes (description, help url, etc..)
* Skills definitions and their attributes (description, points, self-reporting, etc...)
* Skill Groups
* Configured display order of subjects and skills is preserved in the copied project
* Levels
* Badges
* Project-Based Dependencies
* Re-used Skills
* Project Settings are copied with the exception of the exclusions specified below

The following training profile elements are **NOT** copied into a new project:

* Catalog imported skills are not copied
* Cross-Project Dependencies are not copied
* If the original project Visibility setting was changed to be `Discoverable on Progress And Ranking` the copied project will instead use the default value of `Public Not Discoverable`

> TIP
> Once a project has been copied, the new project is disconnected from the original such that changes to the original project will not be reflected in the copy