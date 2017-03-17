var popups = {};

//create a popup modal
function popup(obj){
	var _tempDiv = xdom.createElement("div");
	obj.actions = null;
	obj.el = _tempDiv;
	modal(obj, "popup", function(){
		obj._c._id_modal = xutil.generateId();
		msg(obj);
		X$._update();
		thisX.closeLoading();
	}, false);
}
var modalCallbacks = {};
//create a modal component
//obj: parameters for modal
//element: element where the modal will be placed
//isPopup: if is popup
//callback: callback function when ready
function modal(obj, type, callback, isSpa){
    var isPopup = type == "popup";
	if(isPopup){
		thisX.showLoading();
	}
	var jsName = obj.url.replace(/\.html$/, '') + ".m.js";
	if(!window['_x_modal_parameters']){
		window['_x_modal_parameters'] = {};
	}
	if(!window['_x_modal_parameters'][obj.url]){
		window['_x_modal_parameters'][obj.url] = [];
	}
	window['_x_modal_parameters'][obj.url].push({callback: obj.callback, parameter: obj.parameter});
	xremote.importScript(jsName, obj.el, _createModalCallback(obj, callback), null, isSpa);
}

//called by 
function _createModalCallback(obj, callback){
	return function(_c){
		obj._c = _c;
		obj._started = false;
		if(obj.toggle){
			toggleModal(obj, callback);				
		}else{
			if(callback){
				callback();					
			}
		}
	}
}

//to be written on server based on annotations
function modalS(url, toggle, parentElementId, callback){
    var execModal = function(){
        var modalEl = xdom.createElement('xmodal');
        modalEl.style.display = 'none';
        X._(parentElementId).appendChild(modalEl);
        var obj = {url:url, toggle: toggle, el: modalEl};
        modal(obj, "modal", function(){
            obj._c.toggle = function(){
                toggleModal(obj);
            };
            if(callback){
                callback(obj._c);
            }
        }, false);
    }
    if(X$._changingState){
        X$.onNewPage(execModal);
    }else{
        execModal();
    }

}

//to be used by spa feature
function setSpaModalNode(insertPoint){
    X$.spaNode = xdom.createElement('xspamodal');
    xdom.setAtt(X$.spaNode, 'data-xroot-ctx', '_x_mainSpa');
    insertPoint.xsetModal(X$.spaNode);
    _spaModal(window.location.pathname + window.location.search, true);
}

function _parseUrl(url){
    var qmIndex = url.indexOf('?');
    var path = qmIndex >= 0 ? url.substring(0, qmIndex) : url;
    path = path.substring(xutil.getCtx().length);
    return {
        path: path,
        query: qmIndex >= 0 ? url.substring(qmIndex) : '',
        tpl: xresources.getTplInfo(path)
    }
}

var lastUrl = window.location.pathname + window.location.search;
//refreshs the spa modal node
function _spaModal(gotoUrl, skipUpdateState){
    X$._clearInstances();
	var current = _parseUrl(lastUrl);
	var goto = _parseUrl(gotoUrl);
	if(!goto.tpl || !current.tpl || goto.tpl != current.tpl){
	    //incompatible window (not the same tamplate, or no template at all)
        window.location = goto.path + goto.query;
	}else{
	    var tempNode = document.createElement('div');
	    if(!skipUpdateState){
	        var newPath = xutil.getCtx() + goto.path + goto.query;
	        if(xutil.isRunningOnFileProtocol()){
                //running locally push state is disable
                xutil.setQueryParams(newPath);
            }else{
	            history.pushState(null, null, newPath);
	        }
	        X$._lastUrl = X$._currentUrl;
            X$._currentUrl = window.location.toString();
	    }
	    var obj = {url: goto.path, toggle: true, el: tempNode, isSpa: true};

    	modal(obj, "spa", function(){
            while (X$.spaNode.firstChild) {
                X$.spaNode.removeChild(X$.spaNode.firstChild);
            }
            while (tempNode.childNodes.length > 0) {
                X$.spaNode.appendChild(tempNode.childNodes[0]);
            }
            X$._changingState = false;
            X$._newPage();
    	}, true);
	}
}

function onPushStateSpa(newUrl, skipUpdateState){
    if(X$.spaNode){
        _spaModal(newUrl, skipUpdateState);
    }else{
        window.location = newUrl;
    }
}

