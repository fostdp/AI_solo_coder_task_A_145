# 古代鱼洗铜盆振动模态仿真与喷水高度分析系统

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose 编排                          │
│                                                                 │
│  ┌──────────┐   MQTT    ┌──────────────┐   HTTP/WS   ┌───────┐ │
│  │ Simulator ├─────────►│ MQTT Broker  │◄────────────┤       │ │
│  │ (Python)  ├────┐     │ (Mosquitto)  │             │       │ │
│  └──────────┘    │     └──────────────┘             │       │ │
│       │          │                                   │       │ │
│       │ HTTP     │     ┌──────────────────────────┐  │       │ │
│       └─────────►│     │   SpringBoot Backend     │  │       │ │
│                  └────►│                          │  │  Nginx│ │
│                        │  ┌──────────────────┐   │  │ (前端) │ │
│                        │  │  dtu_receiver     │   │  │       │ │
│                        │  │  传感器采集+校验   │   │  │       │ │
│                        │  └───────┬──────────┘   │  │       │ │
│                        │          │ Event         │  │       │ │
│                        │  ┌───────▼──────────┐   │  │       │ │
│                        │  │ vibration_sim     │   │  │       │ │
│                        │  │ 有限元+流固耦合    │   │  │       │ │
│                        │  └───────┬──────────┘   │  │       │ │
│                        │          │ Event         │  │       │ │
│                        │  ┌───────▼──────────┐   │  │       │ │
│                        │  │ water_jet_analyzer│   │  │       │ │
│                        │  │ 驻波分析+喷水预测  │   │  │       │ │
│                        │  └───────┬──────────┘   │  │       │ │
│                        │          │ Event         │  │       │ │
│                        │  ┌───────▼──────────┐   │  │       │ │
│                        │  │ alarm_ws          │   │  │       │ │
│                        │  │ 告警评估+WS推送    │──┼──►       │ │
│                        │  └──────────────────┘   │  │       │ │
│                        │          │ Actuator      │  └───┬───┘ │
│                        └──────────┼───────────────┘      │     │
│                                   │                       │     │
│                        ┌──────────▼──────────┐   ┌───────▼───┐ │
│                        │    PostgreSQL 16     │   │ Prometheus │ │
│                        │    + BRIN/GIN索引     │   │ + Grafana  │ │
│                        └─────────────────────┘   └───────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 模块说明

| 模块 | 技术栈 | 职责 |
|------|--------|------|
| **dtu_receiver** | SpringBoot | 传感器数据采集、校验（频率0~10000Hz, 振幅≥0, 喷水高度≥0, 水温-10~100°C）、发布 `SensorDataIngestedEvent` |
| **vibration_simulator** | SpringBoot | 有限元模态分析（薄圆柱壳理论 + ALE流固耦合）、监听传感器事件自动触发、发布 `VibrationModeComputedEvent` |
| **water_jet_analyzer** | SpringBoot | 驻波理论 η=A·Jₙ(kr)·cos(nθ)·cos(ωt) 喷水高度预测 + Weber数飞溅模型、发布 `SprayAnalysisCompletedEvent` |
| **alarm_ws** | SpringBoot + STOMP | 告警评估（频率漂移/喷水偏差）、WebSocket `/topic/alerts` 推送 |
| **frontend** | Three.js + Chart.js + Nginx | 鱼洗3D模型 + GPU着色器水面 + 粒子喷水 + 振动面板 + Gzip压缩 |
| **simulator** | Python + paho-mqtt | 传感器数据模拟，支持HTTP/MQTT/双模式，可固定频率/水温 |
| **PostgreSQL** | 16-alpine | 5张表 + BRIN时序索引 + GIN JSONB索引 + 部分索引 |
| **Mosquitto** | Eclipse Mosquitto 2 | MQTT消息代理，1883(TCP) + 9001(WebSocket) |
| **Prometheus + Grafana** | 开源监控 | Spring Actuator指标采集 → Prometheus → Grafana可视化 |

