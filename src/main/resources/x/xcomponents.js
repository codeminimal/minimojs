var comps;
var components;
function _setComponents(c){
	components = c;
}

//called on initialization of X
function init(){
	try{
		thisX.debug("xstartup", "XComponents INIT");
		%xcomponents%
		comps = _comps
		thisX.debug("xstartup", "XComponents _setComponents");
		_setComponents(components);
		thisX.debug("xstartup", "XComponents initComponent");
		initComponents();
		thisX.debug("xstartup", "XComponents buildComponent");
		createComponentConstructors();
	}catch(e){
		var msg = "Error loading custom components: " + e.message;
		xlog.error(msg, e);
		throw new Error(msg);
	}
	thisX.debug("xstartup", "XComponents INIT done");
}

//constructor of components
function _buildCreateFunction(compName){
	return function(comp){
		var result = {
			_x_comp: thisX.comp[compName]
		};
		for(var k in comp){
			result[k] = comp[k];
		}
		return result;
	}
}

//Initialization method called when the component is dymanic created
function _startComp(_html, comp, fnInsert){
	var _div = xdom.createElement('div');
	_div.innerHTML = _html;
	var xid = comp.xid;
	var len = _div.childNodes.length;
	xutil.range(0, len, function(j){
		if(xid){
			if(j == 0){
				xdom.setAtt(_div.childNodes[0], "_s_xid_", xid);
			}
			if(j == len -1){
				xdom.setAtt(_div.childNodes[0], "_e_xid_", xid);
			}					
		}
		fnInsert(_div.childNodes[0]);	
	});
}

//private auxiliary method to dynamically insert components 
function _insertComp(handle, xid, beforeInsideAfter){
	var _html = handle._x_comp.getHtml(handle);
	if(handle.innerHTML){
		_html = _html.replace('{xbody}', handle.innerHTML);
	}
	_startComp(_html, handle, function(node){
		if(beforeInsideAfter == -1){
			var el = xdom.getElementsByAttribute("_s_xid_", xid)[0] || document.getElementById(xid);
			el.parentNode.insertBefore(node, el);
		}else if(beforeInsideAfter == 0){
			var el = document.getElementById(xid);
			el.appendChild(node);
		}else{
			var el = xdom.getElementsByAttribute("_e_xid_", xid)[0] || document.getElementById(xid);
			el.parentNode.insertBefore(node, el.nextSibling);
		}
		_postCreateComp(handle, xid);
		_configComps();
	});
}

//private
function _postCreateComp(ctx){
	if(ctx.onReady){
		ctx.onReady();
	};
}

//start component's methods
function initComponents(){
	thisX.comp = components;
	thisX.comp.insertBefore = function(handle, xid){
		_insertComp(handle, xid, -1);
	};
	thisX.comp.insertAfter = function(handle, xid){
		_insertComp(handle, xid, 1);
	};
	thisX.comp.append = function(handle, xid){
		_insertComp(handle, xid, 0);
	};
	thisX.comp.updateValue = function(comp){
		xobj.updateObject(comp);
	}
};

//create component's constructors
function createComponentConstructors(){
	xutil.each(comps, function(comp){
		var compName = comp[0];
		components['new' + compName[0].toUpperCase() + compName.substring(1)] = _buildCreateFunction(compName);
	});
}

var _handles = {};
function registerAll(compMap){
	for (var k in compMap) {
		_handles[k] = {};
		var list = compMap[k];
		for (var i = 0; i < list.length; i++) {
			var comp = list[i];
			var id = comp.xcompId;
			delete comp.xcompId;
			_handles[k][id] = comp;
		}
	}
}

