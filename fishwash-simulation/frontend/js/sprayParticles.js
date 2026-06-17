var SprayParticles = (function () {
  var GRAVITY = -9.81;
  var MAX_PARTICLES = 5000;
  var SECONDARY_BREAKUP_THRESHOLD = 3.5;
  var SPLIT_CHILD_COUNT = 4;
  var SPLIT_SPEED_THRESHOLD = 2.5;
  var SURFACE_TENSION = 0.073;
  var WATER_DENSITY = 1000.0;

  function SprayParticles() {
    this.maxParticles = MAX_PARTICLES;
    this.particleCount = 0;
    this.positions = new Float32Array(MAX_PARTICLES * 3);
    this.velocities = new Float32Array(MAX_PARTICLES * 3);
    this.lives = new Float32Array(MAX_PARTICLES);
    this.sizes = new Float32Array(MAX_PARTICLES);
    this.masses = new Float32Array(MAX_PARTICLES);
    this.active = new Uint8Array(MAX_PARTICLES);
    this.intensity = 0.0;
    this.emissionTimer = 0.0;
    this.waterLevel = 0.08;

    this.geometry = new THREE.BufferGeometry();
    this.geometry.setAttribute('position', new THREE.BufferAttribute(this.positions, 3));
    this.geometry.setAttribute('aSize', new THREE.BufferAttribute(this.sizes, 1));
    this.geometry.setDrawRange(0, 0);

    this.material = new THREE.ShaderMaterial({
      uniforms: {
        uColor: { value: new THREE.Color(0x87CEEB) },
        uPixelRatio: { value: window.devicePixelRatio || 1 }
      },
      vertexShader: [
        'attribute float aSize;',
        'varying float vAlpha;',
        'uniform float uPixelRatio;',
        'void main() {',
        '  vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);',
        '  float dist = -mvPosition.z;',
        '  gl_PointSize = aSize * uPixelRatio * (300.0 / max(dist, 1.0));',
        '  vAlpha = clamp(1.0 - dist / 2.0, 0.2, 1.0);',
        '  gl_Position = projectionMatrix * mvPosition;',
        '}'
      ].join('\n'),
      fragmentShader: [
        'uniform vec3 uColor;',
        'varying float vAlpha;',
        'void main() {',
        '  vec2 uv = gl_PointCoord - 0.5;',
        '  float d = length(uv);',
        '  if (d > 0.5) discard;',
        '  float alpha = (1.0 - d * 2.0) * vAlpha;',
        '  gl_FragColor = vec4(uColor + 0.3 * (1.0 - d), alpha);',
        '}'
      ].join('\n'),
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });

    this.points = new THREE.Points(this.geometry, this.material);
    this._firstFreeIndex = 0;
  }

  SprayParticles.prototype.getMesh = function () {
    return this.points;
  };

  SprayParticles.prototype.setIntensity = function (level) {
    this.intensity = Math.max(0, Math.min(1, level));
  };

  SprayParticles.prototype.setWaterLevel = function (y) {
    this.waterLevel = y;
  };

  SprayParticles.prototype._findFreeSlot = function () {
    for (var i = this._firstFreeIndex; i < this.maxParticles; i++) {
      if (!this.active[i]) {
        this._firstFreeIndex = i + 1;
        return i;
      }
    }
    for (var j = 0; j < this.maxParticles; j++) {
      if (!this.active[j]) {
        this._firstFreeIndex = j + 1;
        return j;
      }
    }
    return -1;
  };

  SprayParticles.prototype._spawnParticle = function (x, y, z, vx, vy, vz, size, life, mass) {
    var idx = this._findFreeSlot();
    if (idx < 0) return -1;

    this.positions[idx * 3] = x;
    this.positions[idx * 3 + 1] = y;
    this.positions[idx * 3 + 2] = z;
    this.velocities[idx * 3] = vx;
    this.velocities[idx * 3 + 1] = vy;
    this.velocities[idx * 3 + 2] = vz;
    this.sizes[idx] = size;
    this.lives[idx] = life;
    this.masses[idx] = mass;
    this.active[idx] = 1;
    this.particleCount++;
    return idx;
  };

  SprayParticles.prototype._killParticle = function (idx) {
    if (this.active[idx]) {
      this.active[idx] = 0;
      this.particleCount--;
      this.sizes[idx] = 0;
      this.lives[idx] = 0;
      if (idx < this._firstFreeIndex) {
        this._firstFreeIndex = idx;
      }
    }
  };

  SprayParticles.prototype._splitParticle = function (idx) {
    if (!this.active[idx]) return;

    var px = this.positions[idx * 3];
    var py = this.positions[idx * 3 + 1];
    var pz = this.positions[idx * 3 + 2];
    var vx = this.velocities[idx * 3];
    var vy = this.velocities[idx * 3 + 1];
    var vz = this.velocities[idx * 3 + 2];
    var size = this.sizes[idx];
    var life = this.lives[idx];
    var mass = this.masses[idx];

    var speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
    if (speed < SPLIT_SPEED_THRESHOLD) return;

    var diameter = size * 0.001;
    var weberNumber = WATER_DENSITY * diameter * speed * speed / SURFACE_TENSION;
    if (weberNumber < SECONDARY_BREAKUP_THRESHOLD) return;

    var childCount = Math.min(SPLIT_CHILD_COUNT, Math.floor(Math.sqrt(weberNumber / SECONDARY_BREAKUP_THRESHOLD)));
    if (childCount < 2) return;

    var childMass = mass / childCount;
    var childSize = size / Math.cbrt(childCount);
    var childLife = life * 0.7;

    for (var i = 0; i < childCount; i++) {
      var spread = speed * 0.3;
      var theta = Math.random() * Math.PI * 2;
      var phi = Math.random() * Math.PI * 0.5;
      var rvx = vx + spread * Math.sin(phi) * Math.cos(theta);
      var rvy = vy + spread * Math.cos(phi) * 0.5;
      var rvz = vz + spread * Math.sin(phi) * Math.sin(theta);

      var jitterX = (Math.random() - 0.5) * size * 0.002;
      var jitterY = (Math.random() - 0.5) * size * 0.002;
      var jitterZ = (Math.random() - 0.5) * size * 0.002;

      this._spawnParticle(
        px + jitterX, py + jitterY, pz + jitterZ,
        rvx, rvy, rvz,
        childSize, childLife, childMass
      );
    }

    this._killParticle(idx);
  };

  SprayParticles.prototype.emitSpray = function (modeOrder, frequency, sprayHeight, basinRadius) {
    if (this.intensity <= 0.01) return;

    var antinodeCount = modeOrder * 2;
    var v0 = Math.sqrt(2 * -GRAVITY * Math.max(0.001, sprayHeight));

    var particlesPerAntinode = Math.floor(2 * this.intensity);
    if (particlesPerAntinode < 1) particlesPerAntinode = 1;

    for (var k = 0; k < antinodeCount; k++) {
      var angle = k * Math.PI / modeOrder;
      var antinodeX = basinRadius * Math.cos(angle);
      var antinodeZ = basinRadius * Math.sin(angle);

      for (var p = 0; p < particlesPerAntinode; p++) {
        var spread = basinRadius * 0.1;
        var sx = antinodeX + (Math.random() - 0.5) * spread;
        var sz = antinodeZ + (Math.random() - 0.5) * spread;
        var sy = this.waterLevel + Math.random() * 0.01;

        var spreadV = v0 * 0.25;
        var vx = (Math.random() - 0.5) * spreadV + Math.cos(angle) * v0 * 0.1;
        var vz = (Math.random() - 0.5) * spreadV + Math.sin(angle) * v0 * 0.1;
        var vy = v0 * (0.7 + Math.random() * 0.3);

        var size = 8 + Math.random() * 10;
        var life = 0.6 + Math.random() * 0.4;
        var mass = size * size * size * 1e-9;

        this._spawnParticle(sx, sy, sz, vx, vy, vz, size, life, mass);
      }
    }
  };

  SprayParticles.prototype.update = function (dt) {
    if (dt <= 0) return;
    if (dt > 0.05) dt = 0.05;

    var splitCheckInterval = 3;
    var frameCounter = (this.frameCounter || 0) + 1;
    this.frameCounter = frameCounter;

    for (var i = 0; i < this.maxParticles; i++) {
      if (!this.active[i]) continue;

      var idx3 = i * 3;

      this.velocities[idx3 + 1] += GRAVITY * dt;

      this.positions[idx3] += this.velocities[idx3] * dt;
      this.positions[idx3 + 1] += this.velocities[idx3 + 1] * dt;
      this.positions[idx3 + 2] += this.velocities[idx3 + 2] * dt;

      this.lives[i] -= dt;

      var vy = this.velocities[idx3 + 1];
      var speed = Math.sqrt(
        this.velocities[idx3] * this.velocities[idx3] +
        vy * vy +
        this.velocities[idx3 + 2] * this.velocities[idx3 + 2]
      );

      if (this.positions[idx3 + 1] <= this.waterLevel && vy < 0) {
        this.positions[idx3 + 1] = this.waterLevel;
        this.velocities[idx3 + 1] = -vy * 0.3;
        this.lives[i] -= 0.1;
      }

      if (this.lives[i] <= 0 || this.positions[idx3 + 1] < -0.1) {
        this._killParticle(i);
        continue;
      }

      if (frameCounter % splitCheckInterval === 0 && speed > SPLIT_SPEED_THRESHOLD) {
        var size = this.sizes[i];
        var diameter = size * 0.001;
        if (diameter > 0) {
          var weberNumber = WATER_DENSITY * diameter * speed * speed / SURFACE_TENSION;
          if (weberNumber > SECONDARY_BREAKUP_THRESHOLD && this.masses[i] > 1e-12) {
            this._splitParticle(i);
          }
        }
      }
    }

    var drawCount = Math.min(this.particleCount, this.maxParticles);
    this.geometry.setDrawRange(0, drawCount);
    this.geometry.attributes.position.needsUpdate = true;
    this.geometry.attributes.aSize.needsUpdate = true;
  };

  SprayParticles.prototype.dispose = function () {
    this.geometry.dispose();
    this.material.dispose();
  };

  return SprayParticles;
})();
