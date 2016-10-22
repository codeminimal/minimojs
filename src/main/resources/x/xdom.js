var root;
function _rootElement(){
	if(!root){
		var elements = document.getElementsByClassName(thisX.CTX);
		if(!elements || elements.length == 0){
			if(thisX.CTX == "main"){
				root = document.body;
			}else if(thisX.CTX == '_x_mainSpa'){
			    root = X$.spaNode;
			}else{
				xlog.error('XDOM: No root element found!!!');
			}
		}else{
			root = elements[0];
		}
	}
	//if not found, is the main context
	return root;
}

function isInThisContext(el){
    return _checkElementInContext(el);
}

function setRoot(r){
	root = r;
}

function _checkParent(parent){
	if(!_checkElementInContext(parent)){
		throw new Error('Invalid context for element');
	}
}

function _checkElementInContext(element){
	var ctxAttr = element && element.getAttribute ? element.getAttribute("data-xroot-ctx") : null;
	if(!element || (ctxAttr && ctxAttr != thisX.CTX)){
		return false;
	}
	if(element.parentElement != _rootElement()){
		return _checkElementInContext(element.parentElement);
	}
	return true
}

function _insertBefore(parent, e, beforeElement){
	var realParent = parent.xiteratorOpenNode ? parent.parentElement : parent;
	realParent.insertBefore(e, beforeElement);
}

function _getChildNodes(e){
	if(e.xiteratorOpenNode){
		var r = [];
		var close = e.xcloseNode;
		var current = e;
		while(current = current.nextSibling){
			if(current == close){
				return r;
			}else{
				r.push(current);
			    if(current.xiteratorOpenNode){
                    current = current.xcloseNode;
                }
			}
		}
		return r;
	}else{
		return e.childNodes;
	}
}

function _getChildren(e){
	if(e.xiteratorOpenNode){
		var r = [];
		var close = e.xcloseNode;
		var current = e;
		while(current = current.nextSibling){
			if(current == close){
				return r;
			}else{
			    if(current.nodeType == 1){
			        r.push(current);
			    }
                if(current.xiteratorOpenNode){
                     current = current.xcloseNode;
                 }
            }
		}
		return r;
	}else{
		return e.children;
	}
}

function _find_childNodes(el, firstLevelOnly, array, fn){
	if(el){
		var childNodes = _getChildNodes(el);
		for(var i = 0; i < childNodes.length; i++){
			var item = childNodes[i];
			if((!item.getAttribute || !item.getAttribute("data-xroot-ctx")) && !item.xiteratorCloseNode){
				if(fn(item)){
					array.push(item);
				}
				if(!firstLevelOnly){
					_find_childNodes(item, firstLevelOnly, array, fn);
				}
			}
		}	
	}
}

function _find_children(el, firstLevelOnly, array, fn){
	if(el){
		var children = _getChildren(el);
		for(var i = 0; i < children.length; i++){
			var item = children[i];
			if(!item.getAttribute("data-xroot-ctx")){
				if(fn(item)){
					array.push(item);
				}
				if(!firstLevelOnly){
					_find_children(item, firstLevelOnly, array, fn);
				}
			}
		}	
	}
}

function _find_first_node_child(element, firstLevelOnly, fn){
	function aux(el){
		if(el){
			var childNodes = _getChildNodes(el);
			for(var i = 0; i < childNodes.length; i++){
				var item = childNodes[i];
				if((!item.getAttribute || !item.getAttribute("data-xroot-ctx")) && !item.xiteratorCloseNode){
					if(fn(item)){
						return item;
					}else if(!firstLevelOnly){
						result = _find_first_node_child(item, firstLevelOnly, fn);
						if(result){
							return result;
						}
					}
				}
			}	
		}
	}
	return aux(element, fn);
}

function _find_first_children(element, firstLevelOnly, fn){
	function aux(el){
		if(el){
			var children = _getChildren(el);
			for(var i = 0; i < children.length; i++){
				var item = children[i];
				if(!item.getAttribute("data-xroot-ctx")){
					if(fn(item)){
						return item;
					}else if(!firstLevelOnly){
						result = _find_first_children(item, firstLevelOnly, fn);
						if(result){
							return result;
						}
					}
				}
			}	
		}
	}
	return aux(element, fn);
}

