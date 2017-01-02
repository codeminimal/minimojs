//set value on input. Called on xobj.loadObj, xobj.updateObject and xobj.grabValueFromPropertyOrInput 
function setValueOnInput(input, value){
	if(input === document.activeElement){
		return;
	}
	xlog.debug("_x_input", "xinputs.setValueOnInput " + (input.getAttribute ? "data-xbind: " + input.getAttribute("data-xbind") + ", type: " + input.getAttribute("type") : " no getAttribute ") + ", name: " + input.name + ", valueToSet: " + value + ", id: " + input.id);
	if(input.getAttribute("type") == "checkbox"){
	    //checkbox is a boolean value or a list value (like multiple choice)
	    var xvalue = input.getAttribute("data-xvalue");
	    var cbValueAtt = input.getAttribute("value");
	    if(xvalue || cbValueAtt){
	        //multiple choice
	        if(!value){
	            value = [];
	        }else if(!(value instanceof Array)){
	            throw Error('Value of ' + input.getAttribute("data-xbind") + ' should be array when using with checkbox with xvalue');
	        }
	        var cbVal;
            if(cbValueAtt){
               cbVal = cbValueAtt;
            }else{
                cbVal = execInCorrectContext(input, xvalue);
            }
            input.checked = value && value.indexOf(cbVal) >= 0;
	    }else{
            //it is boolean, set checked or unchecked
            input.checked = value;
		}
	}else if(input.getAttribute("type") == "radio"){
		//gets all radios and set checked or unchecked
		var elements = xdom.getElementsByName(input.getAttribute("name"));
		xlog.debug("_x_input", "setValueOnRadio radio name: " + input.getAttribute("name") + ", len: " + elements.length);
		for(var i = 0; i < elements.length; i++){
			var item = elements[i];
			xlog.debug("_x_input", "radio name: " + input.getAttribute("name") + ", index: " + i + ", value: " + item.value);
			var xvalue = item.getAttribute("data-xvalue");
			if(xvalue){
				var itemVal = execInCorrectContext(item, xvalue);
				item.checked = xutil.equals(itemVal, value);
			}else{
				item.checked = (item.value == (value != undefined && value != null ? value + "" : ""));				
			}
			xlog.debug("_x_input", "radio name: " + input.getAttribute("name") + ", aftersSet: " + item.checked);
		}
		xlog.debug("_x_input", "setValueOnRadio radio name: " + input.getAttribute("name") + ", END");
	}else if(input.nodeName.toUpperCase() == "SELECT"){
		//gets all options from select
		if(!input.multiple){
			input.selectedIndex = -1;
		}
		var options = input.options;
		for(var i = 0; i < options.length; i++){
			var optVal;
			var xvalue = options[i].getAttribute("data-xvalue");
			if(xvalue){
				optVal = execInCorrectContext(options[i], xvalue);
			}else if(options[i].getAttribute("value")){
				optVal = options[i].getAttribute("value");
			}else{
				optVal = options[i].innerHTML;
			}
			if(input.multiple){
				for (var j = 0; j < value.length; j++) {
					var v = value[j];
					if(xutil.equals(v, optVal)){
						options[i].selected = true;
						break;
					}
				}
			}else{
				if(xutil.equals(value, optVal)){
					input.selectedIndex = i;
					break;
				}
			}
		}
	}else if(input.getAttribute("data-xtype") == 'autocomplete'){
		//autocomplete
		thisX.getAutocomplete(input).setInputValue(value);
	}else{
		var compVal = input.value;
		var xtype = input.getAttribute('data-xtype');
		//is ref object (script var in html). Replaces val for the real val
		if(compVal != value){
			if(xtype == 'date' || xtype == 'datetime' ||
					xtype == 'time'){
				//xtype is date
				var isDate = value instanceof Date;
				if(isDate || input.getAttribute("data-xdatetype") == 'true'){
					var dtFormat = xmask.getMask(input);
					value = xmask.formatDate(value, dtFormat);
					xdom.setAtt(input, "data-xdatetype", 'true');
				}else{
					xdom.setAtt(input, "data-xdatetype", 'false');
				}
			}else if(xtype == 'imoney' || xtype == 'money'){
				//xtype is money
				//TODO mask
				if(!value){
                    value = '0,0'
				}else if(!isNaN(value)){
			        var sp = (value + '').split('.');
                    var decimal = sp[1];
                    value = sp[0] + ',' + decimal + (decimal.length == 1 ? '0' : '');
				}
			}
			input.value = value;
		}
	}
	xlog.debug("_x_input", "xinputs.setValueOnInput data-xbind: " + input.getAttribute("data-xbind") + ", END");
}

