var metaObj = %meta%;

//import remote script
function importScript(jsName, insertPoint, callback, parentX, isSpa){
	xlog.debug("ximport", "Importing script " + jsName);
	var X = new _XClass(parentX);
	X.isSpa = isSpa;
	X._getJS(jsName, insertPoint, callback);
}

var importCallback = {}

//to be written on server based on annotations
function importS(jsName, callback){
	importCallback[jsName] = callback;
	importScript(jsName, null, callback, X);
}
//to be written on server when a util script returns from server
function _addImportToInitChord(object){
	addToInitChord(object, importCallback[object.resourceName]);
}

//add to initialization chord
function addToInitChord(object, callback){
	var fnName = '__xtemp__fn__' + xutil.generateId();
	X[fnName] = function(){
		if(callback){
			callback(object);			
		}
	}
	thisX.eval('var fnName = "' + fnName + 
			'";if(__xbinds__){var __chord = X.createChord(__xbinds__.length, function(){try{onInit()}catch(e){};X[fnName]()});__xbinds__.__exec(__chord);}else{try{onInit()}catch(e){};try{X[fnName]();}catch(e){}}');
}

function go(path, param) {
	//xvisual.showLoading();
	setTimeout(function(){
		if(param){
			path += '?_xjp=' + encodeURIComponent(xutil._btoa(xutil.stringify(param)));
		}
        xvisual.onPushStateSpa(path);
	},10);
}

function _prepareUrl(url) {
	if (url[0] == '/') {
		url = url.substring(1);
	}
	return url;
}

//Get the arguments for a service call.
//If the last two elements are functions, one is a callback and the other is the exception handler
function _getArgs(args){
	var p = Array.prototype.slice.call(args);
	var callback = p.pop();
	var exceptionHandler = null;
	if(typeof(p[p.length -1]) == 'function'){
		exceptionHandler = callback;
		callback = p.pop();
	}
	return [p, callback, exceptionHandler];
}

//makes a post to a hidden form
function _postHiddenForm(path, params) {
    var form = xdom.createElement("form");
    xdom.setAtt(form, "method", "POST");
    xdom.setAtt(form, "action", path);

    for(var key in params) {
        if(params.hasOwnProperty(key)) {
            var hiddenField = xdom.createElement("input");
            xdom.setAtt(hiddenField, "type", "hidden");
            xdom.setAtt(hiddenField, "name", key);
            xdom.setAtt(hiddenField, "value", params[key]);

            form.appendChild(hiddenField);
         }
    }

    document.body.appendChild(form);
    form.submit();
}

//all service calls when on initmode must have their callback called so the X instance can start to update screen
var _initMode;
function setInitMode(){
    _initMode = true;
}
function unsetInitMode(){
    _initMode = false;
    _checkReady();
}
var _countCallsInInitMode = 0;

function _configCallbackForInitMode(callback){
    //if there is a callback and it is in initMode (onInit not finished) we need to wait callback return
    //we put it on the function so we avoid if it was a call on a setTimeout for instance
    if(callback && _initMode){
        callback._initMode = true;
        _countCallsInInitMode++;
    }
}

function decreaseCountInInitMode(){
    _countCallsInInitMode--;
    _checkReady();
}

function _checkReady(){
    if(_countCallsInInitMode == 0){
        thisX._ready = true;
    }
}

