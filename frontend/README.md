# 前端（4 页）· 硬质合金刀具出口管理系统

当前前端只覆盖后端已实现的接口，四个页面：

| 页面 | 路由 | 依赖接口 |
|---|---|---|
| 登录 | `/login` | `POST /api/v1/auth/login`、`GET /api/v1/auth/me`、`POST /auth/refresh` |
| 工作台 | `/dashboard` | `GET /api/v1/customers`（total + 按类型计数）、`GET /api/v1/meta/dict/customer_type`、当前用户 |
| 客户列表 | `/customers` | `GET /api/v1/customers`、`POST /api/v1/customers`、`GET /api/v1/meta/dict/*` |
| 客户详情 | `/customers/:id` | `GET /api/v1/customers/{id}`、客户更新/删除、联系人增删、`GET /{id}/timeline`、字典 |

**没有 mock 数据，也不调用未实现接口。** `GET /api/v1/dashboard` 在《接口文档》11.4 中已定义但后端未实现，
工作台因此只展示客户聚合指标，并在页面上明确标注。

## 技术栈

Vue 3.5 + TypeScript 5.4 + Vite 5.4 + Element Plus 2.14 + Pinia + vue-router 4 + axios。
Element Plus 组件由 `unplugin-vue-components` 在模板编译时按需引入，页面组件按路由懒加载；
`v-loading` 指令与 `ElMessage / ElMessageBox` 不经过模板解析，因此在 `src/main.ts` 显式安装并引样式。
版本已在 `package.json` 中锁定为本次验证过的组合；样式为浅色低饱和设计令牌
（`src/styles/index.css`），无额外 UI 依赖。

## 启动

前置：后端已启动在 `http://localhost:8080`（数据库迁移与种子数据已执行）。

```bash
cd frontend
pnpm install
pnpm dev
# 打开 http://localhost:5173
```

账号来自 `backend/src/main/resources/db/migration/V2__seed.sql` 的种子数据
（管理员 `admin`，密码 `Admin@2026`；正式环境请立即修改）。

### 为什么必须用代理

后端 `WebMvcConfig` 没有 CORS 配置，前端不跨域直连。`vite.config.ts` 将 `/api` 代理到
`http://localhost:8080`；生产环境应由网关或后端静态托管完成同源转发。

### 环境变量

默认 `VITE_API_BASE=/api/v1`（见 `.env.example`），一般不需要改。

## 校验与构建

```bash
pnpm install
pnpm typecheck   # vue-tsc --noEmit
pnpm build       # typecheck + vite build
```

> 本仓库已在 Node v26.13.0 / pnpm 12.16.1 下实际执行：`pnpm install`、`pnpm typecheck`、
> `pnpm build` 全部通过（2026-11-13）。本次未启动后端，因此只做了静态类型与构建校验，
> 未做真实接口联调。
>
> 构建体积（按需引入 + 路由懒加载后）：入口 JS 221 KB（gzip 88 KB），最大业务 chunk 148 KB，
> 全部 JS 分块合计约 625 KB（改造前为单文件 1,180 KB）；CSS 合计约 162 KB（改造前 383 KB）。
> 已无 `chunk > 500 kB` 警告。
>
> `pnpm-workspace.yaml` 中的 `onlyBuiltDependencies` 用于允许 esbuild / vue-demi 的安装脚本；
> pnpm 12 已不再读取 `package.json` 的 `pnpm` 字段，删掉该文件会导致安装后 Vite 无法启动。

## 目录结构

```
frontend/
├── src/
│   ├── api/            # 唯一发 HTTP 的地方：http.ts（拦截器/刷新/错误）、各业务模块、手写类型
│   ├── stores/         # auth（登录态/权限点）、dict（字典缓存）
│   ├── domain/         # 无接口的常量：字典兜底、加工材料/工序取值
│   ├── components/     # 展示型组件：PageHeader / StatCard / EmptyState / DictLabel / 两个表单弹窗
│   ├── layouts/        # DefaultLayout（侧栏 + 顶栏）
│   ├── views/          # 4 个页面 + 404
│   ├── router/         # 路由与登录守卫
│   ├── utils/          # 日期/金额/头像格式化
│   └── styles/         # 设计令牌与 Element Plus 浅色覆盖
├── vite.config.ts      # /api 代理
├── pnpm-workspace.yaml # 允许 esbuild / vue-demi 安装脚本（pnpm 12 设置）
└── index.html
```

低耦合约定：

- 页面不直接写 `axios`，只调用 `src/api/*`；
- 权限判断用登录响应里的 `perms` 与 `roleCode`（`useAuthStore().hasPerm/hasRole`）；客户写操作按目标两角色模型 `ADMIN/SALES`，删除仅 `ADMIN`（常量集中在 `domain/roles.ts`）；
- 字典统一走 `useDictStore`，组件用 `DictLabel` 渲染中文标签；
- 业务常量集中在 `domain/` 与 `api/types.ts`，不与视图混写。

## 与接口文档的差异 / 已知限制

1. `contract/openapi.yaml` 不存在，本次也未启动后端，无法用 `openapi-typescript` 生成类型；
   `src/api/types.ts` 为**手写**，逐字段对齐后端 DTO 与《接口文档》。后端 DTO 变更时需同步该文件。
2. 联系人 `GET/POST/DELETE /customers/{id}/contacts` 是后端已实现、但《接口文档》未收录的补充接口；
   字段按 `ContactUpsertRequest / ContactView` 对齐。
3. `machiningMaterials / machiningOps / machineBrands` 没有字典接口，界面按原始代码展示；
   表单提供《设计清单》C 段的取值建议，并允许自定义输入。
4. `sanctionsStatus` 没有字典接口，界面用本地映射（`clear/review/blocked` → 正常/待复核/已阻断）展示。
5. 客户时间轴每页固定 50 条且不返回 `total`，页面以“返回条数是否等于 50”判断是否还有下一页。
6. 报价、订单、产品、价格、关税、运价、合规等页面未实现；客户详情里的报价单不可点击跳转。
7. 工作台看板（`/dashboard`）、待办、钨价、汇率等指标未接入。

## 权限与按钮（目标两角色模型）

- 目标模型只有两级：`ADMIN`（管理员，全部权限、可删除客户）与 `SALES`（业务员，仅自己名下客户，可新建/编辑客户、维护联系人，不可删除，看不到成本与审批）。
- 前端门禁：新建/编辑/维护联系人 = `customer:write` 且角色 ∈ {ADMIN, SALES}；删除客户 = 仅 ADMIN。
- 后端已迁移为 2 角色（`V4__two_roles.sql`）：`ADMIN` / `SALES`；迁移说明见 [内部自用精简-影响与后端清单.md](../doc/内部自用精简-影响与后端清单.md)。
- 旧角色（BOSS / SALES_MANAGER / DOC_SPECIALIST / FINANCE）在数据库中已删除，登录不再出现；前端保留的旧角色判断只用于过渡期兼容，可后续清理。
- 种子账号：`admin`（ADMIN，来自 `V2__seed.sql`）与 `sales1`（SALES，来自 `V6__seed_sales_user.sql`）；`SALES` 只能看到自己名下的客户，数据范围由后端强制。

## 后端前置检查（前端联调前）

1. 确认 `GET /api/v1/meta/dict/customer_type` 已实现（后端已新增 meta/dict 模块）。
2. 确认 `CustomerUpsertRequest` 的 OpenAPI 描述中不再出现 `oem`。
3. 若尚未应用 `V5__fix_customer_type_label.sql`，字典 `end_user` 的 label 仍是“终端用户”；前端显示接口返回值（以字典为准）。