function _find(parent, stopWhenFound, firstLevelOnly, testFn){
	var result = [];
	if(stopWhenFound){
		var obj = _find_first_children(parent, firstLevelOnly, testFn);
		if(obj){
			result.push(obj);
		}
	}else{
		var result = [];
		_find_children(parent, firstLevelOnly, result, testFn);		
	}
	return result;
}

function _findNodes(parent, stopWhenFound, firstLevelOnly, testFn){
	var result = [];
	if(stopWhenFound){
		var obj = _find_first_node_child(parent, firstLevelOnly, testFn);
		if(obj){
			result.push(obj);
		}
	}else{
		var result = [];
		_find_childNodes(parent, firstLevelOnly, result, testFn);		
	}
	return result;
}

function getElementById(id){
	var array = document.querySelectorAll('*[id="' + id + '"]');
	for (var i = 0; i < array.length; i++) {
		var el = array[i];
		if(_checkElementInContext(el)){
			return el;
		}		
	}
	return null;
}

function getElementsByName(name){
	var result = [];
	var array = document.getElementsByName(name);
	for (var i = 0; i < array.length; i++) {
		var el = array[i];
		if(_checkElementInContext(el)){
			result.push(el);
		}
	}
	return result;
}

//find all by tag name. Can pass a list of args
function getElementsByTagNames(){
	var result = [];
	var rootEl = _rootElement();
	var args = arguments;
	_find_children(rootEl, false, result, function(item){
		for(var i in args){
			if(args[i].toUpperCase() == item.nodeName){
				return true;
			}
		}
	});
	return result;
}

function eachInput(){
	var result = [];
	var array = getElementsByTagNames('input', 'button', 'select', 'textarea');
	for(var i in array){
		result.push(array[i]);
	};
	xutil.each(getElementsByAttribute('onclick', null), function(el){
		if(array.indexOf(el) == -1){
			result.push(el);
		}
	});
	return result;
}

function getInputs(){
	var array = getElementsByTagNames('input', 'button', 'select', 'textarea');
	xutil.each(getElementsByAttribute('onclick', null), function(el){
		if(array.indexOf(el) == -1){
			array.push(el);
		}
	});
	return array;
}

//parse a element attributes into a map
function parseAttributes(element){
	var result = {};
	xutil.each(element.attributes, function(attr){
		if(typeof(attr.nodeValue) == 'string'){
			result[attr.nodeName] = attr.nodeValue;
		}
	});
	return result;
}

//parse a element into a map
function parseElement(comp){
	var attr = parseAttributes(comp);
	var result = {innerHTML: comp.innerHTML};
	for(var k in attr){
		result[k] = attr[k];
	}
	return result;
}

//element creation utility
//function createElement(name, attr, parent){
//	var el = createElement(name);
//	for(var k in attr){
//		setAtt(el, k, attr[k]);
//	}
//	if(parent){
//		parent.appendChild(el)
//	}
//	return el;
//}

function getRootContextElement(){
	return _rootElement();
}

function getNodesByTagName(name, deepSearch, attribute, stopWhenFound){
	var rootEl = _rootElement();
	return getChildNodesByTagName(rootEl, name, deepSearch, attribute, stopWhenFound);
}

//find child node by tag name. Can pass a list of args
function getChildNodesByTagName(parent, name, deepSearch, attribute, stopWhenFound){
	_checkParent(parent);
	return _find(parent, stopWhenFound, !deepSearch, function(node){
		if(node.nodeName == name.toUpperCase()){
			if(!attribute || (node.getAttribute && node.getAttribute(attribute.name) == attribute.value)){
				return true; 
			}
		}
	});
}
//find child node by tag name without checking parent
function getChildNodesByTagNameIgnoreParent(parent, name, deepSearch, attribute, stopWhenFound){
	return _find(parent, stopWhenFound, !deepSearch, function(node){
		if(node.nodeName == name.toUpperCase()){
			if(!attribute || (node.getAttribute && node.getAttribute(attribute.name) == attribute.value)){
				return true; 
			}
		}
	});
}
//find nodes by classname
function getChildNodesByClassName(parent, name, deepSearch, stopWhenFound){
	_checkParent(parent);
	return _find(parent, stopWhenFound, !deepSearch, function(node){
		var classes = node.getAttribute("class");
		if(classes){
			classes = classes.split(" ");
			if(classes.indexOf(name) >= 0 && !node.getAttribute("data-xroot-ctx")){
				if(!attribute || (node.getAttribute && node.getAttribute(attribute.name) == attribute.value)){
					return true;
				}
			}					
		}
	});
}

