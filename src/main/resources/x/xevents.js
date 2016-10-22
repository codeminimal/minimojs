//all functions to be called on screen is ready
var execWhenReady = [];
var calledReady = false;
//this method is written on server
function _addExecuteWhenReady(fn){
	if(fn){
		execWhenReady.push(fn);		
	}
}

function setModal(){
    thisX._loaded = true;
}

//private method executed on screen is ready
function ready() {
	if(calledReady){
		return;
	}
	thisX.debug("xstartup", "READY function");
	calledReady = true;
	thisX._loaded = true;
	xdefaultservices.init();
	var isRemote = %is_remote%;
	if(!isRemote){
		thisX.debug("xstartup", "COMPONENTS Init..");
		thisX.debug("xstartup", "Default Services Init..");
		xutil.each(execWhenReady, function(item){
			thisX.debug("xstartup", "Exec execWhenReady " + item);
			item();
		});
	}
	if(!X$._startedMutationObserver){
	    X$._startedMutationObserver = true;
	    X$._startMutationObserver();
	}
	thisX.debug("xstartup", "READY done");
}

//onunload event
function onUnloadCall(){
	var onExit;
	try{
		onExit = thisX.eval('onExit');	
	}catch(e){}
	if(onExit){
		return onExit();
	}
}

//method called when X instantiation is finished
function onStart(){
	thisX.debug("xstartup", "onStart");
	if ( document.addEventListener ) {
		document.addEventListener( "DOMContentLoaded", function(){
			document.removeEventListener( "DOMContentLoaded", arguments.callee, false );
			ready();
		}, false );
	} else if ( document.attachEvent ) {
		document.attachEvent("onreadystatechange", function(){
			if ( document.readyState === "complete" ) {
				document.detachEvent( "onreadystatechange", arguments.callee );
				ready();
			}
		});
		if ( document.documentElement.doScroll && window == window.top ) (function(){
			try {
				document.documentElement.doScroll("left");
			} catch( error ) {
				setTimeout( arguments.callee, 0 );
				return;
			}
			ready();
		})();
	}
	if ( window.addEventListener ) {
		window.addEventListener( "load", function(){
			ready();
		}, false );
	} else if ( window.attachEvent ) {
		window.attachEvent("onload", function(){
			ready();
		});
	}
	window.onbeforeunload = onUnloadCall;
	thisX.debug("xstartup", "onStart done");
}

_expose(onStart);
_external(_addExecuteWhenReady);
_expose(setModal);