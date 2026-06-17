-- ============================================================
-- 古代鱼洗铜盆振动模态仿真与喷水高度分析系统
-- 数据库初始化脚本
-- ============================================================

-- ============================================================
-- 1. 鱼洗设备表
-- 存储鱼洗铜盆的设备信息、材质参数与几何参数
-- ============================================================
CREATE TABLE IF NOT EXISTS fishwash_device (
    id                     SERIAL PRIMARY KEY,
    device_code            VARCHAR(50) UNIQUE NOT NULL,
    device_name            VARCHAR(100) NOT NULL,
    era                    VARCHAR(50),
    material_params        JSONB,
    geometry_params        JSONB,
    baseline_resonance_freq DOUBLE PRECISION,
    baseline_spray_height  DOUBLE PRECISION,
    status                 VARCHAR(20) DEFAULT 'ACTIVE',
    created_at             TIMESTAMP DEFAULT NOW(),
    updated_at             TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE  fishwash_device IS '鱼洗设备表';
COMMENT ON COLUMN fishwash_device.device_code IS '设备编号';
COMMENT ON COLUMN fishwash_device.device_name IS '设备名称';
COMMENT ON COLUMN fishwash_device.era IS '所属朝代';
COMMENT ON COLUMN fishwash_device.material_params IS '材质参数(密度、弹性模量、泊松比、厚度等)';
COMMENT ON COLUMN fishwash_device.geometry_params IS '几何参数(半径、高度、壁厚、底厚等)';
COMMENT ON COLUMN fishwash_device.baseline_resonance_freq IS '基准共振频率(Hz)';
COMMENT ON COLUMN fishwash_device.baseline_spray_height IS '基准喷水高度(cm)';
COMMENT ON COLUMN fishwash_device.status IS '设备状态';

-- ============================================================
-- 2. 传感器数据表
-- 每分钟采集一次：摩擦频率、振幅、喷水高度、水温
-- ============================================================
CREATE TABLE IF NOT EXISTS sensor_data (
    id             BIGSERIAL PRIMARY KEY,
    device_id      INTEGER REFERENCES fishwash_device(id),
    friction_freq  DOUBLE PRECISION NOT NULL,
    amplitude      DOUBLE PRECISION NOT NULL,
    spray_height   DOUBLE PRECISION NOT NULL,
    water_temp     DOUBLE PRECISION NOT NULL,
    recorded_at    TIMESTAMP NOT NULL,
    ingested_at    TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE  sensor_data IS '传感器数据表(每分钟上报)';
COMMENT ON COLUMN sensor_data.friction_freq IS '摩擦频率(Hz)';
COMMENT ON COLUMN sensor_data.amplitude IS '振幅(mm)';
COMMENT ON COLUMN sensor_data.spray_height IS '喷水高度(cm)';
COMMENT ON COLUMN sensor_data.water_temp IS '水温(℃)';
COMMENT ON COLUMN sensor_data.recorded_at IS '采集时间';
COMMENT ON COLUMN sensor_data.ingested_at IS '入库时间';

-- ============================================================
-- 3. 振动模态表
-- 基于有限元分析的模态振型与共振频率结果
-- ============================================================
CREATE TABLE IF NOT EXISTS vibration_mode (
    id                     BIGSERIAL PRIMARY KEY,
    device_id              INTEGER REFERENCES fishwash_device(id),
    mode_order             INTEGER NOT NULL,
    resonance_freq         DOUBLE PRECISION NOT NULL,
    mode_shape             JSONB NOT NULL,
    damping_ratio          DOUBLE PRECISION,
    fem_mesh_info          JSONB,
    fluid_coupling_factor  DOUBLE PRECISION,
    calculated_at          TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE  vibration_mode IS '振动模态表';
COMMENT ON COLUMN vibration_mode.mode_order IS '模态阶数';
COMMENT ON COLUMN vibration_mode.resonance_freq IS '共振频率(Hz)';
COMMENT ON COLUMN vibration_mode.mode_shape IS '振型数据(节点位移数组)';
COMMENT ON COLUMN vibration_mode.damping_ratio IS '阻尼比';
COMMENT ON COLUMN vibration_mode.fem_mesh_info IS '有限元网格信息';
COMMENT ON COLUMN vibration_mode.fluid_coupling_factor IS '流固耦合因子';
COMMENT ON COLUMN vibration_mode.calculated_at IS '计算时间';

-- ============================================================
-- 4. 喷水高度分析表
-- 基于驻波理论的喷水高度预测与偏差分析
-- ============================================================
CREATE TABLE IF NOT EXISTS spray_analysis (
    id                    BIGSERIAL PRIMARY KEY,
    device_id             INTEGER REFERENCES fishwash_device(id),
    friction_freq         DOUBLE PRECISION NOT NULL,
    predicted_spray_height DOUBLE PRECISION NOT NULL,
    actual_spray_height   DOUBLE PRECISION,
    standing_wave_nodes   INTEGER,
    splash_model_params   JSONB,
    deviation_ratio       DOUBLE PRECISION,
    analyzed_at           TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE  spray_analysis IS '喷水高度分析表';
COMMENT ON COLUMN spray_analysis.friction_freq IS '输入摩擦频率';
COMMENT ON COLUMN spray_analysis.predicted_spray_height IS '预测喷水高度(cm)';
COMMENT ON COLUMN spray_analysis.actual_spray_height IS '实际喷水高度(cm)';
COMMENT ON COLUMN spray_analysis.standing_wave_nodes IS '驻波节点数';
COMMENT ON COLUMN spray_analysis.splash_model_params IS '飞溅模型参数';
COMMENT ON COLUMN spray_analysis.deviation_ratio IS '偏差率';
COMMENT ON COLUMN spray_analysis.analyzed_at IS '分析时间';

-- ============================================================
-- 5. 告警记录表
-- 共振频率漂移告警与喷水高度异常告警
-- ============================================================
CREATE TABLE IF NOT EXISTS alert_record (
    id              BIGSERIAL PRIMARY KEY,
    device_id       INTEGER REFERENCES fishwash_device(id),
    alert_type      VARCHAR(50) NOT NULL,
    alert_level     VARCHAR(20) NOT NULL,
    alert_message   TEXT NOT NULL,
    metric_value    DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    is_resolved     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW(),
    resolved_at     TIMESTAMP
);

COMMENT ON TABLE  alert_record IS '告警记录表';
COMMENT ON COLUMN alert_record.alert_type IS '告警类型: RESONANCE_DRIFT / SPRAY_ABNORMAL';
COMMENT ON COLUMN alert_record.alert_level IS '告警级别: WARNING / CRITICAL';
COMMENT ON COLUMN alert_record.alert_message IS '告警消息';
COMMENT ON COLUMN alert_record.metric_value IS '触发值';
COMMENT ON COLUMN alert_record.threshold_value IS '阈值';
COMMENT ON COLUMN alert_record.is_resolved IS '是否已解决';
COMMENT ON COLUMN alert_record.created_at IS '告警创建时间';
COMMENT ON COLUMN alert_record.resolved_at IS '告警解决时间';

-- ============================================================
-- 索引
-- ============================================================

-- 传感器数据表索引（高频查询：按设备+时间范围检索）
CREATE INDEX idx_sensor_data_device_id ON sensor_data(device_id);
CREATE INDEX idx_sensor_data_recorded_at ON sensor_data(recorded_at);
CREATE INDEX idx_sensor_data_device_recorded ON sensor_data(device_id, recorded_at);

-- 振动模态表索引
CREATE INDEX idx_vibration_mode_device_id ON vibration_mode(device_id);
CREATE INDEX idx_vibration_mode_device_order ON vibration_mode(device_id, mode_order);

-- 喷水高度分析表索引
CREATE INDEX idx_spray_analysis_device_id ON spray_analysis(device_id);
CREATE INDEX idx_spray_analysis_analyzed_at ON spray_analysis(analyzed_at);

-- 告警记录表索引（常用查询：未解决告警、按类型筛选）
CREATE INDEX idx_alert_record_device_id ON alert_record(device_id);
CREATE INDEX idx_alert_record_alert_type ON alert_record(alert_type);
CREATE INDEX idx_alert_record_is_resolved ON alert_record(is_resolved);
CREATE INDEX idx_alert_record_created_at ON alert_record(created_at);
CREATE INDEX idx_alert_record_type_unresolved ON alert_record(alert_type, is_resolved) WHERE is_resolved = FALSE;

-- 设备表索引
CREATE INDEX idx_fishwash_device_status ON fishwash_device(status);

-- 时序数据优化：BRIN索引（适合按时间顺序写入的大表）
CREATE INDEX idx_sensor_data_recorded_at_brin ON sensor_data USING BRIN(recorded_at) WITH (pages_per_range = 32);
CREATE INDEX idx_spray_analysis_analyzed_at_brin ON spray_analysis USING BRIN(analyzed_at) WITH (pages_per_range = 32);

-- JSONB索引：支持材质参数查询
CREATE INDEX idx_device_material_params_gin ON fishwash_device USING GIN(material_params);
CREATE INDEX idx_device_geometry_params_gin ON fishwash_device USING GIN(geometry_params);

-- 活跃告警快速查询（部分索引）
CREATE INDEX idx_alert_record_active ON alert_record(device_id, alert_type, created_at DESC) WHERE is_resolved = FALSE;

-- 传感器数据最新记录查询优化
CREATE INDEX idx_sensor_data_device_latest ON sensor_data(device_id, recorded_at DESC);

-- ============================================================
-- 示例数据：鱼洗设备
-- ============================================================

-- 设备1：汉代双鱼纹鱼洗（典型鱼洗器型）
INSERT INTO fishwash_device (device_code, device_name, era, material_params, geometry_params, baseline_resonance_freq, baseline_spray_height, status) VALUES
(
    'FW-HAN-001',
    '汉代双鱼纹鱼洗',
    '汉代',
    '{
        "density": 8500.0,
        "elasticModulus": 1.0e11,
        "poissonRatio": 0.34,
        "thickness": 0.0025,
        "density_unit": "kg/m³",
        "elasticModulus_unit": "Pa",
        "thickness_unit": "m",
        "material_name": "青铜(铜锡合金)",
        "tin_content_pct": 15.0
    }'::jsonb,
    '{
        "radius": 0.18,
        "height": 0.14,
        "wallThickness": 0.0025,
        "bottomThickness": 0.003,
        "rimDiameter": 0.36,
        "handleHeight": 0.08,
        "unit_m": "m"
    }'::jsonb,
    285.6,
    15.2,
    'ACTIVE'
);

-- 设备2：汉代四鱼纹大鱼洗（较大型器型）
INSERT INTO fishwash_device (device_code, device_name, era, material_params, geometry_params, baseline_resonance_freq, baseline_spray_height, status) VALUES
(
    'FW-HAN-002',
    '汉代四鱼纹大鱼洗',
    '汉代',
    '{
        "density": 8500.0,
        "elasticModulus": 1.0e11,
        "poissonRatio": 0.34,
        "thickness": 0.003,
        "density_unit": "kg/m³",
        "elasticModulus_unit": "Pa",
        "thickness_unit": "m",
        "material_name": "青铜(铜锡合金)",
        "tin_content_pct": 12.0
    }'::jsonb,
    '{
        "radius": 0.22,
        "height": 0.17,
        "wallThickness": 0.003,
        "bottomThickness": 0.0035,
        "rimDiameter": 0.44,
        "handleHeight": 0.10,
        "unit_m": "m"
    }'::jsonb,
    243.8,
    22.5,
    'ACTIVE'
);