//find child by name
function getChildElementsByName(parent, name, deepSearch){
	_checkParent(parent);
	return _find(parent, false, !deepSearch, function(item){
		if(name.toUpperCase() == item.nodeName){
			result.push(item);						
		}
		if(deepSearch){
			children(item);						
		}
	});
}

//finds child elements by attribute value. Can be a like search of attribute name
function _findChildElementsByAttribute(attrName, value, parent, like, stopWhenFound, result){
	var array = _find(parent, stopWhenFound, false, function(node){
		if(node.nodeType == 1){
			var attrValue = node.getAttribute(attrName);	
			if(attrValue){
				if(result.indexOf(node) < 0 && (value == null || attrValue == value || (like && attrValue.indexOf(value) == 0))){
					return true;
				}
			}
		}
	});
	for(var i = 0; i < array.length; i++){
		result.push(array[i]);
	}
}

//finds child elements by property value. Can be a like search of attribute name
function _findNodesByProperty(property, value, parent, like, stopWhenFound, result){
	var array = _findNodes(parent, stopWhenFound, false, function(node){
		var attrValue = node[property];	
		if(result.indexOf(node) < 0 && (attrValue == value || (like && attrValue.indexOf(value) == 0))){
			return true;
		}
	});
	for(var i = 0; i < array.length; i++){
		result.push(array[i]);
	}
}

function getChildNodesByProperty(el, property, value, like, stopWhenFound){
	_checkParent(el);
	var result = [];
	_findNodesByProperty(property, value, el, like, stopWhenFound ,result);
	return result;
}

function findFirstElementByAttribute(attrName, value, like){
	var result = getElementsByAttribute(attrName, value, like, true);
	return result.length == 0 ? null : result[0];
}

function findFirstIteratorWithNoneStatus(){
	var result = [];
	_findNodesByProperty('xiteratorStatus', 'none', _rootElement(), false, true, result);
	return result.length == 0 ? null : result[0];
}

//find elements which have an attribute with a certain name and value.
function getElementsByAttribute(attrName, value, like, stopWhenFound){
	var result = [];
	var rootEl = _rootElement();
	_findChildElementsByAttribute(attrName, value, rootEl, like, stopWhenFound, result);
	return result;
}

function getChildElementsByAttribute(el, attrName, value, like, stopWhenFound){
	_checkParent(el);
	var result = [];
	_findChildElementsByAttribute(attrName, value, el, like, stopWhenFound ,result);
	return result;
}
function positionOffset(element) {
    var top = 0, left = 0;
    do {
        top += element.offsetTop  || 0;
        left += element.offsetLeft || 0;
        element = element.offsetParent;
    } while(element);

    return {
        top: top,
        left: left
    };
};

function removeClass(element, className){
	var classes = element.getAttribute("class");
	if(classes){
		classes = classes.split(" ");
		var index = classes.indexOf(className);
		if(index > 0){
			classes.splice(index, 1);
		}
		setAtt(element, "class", classes.join(" "));
	}
}

function addClass(element, className){
	var classes = element.getAttribute("class");
	setAtt(element, "class", (classes ? " " : "") + className);
}

//this function is not context safe
function findParentWithAttribute(el, attName){
	return _findAttrInParent(el.parentElement, attName);
}

function _findAttrInParent(el, attName){
	if(el == document){
		return null;
	}
	if(el && el.getAttribute(attName)){
		return el;
	}else if(el.parentElement){
		return _findAttrInParent(el.parentElement, attName);
	}else{
		return null;
	}
}

//find a parent element with by a condition
function findParentWithProperty(el, pName, condition){
	return _findPropInParent(el.parentElement, pName, condition);
}

//finds the parent iterator (nested) if any
function findParentIterator(el){
	return _findParentIteratorAux(el, el.xiterId)
}

function _findParentIteratorAux(el, xiterId){
	if(el == document || !el){
		return null;
	}
	var prevSib = el;
	while(prevSib = prevSib.previousSibling){
		if(prevSib.xiteratorOpenNode){
			return prevSib;
		}else if(prevSib.xiteratorCloseNode){
			prevSib = prevSib.xopenNode;
		}
	}
	if(el.parentElement && el.parentElement.xiterId && el.parentElement.xiterId != xiterId){
		return el.parentElement;
	}else{
		return _findParentIteratorAux(el.parentElement, xiterId);
	}
}

