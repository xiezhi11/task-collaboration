# 项目任务协作系统

## 项目简介

当前系统项目任务协作系统，用于管理项目任务从创建、分配、执行到验收的完整过程。系统采用 SpringBoot + Vue + H2 技术栈实现。

## 技术栈

- **后端**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.5
- **前端**: Vue 3 + Element Plus 2.4 + Vue Router 4
- **数据库**: H2 (文件存储模式，支持数据持久化)
- **构建工具**: Maven (后端) + npm (前端)

## 核心流程

1. **任务创建**: 负责人创建任务草稿，填写项目名称、任务标题、描述、执行人、起止日期、优先级等信息
2. **任务发布**: 负责人发布任务，未分配执行人的任务进入"待分配"状态，已分配的进入"待开始"状态
3. **任务分配**: 负责人为任务指定执行人，任务从"待分配"进入"待开始"状态
4. **进度更新**: 执行人更新任务进度，进度>0时任务自动进入"进行中"状态
5. **提交验收**: 执行人将进度更新到100%后，可提交验收，任务进入"待验收"状态
6. **验收处理**: 负责人验收通过则任务进入"已完成"状态；驳回则进入"已驳回"状态，执行人可修改后重新提交
7. **驳回重提**: 被驳回的任务，执行人修改进度后重新提交验收，支持多轮驳回与重提

## 主要目录结构

```
task-collaboration/
├── src/
│   └── main/
│       ├── java/com/taskcollab/
│       │   ├── TaskCollaborationApplication.java    # 启动类
│       │   ├── common/                              # 公共类
│       │   │   ├── Result.java                      # 统一返回结果
│       │   │   ├── TaskStatus.java                  # 任务状态常量
│       │   │   ├── TaskStateMachine.java            # 状态机（统一状态流转）
│       │   │   └── UserContext.java                 # 用户上下文
│       │   ├── config/                              # 配置类
│       │   │   ├── MybatisPlusConfig.java           # MyBatis-Plus配置
│       │   │   ├── WebConfig.java                   # Web配置(拦截器、跨域)
│       │   │   └── GlobalExceptionHandler.java      # 全局异常处理
│       │   ├── controller/                          # 控制层
│       │   │   ├── TaskController.java              # 任务接口
│       │   │   └── AuthController.java              # 认证接口
│       │   ├── dto/                                 # 数据传输对象
│       │   ├── entity/                              # 实体类
│       │   ├── mapper/                              # Mapper接口
│       │   └── service/                             # 业务逻辑层
│       └── resources/
│           ├── application.yml                      # 应用配置
│           ├── db/
│           │   ├── schema.sql                       # 建表脚本
│           │   └── data.sql                         # 初始化数据
│           └── data/                                # H2数据库文件目录
├── frontend/                                        # 前端项目
│   ├── src/
│   │   ├── api/                                     # API接口封装
│   │   ├── utils/                                   # 工具类
│   │   ├── views/                                   # 页面组件
│   │   │   ├── TaskList.vue                         # 任务列表页
│   │   │   ├── TaskDetail.vue                       # 任务详情页
│   │   │   └── Kanban.vue                           # 任务看板页
│   │   ├── router/                                  # 路由配置
│   │   ├── App.vue                                  # 根组件
│   │   └── main.js                                  # 入口文件
│   ├── package.json                                 # 前端依赖
│   └── vue.config.js                                # Vue配置
├── pom.xml                                          # Maven配置
└── readme.md                                        # 项目说明文档
```

## 关键代码模块

### 后端核心模块

1. **TaskService**: 任务核心业务逻辑
   - `createTask()` - 创建任务
   - `publishTask()` - 发布任务
   - `assignTask()` - 分配任务
   - `updateProgress()` - 更新进度
   - `submitAccept()` - 提交验收
   - `acceptPass()` - 验收通过
   - `acceptReject()` - 验收驳回
   - `getKanban()` - 获取看板数据
   - `dragUpdateStatus()` - 看板拖拽更新状态（受状态机和权限双重控制）

2. **权限控制**:
   - 负责人/管理员: 创建任务、修改任务、分配执行人、查看全部任务、验收处理
   - 执行成员: 查看分配给自己的任务、更新进度、提交验收
   - 权限判断同时在 Service 层和前端页面进行双重控制