## 事件驱动架构

```
SensorDataIngestedEvent ──► vibration_simulator (自动模态分析)
                       ├──► water_jet_analyzer  (自动喷水分析)
                       └──► alarm_ws            (共振漂移告警)

VibrationModeComputedEvent ──► water_jet_analyzer (用新模态重算喷水)

SprayAnalysisCompletedEvent ──► alarm_ws (喷水偏差告警)
```

## 快速部署

### 前置要求

- Docker 20.10+
- Docker Compose v2.0+

### 一键启动

```bash
cd docker
docker compose up -d
```

### 逐服务启动

```bash
cd docker

# 1. 先启动基础设施
docker compose up -d postgres mqtt-broker

# 2. 等待数据库就绪后启动后端
docker compose up -d backend

# 3. 启动前端和监控
docker compose up -d frontend prometheus grafana

# 4. 启动模拟器
docker compose up -d simulator
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端界面 | http://localhost |
| 后端API | http://localhost:8080 |
| Actuator健康检查 | http://localhost:8080/actuator/health |
| Prometheus指标 | http://localhost:8080/actuator/prometheus |
| Prometheus控制台 | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin123) |
| MQTT WebSocket | ws://localhost:9001 |
| PostgreSQL | localhost:5432 (fishwash/fishwash123) |

## 传感器模拟器用法

### Docker内运行（默认配置）

模拟器在 docker-compose 中已配置为同时通过 HTTP 和 MQTT 上报数据，间隔60秒。

### 本地运行

```bash
cd simulator
pip install -r requirements.txt
```

#### HTTP模式（默认）

```bash
python sensor_simulator.py --api-url http://localhost:8080 --device-ids 1 2 --interval 60
```

#### MQTT模式

```bash
python sensor_simulator.py --mode mqtt --mqtt-host localhost --mqtt-port 1883 --device-ids 1 2
```

#### 双模式（HTTP + MQTT）

```bash
python sensor_simulator.py --mode both \
  --api-url http://localhost:8080 \
  --mqtt-host localhost --mqtt-port 1883 \
  --device-ids 1 2
```

#### 固定摩擦频率（测试共振场景）

```bash
# 将频率固定在285.6Hz（设备1的共振频率）
python sensor_simulator.py --fixed-freq 285.6 --device-ids 1
```

#### 固定水温（测试温度影响）

```bash
# 将水温固定在25°C
python sensor_simulator.py --fixed-temp 25.0 --device-ids 1 2
```

#### 自定义漂移概率

```bash
# 高漂移概率（更容易触发告警）
python sensor_simulator.py --drift-probability 0.5 --device-ids 1 2
```

#### 完整参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--mode` | http | 发布模式: http / mqtt / both |
| `--api-url` | http://localhost:8080 | API地址 |
| `--mqtt-host` | localhost | MQTT Broker地址 |
| `--mqtt-port` | 1883 | MQTT Broker端口 |
| `--mqtt-user` | 无 | MQTT用户名 |
| `--mqtt-password` | 无 | MQTT密码 |
| `--device-ids` | 1 2 | 模拟的设备ID列表 |
| `--interval` | 60 | 上报间隔（秒） |
| `--resonance-freq` | 自动 | 覆盖共振频率 |
| `--fixed-freq` | 无 | 固定摩擦频率（Hz），关闭随机游走 |
| `--fixed-temp` | 无 | 固定水温（°C），关闭随机游走 |
| `--drift-probability` | 0.1 | 每步频率漂移概率 |
| `--verbose` | false | 详细日志 |

## API端点