//find the element with the iteration instructions
function findIteratorElement(el){
	if(el == document || !el){
		return null;
	}else if(el.xiteratorElement){
		return el;
	}
	var prevSib = el;
	while(prevSib = prevSib.previousSibling){
		if(prevSib.xiteratorElement){
			return prevSib;
		}else if(prevSib.xiteratorCloseNode){
			prevSib = prevSib.xopenNode;
		}
	}
	if(el.parentElement && el.parentElement.xiteratorElement){
		return el.parentElement;
	}else{
		return findIteratorElement(el.parentElement);
	}
}

function _findPropInParent(el, pName, condition){
	if(el == document){
		return null;
	}
	if(el && el[pName] && (!condition || condition(el[pName]))){
		return el;
	}else if(el.parentElement){
		return _findPropInParent(el.parentElement, pName, condition);
	}else{
		return null;
	}
}

function _createInsertPoint(element){
	var parent = element.parentElement;
	return {
		appendChild: function(el){
			parent.insertBefore(el, element);
		}
	};
}

function _findChildInStruct(json, name, remove){
	var lcName = name.toLowerCase();
	for(var i = 0; i < json.c.length; i++){
		if(json.c[i].n && json.c[i].n.toLowerCase() == lcName){
			var result = json.c[i];
			if(remove){
				json.c.splice(i, 1);
			}
			return result;
		}
	}
}

var dynamicAttributes = {};
var dynamicOutAttributes = {};

//update dynamic attributes
function updateElementsAttributeValue(){
	if(thisX.isImport){
		return;
	}
	var rootEl = _rootElement();
	for (var id in dynamicAttributes) {
		var e = getElementsByAttribute("data-xdynid", id, false, true);
		if(!e || e.length == 0){
			continue;
		}
		e = e[0];
		var atts = dynamicAttributes[id];
		for (var attName in atts){
			var att = atts[attName];
			try{
				var val = [];
				for(var i = 0; i < att.length; i++){
					var item = att[i];
					if(item.v){
						val.push(item.v);
					}else{
						val.push(thisX.eval(item.s));
					}
				}
				val = val.join('');
				if(attName == 'checked'){
					e.checked = val.toUpperCase() == 'TRUE';
				}else if(attName == 'disabled'){
					e.disabled = val.toUpperCase() == 'TRUE';
				}else{
					setAtt(e, attName, val);
				}
			}catch(ex){
				xlog.error("Error updating attribute " + attName + " of " + (e.getAttribute("id") || e) + ".", ex);
			}
		}
	}
}

//check if ctx for component is already created
function _checkCompId(e, compCtxSuffix){
	xcomponents.prepareComponentContext(e, compCtxSuffix, thisX, "");
}

