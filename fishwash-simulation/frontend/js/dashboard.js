class Dashboard {
    constructor() {
        this.vibrationChart = null;
        this.sprayChart = null;
        this.selectedDevice = null;
        this.alertCount = 0;
        this.apiBase = 'http://localhost:8080';
    }

    initDeviceSelector() {
        const selector = document.getElementById('deviceSelector');
        fetch(this.apiBase + '/api/devices')
            .then(response => response.json())
            .then(devices => {
                selector.innerHTML = '<option value="">-- 请选择设备 --</option>';
                devices.forEach(device => {
                    const option = document.createElement('option');
                    option.value = device.id;
                    option.textContent = device.deviceName || device.name || device.id;
                    selector.appendChild(option);
                });
            })
            .catch(err => {
                console.error('Failed to fetch devices:', err);
            });

        selector.addEventListener('change', (e) => {
            this.selectedDevice = e.target.value;
            if (this.selectedDevice) {
                this.fetchLatestData(this.selectedDevice);
                this.fetchVibrationModes(this.selectedDevice);
            }
        });
    }

    updateSensorDisplay(data) {
        if (!data) return;
        const fields = [
            { id: 'frictionFreq', key: 'frictionFreq', unit: 'Hz' },
            { id: 'amplitude', key: 'amplitude', unit: 'mm' },
            { id: 'sprayHeight', key: 'sprayHeight', unit: 'cm' },
            { id: 'waterTemp', key: 'waterTemp', unit: '°C' }
        ];
        fields.forEach(field => {
            const el = document.getElementById(field.id);
            if (el) {
                const val = data[field.key];
                el.textContent = val !== undefined && val !== null ? this.formatValue(val, field.unit) : '--';
            }
        });
    }

    initCharts() {
        const vibrationCtx = document.getElementById('vibrationChart').getContext('2d');
        this.vibrationChart = new Chart(vibrationCtx, {
            type: 'bar',
            data: {
                labels: [],
                datasets: [{
                    label: '共振频率 (Hz)',
                    data: [],
                    backgroundColor: 'rgba(139, 105, 20, 0.7)',
                    borderColor: 'rgba(139, 105, 20, 1)',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        labels: {
                            color: '#B8A080',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 11 }
                        }
                    }
                },
                scales: {
                    x: {
                        title: {
                            display: true,
                            text: '模态阶数',
                            color: '#8B7355',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 }
                        },
                        ticks: { color: '#8B7355' },
                        grid: { color: 'rgba(139, 105, 20, 0.1)' }
                    },
                    y: {
                        title: {
                            display: true,
                            text: '频率 (Hz)',
                            color: '#8B7355',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 }
                        },
                        ticks: { color: '#8B7355' },
                        grid: { color: 'rgba(139, 105, 20, 0.1)' }
                    }
                }
            }
        });

        const sprayCtx = document.getElementById('sprayChart').getContext('2d');
        this.sprayChart = new Chart(sprayCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: '喷水高度 (cm)',
                    data: [],
                    borderColor: 'rgba(70, 130, 180, 1)',
                    backgroundColor: 'rgba(70, 130, 180, 0.15)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointBackgroundColor: 'rgba(70, 130, 180, 1)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        labels: {
                            color: '#B8A080',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 11 }
                        }
                    }
                },
                scales: {
                    x: {
                        title: {
                            display: true,
                            text: '时间',
                            color: '#8B7355',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 }
                        },
                        ticks: { color: '#8B7355' },
                        grid: { color: 'rgba(139, 105, 20, 0.1)' }
                    },
                    y: {
                        title: {
                            display: true,
                            text: '高度 (cm)',
                            color: '#8B7355',
                            font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 }
                        },
                        ticks: { color: '#8B7355' },
                        grid: { color: 'rgba(139, 105, 20, 0.1)' }
                    }
                }
            }
        });
    }

    updateVibrationChart(modes) {
        if (!this.vibrationChart || !modes) return;
        const labels = modes.map(m => m.modeOrder + '阶');
        const data = modes.map(m => m.resonanceFreq);
        this.vibrationChart.data.labels = labels;
        this.vibrationChart.data.datasets[0].data = data;
        this.vibrationChart.update();
    }

    updateSprayChart(analyses) {
        if (!this.sprayChart || !analyses) return;
        const labels = analyses.map(a => {
            if (a.timestamp) {
                const d = new Date(a.timestamp);
                return d.getHours().toString().padStart(2, '0') + ':' +
                       d.getMinutes().toString().padStart(2, '0') + ':' +
                       d.getSeconds().toString().padStart(2, '0');
            }
            return '';
        });
        const data = analyses.map(a => a.predictedSprayHeight || a.sprayHeight);
        this.sprayChart.data.labels = labels;
        this.sprayChart.data.datasets[0].data = data;
        this.sprayChart.update();
    }

    showAlert(alert) {
        const alertList = document.getElementById('alertList');
        const emptyMsg = alertList.querySelector('.alert-empty');
        if (emptyMsg) {
            emptyMsg.remove();
        }

        const item = document.createElement('div');
        item.className = 'alert-item ' + (alert.alertLevel || alert.level || 'INFO');

        const time = document.createElement('div');
        time.className = 'alert-item-time';
        time.textContent = new Date(alert.createdAt || alert.timestamp || Date.now()).toLocaleString('zh-CN');

        const message = document.createElement('div');
        message.className = 'alert-item-message';
        message.textContent = alert.alertMessage || alert.message || alert.content || '';

        item.appendChild(time);
        item.appendChild(message);
        alertList.insertBefore(item, alertList.firstChild);

        this.alertCount++;
        this.updateAlertIndicator(alert.alertLevel || alert.level);
    }

    updateAlertIndicator(level) {
        const indicator = document.getElementById('alertIndicator');
        const alertText = indicator.querySelector('.alert-text');
        if (level === 'CRITICAL' || level === 'WARNING') {
            indicator.classList.add('has-alert');
            alertText.textContent = level === 'CRITICAL' ? '严重告警' : '警告';
        }
    }

    formatValue(val, unit) {
        if (val === null || val === undefined) return '--';
        const num = typeof val === 'number' ? val : parseFloat(val);
        if (isNaN(num)) return '--';
        return num.toFixed(2) + ' ' + unit;
    }

    fetchLatestData(deviceId) {
        if (!deviceId) return;
        fetch(this.apiBase + '/api/sensor-data/' + deviceId + '/latest')
            .then(response => response.json())
            .then(data => {
                this.updateSensorDisplay(data);
            })
            .catch(err => {
                console.error('Failed to fetch sensor data:', err);
            });
    }

    fetchVibrationModes(deviceId) {
        if (!deviceId) return;
        fetch(this.apiBase + '/api/simulation/vibration-modes/' + deviceId)
            .then(response => response.json())
            .then(data => {
                this.updateVibrationChart(data);
            })
            .catch(err => {
                console.error('Failed to fetch vibration modes:', err);
            });
    }
}