| 方法 | 路径 | 模块 | 说明 |
|------|------|------|------|
| POST | `/api/sensor-data/{deviceId}` | dtu_receiver | 上报传感器数据 |
| GET | `/api/sensor-data/{deviceId}/latest` | dtu_receiver | 获取最新数据 |
| GET | `/api/sensor-data/{deviceId}/history` | dtu_receiver | 历史数据 |
| POST | `/api/simulation/vibration-mode/{deviceId}` | vibration_simulator | 计算振动模态 |
| POST | `/api/simulation/shape/{deviceId}` | vibration_simulator | 计算振型 |
| POST | `/api/simulation/modal-analysis/{deviceId}` | vibration_simulator | 完整模态分析 |
| GET | `/api/simulation/vibration-modes/{deviceId}` | vibration_simulator | 查询模态列表 |
| POST | `/api/spray/analysis/{deviceId}` | water_jet_analyzer | 喷水高度分析 |
| GET | `/api/spray/history/{deviceId}` | water_jet_analyzer | 分析历史 |
| GET | `/api/alerts/active` | alarm_ws | 活跃告警 |
| GET | `/api/alerts/active/{deviceId}` | alarm_ws | 设备告警 |
| PUT | `/api/alerts/{id}/resolve` | alarm_ws | 解决告警 |
| GET | `/api/devices` | config | 设备列表 |
| WS | `/ws` → `/topic/alerts` | alarm_ws | WebSocket告警推送 |

## 配置外置

所有物理参数通过 `application.yml` 的 `fishwash:` 节点配置：

```yaml
fishwash:
  material:
    bronze-standard:
      density: 8500.0          # kg/m³
      elastic-modulus: 1.0e11   # Pa
      poisson-ratio: 0.34
      tin-content-pct: 15.0
    bronze-high-tin:
      density: 8700.0
      elastic-modulus: 0.95e11
      poisson-ratio: 0.33
      tin-content-pct: 20.0
  fluid:
    water-density: 1000.0      # kg/m³
    surface-tension: 0.073     # N/m
    gravity: 9.81              # m/s²
  ale:
    transition-ratio: 0.15
    mesh-distortion-threshold: 0.35
    artificial-viscosity: 0.02
    stability-factor: 0.85
    surface-stability: 0.92
  splash:
    secondary-breakup-threshold: 3.5
    coefficient: 0.65
  alert:
    resonance-drift-warning: 0.05
    resonance-drift-critical: 0.15
    spray-deviation-warning: 0.30
    spray-deviation-critical: 0.50
```

Docker 环境可通过环境变量覆盖：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fishwash
SPRING_DATASOURCE_USERNAME=fishwash
SPRING_DATASOURCE_PASSWORD=fishwash123
```

## 监控

### Spring Actuator

- `/actuator/health` - 健康检查
- `/actuator/prometheus` - Prometheus指标
- `/actuator/metrics` - 指标列表

### Grafana配置

1. 访问 http://localhost:3000 (admin/admin123)
2. 添加数据源 → Prometheus → URL: `http://prometheus:9090`
3. 导入Dashboard或自定义面板，常用指标：
   - `jvm_memory_used_bytes` - JVM内存
   - `http_server_requests_seconds` - API延迟
   - `hikaricp_connections_active` - 数据库连接池
   - `application_fsm_spring_events_total` - Spring Events吞吐

## PostgreSQL索引策略

| 表 | 索引类型 | 用途 |
|----|---------|------|
| sensor_data | B-tree (device_id, recorded_at DESC) | 最新数据查询 |
| sensor_data | BRIN (recorded_at) | 时序范围扫描 |
| fishwash_device | GIN (material_params, geometry_params) | JSONB查询 |
| alert_record | 部分索引 (WHERE is_resolved=FALSE) | 活跃告警查询 |
| vibration_mode | B-tree (device_id, mode_order) | 模态检索 |

## 本地开发（非Docker）

```bash
# 1. 启动PostgreSQL
createdb fishwash
psql -d fishwash -f database/init.sql

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端（任意静态服务器）
cd frontend
python -m http.server 8000

# 4. 启动模拟器
cd simulator
pip install -r requirements.txt
python sensor_simulator.py --verbose
```