function _getValFromOption(input, opt){
	var val;
	var xvalue = opt.getAttribute("data-xvalue");
	if(xvalue){
		val = execInCorrectContext(opt, xvalue);
	}else if(opt.getAttribute("value")){
		val = opt.getAttribute("value");
	}else{
		val = opt.innerHTML;
	}
	return val;
}

//gets value from input. Called from xobj.buildObj, xobj.updateObject, xobj.grabValueFromPropertyOrInput
function getValueFromInput(input){
	var val;
	var type = input.getAttribute ? input.getAttribute('data-xtype') : null;
	var checkbox = input.getAttribute ? input.getAttribute('type') == "checkbox" : null;
	if(input.nodeName.toUpperCase() == "SELECT"){
		if(input.multiple){
			val = [];
			var options = input.options;
			for (var i=0; i<options.length; i++) {
				var opt = options[i];
				if (opt.selected) {
					val.push(_getValFromOption(input, opt));
				}
			}
		}else{
			var selectedOption = input.options[input.selectedIndex];
			val = selectedOption ? _getValFromOption(input, selectedOption) : null;
		}
	}else if(checkbox){
	    //checkbox is a boolean value or a list value (like multiple choice)
        var xvalue = input.getAttribute("data-xvalue");
        var cbValueAtt = input.getAttribute("value");
        if(xvalue || cbValueAtt){
            //multiple choice
            try{
                val = thisX.eval(input.getAttribute("data-xbind"));
            }catch(e){}
            val = val || [];
            var cbVal;
            if(cbValueAtt){
                cbVal = cbValueAtt;
            }else{
                cbVal = execInCorrectContext(input, xvalue);
            }
            if(input.checked){
                //add element to array
                if(val.indexOf(cbVal) < 0){
                    val.push(cbVal);
                }
            }else{
                //remove element to array
                var index = val.indexOf(cbVal);
                if(index >= 0){
                    val.splice(index, 1);
                }
            }
        }else{
            //it is boolean, set checked or unchecked
            val = input.checked;
        }
	}else if (type) {
		xlog.debug("_x_input", "xinputs.getValueFromInput data-xbind: " + input.getAttribute("data-xbind") + ", xtype: " + type);
		if (type == 'imoney'){
			if(input.value.indexOf(',') < 0 && input.value.indexOf('.') < 0){
				type = 'int';
			}else{
				type = 'money';
				input.value = input.value.replace('.', ',');
				var split = input.value.split(',');
				val = split[0] + '.' + xmask.padR(split[1], '0', 2);
				xlog.debug("_x_input", "xinputs.getValueFromInput mo val: " + val);
			}
		}
		xlog.debug("_x_input", "xinputs.getValueFromInput new type: " + type + ", value: " + (val || input.value));
		if (type == 'int' || type == 'percent') {
			val = parseInt(xmask.getNumber(val || input.value));
			if(isNaN(val)){
				val = 0;
			}
			xlog.debug("_x_input", "xinputs.getValueFromInput int: " + val);
		} else if (type == 'money') {
			val = parseInt(xmask.getNumber(val || input.value)) / 100;
			if(isNaN(val)){
				val = 0;
			}
			xlog.debug("_x_input", "xinputs.getValueFromInput money: " + val);
		} else if (type == 'boolean') {
			val = input.value.toUpperCase() == 'TRUE';
		} else if (type == 'autocomplete') {
			val = thisX.getAutocomplete(input).getValue();
			if (!val) {
				val = null;
			}
		} else {
			if(input.getAttribute && input.getAttribute("data-xvalue")){
				val = execInCorrectContext(input, input.getAttribute("data-xvalue"));
			}else{
				val = input.value;
				if (input.getAttribute('data-xdatetype') == 'true' && (type == 'date' || type == 'datetime' || type == 'time')){
					var dtFormat = xmask.getMask(input);
					val = xmask.parseDate(val, dtFormat, type);
				}
			}
		}
	}else if(input.getValue){
		val = input.getValue();
	} else {
		if(input.getAttribute && input.getAttribute("data-xvalue")){
			val = execInCorrectContext(input, input.getAttribute("data-xvalue"));
		}else{
			val = input.value;				
		}
	}
	xlog.debug("_x_input", "xinputs.getValueFromInput return: " + val);
	if(typeof(val) == 'string' && input.getAttribute){
		var trimoff = input.getAttribute('data-xtrimoff');
		if(!trimoff || trimoff.toLowerCase() == 'true'){
			val = thisX.trim(val);
			if(!val){
				val = null;
			}
		}
	}
	return val;
}

