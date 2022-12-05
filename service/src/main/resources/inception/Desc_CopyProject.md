To use an existing Project as a template you can easily copy its training profile (subjects, skills, badges, etc..) into a brand-new project.
To copy a project please use the
![copy project button](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAD0AAAAoCAIAAADCG2WWAAACJUlEQVR4Xu2Wy0sbURSH56/JpotA6cZV2013QqlgoRoEN1UUKrSmGIymqxqLb0XFF5FQstGFINJNNtpi6aYUKkpjsJUoKEl85B3N8Ybr487JcDrDJMEL8/FbhMm95/cxSeZGATlR8AVJsLyri2J7VidjdN1vtg5fKjdGKyxvcxitsLzNYbTC8jaH0Qppvb0zi9OB5UKhgN8R0D90fmmltbdPTDKdHvEFcvk8XqpGb0UhDwd+2J+5Pi/dw1OEut6hAE8dLXzgbU7PE/baV20f+ml1qiKxDX/Hr7PTCRsPWO7Oef/KGt5wAzVUzZPG15re7EWndwSvFqAq/k1wVzGKa2CCFyx9CeINN1BDAYKbP9jHxRI9OS31Po7Ff/8J/9oJbYXCeKcAVZEKQ8SHopwlkt1Dk4MLn/FqAWooQOO7Hq64Fzko9a5/0+X8NMbSOzqNdwrQFRANQsgjRnnZ4UpnsnidGnpow1s34X2bh88deKcAXQH7s/h70tLzMX9xgdepoYdWw/swgL1zOepnzqGHVsP7eBV74xVa0EOr4R1bl9P77Kec3qldOb2zRxX0ZidL03tPqTHPizYn3ilAV8BluoLe7JF6FI1nslkx7CHLg7epoSuKfLVXylsz7P8J3qAFXVHk++Myew/M+WvqmzVT1+78753m0BVFIouwWQPfHvGUwbssGK2wvM1htMLyNofRCsvbHEYrpPW2lZxwUkTX/b6HyOp9BULSATUlpI2MAAAAAElFTkSuQmCC)
copy button available on a project card on the Project page. The system will prompt you to enter a new project name and optionally modify the project id.
The following training profile elements are copied into the new project:

* Subjects and their attributes (description, help url, etc..)
* Skills definitions and their attributes (description, points, self-reporting, etc...)
* Skill Groups
* Configured display order subjects and skills is preserved in the copied project
* Levels
* Badges
* Project-Based Dependencies
* Re-used Skills
* Project's Settings are copied with the exception of the exclusions specified below

The following training profile elements are **NOT** copied into a new project:

* Catalog imported skills are not copied
* Cross-Project Dependencies are not copied
* If the original project Visibility setting was changed to be `Discoverable on Progress And Ranking` the copied project will instead use the default value of `Public Not Discoverable`

> TIP
> Once a project has been copied, the new project is disconnected from the original such that changes to the original project will not be reflected in the copy