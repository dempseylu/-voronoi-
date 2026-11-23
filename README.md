# -voronoi- 基站覆盖区与人口分布分析

用基站坐标构建 Voronoi 覆盖区，再把手机信令记录按坐标落到对应覆盖区，输出按时段统计人口分布的 GeoJSON，可直接用 Leaflet / Mapbox / QGIS 渲染。

> 项目源自 **2020 年第十一届中国大学生服务外包创新创业大赛 A06【交通时空大数据分析挖掘系统】【东软】**，实现其中「人群密度（热力）分析」一项。官方数据见 [data/a06/SOURCE.md](data/a06/SOURCE.md)，原始开发日志见 [docs/DEVLOG.md](docs/DEVLOG.md)，缺陷修复清单见 [docs/FIXES.md](docs/FIXES.md)。

## 处理流程

```
基站坐标 ──▶ 去重排序 ──▶ Delaunay 三角剖分 ──▶ 邻接关系 ──▶ 半平面裁剪 ──▶ Voronoi 覆盖区
                                                                              │
信令记录 ──▶ 坐标 + 小时 ──▶ 多边形包含判断（射线法）──▶ 分时段人口 ──▶ GeoJSON

```

| 阶段 | 类 | 说明 |
| --- | --- | --- |
| 读数据 | `io.*Source` | 支持 MySQL（MyBatis）和 CSV 两种来源 |
| 三角剖分 | `algorithm.DelaunayTriangulator` | Bowyer-Watson 增量法，外接圆用归一化行列式判定 |
| 覆盖区 | `algorithm.VoronoiBuilder` | 对每个基站，用 Delaunay 邻接的半平面求交 |
| 人口归属 | `algorithm.PopulationAnalyzer` | 多边形包含 + 最近基站兜底 |
| 导出 | `io.GeoJsonExporter` | RFC 7946 GeoJSON，外环逆时针闭合 |

## 快速开始

要求 JDK 8+、Maven 3.6+。

```bash
# 构建（含 49 个单元测试）
mvn clean package

# 用自带样例数据跑一遍，不需要数据库
java -jar target/voronoi-analysis.jar \
  --stations=csv:samples/stations.csv \
  --users=csv:samples/users.csv \
  --out=out/cells.geojson
```

样例输出：

```
Loaded 25 stations and 600 user records
Triangulation : 32 triangles, 16 boundary points
Voronoi cells : 25 (duplicates skipped: 0)
Population    : users=600, inside=464, nearest=136, unassigned=0, unknownHour=0
Total area    : 285.382 km2
Output        : .../out/cells.geojson (185 ms)
```

## 真实赛题数据（2020 服创大赛 A06）

本项目的来源是 **2020 年第十一届中国大学生服务外包创新创业大赛 A 类命题 A06【交通时空大数据分析挖掘系统】【东软】**。赛题原文写明「东软提供原始数据（脱敏信令数据）和各模块所需的静态数据」，官方附件至今仍可下载。

仓库已经带上这份数据，开箱可用：

```bash
java -jar target/voronoi-analysis.jar \
  --stations=csv:data/a06/stations.csv \
  --users=csv:data/a06/users.csv \
  --out=out/a06-cells.geojson
```

| 指标 | 结果 |
| --- | --- |
| 基站（按坐标去重后） | 1519 |
| 三角网 | 3023 个三角形，13 个边界点 |
| 覆盖区 | 1519 个 |
| 信令 | 9849 条，全部落入覆盖区 |
| 总耗时 | 446 ms |

`data/a06/raw/` 是官方原始文件，`data/a06/stations.csv` 与 `users.csv` 由清洗脚本生成：

```bash
python3 tools/import_a06.py
```

脚本实现了 `数据说明.docx` 里的 7 条清洗规则（去特殊字符、去空字段、时间戳转换、只留 2018-10-03、关联坐标、按人按时间排序）。细节见 [data/a06/SOURCE.md](data/a06/SOURCE.md)。

## 命令行参数

| 参数 | 说明 |
| --- | --- |
| `--stations=<db\|csv:path>` | 基站来源，`db` 表示走数据库 |
| `--users=<db\|csv:path>` | 信令来源 |
| `--out=<path>` | 输出文件，默认 `out/cells.geojson` |
| `--clip-margin=<0..1>` | 覆盖区裁剪窗口相对基站外接框的外扩比例，默认 `0.05` |
| `--help` | 打印帮助 |

不带参数时，按 `application.properties` 里的 `stations.source` / `users.source` 读取。

## 配置

`src/main/resources/application.properties`：

```properties
db.url=jdbc:mysql://localhost:3306/yy?serverTimezone=Asia/Shanghai&useSSL=false
db.username=root
db.password=
stations.source=db
users.source=db
output=out/cells.geojson
clip.margin=0.05
```

数据库密码不要写进文件，用环境变量覆盖：

```bash
export VORONOI_DB_PASSWORD='...'
```

可覆盖的变量：`VORONOI_DB_URL`、`VORONOI_DB_USERNAME`、`VORONOI_DB_PASSWORD`、`VORONOI_DB_DRIVER`。

### 数据库表结构

```sql
-- 基站表
CREATE TABLE jizhandata (
  lac       INT PRIMARY KEY,     -- 基站编号
  longitude DOUBLE NOT NULL,
  latitude  DOUBLE NOT NULL
);

-- 信令表
CREATE TABLE dempsey (
  time      BIGINT NOT NULL,     -- yyyyMMddHHmmss、epoch 毫秒或 0-23 的小时
  imsi      BIGINT NOT NULL,
  longitude DOUBLE NOT NULL,
  latitude  DOUBLE NOT NULL,
  lac       INT
);
```

## CSV 格式

`stations.csv`：

