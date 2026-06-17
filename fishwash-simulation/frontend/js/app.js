var FishWashApp = (function () {
  var API_BASE = 'http://localhost:8080';

  function FishWashApp() {
    this.scene = null;
    this.camera = null;
    this.renderer = null;
    this.controls = null;
    this.fishWashModel = null;
    this.waterSurface = null;
    this.sprayParticles = null;
    this.dashboard = null;
    this.wsClient = null;
    this.clock = new THREE.Clock();
    this.currentModeOrder = 2;
    this.currentFrequency = 3.0;
    this.currentAmplitude = 0.005;
    this.currentSprayHeight = 0.1;
    this.selectedDeviceId = null;
    this.animating = false;
    this.animationId = null;
  }

  FishWashApp.prototype.initScene = function () {
    var container = document.getElementById('viewport3d');
    if (!container) return;

    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.Fog(0x111122, 0.5, 2.0);

    var w = container.clientWidth;
    var h = container.clientHeight;

    this.camera = new THREE.PerspectiveCamera(50, w / h, 0.01, 10);
    this.camera.position.set(0.4, 0.3, 0.4);
    this.camera.lookAt(0, 0.05, 0);

    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(w, h);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    this.renderer.setClearColor(0x111122);

    container.appendChild(this.renderer.domElement);

    this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
    this.controls.target.set(0, 0.05, 0);
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.05;
    this.controls.update();

    var ambientLight = new THREE.AmbientLight(0x404040, 1.0);
    this.scene.add(ambientLight);

    var directionalLight = new THREE.DirectionalLight(0xFFFFFF, 1.0);
    directionalLight.position.set(0.3, 0.5, 0.3);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 1024;
    directionalLight.shadow.mapSize.height = 1024;
    this.scene.add(directionalLight);

    var pointLight = new THREE.PointLight(0xB87333, 0.5, 1.0);
    pointLight.position.set(0, 0.2, 0);
    this.scene.add(pointLight);

    var groundGeom = new THREE.PlaneGeometry(2, 2);
    var groundMat = new THREE.MeshPhongMaterial({ color: 0x222222 });
    var ground = new THREE.Mesh(groundGeom, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.01;
    ground.receiveShadow = true;
    this.scene.add(ground);
  };

  FishWashApp.prototype.initComponents = function () {
    this.fishWashModel = new FishWashModel();
    this.scene.add(this.fishWashModel.getMesh());

    this.waterSurface = new WaterSurface();
    this.scene.add(this.waterSurface.getMesh());

    this.sprayParticles = new SprayParticles();
    this.scene.add(this.sprayParticles.getMesh());

    this.dashboard = new Dashboard();
    this.dashboard.initDeviceSelector();
    this.dashboard.initCharts();

    this.wsClient = new WebSocketClient(this.dashboard);
    this.wsClient.connect();
  };

  FishWashApp.prototype.bindControls = function () {
    var self = this;

    var deviceSelector = document.getElementById('deviceSelector');
    if (deviceSelector) {
      deviceSelector.addEventListener('change', function (e) {
        var deviceId = e.target.value;
        if (deviceId) {
          self.selectedDeviceId = deviceId;
          self.onDeviceSelect(deviceId);
        }
      });
    }

    var modeSelector = document.getElementById('modeOrder');
    var simulateBtn = document.getElementById('simulateBtn');
    var frictionSlider = document.getElementById('frictionSlider');
    var frictionSliderValue = document.getElementById('frictionSliderValue');

    if (simulateBtn) {
      simulateBtn.addEventListener('click', function () {
        var modeOrder = modeSelector ? parseInt(modeSelector.value) : 2;
        self.onSimulationTrigger(modeOrder);
      });
    }

    if (frictionSlider) {
      frictionSlider.addEventListener('input', function (e) {
        var freq = parseFloat(e.target.value);
        self.onFrequencyChange(freq);
        if (frictionSliderValue) {
          frictionSliderValue.textContent = freq.toFixed(0) + ' Hz';
        }
      });
    }
  };

  FishWashApp.prototype.onDeviceSelect = function (deviceId) {
    var self = this;

    fetch(API_BASE + '/api/sensor-data/' + deviceId + '/latest')
      .then(function (res) { return res.json(); })
      .then(function (data) {
        self.dashboard.updateSensorDisplay(data);
        if (data.frictionFreq) {
          self.onFrequencyChange(data.frictionFreq);
        }
      })
      .catch(function () {
        console.log('Sensor data not available');
      });

    fetch(API_BASE + '/api/simulation/vibration-modes/' + deviceId)
      .then(function (res) { return res.json(); })
      .then(function (data) {
        if (data && data.data) {
          self.dashboard.updateVibrationChart(data.data);
        } else if (Array.isArray(data)) {
          self.dashboard.updateVibrationChart(data);
        }
      })
      .catch(function () {
        console.log('Vibration modes not available');
      });
  };

  FishWashApp.prototype.onSimulationTrigger = function (modeOrder) {
    this.currentModeOrder = modeOrder;
    this.fishWashModel.setModeOrder(modeOrder);
    this.fishWashModel.startVibrationAnimation();
    this.sprayParticles.setIntensity(0.8);

    if (!this.selectedDeviceId) return;

    var self = this;

    fetch(API_BASE + '/api/simulation/modal-analysis/' + this.selectedDeviceId + '?maxModeOrder=' + modeOrder, {
      method: 'POST'
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        if (data && data.data) {
          self.dashboard.updateVibrationChart(data.data);
        }
      })
      .catch(function () {
        console.log('Modal analysis API not available');
      });

    var frictionSlider = document.getElementById('frictionSlider');
    var freq = frictionSlider ? parseFloat(frictionSlider.value) : this.currentFrequency;

    fetch(API_BASE + '/api/simulation/spray-analysis/' + this.selectedDeviceId, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ frictionFreq: freq, measuredSprayHeight: null })
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        var result = data && data.data ? data.data : data;
        if (result && result.predictedSprayHeight) {
          self.currentSprayHeight = result.predictedSprayHeight / 100.0;
        }
      })
      .catch(function () {
        console.log('Spray analysis API not available');
      });
  };

  FishWashApp.prototype.onFrequencyChange = function (freq) {
    this.currentFrequency = freq / 50.0;
  };

  FishWashApp.prototype.animate = function () {
    var self = this;
    this.animating = true;

    function loop() {
      if (!self.animating) return;
      self.animationId = requestAnimationFrame(loop);

      var dt = self.clock.getDelta();
      var elapsed = self.clock.getElapsedTime();

      self.waterSurface.updateWave(
        self.currentModeOrder,
        self.currentFrequency,
        self.currentAmplitude * 10,
        elapsed
      );

      self.sprayParticles.emitSpray(
        self.currentModeOrder,
        self.currentFrequency,
        self.currentSprayHeight,
        0.2
      );
      self.sprayParticles.update(dt);

      self.controls.update();
      self.renderer.render(self.scene, self.camera);
    }

    loop();
  };

  FishWashApp.prototype.onWindowResize = function () {
    var container = document.getElementById('viewport3d');
    if (!container) return;
    var w = container.clientWidth;
    var h = container.clientHeight;
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(w, h);
  };

  FishWashApp.prototype.init = function () {
    this.initScene();
    this.initComponents();
    this.bindControls();

    var self = this;
    window.addEventListener('resize', function () {
      self.onWindowResize();
    });

    this.animate();
  };

  FishWashApp.prototype.destroy = function () {
    this.animating = false;
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
    this.fishWashModel.stopVibrationAnimation();
    if (this.wsClient) {
      this.wsClient.disconnect();
    }
    window.removeEventListener('resize', this.onWindowResize);
    this.renderer.dispose();
  };

  return FishWashApp;
})();

document.addEventListener('DOMContentLoaded', function () {
  var app = new FishWashApp();
  app.init();
  window.fishWashApp = app;
});
