console.log("INIT X..");

function _(id, _doc){
	return (_doc || document).getElementById(id);
}

function _c(classes, _doc){
	var l = [];
	var cArray = classes.split(' ');
	for(var i = 0; i < cArray.length; i++){
		var ela = (_doc || document).getElementsByClassName(cArray[i]);
		for(var j = 0; j < ela.length; j++){
			l.push(ela[j]);
		}
	}
	return l;
}

var X$ = X$ || new function(){
	this._requiredUsed = [];
	this._firstUpdate = true;
	this._containsRequired = function(src){
		if(this._requiredUsed.indexOf(src) < 0){
			this._requiredUsed.push(src);
			return false;
		}
		return true;
	};
	var newNodes =[];
	//check if the added nodes already have parent
	function checkAddedNodes(){
	    for(var i = 0; i < newNodes.length; i++){
            var el = newNodes[i];
            if(el._xcreated){
                newNodes.splice(i--, 1);
                continue;
            }
            var x;
            for (var j = 0; j < instances.length; j++) {
                if(instances[j].isInThisContext(el)){
                    x = instances[j];
                    break;
                }
            }
            if(!x){
                continue;
            }
            newNodes.splice(i--, 1);

            var lName = el.nodeName.toLowerCase();
            if(!el._xsetAttribute){
                el._xsetAttribute = el.setAttribute;
                el.setAttribute = function(n, v){
                    this._xsetAttribute(n, v);
                    if(n.indexOf('on') == 0){
                        x.configureEvent(n.substring(2), el);
                    } else if(n == 'data-xbind'){
                        x.addXBind(el);
                    }
                };
            }
            if(['input', 'button', 'select', 'textarea'].indexOf(lName) >= 0){
                x.addInput(el);
                x.configureAutocomplete(el);
            } else if(lName == 'xscript'){
                x.addXScript(el);
            } else if(lName == 'a'){
                x.configureHref(a);
                x.addA(el);
            }
        }
	}

    var newPageListeners = [];
    this.onNewPage = function(fn){
        newPageListeners.push(fn);
    };

    this._newPage = function(){
        for(var i = 0; i < newPageListeners.length; i++){
            try{
                newPageListeners[i]();
            }catch(e){
                console.log("Error on new page listener" + e.message);
            }
        }
        newPageListeners = [];
    };

	var isShowingLoading = false;

    this._setBlurryBackground = function(on, idPopup){
    	if(on){
    		var div = document.createElement('div');
    		div.setAttribute("id", "_x_bb_" + idPopup);
    	    var style = "position: fixed;top: 0px;left: 0px; width: 100%; height: 100%;opacity: 0.5;background-color:white;z-index: " +
    	        (X$._highestZIndex() + 1) + ";"
    	    div.setAttribute("style", style);
    		document.body.appendChild(div);
    	}else{
    		var div = document.getElementById("_x_bb_" + idPopup);
    		if(div){
    	        div.remove();
    		}
    	}
    };

    this._highestZIndex = function(){
        var highestZ;
        var onefound = false;
        var divs = document.getElementsByTagName('*');
        if( ! divs.length ) { return highestZ; }
        for( var i=0; i<divs.length; i++ ) {
           if( divs[i].style.position && divs[i].style.zIndex ) {
              if( ! onefound ) {
                 highestZ = parseInt(divs[i].style.zIndex);
                 onefound = true;
                 }
              else {
                 var ii = parseInt(divs[i].style.zIndex);
                 if( ii > highestZ ) { highestZ = ii; }
                 }
              }
           }
        return highestZ;
    }

    this._showLoading = function() {
    	if(!isShowingLoading){
    		isShowingLoading = true;
    		X$._setBlurryBackground(true, 'loading');
    		var dv = document.createElement("div");
    		dv.setAttribute("style","background:{{backgroundLoader}};width: 100%;margin: 0;position: fixed;height: 100%;left: 0;top: 0;border: 0;" +
    				"-webkit-border-radius: 0;-moz-border-radius: 0;-o-border-radius: 0;border-radius: 0;z-index: 3333;");
    		var idModal = "_loading_modal_";
    		dv.setAttribute("id", idModal);
    		var size = 40;
    		var left = parseInt((window.innerWidth - size) / 2);
    		var top = parseInt((window.innerHeight - size) / 2);
    		var style = "style='position: relative;width: " + size + "px; height: " + size
    				+ "px; left: " + left + "px; top: " + top + "px;'";

    		var html = '<img ' + style
    				+ ' src="%ctx%/x/loader.gif"/>';

    		dv.innerHTML = html;
    		document.body.appendChild(dv);
    	}
    };

    this._closeLoading = function(before, after) {
    	if(isShowingLoading){
    		setTimeout(function(){
    			if(before){
    			    before();
    			}
    			var dv = document.getElementById("_loading_modal_");
    			if(dv){
    				dv.parentNode.removeChild(dv);
    			}
    			X$._setBlurryBackground(false, 'loading');
    			isShowingLoading = false;
    			if(after){
    				after();
    			}
    		}, 200);
    	}
    };

	function scheduleRefreshNodes(){
	    setTimeout(function(){
	        checkAddedNodes();
	        if(newNodes.length){
	            scheduleRefreshNodes();
	        }
	    },10);
	}
	function _findXInstanceForElement(e){
	    var attRoot = e.getAttribute("data-xroot-ctx");
	    if(attRoot){
	        for (var i = 0; i < instances.length; i++) {
	            if(instances[i].CTX == attRoot){
	                return instances[i];
	            }
	        }
	        throw Error("Unknown ERROR. X Instance not found for ctx " + instances[i].CTX);
	    }
	    if(!e.parentElement){
	        throw Error("Unknown ERROR. X Instance not found for element " + e.outerHTML);
	    }
	    return _findXInstanceForElement(e.parentElement);
	}
	this._startMutationObserver = function(){
        // cria uma nova instância de observador
        var observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                for(var i = 0; i < mutation.addedNodes.length; i++){
                    newNodes.push(mutation.addedNodes[i]);
                }
                for(var i = 0; i < mutation.removedNodes.length; i++){
                    var index = newNodes.indexOf(mutation.removedNodes[i]);
                    if(index >= 0){
                        newNodes.splice(index, 1);
                        //TODO depois fazer com que remova tambem dos arrays principais
                    }
                }
                if(mutation.target.getAttribute("data-xonmutate")){
                    _findXInstanceForElement(mutation.target)._fireEvent('mutate', mutation.target, {
                        mutationRecord: mutation
                    });
                }
                scheduleRefreshNodes();
            });
        });

        // configuração do observador:
        var config = { childList: true, subtree: true, attributeOldValue: true, attributes: true };

        // passar o nó alvo, bem como as opções de observação
        observer.observe(document.body, config);
    };

	this.copyArray = function(a){
		var r = [];
		if(a){
			for (var i = 0; i < a.length; i++) {
				r.push(a[i]);
			}			
		}
		return r;
	};
	this.canCloseInitLoad = function(){
		var tempLoadDiv = document.getElementById("_xtemploaddiv_");
		if(!tempLoadDiv){
			return false;
		}
		return true;
	}
	this._instanceCounter = 0;
	this.getJsCallbacks = {};
	this.register = function(struct, components, resourceName, fncontroller, xInstance){
		var array = this.getJsCallbacks[resourceName];
		if(!array && resourceName.endsWith("/index.js")){
            resourceName = resourceName.replace(/\/index\.js$/, ".js");
            array = this.getJsCallbacks[resourceName];
        }
		delete this.getJsCallbacks[resourceName];
		for (var i = 0; i < array.length; i++) {
			var cb = array[i];
			//clear timeoutcheck (when login is not valid anymore)
			clearTimeout(array[i]);
			cb(struct, fncontroller, components);
		}
	};
	this._clearInstances = function(){
	    this._changingState = true;
	    //clear all instances except the main (for spa)
	    for(var i = 1 ; i < instances.length; i++){
	        var instance = instances[i];
	        instance._clear();
	    }
	    instances.length = 1;
	};
	//callback when scripts comes from server
	//when it is global insertPoint, resName are empty
    this._onScript = function(struct, fncontroller, components, thisX, callback, insertPoint, resName){
        var controller = new fncontroller(thisX);
        //prepare context
        thisX.isImport = false;
        if(!document.body.getAttribute("data-x_ctx") || thisX.isSpa){
            //main controller
            thisX.isMain = true;
            if(thisX.isSpa){
                thisX.CTX = '_x_mainSpa';
            }else{
                thisX.setAtt(document.body, "data-x_ctx", thisX.CTX);
            }
            thisX.setAtt(document.body, "data-xroot-ctx", thisX.CTX);
        }else if(controller.isModal){
            thisX.CTX = "ctx_" + thisX.generateId();
            thisX.setAtt(insertPoint, "data-xroot-ctx", thisX.CTX);
            insertPoint.className += thisX.CTX;
            thisX.setRoot(insertPoint);
        }else{
            thisX.isImport = true;
        }
        thisX._setEvalFn(controller._x_eval);
        if(struct){
            if(!thisX.isAuthorized(resName)){
                //this is just to help developer, not to protect source. The data is the one that must be protected
                window.location = '/x/unauthorized';
            }else{
                X$.setModalResource(resName);
                thisX._createElements(struct, components, insertPoint, 0, function(){
                    thisX.setController(controller, callback);
                });
            }
        }else{
            thisX.setController(controller, callback);
        }
    };
	var instances = [];
	this._addInstance = function(x){
		instances.push(x);
	};
	this._instances = instances;
	var readyEvents = [];
	this.ready = function(fn){
		if(ready){
			fn();
		}else{
			readyEvents.push(fn);			
		}
	};
	var ready = false;
	this._checkReadyInterval = setInterval(function(){
		if(X$._ifAllReady() && !ready){
			clearInterval(X$._checkReadyInterval);
			ready = true;
			X$._update();
			for (var i = 0; i < readyEvents.length; i++) {
				X$._ifAllReady(readyEvents[i]());
			}
			try{
				$.holdReady(false);
			}catch(e){};
		}
	},50);
	this.isReady = function(){
		return ready;
	};
	this._ifAllReady = function(){
		for (var i = 0; i < instances.length; i++) {
			if(!instances[i]._ready){
				return false;
			}
		}
		%parameters_loaded%
		return X$.canCloseInitLoad();
	};
	this._update = function(){
		//check first if all instances are ready
		if(ready && !this._changingState){
			//all ready
			var updated = false;
			for (var i = 0; i < instances.length; i++) {
				if(instances[i]._controllerSet){
					instances[i]._update();
					updated = true
				}
			}
			if(X$._firstUpdate && updated){
				X$._firstUpdate = false;
				var tempLoadDiv = document.getElementById("_xtemploaddiv_");
				tempLoadDiv.remove();
			}
		}
	};
	this.setDebugFlagOn = function(flag){
		if(ready){
			for (var i = 0; i < instances.length; i++) {
				instances[i].setDebugFlagOn(flag);
			}
		}
	};
	this._modalProperties = {};
	var currentModalResource;
	this.setModalResource = function(res){
		currentModalResource = res.split(".")[0];
	};
	this.setModalInfo = function(json){
		this._modalProperties[currentModalResource] = {};
		for(var k in json.a){
			var att = json.a[k];
			var val = [];
			for(var j = 0; j < att.length; j++){
				val.push(att[j].v);
			}
			this._modalProperties[currentModalResource][k] = val.join('');
		}
	};
}

