URL pointing to a **help article** providing further information about this *skill* or <em>capability</em>. Please note that this property works in conjunction with the Root Help Url project setting.

<br>
#### Project Setting: Root Help Url

Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`. For example, if

* `Root Help Url` = `http://www.myHelpDocs.com`
* and a Skill definition's `Help Url` = `/important/article`

then the client display will concatenate `Root Help Url` and `Help Url` to produce `http://www.myHelpDocs.com/important/article`.

> If a Skill's `Help Url` starts with `http` or `https` then `Root Help Url` will NOT be utilized.

If a Skill's `Help Url` is blank then no url will be displayed even if `Root Help Url` is configured. In other words `Root Help Url` only works in conjunction with a Skill's `Help Url`.