# 缺陷清单与修复对照

本次重构修复了 2020 年版本中 15 处会算错或跑不起来的问题。每一条都对应一个自动化测试，测试名写在最后一列。

## 一、导致程序无法运行

| # | 原代码问题 | 根因 | 修复方式 | 对应测试 |
| --- | --- | --- | --- | --- |
| 1 | `Point` 没有无参构造器，`s` 字段为 null | `@Data` 遇到显式构造器就不再生成构造器；坐标嵌套在 `Site` 里，MyBatis 无法回填 | 改成扁平的 `Station`（`longitude`/`latitude` 直接是字段）并提供无参构造器与 setter | `AppConfigTest#myBatisFactoryRegistersBothMappers`、`CsvSourceTest` |
| 2 | `select lac,longitude,latitude` 映射不到 `Point.id` | 列名 `lac` 与属性 `id` 不同名，且没有 `resultMap` | SQL 显式写 `lac AS id`，字段与列一一对应 | 同上 |
| 3 | 输出路径写死 `D:\123.json` | 硬编码 Windows 路径 | 改为 `--out` 参数，默认值来自 `application.properties` | `PipelineTest#geoJsonExportIsValidAndClosed` |
| 4 | 数据库密码明文写在 `mybatis-config.xml` | 配置文件提交进仓库 | 密码默认留空，用 `VORONOI_DB_PASSWORD` 环境变量覆盖 | `AppConfigTest#defaultsAreLoadedFromTheClasspath` |
| 5 | `mybatis-config.xml` 放在 `src/main/config` | Maven 只复制 `src/main/resources` | 配置移入 resources，MyBatis 改为代码构建 | `AppConfigTest` |

## 二、算法结果错误

| # | 原代码问题 | 根因 | 修复方式 | 对应测试 |
| --- | --- | --- | --- | --- |
| 6 | 重复点造出零面积三角形，圆心变 `NaN` | 读入后没有去重；`compareTo` 相等时返回 1 | 用 `TreeSet` 排序去重，`compareTo` 相等返回 0；退化三角形直接拒绝 | `duplicatePointsAreRemoved`、`collinearPointsProduceNoTrianglesAndNoNaN` |
| 7 | 近共线三角形半径达 1e12，圆心吞噬所有点 | 用 `距离 < 半径` 判外接圆，没有退化保护 | 改用归一化行列式判据，并在构造三角形时按面积剔除退化情形 | `noPointLiesInsideAForeignCircumcircle`、`triangleCountMatchesEulerFormulaForRandomPoints` |
| 8 | 39 个点算出 78000+ 个三角形 | 上述两条叠加，导致空洞（cavity）无限扩张 | 修正 6、7 后，三角形数满足 `2n-2-h` | `gridProducesTheExpectedTriangleCount` |
| 9 | 边界边链表在第一条反向边处断裂 | `broderEdges.addLast(edge)` 被注释掉了（原第 277 行） | 不再手工串链表，直接用「只被一个三角形使用」的边作为边界 | `boundaryEdgesEncloseTheHull` |
| 10 | 凸边修复只执行一轮 | `while(true)` 循环体末尾是无条件 `break` | 删掉整个修复阶段：外接圆判据正确时，三角网边界本身就是凸包 | `gridProducesTheExpectedTriangleCount`（边界 16 点） |
| 11 | 垂直弦永远被判为「不相交」 | `isCrossOrNot` 只处理了 `xB>xA` 和 `xB<xA` | 全部相交判断改用叉积符号法 | `segmentsIntersectHandlesVerticalSegments` |
| 12 | 垂直的 Voronoi 边永远删不掉 | `k=(yB-yA)/(xB-xA)` 对垂直线得到 `±Inf`，交点变 `NaN` | 放弃「连圆心再删相交边」，改为半平面裁剪 | `cellAreasTileTheClipWindow`、`cellsAreConvexAndCounterClockwise` |
| 13 | 覆盖区面积偏大 1.44 倍 | `S_km` 累加的是累计面积，且地球半径误写 3671 | 每个三角形独立换算；用局部等距投影，按覆盖区平均纬度取 `cos` | `squareMetreAreaMatchesTheSphericalReference` |
| 14 | 人口变化率几乎全是 0 | `int / int` 整除 | 改为 `(next - current) / (double) current` | `hourlyCountsAndChangeRateUseFloatingPoint` |
| 15 | 空时段的人口阈值出现 `ln(0)` | 公式未处理 0 人和 1 人 | 空时段取 0，单人时段取 2，其余按公式取整 | `capacityIsFiniteForEmptySingleAndBusyHours` |

## 三、工程性缺陷

| # | 原代码问题 | 修复方式 |
| --- | --- | --- |
| 16 | 每个用户遍历所有覆盖区做面积差比较，且没有「不在任何区」的判定 | 改为多边形包含测试（射线法）+ 最近基站兜底，并统计 `inside / nearest / unassigned` |
| 17 | `Edge` 重写 `equals` 却没有 `hashCode` | 同时实现 `equals` 与 `hashCode`，可安全用作 `HashMap` 键（边计数依赖它） |
| 18 | 无测试、无构建入口、`war` 打包但没有任何 Servlet | 改为 `jar` + shade 可执行包，`Main` 提供 CLI；新增 49 个单元测试 |
| 19 | 配置散落、密码硬编码、`System.out` 打日志 | 统一 `AppConfig`（properties + 环境变量），日志改用 `java.util.logging` |
| 20 | 死代码与未使用 import（`sun.font.*`、`DeflaterOutputStream` 等） | 全部删除，只保留被调用到的 API |
| 21 | `target/` 构建产物被提交进仓库 | 新增 `.gitignore` 并将 `target/` 从版本控制中移除 |

## 验证结果

```
mvn test
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
```

其中算法正确性用三类不变量校验：

1. **欧拉公式**：随机点、网格点的三角形数都等于 `2n - 2 - h`。
2. **空外接圆性质**：任意三角形内部不含其他输入点。
3. **面积守恒**：所有 Voronoi 覆盖区面积之和等于裁剪窗口面积（误差 < 1e-9 相对值）。