//set visible or not the modal (not popup)
function toggleModal(obj, callback){
	xobj.updateAllObjects();
	xobj.updateXScripts();
	if(obj.showLoading){
		thisX.showLoading();
	}
	var beforeShowModal = thisX.beforeShowModal;
	try {
		thisX.debug("xstartup", "XObj calling before show modal");
		thisX.eval('if(X.beforeShowModal){X.beforeShowModal("' + obj.url + '");}');
	} catch (e) {
		xlog.error("xstartup", "XObj error calling init");
		throw e;
	}
	if(!obj.isSpa){
	    for (var i = 0; i < obj.el.parentNode.children.length; i++) {
            var child = obj.el.parentNode.children[i];
            child.style.display = child === obj.el ? "block" : "none";
        }
	}
	obj._c._x_eval("X._loadObjects()");
	if(callback){
		callback();
	}
}
//id gen for modal
function _generateIdModal(){
	return "modal_" + xutil.generateId();
}

function setModalPosition(elmod){
	var _docHeight = (document.height !== undefined) ? document.height : document.body.offsetHeight;
	if(elmod.offsetHeight > (_docHeight * 0.9)){
		elmod.style.top = '0%';
		elmod.style.position = 'relative';
		elmod.style.transform = 'translate(-50%, 0%)';
	}else{
		elmod.style.position = 'fixed';
		elmod.style.top = '50%';
		elmod.style.transform = 'translate(-50%, -50%)';
	}
}

var popupTemplates = {%popupmodaltemplates%};

//alert message
function msg(obj) {
	var mprop = X$._modalProperties[obj.url] || {};
	if (!obj.size) {
		obj.size = {
			width : mprop.width || 400
		};
	}
	var popupTemplate = popupTemplates[mprop.template || obj.template] || popupTemplates._xdefaultTemplate;
	if(!popupTemplate){
		popupTemplate = popupTemplates[popupTemplates._xdefaultTemplateRef];
	}
	var html = popupTemplate.replace('{obj.title}', mprop.title || obj.title)
		.replace('{obj.msg}', obj.msg || '<xmodalinsertpoint/>');
	var elmod = buildHtmlNodeFromString(html);
	elmod.innerHTML = html;
	var idModal;
	var isAlert = obj.msg;
	if(!isAlert){
		var array = xdom._findNodes(elmod, true, false, function(node){
			if(node.nodeName.toLowerCase() == 'xmodalinsertpoint'){
				return true;
			}
		});
		array[0].appendChild(obj.el);
		idModal = obj._c._id_modal
	}else{
		idModal = "md_" + xutil.generateId();
	}
	var bgDiv = xdom.createElement('div');
	xdom.setAtt(bgDiv, "id", idModal);
	var modalFooter;
	xdom._findNodes(elmod, false, false, function(node){
		if(node.classList){
			if(node.classList.contains("closeModal")){
				node._idModal = idModal;
				node._skipEventConfig = true;
				node.onclick = function(){
					closeMsg(this._idModal);
				}				
			}else if(node.classList.contains("modal-footer")){
				if(!isAlert){
					node.style.display = 'none';
				}else{
					modalFooter = node;					
				}
			}
		}
	});
	elmod.style.maxWidth = obj.size.width + 'px';
	elmod.style.left = '50%';
	elmod.style.opacity = 0;
	var hzi = xutil.highestZIndex();
	X$._setBlurryBackground(true, idModal);
	elmod.style.zIndex = hzi + 2;
	xdom.setAtt(bgDiv, "style", "position: fixed;overflow-y: auto;top: 0;left: 0;bottom: 0;right: 0;outline: 0;z-index: " + (hzi + 1));
	
	bgDiv.appendChild(elmod);
	document.body.appendChild(bgDiv);
	setModalPosition(elmod);
	thisX.addAfterCheck(function(){
		setModalPosition(elmod);
	});
	elmod.style.opacity = 1;
	if(isAlert){
		for(var i in obj.actions) {
			var action = obj.actions[i];
			var id = '_a_btn_' + xutil.generateId();
			var btn = xdom.createElement("button");
			btn.action = action;
			btn.innerHTML = action.label;
			if(action.classes){
				xdom.setAtt(btn, "class", action.classes);
			}
			if(action.style){
				xdom.setAtt(btn, "style", action.style);
			}
			btn._skipEventConfig = true;
			btn._id_modal = idModal;
			xdom.setAtt(btn, "id", id);
			btn.onclick = function(){
				if(this.action.fn){
					this.action.fn();
				}
				closeMsg(this._id_modal);
			}
			modalFooter.appendChild(btn);
		}
	}
}