function _createHTML(json, insertPoint, index, onFinish, compCtxSuffix){
	if(insertPoint == document){
		var body;
		var required = _findChildInStruct(json, 'xrs', true);
		if(json.n.toUpperCase() == 'DOCUMENT'){
			json = _findChildInStruct(json, 'html', false);
		}else if(json.n.toUpperCase() != 'HTML'){
			throw new Error('Invalid html. Json is not html structure');
		}
		var head = _findChildInStruct(json, 'head', false) || {name: 'head'};
		var body = _findChildInStruct(json, 'body', false) || {name: 'body'};
		_setAttributesOnElement(document.body, body, null, true);
		required.inHead = true;
		head.requiredSources = required;
		_createHTML(head, document.head, 0, function(){
			_createHTML(body, document.body, 0, function(){
				xlog.debug("html_creation", "html creation finished");
				onFinish();
			}, compCtxSuffix);
		});
		return;
	}else if(!json.processedRequired && json.n.toUpperCase() == 'DOCUMENT'){
		var required = _findChildInStruct(json, 'xrs', true);
		json.requiredSources = required;
		json.processedRequired = true;
	}
	if(json.requiredSources){
		//create required sources
		var processedRequired = false;
		while(json.requiredSources.c && index < json.requiredSources.c.length){
			var rq = _getAttributeFromStruct(json.requiredSources.c[index], 'src')[0].v;
			if(rq[0] == '/'){
			    rq = rq.substring(1);
			}
			var ext = rq.substring(rq.length-4).toLowerCase()
			if(X$._containsRequired(rq) && ext != '.css'){
				index++;
				continue;
			}
			processedRequired = true
			var e;
			if(ext == '.css'){
				var scoped = !json.requiredSources.inHead;
				if(scoped){
					e = createElement("style");
					setAtt(e, "scoped", true);
					e.innerHTML = "@import url(/res/" + rq + ");";
					insertPoint.appendChild(e);
					_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
					return;
				}else{
					e = createElement("link");
					setAtt(e, "rel", "stylesheet");
					setAtt(e, "type", "text/css");
					setAtt(e, "href", "/res/" + rq);
					e.onload = function(){
						_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
					}
				}
			}else{
				//js
				e = createElement("script");
				setAtt(e, "src", "/res/" + rq);
				e.onload = function(){
					_checkHoldJQuery();
					_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
				}
			}
			document.body.appendChild(e);
			break;
		}
		if(!processedRequired){
			index = 0;
			json.requiredSources = false;
		}
	}
	if(!json.requiredSources){
		if(json.c && index < json.c.length){
			//iterate over children
			var child = json.c[index];
			if(child.n == 'modal-info'){
				X$.setModalInfo(child);
				child = json.c[++index];
			}
			if(child.t){
				//text
				_createTextNode(insertPoint, child, json.n.toUpperCase() == 'SCRIPT');
				_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
			}else if(child.x){
				//xscript
				_createXScriptNode(insertPoint, child, compCtxSuffix);
				_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
			}else{
				//element
				var e;
				var dynId = xutil.generateId();
				var isIterator = _isIterator(child);
				var hiddenIterator = false;
				if(child.n.toLowerCase() == 'xiterator'){//invisible iterator
					hiddenIterator = true;
					e = document.createTextNode('');
					e.dynId = dynId;
					e.xiteratorOpenNode = true;
				}else{
					e = createElement(child.n);
					_setAttributesOnElement(e, child, dynId, true);
					_setHiddenAttributesOnElement(e, child);
					_checkDynOutAttributesOnElement(e, child, dynId);
				}
				_checkCompId(e, compCtxSuffix);
				insertPoint.appendChild(e);
				if(isIterator){
					e.xiteratorElement = true;
					e.xiterId = _prepareIterator(child);			
					if(hiddenIterator){
						var ce = document.createTextNode('');
						e.xcloseNode = ce;
						ce.xopenNode = e;
						ce.dynId = dynId;
						ce.xiteratorCloseNode = true;
						insertPoint.appendChild(ce);
					}
					e.xiteratorStatus = 'none';
					_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
				}else{
					_createHTML(child, e, 0, function(){
						_createHTML(json, insertPoint, index+1, onFinish, compCtxSuffix);
					}, compCtxSuffix);
				}
			}
		}else{
			onFinish();
		}
	}
}

//set jquery to wait for document ready
function _checkHoldJQuery(){
	try{
		if($ && $.holdReady && !thisX._holdingReady){
			thisX._holdingReady = true;
			$.holdReady(true);
		}		
	}catch(e){};
}

function _prepareIterator(html){
    //important: it is html.h.xiterId with uppercase I
	html.xiterid = html.xiterid || html.h.xiterId || xutil.generateId();
	html.h = html.h || {};
	html.h.status = "none";
	var listOrTimes = _getAttributeFromStruct(html, "data-xiterator-list") || _getAttributeFromStruct(html, "list");
	var isTimes = !listOrTimes;
	if(isTimes){
		listOrTimes = _getAttributeFromStruct(html, "data-xiterator-count") || _getAttributeFromStruct(html, "count");
	}
	var variable = _getAttributeFromStruct(html, "data-xiterator-var") || _getAttributeFromStruct(html, "var");
	var varindex = _getAttributeFromStruct(html, "data-xiterator-indexvar") || _getAttributeFromStruct(html, "indexvar");
	xvisual.__registerIterator(html.xiterid, listOrTimes[0], variable ? variable[0] : null, varindex ? varindex[0] : null, html, !isTimes)
	if(html.c){
		for(var i = 0; i < html.c.length;i++){
			var child = html.c[i];
			if(_isIterator(child)){
				_prepareIterator(child);
			}
		}		
	}
	return html.xiterid;
}