function _configEvent(eventName, updateInputs, input) {
    if(input._skipEventConfig){
        return;
    }
    if(!input.getAttribute('data-x_event_' + eventName)){
        xdom.setAtt(input, 'data-x_event_' + eventName, "true");
        input["on" + eventName] = _createFnEvent(input, eventName, updateInputs);
    }
}

function _configEventHref(a) {
    if(a.href && !a.getAttribute('data-x_event_hrefclick')){
        if(a.href.indexOf("javascript:") == 0){
            xdom.setAtt(a, 'data-x_event_hrefclick', "true");
            a["onclick"] = _createFnEvent(a, "href", true);
        }
    }
}

function _createFnEvent(input, eventName, updateInputs) {
	var fn;
	if(eventName == "href"){
		fn = input.href.substring("javascript:".length);
		input.href = 'javascript:;';
	}else{
		fn = input.getAttribute("on" + eventName);
		xdom.setAtt(input, "on" + eventName, "");
	}
	if(fn){
		xdom.setAtt(input, "data-xon" + eventName, fn);
	}
	return function(e) {
		return _fireEventAUX(eventName, input, e, updateInputs);
	};
}

function addEventListener(input, eventName, scr){
	var fn = input.getAttribute("data-xon" + eventName);
	xdom.setAtt(input, "data-xon" + eventName, fn + ";" + scr);
}

//the event function
function _fireEventAUX(eventName, input, e, updateInputs){
    xutil.markFocused();
    xobj.updateObject(input);
	xlog.debug("_x_event", "Fired Event " + eventName + " of input of data-xbind " + input.getAttribute("data-xbind") + ", id: " + input.id);
	var fn = input.getAttribute("data-xon" + eventName);
	if(fn){
		xlog.debug("_x_event", "Firing ev: " + eventName + ", function: " + fn);
		//set current event on X
		xsetCurrentEvent(e);
		try{
			var resultFn = execInCorrectContext(input, fn);
			if(resultFn && typeof(resultFn) == 'function'){
			    resultFn(e);
			}
			if(input._compCtx){
				X$._update();
			}
		}catch(e){
			xlog.error('Error firing ' + eventName + ' script: ' + fn, e);
		}
		xsetCurrentEvent(null);
		xlog.debug("_x_event", "After fired ev: " + eventName + " data-xbind: " + input.getAttribute("data-xbind") + " val: " + input.value);
	}
	X$._update();
    updateDisabled = false;
    xutil.setFocused();
    for (var i = 0; i < _afterCheck.length; i++) {
        var c = _afterCheck[i];
        c();
    }

	xlog.debug("_x_event", "After updateObjectects ev: " + eventName + " data-xbind: " + input.getAttribute("data-xbind") + " val: " + input.value);
	var result = xmask.mask(e);
	if(result != undefined && result == false || result == true){
		return result;
	}
}

function execInCorrectContext(input, fn){
    var fnName = 'eval';
    var ctx;
    var xiterId = input.xcontentIterId || input.xiterId;
    if(xiterId){
        ctx = xvisual.getIteratorCtx(input);
        ctx.set(input.xiterIndex);
    }
    if(input._compCtx){
        ctx = input._compCtx;
        fnName = '_xcompEval';
    }
    if(!ctx){
        ctx = thisX;
    }
    return ctx[fnName](decodeURIComponent(fn));
}

