var classes = {};
var controllers = {};

var inputsByObj;
var inputsByProperty;
var inputArray;
var xscriptArray;
var aArray;
var objCacheInitialized = false;
function _initializeInputMaps(){
    inputsByObj = {};
    inputsByProperty = {};
    inputArray = [];
    xscriptArray = [];
    objCacheInitialized = true;
    var inputs = xdom.getElementsByAttribute('data-xbind');
    for(var i = 0; i < inputs.length; i++){
        var input = inputs[i];
        addXBind(input);
    }
    inputArray = xdom.getInputs();
    xscriptArray = xdom.getElementsByTagNames("xscript");
    aArray = xdom.getElementsByTagNames('a');
}

//cleaner in case we cant identify the element was removed
function clearObjects(){
    if(inputsByObj){
        _cleanerMapAux(inputsByObj);
        _cleanerMapAux(inputsByProperty);
        _cleanerListAux(inputArray);
        _cleanerListAux(xscriptArray);
        _cleanerListAux(aArray);
    }
}

//aux function to clear map of inputs
function _cleanerMapAux(m){
    for(var k in m){
        _cleanerListAux(m[k]);
    }
}

//aux function to clear list of inputs
function _cleanerListAux(l){
    for(var i = 0; i < l.length; i++){
        if(!l[i].parentNode){
            l.splice(i--, 1);
        }
    }
}