//create a remote function of a service
function _createRemoteFunction(alias, method, responseInOutputStream){
	var methodName = method.name;
	var type = method.type;
	var nocache = method.nocache;
	//prepare the parameters
	function _fillParameters(args, parametersMap){
		var params = [];
		for(var i in args[0]){
			//stringify and encode
			var clone = xutil.clone(args[0][i], {changeDateToLong: true});
			var json = xutil.stringify(clone);
			var paramName = "_param" + i;
			var paramValue = encodeURIComponent(json);
			if(parametersMap){
				parametersMap[paramName] = paramValue;
			}else{
				params.push(paramName + "=" + paramValue);				
			}
		}
		//check nocache
		if(nocache == "true"){
			if(parametersMap){
				parametersMap["_xnocache"] = xutil.generateId();
			}else{
				params.push("&_xnocache=" + xutil.generateId());			
			}
		}
		//timezone
		var tz = (new Date().getTimezoneOffset() / 60) * -1;
		if(parametersMap){
			parametersMap['_tz'] = tz;
		}else{
			params.push('&_tz=' + tz);
			return params.join("&");
		}
	}
	if(type == 'GET'){
		//get call
		return function(){
			var args = _getArgs(arguments);
			var callback = args[1];
			_configCallbackForInitMode(callback);
			var exceptionCallback = args[2];
			_configCallbackForInitMode(exceptionCallback);
			var paramArray = [alias + '/' + methodName + '?' + _fillParameters(args), callback];
			if(exceptionCallback){
				paramArray.push(exceptionCallback);
			}
			if(!responseInOutputStream){
				//repose is json
				_callGET.apply(X, paramArray);				
			}else{
				//repose is not json (image or other kind of resource)
				location.reload("/x/" + _prepareUrl(paramArray[0]));
			}
		}
	}else{
		//post call
		return function(){
			var args = _getArgs(arguments);
			var callback = args[1];
			_configCallbackForInitMode(callback);
			var exceptionCallback = args[2];
			_configCallbackForInitMode(exceptionCallback);
			if(_isUploading){
				//has a upload file
				_isUploading = false;
				if(exceptionCallback){
					paramArray.push(exceptionCallback);
				}
				_upload(alias + '/' + methodName, _fillParameters(args), callback, exceptionCallback);
			}else{
				if(!responseInOutputStream){
					//repose is json
					var paramArray = [alias + '/' + methodName, _fillParameters(args), callback];
					if(exceptionCallback){
						paramArray.push(exceptionCallback);
					}
					_callPOST.apply(X, paramArray);
				}else{
					//repose is not json (image or other kind of resource)
					var paramArray = {};
					_fillParameters([arguments], paramArray);
					_postHiddenForm("/x/" + _prepareUrl(alias + '/' + methodName), paramArray)
				}
			}
		}
	}
}

function ping(onSuccess, onError){
	ajax({
		type : "get",
		url : "%ctx%/x/_x_ping?_x_nocache=" + xutil.generateId(),
		async : true,
		success : onSuccess || function(){xlog.debug("_x_internal", "ping")},
		error : onError || function(e){xlog.error("ping error: " + e.message, e)}
	});
}

//to be written by server. Makes a bind to a remote service
function bindService(alias){
	var result = {};
	if(!metaObj[alias]){
		var msg = "Service '" + alias + "' not found!";
		xlog.error(msg);
		throw new Error(msg);
	}
	var meta = metaObj[alias];
	for(var i in meta.methods){
		var method = meta.methods[i].name;
		var type = meta.methods[i].type;
		var responseInOutputStream = meta.methods[i].responseInOutputStream == 'true';
		result[method] = _createRemoteFunction(alias, meta.methods[i], responseInOutputStream);
	}
	return result;
}

function _callGET() {
	_callAjaxGET(_prepareUrl(arguments[0]), arguments[1], arguments[2]);
}

function _callPOST() {
	_callAjaxPOST(_prepareUrl(arguments[0]), arguments[1],
			arguments[2], arguments[3]);
}

//not being used
function _callSync() {
	if (arguments.length == 3) {
		_callSyncAjaxGET(_prepareUrl(arguments[0]), arguments[1]);
	} else {
		_callSyncAjaxPOST(_prepareUrl(arguments[0]), arguments[1],
				arguments[2]);
	}
}

//callback to a server response
function _onCallback(successCallback, exceptionCallback, url, resolve, reject){
	return function(){
		var result = thisX.eval("__x = " + arguments[0]);
		if(result.__response){
			if(successCallback){
			    var tempInitMode = false;
			    if(successCallback._initMode && !_initMode){
			        //not in init mode anymore
			        _initMode = true;
			        tempInitMode = true;
			    }
				successCallback.apply(null, [result.result]);
				if(tempInitMode){
				    _initMode = false;
				}
				decreaseCountInInitMode();
			}
			if(resolve){
			    resolve();
			}
		}else{
			if(result.__not_authenticated){
				thisX.go('/x/no_authentication');
			}
			if(exceptionCallback){
			    var tempInitMode = false;
                if(exceptionCallback._initMode && !_initMode){
                    //not in init mode anymore
                    _initMode = true;
                    tempInitMode = true;
                }
				exceptionCallback.apply(null, [{
					name: result.exceptionName,
					message: result.message
				}]);
                if(tempInitMode){
                    _initMode = false;
                }
                decreaseCountInInitMode();
                if(resolve){
                    resolve();
                }
			}else{
				var msg = "Error calling url: " + url + ".\n Exception: " + result.exceptionName + "\nMessage: " + result.message;
				xlog.error(msg);
				if(reject){
				    reject(msg);
                }
			}
		}
		X$._update();
	}
}

