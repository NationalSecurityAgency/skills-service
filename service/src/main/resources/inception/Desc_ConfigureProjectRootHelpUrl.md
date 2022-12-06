Configure project's **Root Help Url** by navigating to `Project -> Settings`. Skill definition's Help Url/Path will be treated as relative to this `Root Help Url`.

Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`. For example, if

* `Root Help Url` = `http://www.myHelpDocs.com`
* and a Skill definition's `Help Url` = `/important/article`

then the client display will concatenate `Root Help Url` and `Help Url` to produce `http://www.myHelpDocs.com/important/article`.