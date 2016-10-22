var userService;

function init(){
	thisX.debug("xstartup", "XDefaultServices INIT");
	userService = thisX.bindService("XUserService");
	
	thisX.debug("xstartup", "XDefaultServices done");
}

function login(login, password, callback){
	userService.login(login, password, function(user){
		window.xuser = user;
		callback(user);
	}, null);
}

function getSessionId(callback){
	userService.getSessionId(function(sessionId){
		callback(sessionId);
	}, null);
}

function createUser(user, callback, onException){
	userService.create(user, callback, onException, null);
}

function logout(callback){
	userService.logout(callback, null);
}

function isUserAllowedTo(action){
	return window.xuser && window.xuser.availableFunctions && window.xuser.availableFunctions.indexOf(action) >= 0;
}

function isUserInRole(role){
	return window.xuser && window.xuser.role == role;
}

_expose(init);
_external(login);
_external(createUser);
_external(logout);
_external(getSessionId);
_external(isUserAllowedTo);
_external(isUserInRole);