var _XClass = function(parent) {
	if(parent){
		this.CTX = parent.CTX;
		this.controllerCtx = parent.controllerCtx || parent;
	}else{
		this.CTX = 'main';
	}
	this.instanceId = X$._instanceCounter++;
	var thisX = this;
	X$._addInstance(this);
	var X = this;
	if(!window.X || window.X.instanceId == undefined){
		window.X = this;
	}
	this.isDevMode = %xdevmode%;
	var isDevMode = this.isDevMode;
	function externalExpose(owner, fn){
		return function(){
			return fn.apply(owner, arguments);
		}
	}
	function xexpose(owner, fn, external, name){
		if(!name){
			name = fn.name;
		}
		if(owner[name]){
			throw new Error("Function " + name + " already exposed in module");
		}
		owner[name] = fn;
		if(external){
			if(thisX[name]){
				throw new Error("Function " + name + " already exposed in X");
			}
			thisX[name] = externalExpose(owner, fn);
		}
	}
	this._loadObjects = function(){
	    xobj.updateAllObjects();
        xobj.updateXScripts();
	};
	function addModule(moduleFunction){
		return new moduleFunction(thisX);
	}
	
	var _afterCheck = [];
	thisX.addAfterCheck = function(f){
		_afterCheck.push(f);
	}
	
	%xmodulescripts%
	xcomponents.init();
	this.temp = {};
	
	function _(id){
		return xdom.getElementById(id);
	}

    //controller context interval functions
    var _intervals = [];
	this._interval = function(f,t){
	    var i = window.setInterval(function(){
	        f();
	        X$._update();
	    },t);
	    _intervals.push(i);
	    return i;
	};
    this._clearInterval = function(i){
        window.clearInterval(i);
        _intervals.splice(_intervals.indexOf(i), 1);
    };

    //controller context timeout functions
    var _timeouts = [];
    this._timeout = function(f,t){
        var i = window.setTimeout(function(){
            f();
            X$._update();
        },t);
        _timeouts.push(i);
        return i;
    };
    this._clearTimeout = function(i){
        window.clearTimeout(i);
        _timeouts.splice(_timeouts.indexOf(i), 1);
    };

    //clear resources
    this._clear = function(){
        for(var i = 0; i < _intervals.length; i++){
            clearInterval(_intervals[i]);
        }

        for(var i = 0; i < _timeouts.length; i++){
            clearTimeout(_timeouts[i]);
        }
    }
	
	thisX._ = _;
	thisX._temp = {};
	
	this.eval = function(fn) {
		try{
			return this._evalFn(fn);
		}catch(e){
			throw new Error('Error on script: ' + fn + '. Cause: ' + e.message);
		}
	};
	var readyEvents = [];
	var ready = false;
	this.ready = function(fn){
		if(ready){
			X$.ready(fn);
		}else{
			readyEvents.push(fn);			
		}
	};

	this._getJS = function(resName, insertPoint, callback){
	    var self = this;
	    var fnCb = function(struct, fncontroller, components){
	        X$._onScript(struct, fncontroller, components, self, callback, insertPoint, resName);
	    };
		if(!xresources.isImportable(resName)){
		    throw new Error('Invalid resouce to import ' + resName);
		}
		if(!X$.getJsCallbacks[resName]){
			X$.getJsCallbacks[resName] = [];
			var scr = xdom.createElement("script");
			xdom.setAtt(scr, "src", "%ctx%" + resName);
			document.body.appendChild(scr);
		}
		//TODO it is temporary. Will be replaced by app.cache
		X$.getJsCallbacks[resName].push(fnCb);
	}
	
	xevents.onStart();
	
	this._setEvalFn = function(e){
		var _eval;
		this.defineProperty(this, '_evalFn',
		    function() {
				return _eval;
			},
			function(v) {
				_eval = v;
			}
		);
		
		this._evalFn = e;
	}
	
	this.setController = function(controller, callback) {
		xcomponents.startInstances();
		this._controller = controller;
		if(!this.isImport){
			xinputs.configEvents();
			thisX.debug("xstartup", "XObj update all objects");
            if(controller.isModal || this.isSpa){
                xevents.setModal();
            }
        }
		thisX.debug("xstartup", "XObj showing screen");
		try {
			thisX.debug("xstartup", "XObj calling before show page");
			thisX.eval('if(X.beforeShowPage){X.beforeShowPage("' + window.location.pathname.substring("%ctx%".length) + '");}');
		} catch (e) {
			xlog.error("xstartup", "XObj error calling init");
			throw e;
		}
		
		//exec chord of imports and services and exec onInit
		try {
			thisX.debug("xstartup", "XObj calling init");
			var __temp_onInit_fn__ = null; 
			try{
				var fn = thisX.eval('onInit');
				__temp_onInit_fn__ = function(){
				    xremote.setInitMode();
					if((thisX.CTX == 'main' || thisX.CTX == '_x_mainSpa') && !thisX.isImport){
					    var query = xutil.getQueryParams();
						var param = query['_xjp'] ? JSON.parse(xutil._atob(decodeURIComponent(query['_xjp']))) : {};
						for(var k in query){
						    if(k != '_xjp' && k != '_xref'){
						        param[k] = query[k];
						    }
						}
						thisX._loadObjects();
						fn(param);
					}else{
						var param = {};
                            var parameters = window['_x_modal_parameters'];
						if(parameters){
							var queue = parameters[thisX._controller.resourceName.split(".")[0]];
							if(queue){
								param = queue.shift();								
							}
						}
						fn(param.callback, param.parameter);
					}
					xremote.unsetInitMode();
				}
			}catch(e){
				__temp_onInit_fn__ = thisX.$(function(){
					thisX._ready = true;
				});
			};
			var binds = thisX.eval('__xbinds__');
			if(binds){
				var __chord = thisX.createChord(binds.length, __temp_onInit_fn__);
				binds.__exec(__chord);
			}else{
				__temp_onInit_fn__();
			}
		} catch (e) {
			xlog.error("xstartup", "XObj error calling init");
			throw e;
		}
		this._controllerSet = true;
		callback(controller);
		ready = true;
		for (var i = 0; i < readyEvents.length; i++) {
			X$.ready(readyEvents[i]);
		}
		setTimeout(X$._update, 100);

	};
	
	var updateDisabled = false;
	//returns the function without the proxy function if any
	function stripFunction(fn){
		return fn._fn || fn;
	}
	this.$ = function(fn){
		var _f = function(){
			xlog.debug("$", "BEFORE: Intercepting " + fn);
			var r;
			try{
				r = fn.apply(this, arguments);	
			}catch(e){
				if(!e._xcatch){
					var msg = "Error calling function " + fn + ". Error: " + e.message;
					xlog.error(msg, e);
					alert(msg);
					e._xcatch = true;					
				}
				throw e;
			}
			xlog.debug("$", "AFTER: Intercepting " + fn);
			return r;
		};
		_f._fn = fn;
		return _f;
	};
	var _updating = false;
	this._update = function(){
		if(!this._ready || _updating || X$._changingState){
			return;
		}
		_updating = true;
		xvisual.updateIterators();
		xobj.clearObjects();
		xobj.updateInputs();
		xdom.updateElementsAttributeValue();
		xinputs.configEvents();
		xobj.updateXScripts();
		_updating =false
	};
	var event; 
	function xsetCurrentEvent(e){
		event = e;
	};
	this.getEvent = function(){
		return event;
	};

	this.defineProperty = function(obj, name, getter, setter){
	    Object.defineProperty(obj, name, {
            get : getter || function(){},
            set : setter || function(){}
        });
	};
	this.defineProperty(this, 'referrer',
        function() {
            return X$._lastUrl || document.referrer;
        },
        function(v) {
        }
    );
};

window.applicationCache.addEventListener('updateready', function(){
    location.reload();
}, false);