-- ============================================================
-- 示例数据：振动模态（基于有限元分析结果）
-- ============================================================

-- 设备1（FW-HAN-001）的前四阶振动模态
INSERT INTO vibration_mode (device_id, mode_order, resonance_freq, mode_shape, damping_ratio, fem_mesh_info, fluid_coupling_factor) VALUES
(
    1, 1, 285.6,
    '{
        "description": "2节径驻波振型(n=2)",
        "nodal_diameters": 2,
        "nodal_circles": 0,
        "max_displacement_mm": 0.85,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 0.85},
            {"theta_deg": 45, "amplitude_mm": 0.60},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 135, "amplitude_mm": 0.60},
            {"theta_deg": 180, "amplitude_mm": 0.85},
            {"theta_deg": 225, "amplitude_mm": 0.60},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 315, "amplitude_mm": 0.60}
        ]
    }'::jsonb,
    0.0085,
    '{
        "element_type": "SHELL181",
        "node_count": 12800,
        "element_count": 12480,
        "mesh_density": "fine"
    }'::jsonb,
    0.72
),
(
    1, 2, 412.3,
    '{
        "description": "3节径驻波振型(n=3)",
        "nodal_diameters": 3,
        "nodal_circles": 0,
        "max_displacement_mm": 1.20,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 1.20},
            {"theta_deg": 30, "amplitude_mm": 1.04},
            {"theta_deg": 60, "amplitude_mm": 0.60},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 120, "amplitude_mm": 0.60},
            {"theta_deg": 150, "amplitude_mm": 1.04},
            {"theta_deg": 180, "amplitude_mm": 1.20},
            {"theta_deg": 210, "amplitude_mm": 1.04},
            {"theta_deg": 240, "amplitude_mm": 0.60},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 300, "amplitude_mm": 0.60},
            {"theta_deg": 330, "amplitude_mm": 1.04}
        ]
    }'::jsonb,
    0.0072,
    '{
        "element_type": "SHELL181",
        "node_count": 12800,
        "element_count": 12480,
        "mesh_density": "fine"
    }'::jsonb,
    0.58
),
(
    1, 3, 567.9,
    '{
        "description": "4节径驻波振型(n=4)",
        "nodal_diameters": 4,
        "nodal_circles": 0,
        "max_displacement_mm": 1.65,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 1.65},
            {"theta_deg": 22.5, "amplitude_mm": 1.52},
            {"theta_deg": 45, "amplitude_mm": 1.17},
            {"theta_deg": 67.5, "amplitude_mm": 0.63},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 112.5, "amplitude_mm": 0.63},
            {"theta_deg": 135, "amplitude_mm": 1.17},
            {"theta_deg": 157.5, "amplitude_mm": 1.52},
            {"theta_deg": 180, "amplitude_mm": 1.65},
            {"theta_deg": 202.5, "amplitude_mm": 1.52},
            {"theta_deg": 225, "amplitude_mm": 1.17},
            {"theta_deg": 247.5, "amplitude_mm": 0.63},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 292.5, "amplitude_mm": 0.63},
            {"theta_deg": 315, "amplitude_mm": 1.17},
            {"theta_deg": 337.5, "amplitude_mm": 1.52}
        ]
    }'::jsonb,
    0.0063,
    '{
        "element_type": "SHELL181",
        "node_count": 12800,
        "element_count": 12480,
        "mesh_density": "fine"
    }'::jsonb,
    0.45
),
(
    1, 4, 738.4,
    '{
        "description": "5节径驻波振型(n=5)",
        "nodal_diameters": 5,
        "nodal_circles": 0,
        "max_displacement_mm": 2.10,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 2.10},
            {"theta_deg": 36, "amplitude_mm": 1.98},
            {"theta_deg": 72, "amplitude_mm": 1.69},
            {"theta_deg": 108, "amplitude_mm": 1.24},
            {"theta_deg": 144, "amplitude_mm": 0.65},
            {"theta_deg": 180, "amplitude_mm": 0.00},
            {"theta_deg": 216, "amplitude_mm": 0.65},
            {"theta_deg": 252, "amplitude_mm": 1.24},
            {"theta_deg": 288, "amplitude_mm": 1.69},
            {"theta_deg": 324, "amplitude_mm": 1.98}
        ]
    }'::jsonb,
    0.0058,
    '{
        "element_type": "SHELL181",
        "node_count": 12800,
        "element_count": 12480,
        "mesh_density": "fine"
    }'::jsonb,
    0.35
);