function showLoading(){
    X$._showLoading();
}

function closeLoading(noupdate, cb){
    X$._closeLoading(function(){
        if(!noupdate){
            xobj.updateInputs();
        }
    }, cb);
}

function buildHtmlNodeFromString(html){
	var dv = xdom.createElement("div");
	dv.innerHTML = html;
	for(var i = 0; i < dv.childNodes.length; i++){
		if(dv.childNodes[i].nodeType == 1){
			return dv.childNodes[i];
		}
	}
}

//close alert
function closeMsg(idModal) {
	var dv = document.getElementById(idModal);
	dv.parentNode.removeChild(dv);
	X$._setBlurryBackground(false, idModal);
	xobj.updateInputs();
}

function _iterHighlightFields(fields, each){
	fields = fields instanceof Array ? fields : [fields]
	xutil.each(fields, function(field){
		if(typeof(field) == 'string'){
			xutil.each(xdom.getElementsByAttribute('data-xbind', field), function(item){
				each.call(null, item);
			});
		}else{
			each.call(null, field);
		}			
	});
}

//Highlight fields
function highlight(fields) {
	_iterHighlightFields(fields, onHighlight);
}

//highlight a field. May be overwritten
function onHighlight(obj) {
	obj.style.border = "1px solid red";
}

//remove highlight 
function removeHighlight(fields) {
	_iterHighlightFields(fields, onRemoveHighlight);
}

//remove highlight a field. May be overwritten
function onRemoveHighlight(obj) {
	obj.style.border = "1px solid #e3e3e3";
}