//the event function can be fired outside through this function
function _fireEvent(eventName, idSelOrInput, event){
	var input = typeof(idSelOrInput) == 'string' ? X._(idSelOrInput) : idSelOrInput;
	if(!input){
		return;
	}
	var e = event || {};
	e.type = eventName;
	e.target = input;
	_fireEventAUX(eventName, input, e, false);
}

//config all events on new inputs
function configEvents(){
	if(thisX.isImport){
		return;
	}
	if(!window._xpushStateConfigured){
	    window._xpushStateConfigured = true;
	    if (X$._isSpa){
	        window.addEventListener('click', function(e){
	            //set the pushState
	            if(e.target.nodeName.toUpperCase() == 'A' && e.target.href && !e.target.getAttribute('data-x_event_hrefclick')){
	                //dealing with #
	                var href = e.target.getAttribute("href");
	                if(href == '#'){
	                    return;
	                }
	                var splitHref = href.split('#');
	                if(splitHref[0] == ''){
                        return;
	                }
	                if(splitHref.length > 1 &&
	                    (splitHref[0] == location.protocol + '//' + location.host + location.pathname ||
	                    splitHref[0] == location.pathname)){
	                    return;
	                }
	                //done with #

                    if(href.indexOf('http:') != 0 && href.indexOf('https:') != 0){
                        xvisual.onPushStateSpa(href);
                        return e.preventDefault();
                    }
	            }
	        }, false);
	        X$._currentUrl = window.location.toString();
	        window.addEventListener('popstate', function(event) {
	            var url = window.location.toString().split("#")[0]
	            if(url != X$._currentUrl){
                    X$._lastUrl = X$._currentUrl;
                    X$._currentUrl = window.location.toString();
                    xvisual.onPushStateSpa(window.location.pathname + window.location.search, true);
                }
            });
        }
	}
	var aArray = xobj.getAArray();
    for(var i = 0; i < aArray.length; i++){
        var a = aArray[i];
        _configEventHref(a);
    }
    var inputs = xobj.getInputArray();
	for(var i = 0; i < inputs.length; i++){
    	var input = inputs[i];
        configureEvent("keyup", input);
        configureEvent("keydown", input);
        configureEvent("change", input);
        configureEvent("blur", input);
        configureEvent("focus", input);
        configureEvent("click", input);
        configureAutocomplete(input);
    }
}

function configureHref(a){
    if(a.nodeName.toUpperCase() == 'A'){
        _configEventHref(a);
    }
}

function configureAutocomplete(input){
    if(input.getAttribute("data-xtype") == 'autocomplete' && !input._xautocomplete){
        var ac = thisX.getAutocomplete(input);
        if(input.autocompletefunction){
            var fn = thisX.eval(input.autocompletefunction);
            ac.setSourceFunction(fn);
        }
        if(input.descriptionfunction){
            var fn = thisX.eval(input.descriptionfunction);
            ac.setDescriptionFunction(fn);
        }
        if(input.finaldescriptionfunction){
            var fn = thisX.eval(input.finaldescriptionfunction);
            ac.setFinalDescriptionFunction(fn);
        }
    }
}

function configureEvent(eventName, input){
    _configEvent(eventName, eventName != "keyup", input);
}

function validateFields() {
	var result = true;
	var array = xdom.getElementsByAttribute('data-xmandatory', 'true');
	for(var i in array){
		var item = array[i];
		var validation = validateField(item);
		if(result){
			result = validation;
		}
	};
	return result;
}

function validateField(itemOrName){
	var result = true;
	var item = itemOrName;
	if(typeof(itemOrName) == "string"){
		var array = xdom.getElementsByAttribute('data-xbind', itemOrName, false, true);
		if(array && array.length > 0){
			item = array[0];
		}else{
			return;
		}
	}
	if(!xdom._checkElementInContext(item)){
		return true;
	}
	if (item.value == '') {
		xvisual.highlight(item);
		result = false;
	} else {
		xvisual.removeHighlight(item);
	}
	return result;
}

_external(validateFields);
_external(validateField);
_external(configureAutocomplete);
_external(configureHref);
_expose(setValueOnInput);
_expose(getValueFromInput);
_expose(configEvents);
_external(_fireEvent)
_external(addEventListener);
_external(configureEvent);
_expose(execInCorrectContext);