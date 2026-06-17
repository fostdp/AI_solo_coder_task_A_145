var WaterSurface = (function () {
  var DEFAULT_RADIUS = 0.18;
  var DEFAULT_SEGMENTS = 64;
  var WATER_LEVEL = 0.08;

  function WaterSurface() {
    this.radius = DEFAULT_RADIUS;
    this.segments = DEFAULT_SEGMENTS;
    this.modeOrder = 2;
    this.frequency = 3.0;
    this.amplitude = 0.02;
    this.waterLevel = WATER_LEVEL;
    this.time = 0.0;

    this.uniforms = {
      uTime: { value: 0.0 },
      uModeOrder: { value: 2.0 },
      uFrequency: { value: 3.0 },
      uAmplitude: { value: 0.02 },
      uRadius: { value: DEFAULT_RADIUS },
      uColorDeep: { value: new THREE.Color(0x10406B) },
      uColorMid: { value: new THREE.Color(0x4682B4) },
      uColorShallow: { value: new THREE.Color(0x87CEEB) },
      uColorCrest: { value: new THREE.Color(0xFFFFFF) }
    };

    var vertexShader = [
      'uniform float uTime;',
      'uniform float uModeOrder;',
      'uniform float uFrequency;',
      'uniform float uAmplitude;',
      'uniform float uRadius;',
      '',
      'varying float vDisplacement;',
      'varying vec3 vNormal;',
      'varying vec3 vViewDir;',
      '',
      'float besselJ(float n, float x) {',
      '  if (x < 0.001) {',
      '    float val = 1.0;',
      '    for (int k = 1; k <= 3; k++) {',
      '      float fk = float(k);',
      '      float sign = mod(fk, 2.0) == 0.0 ? 1.0 : -1.0;',
      '      float term = sign * pow(x / 2.0, n + 2.0 * fk);',
      '      float denom = 1.0;',
      '      for (int j = 1; j <= 8; j++) {',
      '        float fj = float(j);',
      '        if (fj <= fk) denom *= fj;',
      '        if (fj <= n + fk) denom *= (n + fj);',
      '      }',
      '      val += term / denom;',
      '    }',
      '    return val;',
      '  }',
      '  float ax = abs(x);',
      '  float z = 2.0 / ax;',
      '  float y = z * z;',
      '  float xx = x - (n + 0.5) * 3.141592653589793 * 0.5;',
      '  float p = 1.0;',
      '  float q = y * 0.5;',
      '  for (int i = 1; i <= 6; i++) {',
      '    float fi = float(i);',
      '    p += pow(-1.0, fi) * pow(y / 4.0, fi * 2.0) * (1.0 - n * n) * (9.0 - n * n);',
      '    q += pow(-1.0, fi) * pow(y / 4.0, fi * 2.0 + 1.0) * (1.0 - n * n);',
      '  }',
      '  return sqrt(2.0 / (3.141592653589793 * ax)) * (p * cos(xx) - q * sin(xx));',
      '}',
      '',
      'void main() {',
      '  vec3 pos = position;',
      '',
      '  float r = sqrt(pos.x * pos.x + pos.z * pos.z);',
      '  float theta = atan(pos.z, pos.x);',
      '',
      '  float k = uModeOrder * 3.141592653589793 / uRadius;',
      '  float kr = k * r;',
      '  float jn = besselJ(uModeOrder, kr);',
      '  float cosNTheta = cos(uModeOrder * theta);',
      '  float omega = 2.0 * 3.141592653589793 * uFrequency;',
      '  float cosOmegaT = cos(omega * uTime);',
      '',
      '  float displacement = uAmplitude * jn * cosNTheta * cosOmegaT;',
      '',
      '  float edgeFactor = smoothstep(uRadius, uRadius * 0.85, r);',
      '  displacement *= edgeFactor;',
      '',
      '  pos.y += displacement;',
      '',
      '  vDisplacement = displacement / max(uAmplitude, 0.001);',
      '',
      '  float dJdr = uModeOrder * besselJ(uModeOrder - 1.0, kr) / uRadius - uModeOrder * jn / r;',
      '  if (r < 0.001) dJdr = 0.0;',
      '  float dx = uAmplitude * dJdr * cosNTheta * cosOmegaT * (pos.x / max(r, 0.001));',
      '  float dz = uAmplitude * dJdr * cosNTheta * cosOmegaT * (pos.z / max(r, 0.001));',
      '  vec3 normal = normalize(vec3(-dx, 1.0, -dz));',
      '  vNormal = normalMatrix * normal;',
      '',
      '  vec4 mvPos = modelViewMatrix * vec4(pos, 1.0);',
      '  vViewDir = normalize(-mvPos.xyz);',
      '',
      '  gl_Position = projectionMatrix * mvPos;',
      '}'
    ].join('\n');

    var fragmentShader = [
      'uniform vec3 uColorDeep;',
      'uniform vec3 uColorMid;',
      'uniform vec3 uColorShallow;',
      'uniform vec3 uColorCrest;',
      '',
      'varying float vDisplacement;',
      'varying vec3 vNormal;',
      'varying vec3 vViewDir;',
      '',
      'void main() {',
      '  float t = clamp(vDisplacement * 0.5 + 0.5, 0.0, 1.0);',
      '',
      '  vec3 color;',
      '  if (t < 0.3) {',
      '    float f = t / 0.3;',
      '    color = mix(uColorDeep, uColorMid, f);',
      '  } else if (t < 0.7) {',
      '    float f = (t - 0.3) / 0.4;',
      '    color = mix(uColorMid, uColorShallow, f);',
      '  } else {',
      '    float f = (t - 0.7) / 0.3;',
      '    color = mix(uColorShallow, uColorCrest, f);',
      '  }',
      '',
      '  vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));',
      '  float diff = max(dot(vNormal, lightDir), 0.0);',
      '',
      '  vec3 halfDir = normalize(lightDir + vViewDir);',
      '  float spec = pow(max(dot(vNormal, halfDir), 0.0), 64.0);',
      '',
      '  vec3 ambient = color * 0.4;',
      '  vec3 diffuse = color * diff * 0.6;',
      '  vec3 specular = vec3(0.9, 0.95, 1.0) * spec * (0.3 + t * 0.5);',
      '',
      '  vec3 finalColor = ambient + diffuse + specular;',
      '  float alpha = 0.75 + t * 0.2;',
      '',
      '  gl_FragColor = vec4(finalColor, alpha);',
      '}'
    ].join('\n');

    this.material = new THREE.ShaderMaterial({
      uniforms: this.uniforms,
      vertexShader: vertexShader,
      fragmentShader: fragmentShader,
      transparent: true,
      side: THREE.DoubleSide,
      depthWrite: false
    });

    this.geometry = new THREE.CircleGeometry(this.radius, this.segments);
    this.geometry.rotateX(-Math.PI / 2);

    this.mesh = new THREE.Mesh(this.geometry, this.material);
    this.mesh.position.y = this.waterLevel;
    this.mesh.receiveShadow = true;
  }

  WaterSurface.prototype.getMesh = function () {
    return this.mesh;
  };

  WaterSurface.prototype.setModeOrder = function (n) {
    this.modeOrder = n;
    this.uniforms.uModeOrder.value = n;
  };

  WaterSurface.prototype.setFrequency = function (f) {
    this.frequency = f;
    this.uniforms.uFrequency.value = f;
  };

  WaterSurface.prototype.setAmplitude = function (a) {
    this.amplitude = a;
    this.uniforms.uAmplitude.value = a;
  };

  WaterSurface.prototype.setRadius = function (r) {
    this.radius = r;
    this.uniforms.uRadius.value = r;
  };

  WaterSurface.prototype.setWaterLevel = function (y) {
    this.waterLevel = y;
    this.mesh.position.y = y;
  };

  WaterSurface.prototype.updateWave = function (modeOrder, frequency, amplitude, time) {
    if (modeOrder !== this.modeOrder) {
      this.setModeOrder(modeOrder);
    }
    if (frequency !== this.frequency) {
      this.setFrequency(frequency);
    }
    if (amplitude !== this.amplitude) {
      this.setAmplitude(amplitude);
    }
    this.time = time;
    this.uniforms.uTime.value = time;
  };

  WaterSurface.prototype.dispose = function () {
    this.geometry.dispose();
    this.material.dispose();
  };

  return WaterSurface;
})();
