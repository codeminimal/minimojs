var _hasStorage = typeof (Storage) !== "undefined" && window.xuser;

//put value on local storage
function putValue(key, value){
	if (hasStorage()) {
		localStorage.setItem(window.xuser.id + '|' + key, stringify(value));
	}
}
//get value from local storage
function getValue(key){
	if (hasStorage()) {
		return thisX.eval('__xstorage = ' + localStorage.getItem(window.xuser.id + '|' + key) + ';');
	}
	return null;
}
//delete value from local storage
function deleteValue(key){
	if (hasStorage()) {
		localStorage.removeItem(window.xuser.id + '|' + key);
	}
}

//put value on local storage
function putValueOnSession(key, value){
	if (hasStorage()) {
		sessionStorage.setItem(window.xuser.id + '|' + key, stringify(value));
	}
}
//get value from local storage
function getValueFromSession(key){
	if (hasStorage()) {
		return thisX.eval('__xstorage = ' + sessionStorage.getItem(window.xuser.id + '|' + key) + ';');
	}
	return null;
}
//delete value from local storage
function deleteValueFromSession(key){
	if (hasStorage()) {
		sessionStorage.removeItem(window.xuser.id + '|' + key);
	}
}

function getQueryParams(){
    var m = {};
    var pairs = location.search.substring(1).split("&");
    for(var i = 0; i < pairs.length; i++){
        var kv = pairs[i].split('=');
        if(kv.length == 2){
            m[kv[0]] = kv[1];
        }
    }
    return m;
}

function hasStorage(){
	return _hasStorage;
}

function each(array, fn){
	for(var i = 0; i < array.length; i++){
		var result = fn(array[i], i, array.length);
		if(result == false){
			break;
		}
	}
}

function cloneObject(obj, propertyList){
	var result = {};
	for(var k in obj){
		if(!propertyList || propertyList.indexOf(k) >= 0){
			result[k] = obj[k];
		}
	}
	return result;
}

function stringify(obj){ 
	if(obj == null || obj == undefined){
		return "null";
	}
	var meta = obj._x_meta_object_properties;
	if(meta){
		delete obj._x_meta_object_properties;	
	}
	var result = JSON.stringify(obj).replace(/_x_value_on_property_/g, "");
	if(meta){
		obj._x_meta_object_properties = meta;		
	}
	return result;		
}

function clone(obj, optParams) {
    var copy;
    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;
    // Handle Date
    if (obj instanceof Date) {
    	if(optParams && optParams.changeDateToLong){
    		copy = obj.getTime();
    	}else{
    		copy = new Date();
            copy.setTime(obj.getTime());    		
    	}
        return copy;
    }
    // Handle Array
    if (obj instanceof Array) {
        copy = [];
        for (var i = 0, len = obj.length; i < len; i++) {
            copy[i] = clone(obj[i], optParams);
        }
        return copy;
    }
    // Handle Object
    if (obj instanceof Object) {
        copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr) && attr != '_x_meta_object_properties') {
            	copy[attr.replace(/_x_value_on_property_/g, "")] = clone(obj[attr], optParams);
            }
        }
        return copy;
    }
    throw new Error("Unable to copy obj! Its type isn't supported.");
}
//exec n times
function range(start, end, fn){
	for(var i = start; i < end; i++){
		var result = fn(i);
		if(result == false){
			break;
		}
	}
}

function index(o, l, fi){
	if(o && l){
		if(o.indexOf){
			return o.indexOf(l, fi);
		}else if(o.length){
			if(o instanceof Array){
				for(var i = (fi || 0); i < o.length; i++){
					if(l == o[i]){
						return i;
					}
				}
			}else if(typeof(o) == "string"){
				for(var i = (fi || 0); i < o.length; i++){
					if(o.substring(i, l.length + i) == l){
						return i;
					}
				}
			}
		}
	}
	return -1;
}
//current date
function now() {
	Date.now = Date.now || function() {
		return +new Date;
	};
	return Date.now();
}
//generate a unique id
function generateId() {
	return now() + parseInt(Math.random() * 999999);
}
//trim string
function trim(s){
	return s.replace(/^\s+|\s+$/g, '');
}

