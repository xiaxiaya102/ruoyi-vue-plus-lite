## 平台简介

- 本仓库前端是基于 [RuoYi-Vue-Plus plus-ui](https://github.com/JavaLionLi/plus-ui) 精简整理的轻量模板版本，配套 `ruoyi-vue-plus-lite` 后端使用。
- 相比原前端，本模板移除了 demo、workflow、monitor-admin、snailjob、缓存监控等已不适合轻量模板的页面与接口，用于快速落地常规中后台项目。
- 原项目版权、协议与核心能力归属 Dromara RuoYi-Vue-Plus / plus-ui，本仓库仅作为轻量化二次整理版本维护。
- 本仓库为前端技术栈 [Vue3](https://v3.cn.vuejs.org) + [TS](https://www.typescriptlang.org/) + [Element Plus](https://element-plus.org/zh-CN) + [Vite](https://cn.vitejs.dev) 版本。
- 成员项目: 基于 vben5(ant-design-vue) 的前端项目 [ruoyi-plus-vben5](https://github.com/imdap/ruoyi-plus-vben5)
- 成员项目: 基于soybean 的前端项目 [ruoyi-plus-soybean](https://gitee.com/xlsea/ruoyi-plus-soybean)

## 配套后端代码仓库地址

| 介绍         | 项目名              | 项目地址                                                                                                                                                                           |
|------------|:-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 轻量模板       | RuoYi-Vue-Plus-Lite | - [GitHub](https://github.com/xiaxiaya102/ruoyi-vue-plus-lite)                                                                                                                   |
| 🔥 分布式集群框架 | RuoYi-Vue-Plus   | - [Gitee](https://gitee.com/dromara/RuoYi-Vue-Plus)<br> - [GitHub](https://github.com/dromara/RuoYi-Vue-Plus)<br> - [GitCode](https://gitcode.com/dromara/RuoYi-Vue-Plus)      |
| 🔥 微服务框架   | RuoYi-Cloud-Plus | - [Gitee](https://gitee.com/dromara/RuoYi-Cloud-Plus)<br>- [GitHub](https://github.com/dromara/RuoYi-Cloud-Plus)<br> - [GitCode](https://gitcode.com/dromara/RuoYi-Cloud-Plus) |

## 分支说明

- dev 分支：当前轻量模板维护分支。

## 前端运行

```bash
# 安装依赖
pnpm install

# 启动服务
pnpm dev

# 构建生产环境
pnpm build:prod

# 前端访问地址 http://localhost:80
```

## 本框架与RuoYi的业务差异

| 业务         | 功能说明                                                      | 本框架 | RuoYi                         |
| ------------ | ------------------------------------------------------------- | ------ | ----------------------------- |
| 租户管理     | 系统内租户的管理 如:租户套餐、过期时间、用户数量、企业信息等  | 支持   | 无                            |
| 租户套餐管理 | 系统内租户所能使用的套餐管理 如:套餐内所包含的菜单等          | 支持   | 无                            |
| 用户管理     | 用户的管理配置 如:新增用户、分配用户所属部门、角色、岗位等    | 支持   | 支持                          |
| 部门管理     | 配置系统组织机构（公司、部门、小组） 树结构展现支持数据权限   | 支持   | 支持                          |
| 岗位管理     | 配置系统用户所属担任职务                                      | 支持   | 支持                          |
| 菜单管理     | 配置系统菜单、操作权限、按钮权限标识等                        | 支持   | 支持                          |
| 角色管理     | 角色菜单权限分配、设置角色按机构进行数据范围权限划分          | 支持   | 支持                          |
| 字典管理     | 对系统中经常使用的一些较为固定的数据进行维护                  | 支持   | 支持                          |
| 参数管理     | 对系统动态配置常用参数                                        | 支持   | 支持                          |
| 通知公告     | 系统通知公告信息发布维护                                      | 支持   | 支持                          |
| 操作日志     | 系统正常操作日志记录和查询 系统异常信息日志记录和查询         | 支持   | 支持                          |
| 登录日志     | 系统登录日志记录查询包含登录异常                              | 支持   | 支持                          |
| 文件管理     | 系统文件展示、上传、下载、删除等管理                          | 支持   | 无                            |
| 文件配置管理 | 系统文件上传、下载所需要的配置信息动态添加、修改、删除等管理  | 支持   | 无                            |
| 在线用户管理 | 已登录系统的在线用户信息监控与强制踢出操作                    | 支持   | 支持                          |
| 代码生成     | 多数据源前后端代码的生成（java、html、xml、sql）支持CRUD下载  | 支持   | 仅支持单数据源                |
| 系统接口     | 根据业务代码自动生成相关的api接口文档                         | 支持   | 支持                          |