var _x_iterators = {};
//function to register a iterator. Written by server
function __registerIterator(id, listNameORTimesVar, itemVarName, indexVarName, html, isList){
	_x_iterators[id] = {
		html: html,
		listVar: isList ? listNameORTimesVar.v : null,
		countVar: isList ? null : listNameORTimesVar.v,
		itemVar: isList ? (itemVarName ? itemVarName.v : null) : null,
		indexVar: indexVarName ? indexVarName.v : null  
	}
}
var _ctxIdGen = 0; 
var updatingIterators = false;
var iteratorContexts = {};
//updates the iterators on screen
function updateIterators(){
	if(thisX.isImport){
		return;
	}
	if(updatingIterators){
		return;
	}
	updatingIterators = true;
	var elArray = [];
	try{
		//gets all iterator's inner elements on context by the property status=none
		var iterEl;
		while((iterEl = xdom.findFirstIteratorWithNoneStatus())){
			try{
				elArray.push(iterEl);
				var iid = iterEl.xiterId;
				iterEl.xiteratorStatus = "updating";
				var parentIteratorsArray = [];
				var indexes = [];
				var parent;
				var current = iterEl;
				var indexInParent = null;
				
				if(!iteratorContexts[iid]){
					iteratorContexts[iid] = [];
				}
				var ctx;
				//get parent iterators
				while((parent = xdom.findParentIterator(current))){
					var pIid = parent.xiterId;
					indexInParent = current.xiterIndex != undefined ? current.xiterIndex : null;//avoid undefined
					indexes.splice(0, 0, indexInParent);
					parentIteratorsArray.splice(0, 0, _x_iterators[pIid]);
					current = parent;
				}
				if(!iteratorContexts[iid][indexInParent]){
					//create fn context for iterator
					var fnCtx = ['var __transl = {};var __t;var __all;'];
					var fnUpdateCtx = ['this.update = function(){__t = null;__all=[];'];
					var i;
					for(i = 0; i < parentIteratorsArray.length; i++){
						var item = parentIteratorsArray[i];
						fnCtx.push("var _x_list_" + i);
						fnCtx.push("var _x_count_" + i + "=0");
						fnCtx.push("var _xold_list_" + i + "=null");
						fnCtx.push("var _xold_count_" + i);
						if(item.itemVar){
							fnCtx.push("var " + item.itemVar);
							fnCtx.push("var old_" + item.itemVar + "=null");					
						}
						fnCtx.push("var " + item.indexVar + "=__arg[" + i + "]");
						fnUpdateCtx.push("_xold_list_" + i + "=_x_list_" + i);
						fnUpdateCtx.push("_xold_count_" + i + "=_xold_count_" + i);
						fnUpdateCtx.push("try{_x_count_" + i + "=(function (){\nreturn " + item.countVar + ";\n}).call(_xctx)}catch(e){_x_count_" + i + "=0;}");
						if(item.listVar){
							fnUpdateCtx.push("try{_x_list_" + i + "=X$.copyArray((function (){return " + item.listVar + "}).call(_xctx));}catch(e){_x_list_" + i + "=[];}");
							fnUpdateCtx.push("__t='" + item.listVar + "[' + __arg[" + i + "] + ']'");
							fnUpdateCtx.push("for(var __k in __transl){__transl[__k].push(__t);}");
							fnUpdateCtx.push("__transl." + item.itemVar + "=[__t]");
							fnUpdateCtx.push("__all.push(__t)");
							fnUpdateCtx.push("old_" + item.itemVar + "=" + item.itemVar);
							fnUpdateCtx.push(item.itemVar + "=_x_list_" + i + "[__arg[" + i + "]]");
						}
					}
					var iterator = _x_iterators[iid];
					if(iterator.listVar){
						fnCtx.push("var _x_list_" + i);
						fnCtx.push("var _xold_list_" + i);
						fnUpdateCtx.push("_xold_list_" + i + "= _x_list_" + i);
						
						fnUpdateCtx.push("try{_x_list_" + i + "=X$.copyArray((function (){\nreturn " + iterator.listVar + ";\n}).call(_xctx));}catch(e){_x_list_" + i + "=[];}");
						if(!iterator.itemVar){
							iterator.itemVar = "_xv" + xutil.generateId();
						}
						fnCtx.push("var " + iterator.itemVar);
						fnCtx.push("var old_" + iterator.itemVar);
						fnCtx.push("this.isList = true");
					}else{
						fnCtx.push("this.isList = false");
						fnCtx.push("var _x_count_" + i + "=0");
						fnCtx.push("var _xold_count_" + i + "=0");
						fnUpdateCtx.push("_xold_count_" + i + "=_x_count_" + i);
						fnUpdateCtx.push("try{_x_count_" + i + "=(function (){\nreturn " + iterator.countVar + ";\n}).call(_xctx)}catch(e){_x_count_" + i + "=0;}");
					}
					if(!iterator.indexVar){
						iterator.indexVar = "_xiv" + xutil.generateId();
					}
					fnCtx.push("var " + iterator.indexVar);
					fnCtx.push("this.set = function(__temp_index_param_iter){" + iterator.indexVar + " = __temp_index_param_iter;");
					if(iterator.listVar){
						fnCtx.push(iterator.itemVar + " = _x_list_" + i + "[__temp_index_param_iter];");
					}
					fnUpdateCtx.push("}\n")
					fnCtx.push("}");
					fnCtx.push(fnUpdateCtx.join(";\n"));
					fnCtx.push("this.get = function(__temp_index_param_iter){return _x_list_" + i + "[__temp_index_param_iter];}");
					fnCtx.push("this.getOld = function(__temp_index_param_iter){return _xold_list_" + i + "[__temp_index_param_iter];}");
					fnCtx.push("this.length = function(){return this.isList ? _x_list_" + i + ".length : _x_count_" + i + "}");
					fnCtx.push("this.oldLength = function(){return this.isList ? (_xold_list_" + i + " ? _xold_list_" + i + ".length : 0) : _xold_count_" + i + "}");
					fnCtx.push("this.translate = function(param){\n" +
							"if(!this.isList)return param;" +
							"var sp = param.split('.');\n" +
							"var vName = sp[0];\n" +
							"sp.splice(0,1);\n" +
							"var array = __transl[vName];\n" +
							"if(array){\n" +
							"return array.concat(sp).join('.');\n" +
							"};\n" +
							"if(vName != '" + iterator.itemVar + "'){\n" +
							"return param;\n" +
							"};\n" +
							"var result = this.translate('" + iterator.listVar + "');\n" +
							"return result + '[' + " + iterator.indexVar + " + ']' + (sp.length > 0 ? '.' + sp.join('.') : '');\n" +
							"}");
					fnCtx.push("this.eval = function(f){try{return eval(f);}catch(e){throw new Error('Error on iterator script: ' + f + '. Cause: ' + e.message);}}");
					//
					try{
					    var execCtx;
					    var fnName;
					    if(iterEl._compCtx){
                            execCtx = iterEl._compCtx;
                            fnName = '_xcompEval';
                        }else{
                            execCtx = thisX;
                            fnName = 'eval';
                        }
					    var f = execCtx[fnName]("(function(_xctx){return function(__arg){" + fnCtx.join(";\n") + "}})")(execCtx);
					}catch(e){
					    throw Error("Invalid expression iterator/if " + (iterator.countVar || iterator.listVar));
					}

					ctx = new f(indexes);
					ctx.id = _ctxIdGen++;
					iteratorContexts[iid][indexInParent] = ctx;
				}else{
					ctx = iteratorContexts[iid][indexInParent];
				}
				ctx.update();
				updateIterator(iterEl, indexInParent);
			}catch(e){
				xlog.error("xiterator: Error updating iterator instance", e);
			}
		}
	}catch(e){
		xlog.error("xiterator: Error updating iterators", e);
	}
	for(var i = 0; i < elArray.length; i++){
		elArray[i].xiteratorStatus = "none";
	}
	updatingIterators = false;
}