```csv
id,name,longitude,latitude
1,SY-001,123.30000,41.75000
```

`name` 列可以省略，写成 `id,longitude,latitude`。表头、空行、`#` 注释都会被跳过。

`users.csv`：

```csv
time,imsi,longitude,latitude,lac
20200301192830,460000000000001,123.429374,41.928262,20
```

## 输出

GeoJSON `FeatureCollection`，每个覆盖区一个 `Feature`：

```json
{
  "type": "Feature",
  "geometry": { "type": "Polygon", "coordinates": [[[123.3, 41.75], "...", [123.3, 41.75]]] },
  "properties": {
    "stationId": 1,
    "stationName": "SY-001",
    "areaKm2": 7.231341,
    "totalUsers": 32,
    "hourlyUsers": [0, 1, 0, "... 共 24 个"],
    "hourlyCapacity": [0, 2, 0, "... 共 24 个"],
    "hourlyChangeRate": [0.5, -1.0, "... 共 24 个"]
  }
}
```

- `areaKm2`：覆盖区面积，按覆盖区平均纬度做局部等距投影后计算。
- `hourlyUsers[24]`：该小时落入本覆盖区的信令条数。
- `hourlyCapacity[24]`：论文给出的上界估计 `ceil((1.1 + 1/ln(n)) * n) + 5`，`n=0` 取 0，`n=1` 取 2。
- `hourlyChangeRate[24]`：相对下一小时的变化率，`(next - current) / current`。

## 代码结构

```
src/main/java/org/example/voronoi/
├── Main.java                       命令行入口
├── Pipeline.java                   端到端流程（不依赖 IO，便于测试）
├── algorithm/
│   ├── DelaunayTriangulator.java   Bowyer-Watson 三角剖分
│   ├── Triangulation.java          三角网结果 + 邻接表
│   ├── VoronoiBuilder.java         半平面裁剪生成覆盖区
│   └── PopulationAnalyzer.java     人口归属与分时段统计
├── geometry/
│   ├── Coordinate.java             经纬度值对象（可作 HashMap 键）
│   ├── Edge.java                   无向边（equals/hashCode 成对实现）
│   ├── Triangle.java               逆时针三角形 + 外接圆判定
│   └── Geometry.java               叉积、面积、包含、裁剪、凸包
├── model/                          Station / UserRecord / VoronoiCell
├── io/                             CSV、JDBC、GeoJSON 导出、来源工厂
├── db/                             MyBatis mapper
└── config/AppConfig.java           properties + 环境变量
src/main/resources/application.properties
src/test/java/                    49 个单元测试
samples/                          自造的小样例（25 基站 + 600 信令）
data/a06/                         官方 A06 赛题数据（含 raw/ 与清洗结果）
tools/import_a06.py               官方数据清洗与格式转换脚本
```

## 算法要点

**三角剖分**。逐点插入：先用一个覆盖全部输入点的超级三角形起手；对每个点，删掉外接圆包含它的三角形，把「只被一个被删三角形使用」的边与新点连成新三角形；最后删掉带超级顶点的三角形。外接圆判据用相对归一化行列式，避免近共线三角形半径爆炸。

**覆盖区**。基站的 Voronoi 元胞 = 到该基站比到任何其他基站都近的点集 = 每个 Delaunay 邻接对应一个半平面，求交即可。初始多边形是裁剪窗口矩形，逐个半平面做 Sutherland-Hodgman 裁剪，结果天然是逆时针凸多边形。边界基站的元胞会被窗口截断，这正是「预测覆盖区」想要的效果。

**面积**。经纬度不是等距坐标，直接乘常数会偏。这里先取覆盖区平均纬度，用局部等距投影把经纬度换成米，再用鞋带公式算面积。城市尺度下与球面公式误差小于 0.2%（有测试守着）。

## 测试

```bash
mvn test
```

49 个测试，覆盖：

| 测试类 | 验证内容 |
| --- | --- |
| `GeometryTest` | 叉积定向、面积、点在多边形内、半平面裁剪、垂直/共线线段相交、凸包 |
| `DelaunayTriangulatorTest` | 三角形数满足 `2n-2-h`、空外接圆性质、无顶点丢失、重复点去重、共线输入不产生 NaN |
| `VoronoiBuilderTest` | 覆盖区面积之和等于裁剪窗口、每个基站在自己的元胞内、元胞为逆时针凸多边形、共线基站仍能铺满 |
| `PopulationAnalyzerTest` | 包含判断、最近兜底、分时段计数、变化率不是整除、空时段不产生 NaN |
| `CsvSourceTest` | 表头/注释/BOM/列数兼容、错误行号提示 |
| `PipelineTest` | 端到端流程 + GeoJSON 结构与闭合性 |
| `RealDataRegressionTest` | 官方 A06 数据：1519 基站、3023 三角形、9849 信令全部落区 |
| `AppConfigTest` | 配置默认值、MyBatis mapper 注册 |

## 已知限制

- Voronoi 只考虑几何距离，没有天线方向角、发射功率、地形遮挡。真实覆盖区需要无线传播模型。
- 裁剪窗口默认只在基站外接框外扩 5%，落在窗口外的信令会走「最近基站」兜底，并在日志中单独计数。A06 数据里有少量远郊基站，会把窗口拉到 2.6 万 km²。
- 三角剖分是 `O(n²)` 的朴素实现。样例 25 个点 185 ms；1519 个基站 446 ms；上万基站需要换分治或扫描线版本。
- `hourlyCapacity` 是论文经验公式，不是真实人口。
- 只实现了 A06 赛题的「人群密度」一项，出行分析、驻留分析、出行方式分析没有做。

## 构建产物

`mvn package` 生成 `target/voronoi-analysis.jar`（shade 打好的可执行包），可直接 `java -jar` 运行。
