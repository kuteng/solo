## 备忘
- Java版本 1.8
## 特殊之处
- 增加插件：通过上传Markdown文件来新增或更新文章。

### Markdown文章更新插件
- 文件头中的 `id` 可以设置文章的ID，对应 `oid` 。
  - ID需要完全有 **数字** 组成，否则拼接 *permalink* 时会报错。
- 文件头中的 `pwd` 、 `password` 可以设置文章的查看密码，对应 `articleViewPwd` 。
  - 一旦 *密码* 非空，且摘要为空，那么 *解析逻辑* 不会自动生成摘要。
- 文件头中的 `description` 、 `summary` 、 `abstract` 表示摘要内容。
- 文章头中的 `status` 表示文章状态，其中 `draft` 表示草稿、 `published` 表示可发布，默认为 `published` 。

## TODO
- Markdown插件中，支持人名替换功能。如“贾宝玉”替换为“贾某”。
- Markdown导入插件的请求支持身份验证。

## [Solo官方内容](solo.md)

## 其他
- 许可证的官方内容在[这里](http://www.gnu.org/licenses/agpl-3.0.en.html)