//returns the last iterator context of the element
function getIteratorCtx(e){
	var id = e.xcontentIterId || e.xiterId;
	var iteratorElement = xdom.findIteratorElement(e);
	var indexInParent = iteratorElement && (iteratorElement.xiterIndex || iteratorElement.xiterIndex == 0) ? iteratorElement.xiterIndex : null;
	return iteratorContexts[id][indexInParent] || iteratorContexts[id][null];
}


function _checkDiffLists(ctxList){
	var result = {added:[],removed:[],remaining:{}};
	//times iterator
	if(!ctxList.isList){
		var lastCtxLength = ctxList.oldLength();
		if(lastCtxLength > ctxList.length()){
			for (var i = ctxList.length(); i < lastCtxLength; i++) {			
				result.removed.push(i);
			}
		}else if(ctxList.length() > lastCtxLength){
			for (var i = lastCtxLength; i < ctxList.length(); i++) {
				result.added.push(i);
			}
		}
		return result;
	}
	//list iterator
	var checkPoint = 0;
	var maxChanged = -1;
	for(var i = 0; i < ctxList.length(); i++){
		var icurr = ctxList.get(i);
		var found = false;
		for(var j = checkPoint; j < ctxList.oldLength(); j++){
			var ilast = ctxList.getOld(j);
			if(icurr === ilast || (typeof(icurre) != "object" && typeof(ilast) != "object")){
				found = true;
				if(j != i){
					maxChanged = Math.max(maxChanged, j);
					result.remaining[j] = i;					
				}
				while(checkPoint < j){
					result.removed.push(checkPoint++);
				}
				checkPoint++;
				break;
			}
		}
		if(!found){
			result.added.push(i);
		}
	}
	while(checkPoint < ctxList.oldLength()){
		result.removed.push(checkPoint++);
	}
	//if the index of an add is the same of a remove, keep them, so they will be change by dynamic properties
	for (var i = 0; i < result.removed.length; i++) {
		if(result.removed[i] > maxChanged){
			continue;
		}
		for (var j = 0; j < result.added.length; j++) {
			if(result.added[j] == result.removed[i]){
				result.added.splice(j, 1);
				result.removed.splice(i--, 1);
			}
		}
	}
	return result;
}

