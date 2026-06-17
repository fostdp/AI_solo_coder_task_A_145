var FishWashApp = (function () {
  function FishWashApp() {
    this.basin3d = null;
    this.panel = null;
  }

  FishWashApp.prototype.init = function () {
    this.basin3d = new FishBasin3D();
    this.panel = new VibrationPanel();

    var container = document.getElementById('viewport3d');
    this.basin3d.init(container);

    this.panel.onSimulate = function (modeOrder) {
      this.basin3d.startSimulation(modeOrder);
    }.bind(this);

    this.panel.onFrequencyChange = function (freq) {
      this.basin3d.setFrequency(freq);
    }.bind(this);

    this.panel.onDeviceChange = function (deviceId) {
      this.basin3d.stopSimulation();
    }.bind(this);

    this.panel.init();

    var self = this;
    window.addEventListener('resize', function () {
      var c = document.getElementById('viewport3d');
      if (c) {
        self.basin3d.onWindowResize(c);
      }
    });
  };

  FishWashApp.prototype.destroy = function () {
    if (this.basin3d) {
      this.basin3d.destroy();
    }
    if (this.panel) {
      this.panel.destroy();
    }
  };

  return FishWashApp;
})();

document.addEventListener('DOMContentLoaded', function () {
  var app = new FishWashApp();
  app.init();
  window.fishWashApp = app;
});