function _callAjaxGET(purl, success, exception) {
    var resolve;
    var reject;
    var promise = new Promise(function(rs, rj){
        resolve = rs;
        reject = rj;
    });
	var url = "%ctx%/x/" + purl;
	ajax({
		type : "get",
		url : url,
		async : true,
		success : _onCallback(success, exception, purl, resolve, reject),
		error : function(jqXHR, textStatus, errorThrown) {
			// Ignore
		}
	});

}
//not being used
function _callSyncAjaxGET(purl, success, exception) {
    var resolve;
    var reject;
    var promise = new Promise(function(rs, rj){
        resolve = rs;
        reject = rj;
    });
	var url = "%ctx%/x/" + purl;
	ajax({
		type : "get",
		url : url,
		async : false,
		success : _onCallback(success, exception, purl, resolve, reject)
	});
}
function _callAjaxPOST(url, param, success, exception) {
    var resolve;
    var reject;
    var promise = new Promise(function(rs, rj){
        resolve = rs;
        reject = rj;
    });
	ajax({
		type : "POST",
		url : "%ctx%/x/" + url,
		data : param,
		async : true,
		dataType : "html",
		success : _onCallback(success, exception, url, resolve, reject)
	});
}
//not being used
function _callSyncAjaxPOST(url, param, success, exception) {
    var resolve;
    var reject;
    var promise = new Promise(function(rs, rj){
        resolve = rs;
        reject = rj;
    });
	ajax({
		type : "POST",
		url : "%ctx%/x/" + url,
		data : param,
		async : false,
		dataType : "html",
		success : _onCallback(success, exception, url, resolve, reject)
	});
}
function ajax(param){
	var type = param.type;
	var url = '%sitedomain%' + param.url;
	var async = param.async || true;
	var data = param.data;
	var success = param.success;

	var xmlhttp;
	if (window.XMLHttpRequest){
		xmlhttp = new XMLHttpRequest();
	}else{
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}
	xmlhttp.onreadystatechange = function(){
		if (xmlhttp.readyState == 4){
			if(xmlhttp.status == 200){
				success(xmlhttp.responseText);
			}else {
				if(param.error){
					param.error(xmlhttp.status);
				}else{
					if (xmlhttp.status == 0) {
						alert('Sem conexao. Verifique sua rede');
					} else if (xmlhttp.status == 404) {
						alert('Ocorreu um erro (404). Por favor contate o administrador.');
					} else if (xmlhttp.status == 500) {
						alert('Ocorreu um erro (500). Por favor contate o administrador.');
					}
				}
			}
		}
	}
	xmlhttp.open(type, url, async);
	xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");  
	xmlhttp.send(data);
}

//get html for a modal
function getHtmlPage(url, callback){
	var _url = "%ctx%" + url + "?_xmd=true";
	ajax({
		type : "get",
		url : _url,
		async : true,
		success : callback,
		error : function(status) {
			if(403){
				alert('Resource ' + url + ' not found');
			}else{
				alert('Error ' + status + ' on resource ' + url);
			}
		}
	});
}

var _file;
var _formUpload;
var _isUploading;
//prepare a upload form
function prepareForUpload(id){
	var file = xdom.getElementById(id);
	if(!file.getAttribute("name") || file.getAttribute("name") == ""){
		xdom.setAtt(file, "name", "_file_upload_" + id);
	}
	var ifr = xdom.createElement("iframe");
	xdom.setAtt(ifr, "src", "");
	xdom.setAtt(ifr, "style", "display: none;");
	xdom.setAtt(ifr, "name", "upload_iframe");
	file.parentNode.insertBefore(ifr, file);
	file.parentNode.removeChild(file);
	var frm = xdom.createElement("form");
	xdom.setAtt(frm, "enctype", "multipart/form-data");
	xdom.setAtt(frm, "target", "upload_iframe");
	xdom.setAtt(frm, "method", "POST");
	frm.appendChild(file);
	ifr.parentNode.insertBefore(frm, ifr.nextSibling);
	
	_file = file;
	_formUpload = frm;
	_isUploading = true;
}

var _callbackUpload = null;
//submits the upload
function _upload(url, parameters, callback, onException) {
	xdom.setAtt(_formUpload, "action", "%ctx%/x/" + url + "?" + parameters);
	_callbackUpload = _onCallback(callback, onException, url);
	_formUpload.submit();
	_formUpload = null;
}
//upload response
function _uploadResponse(response) {
	_file.value = null;
	_file = null;
	_callbackUpload(response);
	xobj.updateInputs();
}

_expose(importScript);
_external(importS, 'import');
_external(ping);
_external(ajax);
_external(bindService);
_external(prepareForUpload);
_external(getHtmlPage);
_external(go);
_external(_uploadResponse);
_expose(addToInitChord);
_external(_addImportToInitChord, '_addImportToInitChord');
_expose(setInitMode);
_expose(unsetInitMode);