//update attributes of an existing element in a iterator
function _updateIterElementAttributes(el, html, ctx, xiterId){
    if(!html.c){
        return;
    }
	var children = xdom._getChildren(el);
	main: for(var j = 0; j < children.length; j++){
		var e = children[j];
		if(e.nodeName.toUpperCase() == 'XSCRIPT' || e.xscript){
			continue;
		}
		var c;
		var k = 0;
		while(true){
		    if(k >= html.c.length){
		        //not found
		        continue main;
		    }
			c = html.c[k++];
			if(c.n && c.n != 'xiterator' && c._nodeindex == e._nodeindex){
				break;
			}
		}
		for(var attName in c.a){
			if(attName == 'value' && e == document.activeElement){
				continue;
			}
			ctx.set(e.xiterIndex);
			var fnCtx;
            if(e._compCtx){
                fnCtx = function(xscr){
                    return ctx.eval("(function(){return " + xscr + "})").call(e._compCtx);
                }
            }else{
                fnCtx = function(xscr){
                    return ctx.eval(xscr);
                }
            }
			var val = [];
			for(var l = 0; l < c.a[attName].length; l++){
				var v = c.a[attName][l];
				if(v.v){
					val.push(v.v);
				}else{
					val.push(fnCtx(v.s));
				}
			}
			if(attName.indexOf('on') == 0 && e.getAttribute("data-x" + attName)){
				attName = "data-x" + attName;
			}
			val = val.join('');
			if(attName == 'href' && val.indexOf('javascript:') == 0 && e.getAttribute("data-x_event_hrefclick") == 'true'){
                e.href = 'javascript:;';
            }else if(attName == 'data-xbind'){
				var t = ctx.translate(val);
				if(!t){
					t = val;
				}
				xdom.setAtt(e, attName, t);
			}else if(attName == 'checked'){
				e.checked = val.toUpperCase() == 'TRUE';
			}else if(attName == 'disabled'){
				e.disabled = val.toUpperCase() == 'TRUE';
			}else if(attName == 'value'){
				e.value = val;
			}else if(val != e.getAttribute(attName)){
				if(attName == 'id'){
					val = val.replace('#modal:', thisX.CTX + ":");
				}
				xdom.setAtt(e, attName, val);
			}
		}
		if(xiterId == e.xcontentIterId){
			_updateIterElementAttributes(e, c, ctx, xiterId);
		}
	}
}

//update scripts in an element iterator
function _updateIterElementScripts(el, ctx, xiterId){
	var array = xdom.getChildNodesByProperty(el, "xscript", true, false, false);
	for(var i = 0; i < array.length; i++){
		var node = array[i];
		if(node.xcontentIterId == xiterId){
			ctx.set(node.xiterIndex);
			var fnCtx;
			if(node._compCtx){
				fnCtx = function(){
				    return ctx.eval("(function(){return " + node.xdataXScript + "})").call(node._compCtx);
				}
			}else{
			    fnCtx = function(){
			        return ctx.eval(node.xdataXScript);
			    }
			}
			var html;
			try{
				html = fnCtx();
				html = html == null || html == undefined ? '' : html;
				html += '';
			}catch(e){
				html = '';
			}
			if(node.nodeType == 3){
			    if(node.textContent != html){
			        node.textContent = html;
			    }
			}else if(node.innerHTML != html){
				node.innerHTML = html;			
			}			
		}
	}
}

function _createElements(html, parent, index, xid, ctx){
	//temporary suffix for components to avoid diferent instances of components to have the same context
	var compCtxSuffix = {};
	_createHTML(html, parent, index, xid, 0, compCtxSuffix, ctx);
}

//check if ctx for component is already created
function _checkCompId(e, compCtxSuffix, ctx){
	ctx.set(e.xiterIndex);
	xcomponents.prepareComponentContext(e, compCtxSuffix, ctx, "this._iter=" + ctx.id + ";");
}

