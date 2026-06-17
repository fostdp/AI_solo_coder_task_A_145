var SprayParticles = (function () {
  function SprayParticles() {
    this.maxParticles = 2000;
    this.particleSystem = null;
    this.positions = null;
    this.velocities = null;
    this.lives = null;
    this.sizes = null;
    this.intensity = 0.5;
    this.gravity = 9.81;
    this.waterLevel = 0.08;
    this.activeCount = 0;
    this.emitIndex = 0;
    this.createParticleSystem();
  }

  SprayParticles.prototype.createParticleSystem = function () {
    var geometry = new THREE.BufferGeometry();

    this.positions = new Float32Array(this.maxParticles * 3);
    this.velocities = new Float32Array(this.maxParticles * 3);
    this.lives = new Float32Array(this.maxParticles);
    this.sizes = new Float32Array(this.maxParticles);

    for (var i = 0; i < this.maxParticles; i++) {
      this.positions[i * 3] = 0;
      this.positions[i * 3 + 1] = -10;
      this.positions[i * 3 + 2] = 0;
      this.velocities[i * 3] = 0;
      this.velocities[i * 3 + 1] = 0;
      this.velocities[i * 3 + 2] = 0;
      this.lives[i] = 0;
      this.sizes[i] = 0;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(this.positions, 3));
    geometry.setAttribute('size', new THREE.BufferAttribute(this.sizes, 1));

    var material = new THREE.PointsMaterial({
      color: 0x87CEEB,
      size: 0.003,
      transparent: true,
      opacity: 0.8,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      sizeAttenuation: true
    });

    this.particleSystem = new THREE.Points(geometry, material);
  };

  SprayParticles.prototype.emitSpray = function (modeOrder, frequency, sprayHeight, basinRadius) {
    var numAntinodes = 2 * modeOrder;
    var particlesPerAntinode = Math.floor(this.intensity * 5) + 1;
    var v0 = Math.sqrt(2 * this.gravity * sprayHeight);
    var spread = 0.02;

    for (var k = 0; k < numAntinodes; k++) {
      var angle = k * Math.PI / modeOrder;
      var ax = basinRadius * Math.cos(angle);
      var az = basinRadius * Math.sin(angle);

      for (var p = 0; p < particlesPerAntinode; p++) {
        var idx = this.emitIndex % this.maxParticles;
        this.emitIndex++;

        this.positions[idx * 3] = ax + (Math.random() - 0.5) * 0.02;
        this.positions[idx * 3 + 1] = this.waterLevel;
        this.positions[idx * 3 + 2] = az + (Math.random() - 0.5) * 0.02;

        this.velocities[idx * 3] = spread * (Math.random() - 0.5);
        this.velocities[idx * 3 + 1] = v0 * (0.8 + Math.random() * 0.4);
        this.velocities[idx * 3 + 2] = spread * (Math.random() - 0.5);

        this.lives[idx] = 0.5 + Math.random() * 1.0;
        this.sizes[idx] = 0.002 + Math.random() * 0.003;
      }
    }

    this.particleSystem.geometry.attributes.position.needsUpdate = true;
    this.particleSystem.geometry.attributes.size.needsUpdate = true;
  };

  SprayParticles.prototype.update = function (dt) {
    var aliveCount = 0;

    for (var i = 0; i < this.maxParticles; i++) {
      if (this.lives[i] <= 0) continue;

      this.velocities[i * 3 + 1] -= this.gravity * dt;

      this.positions[i * 3] += this.velocities[i * 3] * dt;
      this.positions[i * 3 + 1] += this.velocities[i * 3 + 1] * dt;
      this.positions[i * 3 + 2] += this.velocities[i * 3 + 2] * dt;

      this.lives[i] -= dt;

      if (this.lives[i] <= 0 || this.positions[i * 3 + 1] < this.waterLevel * 0.5) {
        this.positions[i * 3 + 1] = -10;
        this.lives[i] = 0;
        this.sizes[i] = 0;
      } else {
        aliveCount++;
        var lifeRatio = this.lives[i] / 1.5;
        this.sizes[i] = (0.002 + Math.random() * 0.002) * lifeRatio;
      }
    }

    this.activeCount = aliveCount;

    this.particleSystem.geometry.attributes.position.needsUpdate = true;
    this.particleSystem.geometry.attributes.size.needsUpdate = true;

    var avgOpacity = Math.min(0.8, aliveCount / 200);
    this.particleSystem.material.opacity = avgOpacity;
  };

  SprayParticles.prototype.setIntensity = function (level) {
    this.intensity = Math.max(0, Math.min(1, level));
  };

  SprayParticles.prototype.getMesh = function () {
    return this.particleSystem;
  };

  return SprayParticles;
})();
