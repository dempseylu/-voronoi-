# A06 赛题数据来源

## 出处

| 项 | 内容 |
| --- | --- |
| 赛事 | 2020 年第十一届中国大学生服务外包创新创业大赛（A 类企业命题） |
| 赛题 | **A06【交通时空大数据分析挖掘系统】【东软】** |
| 发布时间 | 2019-11-13 |
| 命题企业 | 东软集团 |
| 题目页（归档） | <http://web.archive.org/web/20200126213531/http://www.fwwb.org.cn:80/topic/show/2b51e29f-017c-4d8e-b117-e5783bd905c5> |
| 数据附件 | `A06-东软-交通时空大数据分析挖掘系统-数据`（305 KB zip） |
| 下载地址 | <https://www.fwwb.org.cn/attached/file/20191120/20191120133950_804.zip> |
| 下载日期 | 2026-11-23（当时返回 200） |

题目第 7 节原文：**「东软提供原始数据（脱敏信令数据）和各模块所需的静态数据。」**

> [!NOTE]
> 数据版权归命题企业与大赛组委会所有，此处仅用于学习与复现赛题。用于其他用途前请自行确认授权。

## 原始文件（`raw/`）

| 文件 | 内容 | 行数 |
| --- | --- | --- |
| `服创大赛-基站经纬度数据.csv` | longitude, latitude, laci | 2792 |
| `服创大赛-原始数据.csv` | timestamp, imsi, lac_id, cell_id, phone, timestamp1, tmp0, tmp1, nid, npid | 10168 |
| `服创大赛-出行方式静态数据.csv` | 地铁/公交站点经纬度、站名、线路（GBK 编码） | 968 |
| `数据说明.docx` | 字段说明与清洗规则 | — |

`数据说明.docx` 里的字段定义：

```
timestamp   信息记录开始时间        imsi      用户识别码（唯一 id）
lac_id      基站位置区编码          cell_id   扇区编号
phone       电话号                  timestamp1 信息记录结束时间
tmp0        一级行政区编号          tmp1      二级行政区编号
nid         信息类别1               npid      信息类别2
```

基站经纬度用 `lac_id + cell_id` 拼成 `laci` 关联取得，例如 `16789-67567924`。

## 清洗规则（摘自 `数据说明.docx`）

1. 抽取 `timestamp`、`imsi`、`lac_id`、`cell_id` 四个字段
2. 去除 `imsi` 中包含 `#`、`*`、`^` 的记录
3. 去除 `imsi`、`lac_id`、`cell_id` 为空的记录
4. `timestamp` 转换成 `yyyyMMddHHmmss`
5. 去除干扰数据（只保留 2018.10.03 当天）
6. 去除两数据源关联后经纬度为空的记录
7. 以人为单位，按时间正序排序

## 生成文件

`tools/import_a06.py` 按上述规则把 `raw/` 转成项目直接可读的两个文件：

| 文件 | 列 | 行数 |
| --- | --- | --- |
| `stations.csv` | id, name(laci), longitude, latitude | 1519 |
| `users.csv` | time, imsi, longitude, latitude, lac | 9849 |

基站坐标按经纬度去重（原始 2793 条里有 1274 条重复扇区），否则三角剖分会被重复种子拖慢。

重新生成：

```bash
python3 tools/import_a06.py
```

保留全部日期：

```bash
python3 tools/import_a06.py --all-dates
```

## 处理结果

```
java -jar target/voronoi-analysis.jar \
  --stations=csv:data/a06/stations.csv \
  --users=csv:data/a06/users.csv \
  --out=out/a06-cells.geojson
```

| 指标 | 结果 |
| --- | --- |
| 基站 | 1519 |
| 三角网 | 3023 个三角形，13 个边界点 |
| 覆盖区 | 1519 个 |
| 信令 | 9849 条，全部落入覆盖区 |
| 总耗时 | 446 ms |