function _createHTML(html, parent, index, xid, level, compCtxSuffix, ctx){
	var node;
	var insertBeforeElement = null;
	if(level == 0){
		var count = 0;
		var childNodes = xdom._getChildNodes(parent);
		for (var i = 0; i < childNodes.length; i++) {
			var nAux = childNodes[i];
			if(nAux.xiterFirstNode && count++ == index){
				insertBeforeElement = nAux;
			}
		}
		if(!insertBeforeElement && parent.xiteratorOpenNode){
			insertBeforeElement = parent.xcloseNode;
		}
	}
	for(var i = 0; html.c && i < html.c.length; i++){
		//iterate over children
		var child = html.c[i];
		child._nodeindex = i + "";
		if(child.t){
			//text
			node = document.createTextNode(child.t);
			node.xiterIndex = index;
			node.xcontentIterId = xid;
			xdom._insertBefore(parent, node, insertBeforeElement);
		}else if(child.x){
			node = parent.nodeName != 'OPTION' ? xdom.createElement('span') : document.createTextNode('');
			node.xdataXScript = child.x;
			node.xscript = true;
			node.xiterIndex = index;
			node.xcontentIterId = xid;
			node.xnodeId = xutil.generateId();
			xdom._setHiddenAttributesOnElement(node, child);
			_checkCompId(node, compCtxSuffix, ctx);
			xdom._insertBefore(parent, node, insertBeforeElement);
		}else{
			//element
			var isHiddenIterator = false;
			if(child.n.toLowerCase() == 'xiterator'){//invisible iterator
				node = document.createTextNode('');
				node.xiteratorOpenNode = true;
				isHiddenIterator = true;
				if(!child.h){
					child.h = {};
					for (var attName in child.a) {
						var att = child.a[attName];
						child.h[attName] = att[0].v;
					}
					delete child.a;
				}
			}else{
				node = xdom.createElement(child.n);
			}
			node.xiterIndex = index;
			var isIterator = isHiddenIterator || xdom._isIterator(child);
			node.xcontentIterId = xid;
			xdom._setHiddenAttributesOnElement(node, child);
			_checkCompId(node, compCtxSuffix, ctx);
			if(!isIterator){
				_createHTML(child, node, index, xid, level + 1, compCtxSuffix, ctx);
			}else{
				node.xiteratorElement = true;
				node.xiteratorStatus = "none";
				node.xiterId = node.xiterId || child.xiterid;
				if(!_x_iterators[node.xiterId]){
				    __registerIterator(node.xiterId, child.a.list ? child.a.list[0] : child.a.count[0], child.a['var'] ? child.a['var'][0] : '_x', child.a.indexvar ? child.a.indexvar[0] : "_ix", child, child.a.list != null);
				}
			}
			xdom._insertBefore(parent, node, insertBeforeElement);
			if(isHiddenIterator){
				var ce = document.createTextNode('');
				node.xcloseNode = ce;
				ce.xopenNode = node;
				ce.xiteratorCloseNode = true;
				xdom._insertBefore(parent, ce, insertBeforeElement);
			}
		}
		node._nodeindex = child._nodeindex;
		if(level == 0 && i == 0){
			node.xiterFirstNode = true;
		}
	}
}

function _removeIteratorChildNodes(){

}

function updateIterator(el, indexInParent){
	var id = el.xiterId;
	var ctx = iteratorContexts[id][indexInParent];
	var iterator = _x_iterators[id];
	var htmlStruct = iterator.html;
	
	//iterate list
	var diff = _checkDiffLists(ctx);
	//remove the removed items
	var i = 0;
	for(; i < diff.removed.length; i++){
		var indexRemove = diff.removed[i];
		var nodeList = xdom.getChildNodesByProperty(el, "xiterIndex", indexRemove, false, false);
		for (var j = 0; j < nodeList.length; j++) {
		    var node = nodeList[j];
			if(node.xcontentIterId == id){
			    if(node.xiterId){
			        iteratorContexts[node.xiterId] = null;
			    }
			    xdom.removeNode(node);
			}
		}
	}
	//update indexes on elements
	//get all first, because it cannot change one at time
	var changeIdNodeList = [];
	for(var i in diff.remaining){
		changeIdNodeList = changeIdNodeList.concat(xdom.getChildNodesByProperty(el, "xiterIndex", i, false, false));
	}
	//change with all gathered
	for (var j = 0; j < changeIdNodeList.length; j++) {
		if(changeIdNodeList[j].xcontentIterId == id){
			changeIdNodeList[j].xiterIndex = diff.remaining[changeIdNodeList[j].xiterIndex];
		}
	}
	//add the new elements
	for(var j = 0; j < diff.added.length; j++){
		_createElements(htmlStruct, el, diff.added[j], id, ctx);
	}
	//update the ones that havent changed
	_updateIterElementScripts(el, ctx, id);
	_updateIterElementAttributes(el, htmlStruct, ctx, id);
}

_expose(__registerIterator);
_external(popup);
_external(msg);
_external(closeMsg);
_external(showLoading);
_external(closeLoading);
_external(highlight);
_external(onHighlight);
_external(removeHighlight);
_external(onRemoveHighlight);
_expose(updateIterators);
_expose(buildHtmlNodeFromString);
_external(toggleModal);
_external(modal);
_external(modalS);
_expose(getIteratorCtx);
_external(setSpaModalNode);
_expose(onPushStateSpa);