3. **状态流转控制**:
   - 草稿 → 待分配/待开始 (发布)
   - 待分配 → 待开始 (分配)
   - 待开始 → 进行中 (进度>0)
   - 进行中/待开始/已驳回 → 待验收 (提交验收)
   - 待验收 → 已完成 (验收通过)
   - 待验收 → 已驳回 (验收驳回)
   - 已驳回 → 进行中 (更新进度)

### 前端核心模块

1. **TaskList.vue**: 任务列表页
   - 多条件筛选查询
   - 新建、分配、更新进度操作
   - 分页展示

2. **TaskDetail.vue**: 任务详情页
   - 任务完整信息展示
   - 根据角色显示不同操作按钮
   - 操作记录时间线
   - 驳回记录展示

3. **Kanban.vue**: 任务看板页
   - 按状态分组展示任务
   - 拖拽式交互体验
   - 多条件筛选
   - 逾期任务高亮显示

## 后端启动命令

```bash
# 进入项目根目录
cd task-collaboration

# 编译打包
mvn clean package

# 启动应用
mvn spring-boot:run

# 或直接运行jar包
java -jar target/task-collaboration-1.0.0.jar
```

## 前端启动命令

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run serve

# 打包生产版本
npm run build
```

## 访问地址

- **前端页面**: http://localhost:8081
- **后端接口**: http://localhost:8080/api
- **H2控制台**: http://localhost:8080/api/h2-console

## H2控制台配置

登录H2控制台时使用以下配置：
- **JDBC URL**: `jdbc:h2:file:./src/main/resources/data/task_collab`
- **用户名**: `sa`
- **密码**: (空)

## 接口地址

### 认证接口
- `POST /api/auth/login?username=xxx` - 用户登录
- `GET /api/auth/users` - 获取用户列表
- `GET /api/auth/current-user` - 获取当前用户信息

### 任务接口
- `POST /api/tasks` - 创建任务
- `POST /api/tasks/{taskId}/publish` - 发布任务
- `POST /api/tasks/{taskId}/assign?executorId=xxx` - 分配任务
- `POST /api/tasks/progress` - 更新进度
- `POST /api/tasks/{taskId}/submit-accept` - 提交验收
- `POST /api/tasks/{taskId}/accept-pass` - 验收通过
- `POST /api/tasks/accept-reject` - 验收驳回
- `PUT /api/tasks/{taskId}` - 修改任务
- `GET /api/tasks` - 查询任务列表
- `GET /api/tasks/{taskId}` - 获取任务详情
- `GET /api/tasks/kanban` - 获取看板数据
- `GET /api/tasks/{taskId}/logs` - 获取操作记录

## 角色切换说明

系统预置了5个测试用户，通过页面右上角的用户下拉菜单可以切换角色：

| 用户名 | 姓名 | 角色 | 说明 |
|--------|------|------|------|
| admin | 系统管理员 | ADMIN | 超级管理员，拥有全部权限 |
| leader1 | 张经理 | LEADER | 项目负责人，可以创建和验收任务 |
| leader2 | 王经理 | LEADER | 项目负责人，可以创建和验收任务 |
| member1 | 李开发 | MEMBER | 执行成员，只能处理分配给自己的任务 |
| member2 | 王测试 | MEMBER | 执行成员，只能处理分配给自己的任务 |

**切换方式**:
1. 点击页面右上角的用户名称
2. 在下拉菜单中选择要切换的用户
3. 系统自动刷新，当前登录身份切换为选中用户

## 数据库存储说明

- H2数据库采用文件存储模式，数据文件位于 `src/main/resources/data/` 目录下
- 数据库支持追加存储，应用重启后数据不会丢失
- 如需重置数据库，删除 `src/main/resources/data/` 目录下的文件即可

## 常见异常处理

系统已实现以下异常场景的提示：
- 必填项缺失提示
- 开始日期晚于截止日期校验
- 进度值越界(0-100)校验
- 错误状态下提交验收提示
- 非权限用户操作拦截
- 已完成任务重复提交/修改拦截
- 驳回原因必填校验
- 看板空分组友好展示
