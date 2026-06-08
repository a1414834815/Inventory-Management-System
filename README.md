# 仓库货物出入库管理系统 v1.0.0

## 功能模块

| 模块 | 功能 |
|------|------|
| **货品管理** | 货品编码、名称、规格、单位、分类、安全库存管理 |
| **店铺管理** | 出货店铺信息维护 |
| **入库管理** | 入库单创建、进货数量/单价/总价、进货源、日期，自动更新库存 |
| **出库管理** | 一对多店铺出货、出库数量/单价/总价，自动校验并扣减库存 |
| **库存查询** | 实时库存查看、库存价值（移动加权平均）、库存预警 |
| **Excel 导入导出** | 货品/入库/出库数据导入（含模板下载）和导出 |

## 运行环境

- **Java 17** 或更高版本（推荐 [Eclipse Temurin JDK 17](https://adoptium.net/)）
- Windows 7/10/11（64位）

## 快速启动

1. 确保已安装 Java 17+
2. 双击 `启动.bat`
3. 浏览器打开 **http://localhost:8080**

## 数据存储

- 所有数据存储在 `data/` 目录下的 H2 数据库文件中
- 首次启动自动创建数据库表
- 删除 `data/` 目录可重置所有数据

## 技术栈

- **后端**：Java 17 + Spring Boot 3.2 + Spring Data JPA
- **数据库**：H2 Embedded（文件模式，无需安装）
- **前端**：HTML5 + CSS3 + Bootstrap 5 + 原生 JavaScript
- **Excel**：Apache POI 5.2

## 项目结构

```
├── ims.jar              # 可执行 JAR 包
├── 启动.bat              # Windows 启动脚本
├── data/                # 数据库文件（运行时生成）
├── backend/             # 后端源码（Maven 项目）
│   ├── pom.xml
│   └── src/main/java/com/ims/
│       ├── entity/      # 数据实体
│       ├── repository/  # 数据访问
│       ├── service/     # 业务逻辑
│       ├── controller/  # REST API
│       ├── dto/         # 数据传输对象
│       └── config/      # 配置类
└── README.md
```

## 构建方法（开发者）

```bash
cd backend
mvn clean package -DskipTests
```

构建产物：`backend/target/ims.jar`
