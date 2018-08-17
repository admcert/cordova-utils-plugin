var exec = require('cordova/exec');

var utils_lib = {
  exitApp: function() {
    exec(function(){}, function(){}, 'utilsLib', 'exitApp', []);
  },
  isEmulator: function(successFn,failureFn) {
    exec(successFn,failureFn, 'utilsLib', 'isEmulator', []);
  },
  isDeviceRooted : function(successFn,failureFn) {
	  exec(successFn,failureFn, 'utilsLib', 'isDeviceRooted', []);
  },
  isXposed: function(successFn,failureFn){
	  exec(successFn,failureFn, 'utilsLib', 'isXposed', []);
  }
};

module.exports = utils_lib