var WaterSurface = (function () {
  function WaterSurface() {
    this.mesh = null;
    this.originalPositions = null;
    this.radius = 0.18;
    this.waterLevel = 0.08;
    this.createWaterMesh();
  }

  WaterSurface.prototype.createWaterMesh = function () {
    var geometry = new THREE.PlaneGeometry(this.radius * 2, this.radius * 2, 64, 64);

    var material = new THREE.MeshPhongMaterial({
      color: 0x1E90FF,
      transparent: true,
      opacity: 0.7,
      shininess: 100,
      specular: 0xFFFFFF,
      side: THREE.DoubleSide
    });

    this.mesh = new THREE.Mesh(geometry, material);
    this.mesh.rotation.x = -Math.PI / 2;
    this.mesh.position.y = this.waterLevel;
    this.mesh.receiveShadow = true;

    this.originalPositions = new Float32Array(geometry.attributes.position.array);
    this.cropToCircle();
  };

  WaterSurface.prototype.cropToCircle = function () {
    var positions = this.mesh.geometry.attributes.position.array;
    for (var i = 0; i < positions.length; i += 3) {
      var x = positions[i];
      var z = positions[i + 2];
      var r = Math.sqrt(x * x + z * z);
      if (r > this.radius) {
        var scale = this.radius / r;
        positions[i] = x * scale;
        positions[i + 2] = z * scale;
      }
    }
    this.mesh.geometry.attributes.position.needsUpdate = true;
    this.originalPositions = new Float32Array(positions);
  };

  WaterSurface.prototype.besselJ = function (n, x) {
    var sum = 0;
    var nFact = 1;
    for (var i = 1; i <= n; i++) {
      nFact *= i;
    }

    for (var k = 0; k < 10; k++) {
      var kFact = 1;
      for (var j = 1; j <= k; j++) {
        kFact *= j;
      }
      var nkFact = 1;
      for (var j = 1; j <= n + k; j++) {
        nkFact *= j;
      }
      var sign = (k % 2 === 0) ? 1 : -1;
      var term = sign / (kFact * nkFact) * Math.pow(x / 2, n + 2 * k);
      sum += term;
    }
    return sum;
  };

  WaterSurface.prototype.updateWave = function (modeOrder, frequency, amplitude, time) {
    if (!this.mesh) return;

    var positions = this.mesh.geometry.attributes.position.array;
    var origPos = this.originalPositions;
    var R = this.radius;
    var k = modeOrder * Math.PI / R;
    var maxDisp = 0;

    var displacements = [];

    for (var i = 0; i < positions.length; i += 3) {
      var ox = origPos[i];
      var oz = origPos[i + 2];

      var r = Math.sqrt(ox * ox + oz * oz);
      if (r > this.radius) {
        positions[i + 1] = 0;
        displacements.push(0);
        continue;
      }

      var theta = Math.atan2(oz, ox);
      var besselVal = this.besselJ(modeOrder, k * r);
      var displacement = amplitude * besselVal * Math.cos(modeOrder * theta) * Math.cos(2 * Math.PI * frequency * time);

      positions[i + 1] = displacement;
      displacements.push(displacement);

      if (Math.abs(displacement) > maxDisp) {
        maxDisp = Math.abs(displacement);
      }
    }

    this.mesh.geometry.attributes.position.needsUpdate = true;
    this.mesh.geometry.computeVertexNormals();

    if (maxDisp > 0.0001) {
      this.applyColorGradient(displacements, maxDisp);
    }
  };

  WaterSurface.prototype.applyColorGradient = function (displacements, maxDisp) {
    var geometry = this.mesh.geometry;
    var count = displacements.length;

    if (!geometry.attributes.color) {
      var colors = new Float32Array(count * 3);
      geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));
    }

    var colorAttr = geometry.attributes.color.array;

    for (var i = 0; i < count; i++) {
      var norm = displacements[i] / maxDisp;
      var r, g, b;

      if (norm > 0) {
        r = 0.12 + 0.88 * norm;
        g = 0.56 + 0.44 * norm;
        b = 1.0;
      } else {
        var t = Math.abs(norm);
        r = 0.12 * (1 - t);
        g = 0.56 * (1 - t) + 0.1 * t;
        b = 1.0 * (1 - t) + 0.4 * t;
      }

      colorAttr[i * 3] = r;
      colorAttr[i * 3 + 1] = g;
      colorAttr[i * 3 + 2] = b;
    }

    geometry.attributes.color.needsUpdate = true;
    this.mesh.material.vertexColors = true;
    this.mesh.material.needsUpdate = true;
  };

  WaterSurface.prototype.setColorByHeight = function (displacement, maxDisp) {
    if (maxDisp < 0.0001) maxDisp = 0.0001;
    var norm = displacement / maxDisp;
    var r, g, b;

    if (norm > 0) {
      r = 0.12 + 0.88 * norm;
      g = 0.56 + 0.44 * norm;
      b = 1.0;
    } else {
      var t = Math.abs(norm);
      r = 0.12 * (1 - t);
      g = 0.56 * (1 - t) + 0.1 * t;
      b = 1.0 * (1 - t) + 0.4 * t;
    }

    return new THREE.Color(r, g, b);
  };

  WaterSurface.prototype.getMesh = function () {
    return this.mesh;
  };

  return WaterSurface;
})();
