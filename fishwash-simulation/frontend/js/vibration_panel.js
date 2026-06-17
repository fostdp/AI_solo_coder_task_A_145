var VibrationPanel = (function () {
  var API_BASE = 'http://localhost:8080';

  function VibrationPanel() {
    this.vibrationChart = null;
    this.sprayChart = null;
    this.selectedDevice = null;
    this.alertCount = 0;
    this.wsClient = null;
    this.onDeviceChange = null;
    this.onSimulate = null;
    this.onFrequencyChange = null;
  }

  VibrationPanel.prototype.init = function () {
    this.initDeviceSelector();
    this.initCharts();
    this.initWebSocket();
    this.bindControls();
  };

  VibrationPanel.prototype.initDeviceSelector = function () {
    var self = this;
    var selector = document.getElementById('deviceSelector');
    if (!selector) return;

    fetch(API_BASE + '/api/devices')
      .then(function (response) { return response.json(); })
      .then(function (result) {
        var devices = result.data || result;
        selector.innerHTML = '<option value="">-- 请选择设备 --</option>';
        devices.forEach(function (device) {
          var option = document.createElement('option');
          option.value = device.id;
          option.textContent = device.deviceName || device.name || device.id;
          selector.appendChild(option);
        });
      })
      .catch(function (err) {
        console.error('Failed to fetch devices:', err);
      });

    selector.addEventListener('change', function (e) {
      self.selectedDevice = e.target.value;
      if (self.selectedDevice) {
        self.fetchLatestData(self.selectedDevice);
        self.fetchVibrationModes(self.selectedDevice);
        if (typeof self.onDeviceChange === 'function') {
          self.onDeviceChange(self.selectedDevice);
        }
      }
    });
  };

  VibrationPanel.prototype.updateSensorDisplay = function (data) {
    if (!data) return;
    var fields = [
      { id: 'frictionFreq', key: 'frictionFreq', unit: 'Hz' },
      { id: 'amplitude', key: 'amplitude', unit: 'mm' },
      { id: 'sprayHeight', key: 'sprayHeight', unit: 'cm' },
      { id: 'waterTemp', key: 'waterTemp', unit: '°C' }
    ];
    var self = this;
    fields.forEach(function (field) {
      var el = document.getElementById(field.id);
      if (el) {
        var val = data[field.key];
        el.textContent = val !== undefined && val !== null ? self.formatValue(val, field.unit) : '--';
      }
    });
  };

  VibrationPanel.prototype.initCharts = function () {
    var vibrationCtx = document.getElementById('vibrationChart');
    if (vibrationCtx) {
      this.vibrationChart = new Chart(vibrationCtx.getContext('2d'), {
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
              labels: { color: '#B8A080', font: { family: '"Noto Serif SC", "SimSun", serif', size: 11 } }
            }
          },
          scales: {
            x: {
              title: { display: true, text: '模态阶数', color: '#8B7355', font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 } },
              ticks: { color: '#8B7355' },
              grid: { color: 'rgba(139, 105, 20, 0.1)' }
            },
            y: {
              title: { display: true, text: '频率 (Hz)', color: '#8B7355', font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 } },
              ticks: { color: '#8B7355' },
              grid: { color: 'rgba(139, 105, 20, 0.1)' }
            }
          }
        }
      });
    }

    var sprayCtx = document.getElementById('sprayChart');
    if (sprayCtx) {
      this.sprayChart = new Chart(sprayCtx.getContext('2d'), {
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
              labels: { color: '#B8A080', font: { family: '"Noto Serif SC", "SimSun", serif', size: 11 } }
            }
          },
          scales: {
            x: {
              title: { display: true, text: '时间', color: '#8B7355', font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 } },
              ticks: { color: '#8B7355' },
              grid: { color: 'rgba(139, 105, 20, 0.1)' }
            },
            y: {
              title: { display: true, text: '高度 (cm)', color: '#8B7355', font: { family: '"Noto Serif SC", "SimSun", serif', size: 12 } },
              ticks: { color: '#8B7355' },
              grid: { color: 'rgba(139, 105, 20, 0.1)' }
            }
          }
        }
      });
    }
  };

  VibrationPanel.prototype.updateVibrationChart = function (modes) {
    if (!this.vibrationChart || !modes) return;
    var labels = modes.map(function (m) { return m.modeOrder + '阶'; });
    var data = modes.map(function (m) { return m.resonanceFreq; });
    this.vibrationChart.data.labels = labels;
    this.vibrationChart.data.datasets[0].data = data;
    this.vibrationChart.update();
  };

  VibrationPanel.prototype.updateSprayChart = function (analyses) {
    if (!this.sprayChart || !analyses) return;
    var labels = analyses.map(function (a) {
      if (a.analyzedAt || a.timestamp) {
        var d = new Date(a.analyzedAt || a.timestamp);
        return d.getHours().toString().padStart(2, '0') + ':' +
               d.getMinutes().toString().padStart(2, '0') + ':' +
               d.getSeconds().toString().padStart(2, '0');
      }
      return '';
    });
    var data = analyses.map(function (a) { return a.predictedSprayHeight || a.sprayHeight; });
    this.sprayChart.data.labels = labels;
    this.sprayChart.data.datasets[0].data = data;
    this.sprayChart.update();
  };

  VibrationPanel.prototype.showAlert = function (alert) {
    var alertList = document.getElementById('alertList');
    if (!alertList) return;
    var emptyMsg = alertList.querySelector('.alert-empty');
    if (emptyMsg) {
      emptyMsg.remove();
    }

    var item = document.createElement('div');
    item.className = 'alert-item ' + (alert.alertLevel || alert.level || 'INFO');

    var time = document.createElement('div');
    time.className = 'alert-item-time';
    time.textContent = new Date(alert.createdAt || alert.timestamp || Date.now()).toLocaleString('zh-CN');

    var message = document.createElement('div');
    message.className = 'alert-item-message';
    message.textContent = alert.alertMessage || alert.message || alert.content || '';

    item.appendChild(time);
    item.appendChild(message);
    alertList.insertBefore(item, alertList.firstChild);

    this.alertCount++;
    this.updateAlertIndicator(alert.alertLevel || alert.level);
  };

  VibrationPanel.prototype.updateAlertIndicator = function (level) {
    var indicator = document.getElementById('alertIndicator');
    if (!indicator) return;
    var alertText = indicator.querySelector('.alert-text');
    if (level === 'CRITICAL' || level === 'WARNING') {
      indicator.classList.add('has-alert');
      if (alertText) {
        alertText.textContent = level === 'CRITICAL' ? '严重告警' : '警告';
      }
    }
  };

  VibrationPanel.prototype.initWebSocket = function () {
    var self = this;
    this.wsClient = {
      stompClient: null,
      connected: false,

      connect: function () {
        var client = this;
        var socket = new SockJS(API_BASE + '/ws');
        client.stompClient = Stomp.over(socket);

        client.stompClient.connect({}, function () {
          client.connected = true;
          client.updateConnectionStatus(true);
          client.stompClient.subscribe('/topic/alerts', function (message) {
            var alert = JSON.parse(message.body);
            self.showAlert(alert);
          });
        }, function (error) {
          client.connected = false;
          client.updateConnectionStatus(false);
        });
      },

      disconnect: function () {
        if (this.stompClient) {
          this.stompClient.disconnect();
        }
        this.connected = false;
        this.updateConnectionStatus(false);
      },

      updateConnectionStatus: function (connected) {
        var statusEl = document.getElementById('connectionStatus');
        if (!statusEl) return;
        var dot = statusEl.querySelector('.status-dot');
        var text = statusEl.querySelector('span:last-child') || statusEl;
        if (connected) {
          if (dot) dot.classList.add('connected');
          if (text) text.textContent = '已连接';
        } else {
          if (dot) dot.classList.remove('connected');
          if (text) text.textContent = '未连接';
        }
      }
    };

    this.wsClient.connect();
  };

  VibrationPanel.prototype.bindControls = function () {
    var self = this;

    var simulateBtn = document.getElementById('simulateBtn');
    if (simulateBtn) {
      simulateBtn.addEventListener('click', function () {
        var modeSelector = document.getElementById('modeOrder');
        var modeOrder = modeSelector ? parseInt(modeSelector.value) : 2;
        if (typeof self.onSimulate === 'function') {
          self.onSimulate(modeOrder);
        }
        self.triggerSimulationAPI(modeOrder);
      });
    }

    var frictionSlider = document.getElementById('frictionSlider');
    var frictionSliderValue = document.getElementById('frictionSliderValue');
    if (frictionSlider) {
      frictionSlider.addEventListener('input', function (e) {
        var freq = parseFloat(e.target.value);
        if (typeof self.onFrequencyChange === 'function') {
          self.onFrequencyChange(freq);
        }
        if (frictionSliderValue) {
          frictionSliderValue.textContent = freq.toFixed(0) + ' Hz';
        }
      });
    }
  };

  VibrationPanel.prototype.triggerSimulationAPI = function (modeOrder) {
    if (!this.selectedDevice) return;
    var self = this;

    fetch(API_BASE + '/api/simulation/modal-analysis/' + this.selectedDevice + '?maxModeOrder=' + modeOrder, {
      method: 'POST'
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        var modes = data && data.data ? data.data : data;
        if (Array.isArray(modes)) {
          self.updateVibrationChart(modes);
        }
      })
      .catch(function () {
        console.log('Modal analysis API not available');
      });

    var frictionSlider = document.getElementById('frictionSlider');
    var freq = frictionSlider ? parseFloat(frictionSlider.value) : 100;

    fetch(API_BASE + '/api/spray/analysis/' + this.selectedDevice, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ frictionFreq: freq, measuredSprayHeight: null })
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        var result = data && data.data ? data.data : data;
        if (result && result.predictedSprayHeight) {
          if (typeof self.onFrequencyChange === 'function') {
            self.onFrequencyChange(freq);
          }
        }
      })
      .catch(function () {
        console.log('Spray analysis API not available');
      });
  };

  VibrationPanel.prototype.fetchLatestData = function (deviceId) {
    var self = this;
    fetch(API_BASE + '/api/sensor-data/' + deviceId + '/latest')
      .then(function (res) { return res.json(); })
      .then(function (data) {
        var sensorData = data && data.data ? data.data : data;
        self.updateSensorDisplay(sensorData);
      })
      .catch(function () {
        console.log('Sensor data not available');
      });
  };

  VibrationPanel.prototype.fetchVibrationModes = function (deviceId) {
    var self = this;
    fetch(API_BASE + '/api/simulation/vibration-modes/' + deviceId)
      .then(function (res) { return res.json(); })
      .then(function (data) {
        var modes = data && data.data ? data.data : data;
        if (Array.isArray(modes)) {
          self.updateVibrationChart(modes);
        }
      })
      .catch(function () {
        console.log('Vibration modes not available');
      });
  };

  VibrationPanel.prototype.formatValue = function (val, unit) {
    if (val === null || val === undefined) return '--';
    var num = typeof val === 'number' ? val : parseFloat(val);
    if (isNaN(num)) return '--';
    return num.toFixed(2) + ' ' + unit;
  };

  VibrationPanel.prototype.destroy = function () {
    if (this.wsClient) {
      this.wsClient.disconnect();
    }
  };

  return VibrationPanel;
})();
