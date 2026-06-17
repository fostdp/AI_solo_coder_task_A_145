var FishBasin3D = (function () {
  function FishBasin3D() {
    this.scene = null;
    this.camera = null;
    this.renderer = null;
    this.controls = null;
    this.fishWashModel = null;
    this.waterSurface = null;
    this.sprayParticles = null;
    this.clock = new THREE.Clock();
    this.currentModeOrder = 2;
    this.currentFrequency = 3.0;
    this.currentAmplitude = 0.02;
    this.currentSprayHeight = 0.15;
    this.animating = false;
    this.animationId = null;
  }

  FishBasin3D.prototype.initScene = function (container) {
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
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
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

  FishBasin3D.prototype.initComponents = function () {
    this.fishWashModel = new FishWashModel();
    this.scene.add(this.fishWashModel.getMesh());

    this.waterSurface = new WaterSurface();
    this.scene.add(this.waterSurface.getMesh());

    this.sprayParticles = new SprayParticles();
    this.scene.add(this.sprayParticles.getMesh());
  };

  FishBasin3D.prototype.startSimulation = function (modeOrder) {
    this.currentModeOrder = modeOrder;
    this.fishWashModel.setModeOrder(modeOrder);
    this.fishWashModel.startVibrationAnimation();
    this.sprayParticles.setIntensity(0.8);
  };

  FishBasin3D.prototype.stopSimulation = function () {
    this.fishWashModel.stopVibrationAnimation();
    this.sprayParticles.setIntensity(0.0);
  };

  FishBasin3D.prototype.setFrequency = function (freq) {
    this.currentFrequency = freq / 50.0;
  };

  FishBasin3D.prototype.setSprayHeight = function (height) {
    this.currentSprayHeight = height;
  };

  FishBasin3D.prototype.setAmplitude = function (amp) {
    this.currentAmplitude = amp;
  };

  FishBasin3D.prototype.animate = function () {
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
        self.currentAmplitude,
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

  FishBasin3D.prototype.onWindowResize = function (container) {
    if (!container) return;
    var w = container.clientWidth;
    var h = container.clientHeight;
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(w, h);
  };

  FishBasin3D.prototype.init = function (container) {
    this.initScene(container);
    this.initComponents();
    this.animate();
  };

  FishBasin3D.prototype.destroy = function () {
    this.animating = false;
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
    this.fishWashModel.stopVibrationAnimation();
    this.fishWashModel = null;
    if (this.waterSurface) {
      this.waterSurface.dispose();
      this.waterSurface = null;
    }
    if (this.sprayParticles) {
      this.sprayParticles.dispose();
      this.sprayParticles = null;
    }
    this.renderer.dispose();
  };

  return FishBasin3D;
})();