function toHtmlEntities(s){
	return s.replace(/\xE1/g, "&aacute;").replace(/\xE9/g, "&eacute;").replace(/\xED/g, "&iacute;").replace(/\xF3/g, "&oacute;")
		.replace(/\xFA/g, "&uacute;").replace(/\xE3/g, "&atilde;").replace(/\xF5/g, "&otilde;").replace(/\xE7/g, "&ccedil;")
		.replace(/\xEA/g, "&ecirc;").replace(/\xE0/g, "&agrave;").replace(/\xC1/g, "&Aacute;").replace(/\xC9/g, "&Eacute;")
		.replace(/\xCD/g, "&Iacute;").replace(/\xD3/g, "&Oacute;").replace(/\xDA/g, "&Uacute;").replace(/\xC3/g, "&Atilde;")
		.replace(/\xD5/g, "&Otilde;").replace(/\xC7/g, "&Ccedil;").replace(/\xCA/g, "&Ecirc;").replace(/\xC0/g, "&Agrave;");
}

function isChrome() {
	return index(navigator.userAgent.toLowerCase(), 'chrome') > -1;
}

function isIE() {
	return (index(navigator.appVersion, "MSIE") != -1);
}

function getIEVersion(){
	return isIE() ? parseFloat(navigator.appVersion.split("MSIE")[1]) : 999;
}

//Create a chord. 
//The notify method must be called $qtd times to call onFinish
function createChord(qtd, onFinish){
	var count = 0;
	return {
		notify: function(){
			count++;
			if(count == qtd){
				onFinish();
			}
		},
		getCount: function(){
			return qtd;
		},
		getOnFinishFunction: function(){
			return onFinish;
		}
	};
}
//merge 2 objects
function merge(obj1, obj2){
	for(var k in obj1){
		obj2[k] = obj1[k];
	}
}

function getCookie(cookieName) {
	var splitted = document.cookie.split(';');
	for(var i = 0; i < splitted.length; i++){
		var kv = splitted[i].trim().split('=');
		if(kv[0] == cookieName){
			return kv[1];
		}
	}
	return null;
}

function setCookie(cookieName, value, expiration) {
	  var now = new Date();
	  var time = now.getTime();
	  var expireTime = time + expiration;
	  now.setTime(expireTime);
	  document.cookie = cookieName + '=' + value + ';expires='  +now.toGMTString() + ';path=/';
}

var focused;
var idFocused;
var xidFocused;
function focus(id){
	if(X._(id)){
		X._(id).focus();
	}else{
		idFocused = id;
	}
}

//mark the focused element for later refocus
function markFocused(){
	if(!focused){
		focused = document.activeElement;
		idFocused = focused.getAttribute("id");
		xidFocused = focused.getAttribute("xid");
	}
}
//set the marked focused element to focus again
function setFocused(){
	if(focused && !focused.parentElement){
		if(idFocused){
			focused = X._(idFocused);
		}else if(xidFocused){
			var array = xdom.getElementsByAttribute('xid', xidFocused, false, true);
			if(array){
				focused = array[0];
			}
		}else{
			focused = null
		}
	}
	if(focused){
		focused.focus();
	}
	focused = null;
	idFocused = null;
	xidFocused = null;
}

//from base64 to string
function _atob(s){
	return window.atob ? atob(s) : s;
}

//from string to base64 
function _btoa(s){
	return window.btoa ? btoa(s) : s;
}

//highest zindex in screen
function highestZIndex() {
	return X$._highestZIndex();
}

function removeFromArray(item, array){
    var index = array.indexOf(item);
    if (index > -1) {
        array.splice(index, 1);
    }
}

function equals(o1, o2){
    if(o1 == o2){
        return true;//same obj
    }else if(o1 == null || o2 == null){
        return false;
    }else if(o1 instanceof Array){
        if(o2 instanceof Array && o1.length == o2.length){
            for(var i = 0; i < o1.length; i++){
                if(!equals(o1[i], o2[i])){
                    return false;
                }
            }
            return true;
        }
    }else if(typeof(o1) == 'object' && typeof(o2) == 'object' && Object.keys(o1).length == Object.keys(o2).length){
        for(var k in o1){
            if(!equals(o1[k], o2[k])){
                return false;
            }
        }
        return true;
    }
    return false;
}



_external(setCookie);
_external(getCookie);
_expose(removeFromArray);
_expose(isChrome);
_expose(isIE);
_expose(getIEVersion);
_external(each);
_external(stringify);
_external(range);
_external(now);
_external(generateId);
_external(index);
_external(trim);
_external(putValue);
_external(getValue);
_external(deleteValue);
_expose(hasStorage);
_external(toHtmlEntities);
_external(cloneObject);
_external(createChord);
_external(merge);
_external(focus);
_expose(setFocused);
_expose(markFocused);
_expose(clone);
_expose(_atob);
_expose(_btoa);
_expose(highestZIndex);
_external(getQueryParams);
_external(equals);
_external(putValueOnSession);
_external(getValueFromSession);
_external(deleteValueFromSession)