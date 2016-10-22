
var _debug_flags = [%debug_flags%];

function log(msg){
	console.log(msg);
}

function debug(matter, msg, e){
	if(_debug_flags.indexOf(matter) >=0 || (_debug_flags.length == 1  && _debug_flags[0] == 'all')){
		if(e){
			error(msg, e);
		}else{
			console.log("DEBUG (" + matter + "):" + msg);			
		}
	}
}

function error(msg, e){
	if(e && e.stack){
		console.log("ERROR: " + msg);
		console.log("STACK_TRACE: ");
		console.log(e.stack);
	}else{
		console.log("ERROR: " + msg + ". Original error: " + e);
	}
}

function setDebugFlagOn(flag){
	_debug_flags.push(flag);
}

_external(setDebugFlagOn);
_external(log);
_external(debug);
_external(error);