function prepareComponentContext(e, compCtxSuffix, ctx, postScript){
	if(e.xcompId && (X.comp[e.xcompName].context || X.comp[e.xcompName].htmxContext)){
		if(!compCtxSuffix[e.xcompId]){
		    //must recreate function from string to create it on the right context
		    var ctx;
		    if(X.comp[e.xcompName].context){
                var fn = X.comp[e.xcompName].context.toString();
                fn = fn.substring(0, fn.length-1) + ";this._xcompEval = function(f){try{return eval(f);}catch(e){throw new Error("+
                    "'Error on component script: ' + f + '. Cause: ' + e.message);}};" + postScript + "}";
                thisX._temp._xtemp_comp_struct = _handles[e.xcompName][e.xcompId];
                ctx = ctx.eval('new ' + fn + '(X._temp._xtemp_comp_struct)');
                delete thisX._temp._xtemp_comp_struct;
            }else{
                var fn = X.comp[e.xcompName].htmxContext.toString();
                ctx.eval('X._temp._fnContext = ' + fn)
                ctx = new thisX._temp._fnContext(_handles[e.xcompName][e.xcompId]);
                if(ctx.defineAttributes){
                    ctx.defineAttributes();
                }
                delete thisX._temp._fnContext;
            }
			e._compCtx = ctx;
			compCtxSuffix[e.xcompId] = ctx;
		}else{
			e._compCtx = compCtxSuffix[e.xcompId];
		}
	}
}

//disable input or component by data-xbind
function disable(varName){
	var elements = xdom.getElementsByAttribute('data-xbind', varName, true);
	xutil.each(elements, function(item){
		xdom.setAtt(item, "disabled", true);
	});
}

var componentInstances;
function register(jsonComp){
	componentInstances = jsonComp;
}
function startInstances(){
	var compCtxSuffix = {};
	for(var compId in componentInstances){
		var array = xdom.findNodesByProperty('xcompId', compId, false, false);
		for (var i = 0; i < array.length; i++) {
			var e = array[i];
			prepareComponentContext(e, compCtxSuffix, thisX, "");			
		}
	}
	componentInstances = null;
}

function _createValProp(mandatory, type, instance, properties, evalFn, forChildElements) {
  return function(child, prop, defaultValue) {
    if (!forChildElements) {
      defaultValue = prop == undefined ? null : prop;
      prop = child;
      child = null;
    };
    evalFn = evalFn || instance._xcompEval;
    if (child) {
      var c = instance._attrs[child];
      if (!c) {
        if (!mandatory) return;
        throw new Error('Property ' + prop + ' of ' + instance._compName + ' is mandatory')
      }
      if (!(c instanceof Array)) {
        throw new Error('Property ' + prop + ' of ' + instance._compName + ' is not a subelement')
      }
      instance[child] = instance[child] || [];
      for (var i = 0; i < c.length; i++) {
        instance[child][i] = instance[child][i] || {};
        var localInstance = instance[child][i];
        var p = c[i];
        localInstance._compName = instance._compName + '.' + child;
        localInstance[prop] = _createValProp(mandatory, type, localInstance, p, evalFn)(prop, defaultValue)
      };
      return;
    };
    var r = (properties || instance._attrs)[prop];
    if(r == undefined){
      r = (properties || instance._attrs)[prop.toLowerCase()];
    }
    if (!r) {
      if (!mandatory) r = defaultValue; else
      throw new Error('Property ' + prop + ' of ' + instance._compName + ' is mandatory')
    }
    if (type == 's') {
      if (typeof r != 'string') {
        throw new Error('Property ' + prop + ' of ' + instance._compName + ' is not string')
      }
      instance[prop] = r;
      return r;
    } else if (type == 'n') {
      if (isNaN(r)) {
        throw new Error('Property ' + prop + ' of ' + instance._compName + ' is not number')
      }
      instance[prop] = parseFloat(r);
      return r;
    } else if (type == 'b') {
      if (r.toUpperCase() != 'TRUE' && r.toUpperCase() != 'FALSE') {
        throw new Error('Property ' + prop + ' of ' + instance._compName + ' is not boolean')
      }
      instance[prop] = r.toUpperCase() == 'TRUE'
    }
    if (type == 'scr') {
      instance[prop] = evalFn(r);
      return r;
    }
  }
}

function _bindValProp(instance) {
  return function(prop, bindTo) {
    bindTo = bindTo || prop;
    var evalFn = instance._xcompEval;
    var varToBind = instance._attrs[prop];
    thisX.defineProperty(instance, bindTo,
        function(){
            return evalFn(varToBind);
        },
        function(v){
            thisX._temp._setVar = v;
            evalFn(varToBind + ' = thisX._temp._setVar');
        }
    );
  }
}
_expose(initComponents);
_expose(createComponentConstructors);
_expose(init);
_external(disable);
_external(registerAll);
_expose(prepareComponentContext);
_expose(register);
_expose(startInstances);
_external(_createValProp);
_external(_bindValProp);