function _getAttributeFromStruct(html, attname){
	if(!html.a){
		return;
	}
	return html.a[attname];
}

function _isIterator(html){
	if(html.n && html.n.toLowerCase() == 'xiterator'){
		return true;
	}
	if(!html.a){
		return;
	}
	for(var attName in html.a){
		if(attName.indexOf("data-xiterator-") == 0){
			return true;
		}			
	}
}

function _createTextNode(insertPoint, child, isScript){
	if(isScript){
		insertPoint.innerHTML = child.t;
	}else{
		e = document.createTextNode(child.t);
		insertPoint.appendChild(e);	
	}
}

function createElement(name){
    var el = document.createElement(name);
    el._xcreated = true;
    var lName = name.toLowerCase();
    if(['input', 'button', 'select', 'textarea'].indexOf(lName) >= 0){
        xobj.addInput(el);
    } else if(lName == 'xscript'){
        xobj.addXScript(el);
    } else if(lName == 'a'){
        xobj.addA(el);
    }
    return el;
}

function _createXScriptNode(insertPoint, child, compCtxSuffix){
	e = createElement('xscript');
	setAtt(e, "data-xscript", child.x);
	_setHiddenAttributesOnElement(e, child);
	_checkCompId(e, compCtxSuffix);
	insertPoint.appendChild(e);			
}

//check if exists and register dynamic out attributes
function _checkDynOutAttributesOnElement(e, child, dynId){
	if(!child.o){
		return;
	}
	var dynOutAttrs = [];
	var hasDynOutAttrs = false;
	for(var z = 0; z < child.o.length; z++){
		var att = child.o[z];
		hasDynOutAttrs = true;
		dynOutAttrs.push(att);
	}
	if(hasDynOutAttrs){
		setAtt(e, "data-xdynid", dynId);
		dynamicOutAttributes[dynId] = dynOutAttrs;
	}
}

//set hidden attributes on element.
function _setHiddenAttributesOnElement(e, json){
	if(!json.h){
		return;
	}
	for(var k in json.h){
		e[k] = json.h[k];
	}
}


//set attributes from htmlStruct and register the dynamic ones
function _setAttributesOnElement(e, child, dynId, skipIteratorAtt){
	if(!child.a){
		return;
	}
	var dynAttrs = {};
	var hasDynAttrs = false;
	for(var attName in child.a){
		var att = child.a[attName];
		if(attName.indexOf("data-xiterator-") == 0 && skipIteratorAtt){
			continue;
		}
		var val = [];
		var isDynAttr = false;
		for(var i = 0; i < att.length; i++){
			var item = att[i];
			if(item.v){
				val.push(item.v);
			}else{
				isDynAttr = true;
			}
		}
		val = val.join('');
		if(attName == 'id'){
			val = val.replace('#modal:', thisX.CTX + ":");
		}
		setAtt(e, attName, val);
		if(isDynAttr){
			hasDynAttrs = true;
			dynAttrs[attName] = att;
		}
	}
	if(hasDynAttrs){
		setAtt(e, "data-xdynid", dynId);
		dynamicAttributes[dynId] = dynAttrs;
	}
}

function _registerDynAtt(dynId, att){
	dynamicAttributes[dynId] = att;
}

var creatingHtml = false;
function _createElements(json, components, insertPoint, index, onFinish){
	//temporary suffix for components to avoid diferent instances of components to have the same context
	var compCtxSuffix = {};
	creatingHtml = true;
	xcomponents.registerAll(components);
	_createHTML(json, insertPoint, index, function(){
	    onFinish();
	    creatingHtml = false;
	    try{
            xobj.updateInputs();
            xobj.updateAllObjects();
        } catch (e) {
            xlog.error("xstartup", "XObj starting objects");
            throw e;
        }
	}, compCtxSuffix);
}

function isCreatingHtml(){
    return creatingHtml;
}