-- 设备2（FW-HAN-002）的前三阶振动模态
INSERT INTO vibration_mode (device_id, mode_order, resonance_freq, mode_shape, damping_ratio, fem_mesh_info, fluid_coupling_factor) VALUES
(
    2, 1, 243.8,
    '{
        "description": "2节径驻波振型(n=2)",
        "nodal_diameters": 2,
        "nodal_circles": 0,
        "max_displacement_mm": 1.10,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 1.10},
            {"theta_deg": 45, "amplitude_mm": 0.78},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 135, "amplitude_mm": 0.78},
            {"theta_deg": 180, "amplitude_mm": 1.10},
            {"theta_deg": 225, "amplitude_mm": 0.78},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 315, "amplitude_mm": 0.78}
        ]
    }'::jsonb,
    0.0092,
    '{
        "element_type": "SHELL181",
        "node_count": 15600,
        "element_count": 15200,
        "mesh_density": "fine"
    }'::jsonb,
    0.68
),
(
    2, 2, 358.5,
    '{
        "description": "3节径驻波振型(n=3)",
        "nodal_diameters": 3,
        "nodal_circles": 0,
        "max_displacement_mm": 1.55,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 1.55},
            {"theta_deg": 30, "amplitude_mm": 1.34},
            {"theta_deg": 60, "amplitude_mm": 0.78},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 120, "amplitude_mm": 0.78},
            {"theta_deg": 150, "amplitude_mm": 1.34},
            {"theta_deg": 180, "amplitude_mm": 1.55},
            {"theta_deg": 210, "amplitude_mm": 1.34},
            {"theta_deg": 240, "amplitude_mm": 0.78},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 300, "amplitude_mm": 0.78},
            {"theta_deg": 330, "amplitude_mm": 1.34}
        ]
    }'::jsonb,
    0.0078,
    '{
        "element_type": "SHELL181",
        "node_count": 15600,
        "element_count": 15200,
        "mesh_density": "fine"
    }'::jsonb,
    0.53
),
(
    2, 3, 497.2,
    '{
        "description": "4节径驻波振型(n=4)",
        "nodal_diameters": 4,
        "nodal_circles": 0,
        "max_displacement_mm": 2.05,
        "displacement_distribution": [
            {"theta_deg": 0, "amplitude_mm": 2.05},
            {"theta_deg": 22.5, "amplitude_mm": 1.89},
            {"theta_deg": 45, "amplitude_mm": 1.45},
            {"theta_deg": 67.5, "amplitude_mm": 0.78},
            {"theta_deg": 90, "amplitude_mm": 0.00},
            {"theta_deg": 112.5, "amplitude_mm": 0.78},
            {"theta_deg": 135, "amplitude_mm": 1.45},
            {"theta_deg": 157.5, "amplitude_mm": 1.89},
            {"theta_deg": 180, "amplitude_mm": 2.05},
            {"theta_deg": 202.5, "amplitude_mm": 1.89},
            {"theta_deg": 225, "amplitude_mm": 1.45},
            {"theta_deg": 247.5, "amplitude_mm": 0.78},
            {"theta_deg": 270, "amplitude_mm": 0.00},
            {"theta_deg": 292.5, "amplitude_mm": 0.78},
            {"theta_deg": 315, "amplitude_mm": 1.45},
            {"theta_deg": 337.5, "amplitude_mm": 1.89}
        ]
    }'::jsonb,
    0.0068,
    '{
        "element_type": "SHELL181",
        "node_count": 15600,
        "element_count": 15200,
        "mesh_density": "fine"
    }'::jsonb,
    0.41
);
