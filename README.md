# ---- 硬质合金刀具出口系统 ----

出口业务后端，覆盖客户、产品、定价、关税、运费、报价、合规、订单、报表与基础数据。

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 25.0.5（Temurin LTS） |
| Spring Boot | 4.1.2 |
| MyBatis-Plus | 3.5.18.1（Boot 4 starter） |
| Flyway | 12.4.0 |
| PostgreSQL | 18.6（驱动 42.7.14） |
| Redis | 8.6.2 |
| springdoc-openapi | 3.2.0 |

## 目录结构

```
scripts/
└── dev-env.ps1             # 当前会话的 JDK 25 / Maven 3.9.16 环境

backend/
├── mvnw / mvnw.cmd         # Maven Wrapper（固定 Maven 3.9.16）
├── .mvn/wrapper/           # Wrapper 配置
└── src/main/java/com/hardmetal/export/
    ├── ExportApplication.java
    ├── shared/                 # 共享能力
    │   ├── config/             # Jackson / OpenAPI / MyBatis-Plus / Redis 配置
    │   ├── domain/             # 跨域值对象（Money、TaxRate、Quantity）
    │   ├── exception/          # 业务异常与错误码
    │   ├── security/           # JWT、登录上下文、权限校验
    │   ├── util/
    │   └── web/                # 统一响应、分页、TraceId、全局异常
    ├── auth/                   # 认证与权限
    ├── customer/               # 客户
    ├── product/                # 产品
    ├── pricing/                # 定价
    ├── tariff/                 # 关税
    ├── freight/                # 运费
    ├── quotation/              # 报价
    ├── compliance/             # 合规
    ├── order/                  # 订单
    ├── reporting/              # 报表
    └── masterdata/             # 基础数据

每个业务域统一分四层：

domain/         聚合根、实体、值对象、领域服务、仓储接口（不依赖 Spring / MyBatis）
application/    用例编排、事务边界、DTO 装配
infrastructure/ MyBatis 持久化、缓存与外部系统适配
interfaces/     REST 控制器与请求 / 响应对象
```

依赖方向：`interfaces → application → domain`，`infrastructure → domain`；`domain` 不反向依赖任何框架。

## 本地服务

| 服务 | 启动方式 |
|---|---|
| PostgreSQL 18.6 | `D:\pgsql\start-pg.bat`（库 `trade`，用户 `hardmetal`） |
| Redis 8.6.2 | `D:\redis\Redis-8.6.2-Windows-x64-msys2\start.bat` |

两者均未注册为 Windows 服务，重启后需要手动启动。

## 构建与运行

先加载当前会话的 JDK 25 / Maven 3.9.16 环境（只影响当前 PowerShell，不改系统全局变量）：

```powershell
. .\scripts\dev-env.ps1
```

如果提示“未对文件进行数字签名”，先对当前 PowerShell 进程放开执行策略（只影响本窗口）：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
. .\scripts\dev-env.ps1
```

构建与启动：

```powershell
cd backend
.\mvnw.cmd -DskipTests package
java -jar target\hardmetal-export-backend-0.0.1-SNAPSHOT.jar
```

`backend\mvnw.cmd`（Linux/macOS 用 `backend/mvnw`）把 Maven 固定为 3.9.16，首次运行会把发行包下载到 `%USERPROFILE%\.m2\wrapper\dists`。Wrapper 只负责 Maven，JDK 25 仍由 `dev-env.ps1` 的 `JAVA_HOME` 提供。

如果不想加载脚本，也可以显式指定：

```powershell
$env:JAVA_HOME='D:\java\java-25'
cd backend
.\mvnw.cmd -DskipTests package
```

启动后：

- 健康检查与接口文档：`http://127.0.0.1:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://127.0.0.1:8080/v3/api-docs`

## 数据库迁移

迁移脚本放在 `backend/src/main/resources/db/migration/`，命名为 `V{版本}__{描述}.sql`，由 Flyway 在启动时执行。

## 约定

- 统一响应 `{code, msg, data, traceId}`，`code = 0` 表示成功。
- 金额以字符串传输，保留 2 位小数；税率 6 位；比率 4 位。
- 数据库时间统一存 UTC，连接会话由 Hikari `connection-init-sql` 固定为 UTC。
- 设计文档（接口文档、实施路线、设计清单等）保存在本地，不随仓库提交。

## Git

仓库默认分支 `main`。添加远端后推送：

```powershell
git remote add origin <your-repo-url>
git push -u origin main
```