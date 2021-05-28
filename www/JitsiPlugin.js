var exec = require('cordova/exec');

exports.join = function(serverUrl, room, audioOnly, success, token, error) {
    exec(success, error, "JitsiPlugin", "join", [serverUrl, room, !!audioOnly, token]);
};

exports.destroy = function(success, error) {
    exec(success, error, "JitsiPlugin", "destroy", []);
};
