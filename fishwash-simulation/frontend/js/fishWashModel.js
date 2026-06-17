var FishWashModel = (function () {
  function FishWashModel() {
    this.group = new THREE.Group();
    this.modeOrder = 2;
    this.amplitude = 0.005;
    this.originalPositions = null;
    this.basinMesh = null;
    this.animating = false;
    this.animationId = null;
    this.time = 0;

    this.createBasin();
    this.createHandles();
    this.createFishRelief();
    this.createRimDecoration();
  }

  FishWashModel.prototype.createBasin = function () {
    var points = [];
    points.push(new THREE.Vector2(0, 0));
    points.push(new THREE.Vector2(0.15, 0));
    points.push(new THREE.Vector2(0.18, 0.01));
    points.push(new THREE.Vector2(0.2, 0.04));
    points.push(new THREE.Vector2(0.2, 0.1));
    points.push(new THREE.Vector2(0.21, 0.12));
    points.push(new THREE.Vector2(0.22, 0.14));
    points.push(new THREE.Vector2(0.24, 0.15));

    var geometry = new THREE.LatheGeometry(points, 64);
    var material = new THREE.MeshPhongMaterial({
      color: 0xB87333,
      shininess: 80,
      specular: 0x666666,
      side: THREE.DoubleSide
    });

    this.basinMesh = new THREE.Mesh(geometry, material);
    this.basinMesh.castShadow = true;
    this.basinMesh.receiveShadow = true;

    this.originalPositions = new Float32Array(geometry.attributes.position.array);

    this.group.add(this.basinMesh);
  };

  FishWashModel.prototype.createHandles = function () {
    var handleMaterial = new THREE.MeshPhongMaterial({
      color: 0xB87333,
      shininess: 80,
      specular: 0x666666
    });

    var handleGeometry = new THREE.TorusGeometry(0.03, 0.008, 8, 16);

    var handle1 = new THREE.Mesh(handleGeometry, handleMaterial);
    handle1.position.set(0.25, 0.135, 0);
    handle1.rotation.y = Math.PI / 2;
    handle1.castShadow = true;

    var handle2 = new THREE.Mesh(handleGeometry, handleMaterial);
    handle2.position.set(-0.25, 0.135, 0);
    handle2.rotation.y = Math.PI / 2;
    handle2.castShadow = true;

    this.group.add(handle1);
    this.group.add(handle2);
  };

  FishWashModel.prototype.createFishRelief = function () {
    var fishMaterial = new THREE.MeshPhongMaterial({
      color: 0x8B6914,
      shininess: 60,
      specular: 0x444444,
      side: THREE.DoubleSide
    });

    var fishGeometry = new THREE.PlaneGeometry(0.06, 0.025);

    var angles = [0, Math.PI / 2, Math.PI, 3 * Math.PI / 2];
    var basinWallRadius = 0.195;
    var fishY = 0.07;

    for (var i = 0; i < 4; i++) {
      var fish = new THREE.Mesh(fishGeometry, fishMaterial);
      var angle = angles[i];
      fish.position.set(
        basinWallRadius * Math.cos(angle),
        fishY,
        basinWallRadius * Math.sin(angle)
      );
      fish.rotation.y = -angle + Math.PI / 2;
      fish.rotation.x = -0.1;
      this.group.add(fish);
    }
  };

  FishWashModel.prototype.createRimDecoration = function () {
    var rimGeometry = new THREE.TorusGeometry(0.235, 0.006, 8, 64);
    var rimMaterial = new THREE.MeshPhongMaterial({
      color: 0xA0682C,
      shininess: 90,
      specular: 0x888888
    });

    var rim = new THREE.Mesh(rimGeometry, rimMaterial);
    rim.position.y = 0.15;
    rim.rotation.x = Math.PI / 2;

    this.group.add(rim);
  };

  FishWashModel.prototype.applyVibrationMode = function (modeOrder, amplitude) {
    this.modeOrder = modeOrder;
    this.amplitude = amplitude;

    if (!this.basinMesh || !this.originalPositions) return;

    var positions = this.basinMesh.geometry.attributes.position.array;
    var origPos = this.originalPositions;

    for (var i = 0; i < positions.length; i += 3) {
      var ox = origPos[i];
      var oy = origPos[i + 1];
      var oz = origPos[i + 2];

      var r = Math.sqrt(ox * ox + oz * oz);
      if (r < 0.001) continue;

      var theta = Math.atan2(oz, ox);
      var deformation = amplitude * Math.cos(modeOrder * theta);
      var heightFactor = oy / 0.15;

      var radialDisp = deformation * heightFactor;
      positions[i] = ox + (ox / r) * radialDisp;
      positions[i + 1] = oy + deformation * 0.3 * heightFactor;
      positions[i + 2] = oz + (oz / r) * radialDisp;
    }

    this.basinMesh.geometry.attributes.position.needsUpdate = true;
    this.basinMesh.geometry.computeVertexNormals();
  };

  FishWashModel.prototype.startVibrationAnimation = function () {
    if (this.animating) return;
    this.animating = true;
    this.time = 0;
    var self = this;

    function loop() {
      if (!self.animating) return;
      self.time += 0.016;
      var dynamicAmp = self.amplitude * Math.cos(2 * Math.PI * 2.0 * self.time);
      self.applyVibrationMode(self.modeOrder, dynamicAmp);
      self.animationId = requestAnimationFrame(loop);
    }

    loop();
  };

  FishWashModel.prototype.stopVibrationAnimation = function () {
    this.animating = false;
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId);
      this.animationId = null;
    }
    this.resetPositions();
  };

  FishWashModel.prototype.resetPositions = function () {
    if (!this.basinMesh || !this.originalPositions) return;
    var positions = this.basinMesh.geometry.attributes.position.array;
    var origPos = this.originalPositions;
    for (var i = 0; i < positions.length; i++) {
      positions[i] = origPos[i];
    }
    this.basinMesh.geometry.attributes.position.needsUpdate = true;
    this.basinMesh.geometry.computeVertexNormals();
  };

  FishWashModel.prototype.setModeOrder = function (n) {
    this.modeOrder = n;
  };

  FishWashModel.prototype.setAmplitude = function (a) {
    this.amplitude = a;
  };

  FishWashModel.prototype.getMesh = function () {
    return this.group;
  };

  return FishWashModel;
})();