function addXScript(xscript){
    if(xdom.isCreatingHtml()){
        return;
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    if(xscriptArray.indexOf(xscript) < 0){
        xscriptArray.push(xscript);
    }
}

function addA(a){
    if(xdom.isCreatingHtml()){
        return;
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    if(aArray.indexOf(a) < 0){
        aArray.push(a);
    }
}

function addInput(input){
    if(xdom.isCreatingHtml()){
        return;
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    if(inputArray.indexOf(input) < 0){
        inputArray.push(input);
    }
}

function addXBind(input){
    if(xdom.isCreatingHtml()){
        return;
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    var bind = input.getAttribute('data-xbind');
    if(!inputsByProperty[bind]){
        inputsByProperty[bind] = [];
    }
    if(inputsByProperty[bind].indexOf(input) < 0){
        inputsByProperty[bind].push(input);
    }
    var root = xutil.trim(bind.split('.')[0]);
    if(!inputsByObj[root]){
        inputsByObj[root] = [];
    }
    if(inputsByObj[root].indexOf(input) < 0){
        inputsByObj[root].push(input);
    }
}

function _getXScripts(){
    if(xdom.isCreatingHtml()){
        return [];
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    return xscriptArray;
}

function getInputArray(){
    if(xdom.isCreatingHtml()){
        return [];
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    return inputArray;
}

function getAArray(){
    if(xdom.isCreatingHtml()){
        return [];
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    return aArray;
}

function _getInputsOfObject(obj){
    if(xdom.isCreatingHtml()){
        return [];
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    return inputsByObj[obj] || [];
}

function _getInputsByProperty(prop){
    if(xdom.isCreatingHtml()){
        return [];
    }
    if(!objCacheInitialized){
        _initializeInputMaps();
    }
    return inputsByProperty[prop] || [];
}

//called by updateInputs. This method loads object into the inputs
function _loadObjIntInputs(objName) {
	xlog.debug("_x_load_obj", "Obj: " + objName)
	//find by attr with like search
	var inputArray = _getInputsOfObject(objName);
	//find by attr with exact search
	var rootArray = _getInputsByProperty(objName);
	if(rootArray){
	    for(var i = 0; i < rootArray.length; i++){
	        if(inputArray.indexOf(rootArray[i]) < 0){
	            inputArray.push(rootArray[i]);
	        }
	    }
	}
	for ( var i in inputArray) {
		if (inputArray[i] !== document.activeElement
				&& !(inputArray[i].getAttribute("type") == "hidden")) {
			//if is not the focused element and not hidden

			var value = null;
			try{
				value = thisX.eval(inputArray[i].getAttribute("data-xbind"));
			}catch(e){}

			if (!value) {
				xinputs.setValueOnInput(inputArray[i], '');
			} else {
				var xtype = inputArray[i].getAttribute('data-xtype');
				if (xtype == 'ifloat'){
					//ifloat val
					xtype = parseInt(value) == value ? 'int' : 'float';
				}
				if (xtype == 'float') {
					//float val
					value = xmask.getDefaultFormatter().format(value);
				} else if (xtype == 'int') {
					//int val
					value = parseInt(value);
				} else if (xtype == 'date') {
					//date val
					var dtFormat = inputArray[i].getAttribute("data-xdateformat") || '%defaultdateformat%';
					if(inputArray[i].getAttribute("data-xdatetype") == 'true'){
						value = value instanceof Date ? value : xmask.parseDate(value, dtFormat, 'date');
					}else{
						value = xmask.padR(xmask.getNumber(value, true), '0', 8);
						value = xmask.applyDateMask(value, dtFormat);
					}
				} else if (xtype == 'datetime') {
					//datetime val
					var dtFormat = inputArray[i].getAttribute("data-xdatetimeformat") || '%defaultdatetimeformat%';
					if(inputArray[i].getAttribute("data-xdatetype") == 'true'){
						value = value instanceof Date ? value : xmask.parseDate(value, dtFormat, 'datetime');
					}else{
						value = xmask.padR(xmask.getNumber(value, true), '0', 12);
						value = xmask.applyDateMask(value, dtFormat);
					}
				} else if (xtype == 'time') {
					//time val
					var dtFormat = inputArray[i].getAttribute("data-xtimeformat") || '%defaulttimeformat%';
					if(inputArray[i].getAttribute("data-xdatetype") == 'true'){
						value = value instanceof Date ? value : xmask.parseDate(value, dtFormat, 'time');
					}else{
						value = xmask.padR(xmask.getNumber(value, true), '0', 4);
						value = xmask.applyDateMask(value, dtFormat);
					}
				}
				if (xtype == 'autocomplete'){
					//autocomplete
					thisX.getAutocomplete(inputArray[i]).setValue(value);
				}else{
					//normal
					xinputs.setValueOnInput(inputArray[i], value);
				}
			}
		}
	}
}

//builds objects from input
function _buildObjFromInputs(name, isSimpleVar) {
	var result = null;
	var inputArray = _getInputsOfObject(name);
	var val;
	var currentObject = null;
	var currentPropName = 0;

	for ( var i in inputArray) {

		if (result == null)
			result = isSimpleVar ? null : {};
		var isRadio = inputArray[i].getAttribute
				&& inputArray[i].getAttribute("type") == 'radio';
		var dataXBind = inputArray[i].getAttribute("data-xbind")
		if (inputArray[i].getAttribute && dataXBind && (!isRadio || inputArray[i].checked)) {
			currentObject = result;
			if (!isSimpleVar) {
				var propName = dataXBind.substring(name.length);
				var splittedPropName = _splitProperties(propName);
				var arrayIndex = 0;
				var isArray = false;
				for ( var j = 0; j < splittedPropName.length; j++) {
					var spPropName = splittedPropName[j];
					isArray = false;
					if (j < splittedPropName.length - 1 && splittedPropName[j+1].indexOf('[') == 0) {
						isArray = true;
					}
					if (spPropName.indexOf('[') == 0){
						spPropName = spPropName.substring(1, spPropName.length-1);
					}
					if (j == splittedPropName.length - 1) {
						currentPropName = spPropName;
					} else {
						if (isArray) {
							if (!currentObject[spPropName]) {
								currentObject[spPropName] = [];
							}
							currentObject = currentObject[spPropName];
						} else if (!currentObject[spPropName]) {
							currentObject[spPropName] = {};
							currentObject = currentObject[spPropName];
						}
					}
				}
			}
			var xvalue = inputArray[i].getAttribute("data-xvalue");
			if(xvalue && inputArray[i].type != 'checkbox'){
				val = execInCorrectContext(inputArray[i], xvalue);
			}else{
				val = xinputs.getValueFromInput(inputArray[i]);
			}
			try {
				if(isSimpleVar){
					result = val;
				}else{
					currentObject[currentPropName] = val;
				}
			} catch (e) {
			}
		}
	}
	return result;
}

function _isArray(objName, dataXBind){
    if(objName.indexOf("[") > 0){
        if(!objName.trim().endsWith("]")){
            var msg = "Invalid bind to object " + dataXBind;
            xlog.error(msg);
            throw new Error(msg);
        }
        return true;
    }
    return false;
}

//set value on objects from input
function _setValueOnObjFromInput(input) {
	var currentObject = null;
	var currentPropName = 0;

    var isRadio = input.getAttribute
            && input.getAttribute("type") == 'radio';
    var dataXBind = input.getAttribute("data-xbind");

    var isSimpleVar = false;
    var objName = dataXBind.split(".");
    if(objName.length == 1){
        isSimpleVar = true;
    }
    objName = objName[0].trim();

    if (!isSimpleVar) {
        currentObject = thisX.eval(objName);
        if(!currentObject){
            if(_isArray(objName, dataXBind)){
                currentObject = [];
            }else{
                currentObject = {};
            }
            X._temp._x_temp_var_ = currentObject;
            thisX.eval(objName + ' = X._temp._x_temp_var_');
            delete thisX._temp['_x_temp_var_'];
        }

        var propName = dataXBind.substring(objName.length);
        var splittedPropName = _splitProperties(propName);
        var arrayIndex = 0;
        var isArray = false;
        for ( var j = 0; j < splittedPropName.length; j++) {
            var spPropName = splittedPropName[j];
            isArray = false;
            if (j < splittedPropName.length - 1 && _isArray(objName, dataXBind)) {
                isArray = true;
            }
            if (spPropName.indexOf('[') == 0){
                spPropName = spPropName.substring(1, spPropName.length-1);
            }
            if (j == splittedPropName.length - 1) {
                currentPropName = spPropName;
            } else {
                if (isArray) {
                    if (!currentObject[spPropName]) {
                        currentObject[spPropName] = [];
                    }
                    currentObject = currentObject[spPropName];
                } else {
                    if (!currentObject[spPropName]) {
                        currentObject[spPropName] = {};
                    }
                    currentObject = currentObject[spPropName];
                }
            }
        }
    }
    var xvalue = input.getAttribute("data-xvalue");
    var val;
    if(xvalue && input.type != 'checkbox'){
        val = execInCorrectContext(input, xvalue);
    }else{
        val = xinputs.getValueFromInput(input);
    }
    try {
        if(isSimpleVar){
            X._temp._x_temp_var_ = val;
            thisX.eval(objName + ' = X._temp._x_temp_var_');
            delete thisX._temp['_x_temp_var_'];
        }else{
            currentObject[currentPropName] = val;
        }
    } catch (e) {
    }
}

function _createProperty(obj, propertyName){
	var meta = obj._x_meta_object_properties;
	if(!meta){
		meta = {}
		obj._x_meta_object_properties = meta;
	}
	if(!meta[propertyName]){
		var _v = obj[propertyName];
		try{
			delete obj[propertyName];
			Object.defineProperty(obj, propertyName, {
				get : function() {
					xlog.debug("xobj_property", "get: " + propertyName + " on "+ xutil.stringify(this));
					return this["_x_value_on_property_" + propertyName];
				},
				set : function(v) {
					xlog.debug("xobj_property", "before set: " + propertyName + ", value: " + v + " on "+ xutil.stringify(this));
					this["_x_value_on_property_" + propertyName] = v;
					xlog.debug("xobj_property", "after set: " + propertyName + ", value: " + v + " on "+ xutil.stringify(this));
				}
			});
			meta[propertyName] = true;
		}catch(e){
			obj[propertyName] = _v;
		}
	}
}

function _varExists(v){
	try{
		return thisX.eval(v);
	}catch(e){
		return false;
	}
}

//update an object from its inputs
function updateObject(input) {
	if(thisX.isImport){
		return;
	}
	var v;
	if (input.getVar) {
		v = input.getVar();
	} else {
		v = input.getAttribute('data-xbind');
	}
	if (v) {
		xlog.debug("updateObject", "Input data-xbind: " + v + ", value: " + input.value
				+ ", id: " + input.id);
		if (input.getAttribute && input.getAttribute("type") == 'checkbox' && !input.getAttribute("data-xvalue")) {
			xlog.debug("updateObject", "Input data-xbind: " + v
					+ ", checkbox checked: " + input.checked);
			try{
				thisX.eval(v + ' = ' + input.checked);
			}catch(e){
			}
		} else if (input.getAttribute && input.getAttribute("type") == 'radio') {
			var elarray = xdom.getElementsByName(input.getAttribute("name"));
			xlog.debug("updateObject", "Input data-xbind: " + v + ", radio name: "
					+ input.getAttribute("name") + ", len: " + elarray.length);
			var objVal = input;
			for ( var i = 0; i < elarray.length; i++) {
				if (elarray[i].checked) {
					objVal = elarray[i];
					break;
				}
			}
			thisX._temp['_x_temp_var_'] = xinputs.getValueFromInput(objVal);
			xlog.debug("updateObject", "Input data-xbind: " + v
					+ ", radio valFromInput: " + thisX._temp['_x_temp_var_']);
			var lastDot = v.lastIndexOf(".");
			if(lastDot > 0){
				_createProperty(thisX.eval(v.substring(0, lastDot)), v.substring(lastDot + 1));
			}
			try{
				thisX.eval(v + ' = X._temp._x_temp_var_');
			}catch(e){
			}
			delete thisX._temp['_x_temp_var_'];
		} else {
			xlog.debug("updateObject", "Input data-xbind: " + v + ", normal input");

			var newVal;
			if (input.getValue) {
				newVal = input.getValue();
			} else {
				newVal = xinputs.getValueFromInput(input);
			}
			xlog.debug("updateObject", "Input data-xbind: " + v + ", new value " + newVal);
			var array = _getInputsByProperty(v);
			for ( var i in array) {
				var item = array[i];
				if (item !== document.activeElement
						&& (!item.getAttribute || item.getAttribute("type") != "radio") && item.getAttribute("data-xtype") != 'autocomplete') {
					xinputs.setValueOnInput(item, newVal);
				}
			}
			_setValueOnObjFromInput(input);
		}
		xlog.debug("updateObject", "Input data-xbind: " + v + ", value: " + input.value
				+ ", END");
	}
	xlog.debug("updateObject", "end update input");
}

function existsObject(varName) {
	try {
		var obj = thisX.eval(varName);
		return obj != null;
	} catch (e) {
		return false;
	}
}

function _splitProperties(v){
	var p = [];
	var splitted = v.split(".");
	for(var i = 0; i < splitted.length; i++){
		var part = splitted[i];
		var lastIndex = 0;
		var index = 0;
		var hasSubProp = false;
		while((index = part.indexOf('[', lastIndex)) >= 0){
			hasSubProp = true;
			var piece = part.substring(lastIndex, index);
			if(piece){
				p.push(piece);
			}
			lastIndex = part.indexOf(']', index);
			piece = part.substring(index, ++lastIndex);
			if(piece){
				p.push(piece);
			}
		}
		if(!hasSubProp && part){
			p.push(part);
		}
	}
	return p;
}

//called from updateAllObjects
var _grabValueFromPropertyOrInput = function(v) {
	var finalVal = null;
	var inputs = _getInputsByProperty(v);
	xutil.each(inputs, function(input) {
        if (input.value != '' &&
				(input.type != 'radio' || input.checked)) {
			finalVal = xinputs.getValueFromInput(input);
			return false;
		}
	});

	var array = _splitProperties(v);
	var varName = array.shift();
	var obj = thisX.eval(varName);
	while (array.length > 0) {
		var property = array.shift();
		varName = varName + (property.indexOf('[') == 0 ? '' : '.') + property;
		var objProperty = thisX.eval(varName);
		if (objProperty == null) {
			var isArray = array.length > 0 && array[0].indexOf('[') == 0;
			var indexComp = null
			if(property.indexOf('[') == 0){
				property = property.substring(1, property.length-1);
			}
			thisX._temp.__x_temp_val = array.length > 0 ? (isArray ? [] : {}) : finalVal;
			thisX.eval(varName + "=X._temp.__x_temp_val");
			obj = thisX._temp.__x_temp_val;
			delete thisX._temp.__x_temp_val
		} else {
			obj = objProperty;
			lastArrayIndex = null;
		}
	}
	var array = _getInputsByProperty(v);
	xutil.each(array, function(item) {
        if (item.getAttribute && item.getAttribute("type") == "radio") {
			if (item.value == obj) {
				item.checked = true;
			}
		} else {
			if (item !== document.activeElement) {
				xinputs.setValueOnInput(item, obj);
			}
		}
	});
};

//updates all var objects
function updateAllObjects() {
	if(thisX.isImport){
		return;
	}
	var createdFromInputs = {};
	var existedBefore = {};
	var updated = {};
	var inputs = getInputArray();
	for(var i = 0; i < inputs.length; i++){
		var input = inputs[i];
		try{
			var v;
			if (input.getBoundVar) {
				v = input.getBoundVar();
			} else if(input.getAttribute){
				v = input.getAttribute('data-xbind');
			}
			if (v) {
				xlog.debug("updateAllObjects", "Each data-xbind: " + v + ", id: "
						+ input.id);
				var split = v.split('.');
				var objName = split[0];
				xlog.debug("updateAllObjects", "Each before check exists: "
						+ input.value);
				if (existedBefore[objName] == null
						&& createdFromInputs[objName] == null) {
					if (existsObject(objName)) {
						existedBefore[objName] = true;
					} else {
						createdFromInputs[objName] = true;
					}
				}
				xlog.debug("updateAllObjects", "Each after check exists: "
						+ input.value);
				if (existedBefore[objName]) {
					xlog.debug("updateAllObjects",
							"Each before verify objOrInputProperty: "
									+ input.value);
					_grabValueFromPropertyOrInput(v);
					xlog.debug("updateAllObjects",
							"Each after verify objOrInputProperty: "
									+ input.value);
				} else if (!updated[objName]) {
					xlog.debug("updateAllObjects", "Each before update: "
							+ input.value);
					updated[objName] = true;
					thisX._temp['_x_temp_var_'] = _buildObjFromInputs(objName, split.length == 1);
					thisX.eval(objName + ' = X._temp._x_temp_var_');
					delete thisX._temp['_x_temp_var_'];
					xlog.debug("updateAllObjects", "Each after update: "
							+ input.value);
				}
				xlog.debug("updateAllObjects", "Each END");
			}
		}catch(e){
			xlog.error("ERROR UPDATING OBJECTS.", e);
		}
	}
	xlog.debug("updateAllObjects", "end update objects");
};

//update inputs from objects
function updateInputs() {
	if (!thisX._loaded || thisX.isImport) {
		return;
	}
	var updated = {};

	var inputList = getInputArray();
	for(var i = 0; i < inputList.length; i++){
		var input = inputList[i];
		var v = input.getVar ? input.getVar() : input.getAttribute('data-xbind');
		if (v) {
			xlog.debug("updateInputs", "Each data-xbind: " + v + ", value: "
					+ input.value + ", id: " + input.id);
			var objName = v.split('.')[0];
			if (!updated[objName]) {
				updated[objName] = true;
				try{
					_loadObjIntInputs(objName);
				}catch(e){
					xlog.error("Error updating input on var " + objName + " of " + (input.getAttribute("id") || input) + ".", e);
				}
			}
			xlog.debug("updateInputs", "Each END data-xbind: " + v + ", value: "
					+ input.value);
		}
	}
	updateXScripts();
	xlog.debug("updateInputs", "end update inputs");
}

function updateXScripts() {
	if(thisX.isImport){
		return;
	}
	var elements = _getXScripts();
	xutil.each(elements, function(el) {
		var scr = el.getAttribute("data-xscript");
		if(scr){
			var v = scr;
			try {
				var res;
				if(el._compCtx){
					res = el._compCtx._xcompEval(v);
				}else{
					res = thisX.eval(v);
				}
				res = res == null || res == undefined ? '' : res;
				el.innerHTML = res;
			} catch (e) {
				el.innerHTML = "";
			}
		}
	});
}

function bindTo(id, dataXbind){
	xdom.setAtt(X._(id), "data-xbind", dataXbind);
}

_expose(updateObject);
_expose(updateAllObjects);
_expose(updateXScripts);
_expose(updateInputs);
_external(bindTo);
_external(addXBind);
_external(addInput);
_external(addXScript);
_expose(getInputArray);
_expose(getAArray);
_expose(clearObjects);
_expose(addA);