function _registerObjects(jsonDynAtt, jsonHiddenAtt, jsonIterators, jsonComp, components){
	xcomponents.registerAll(components);
	
	for(var dynId in jsonDynAtt){
		_registerDynAtt(dynId, jsonDynAtt[dynId]);
	}
	for(var dynId in jsonHiddenAtt){
		var e = getElementsByAttribute('data-xdynid', dynId, false, true)[0];
		if(e){
			for(var n in jsonHiddenAtt[dynId]){
				e[n] = jsonHiddenAtt[dynId][n];
			}			
		}
	}
	xcomponents.register(jsonComp);
	for (var i = 0; i < jsonIterators.length; i++) {
		var iter = jsonIterators[i];
		var temp;
		eval('temp=' + iter[4]);
		xvisual.__registerIterator(iter[0], {v:iter[1]}, {v:iter[2]}, {v:iter[3]}, temp, !iter[5]); 
	}
	//create hidden iterators
	var array = [];
	_find_children({children:[_rootElement()]}, false, array, function(e){
		return e.getAttribute("data-hxiter");
	});
	for (var i = 0; i < array.length; i++) {
		var e = array[i];
		var hiddenIter = e.getAttribute("data-hxiter");
		if(e.nodeName == 'TABLE' && e.firstChild && e.firstChild.nodeName == 'TBODY'){
			e = e.firstChild;
		}
		var split = hiddenIter.split("|");
		for (var j = 0; j < split.length; j++) {
			if(split[j].trim() == ''){
				continue;
			}
			var iter = split[j].split(',');
			var index = parseInt(iter[1]) + j;
			var sibling = null;
			var openNode = document.createTextNode('');
			var dynId = xutil.generateId();
			openNode.dynId = dynId;
			openNode.xiterId = iter[0];
			openNode.xiteratorOpenNode = true;
			openNode.xiteratorElement = true;
			openNode.xiteratorStatus = 'none';
				
			var closeNode = document.createTextNode('');
			openNode.xcloseNode = closeNode;
			closeNode.xopenNode = openNode;
			closeNode.dynId = dynId;
			closeNode.xiteratorCloseNode = true;
				
			if(index < e.childNodes.length){
				sibling = e.childNodes[index];
			}
			e.insertBefore(openNode, sibling);
			e.insertBefore(closeNode, sibling);
		}
	}
}

function findNodesByProperty(property, value, like, stopWhenFound){
	var array = [];
	_findNodesByProperty(property, value, _rootElement(), like, stopWhenFound, array);
	return array;
}

function setAtt(el, attName, attValue){
    el.setAttribute(attName, attValue);
    if(attName.indexOf('on') == 0){
        xinputs.configureEvent(attName.substring(2), el);
    } else if(attName == 'data-xbind'){
        xobj.addXBind(el);
    }
}

function removeNode(node){
    if(node.xiteratorOpenNode){
        var closeNode = node.xcloseNode;
        //child of a hidden iterator
        var child = node;
        while(true){
            var sibling = child.nextSibling;
            child.remove();
            child = sibling;
            if(closeNode == child){
                child.remove();
                break;
            }
        }
    }else{
        node.remove();
    }
}

_external(setAtt);
_external(setRoot)
_external(_registerObjects);
_external(positionOffset);
_external(eachInput);
_external(getInputs);
_external(parseElement);
_external(createElement);
_external(getChildNodesByTagName);
_external(getChildElementsByName);
_external(getElementsByAttribute);
_expose(findParentWithAttribute);
_expose(parseAttributes);
_expose(removeClass);
_expose(addClass);
_expose(getChildNodesByClassName);
_expose(updateElementsAttributeValue);
_external(getChildElementsByAttribute);
_expose(getElementsByTagNames);
_expose(getRootContextElement);
_expose(getNodesByTagName);
_expose(findFirstElementByAttribute);
_external(_createInsertPoint);
_external(_createElements);
_expose(getElementsByName);
_expose(getElementById);
_expose(_setAttributesOnElement);
_expose(_setHiddenAttributesOnElement);
_expose(_isIterator);
_expose(getChildNodesByProperty);
_expose(findParentWithProperty);
_expose(_findNodes);
_expose(_checkCompId);
_expose(_getChildNodes);
_expose(_insertBefore);
_expose(findFirstIteratorWithNoneStatus);
_expose(findParentIterator);
_expose(findIteratorElement);
_expose(_getChildren);
_expose(_checkElementInContext);
_external(_registerDynAtt);
_expose(findNodesByProperty);
_expose(_rootElement);
_expose(getChildNodesByTagNameIgnoreParent);
_external(isInThisContext);
_expose(isCreatingHtml);
_expose(removeNode);