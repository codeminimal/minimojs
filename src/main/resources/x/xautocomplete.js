
var autocompleteInstances = {};
//retrieves the autocomplete component
function getAutocomplete(param){
	if(typeof(param) == 'string'){
		return autocompleteInstances[param];
	}else{
		if(!param._xautocomplete){
			_startAutoComplete(param, param.getAttribute("data-xac-class"), param.getAttribute("data-xac-selectedclass"));
		}
		return param._xautocomplete;
	}
}

//prepares the styles of the dropdown
function _createStyleObj(autocompleteClass, selectedItemClass){
	var result = {
			styleAutocomplete: '',
			styleAutocompleteUl: '',
			styleAutocompleteLi: '',
			styleAutocompleteA: '',
			styleAutocompleteASel: '',
			autocompleteClass: autocompleteClass
	}
	if(!autocompleteClass){
		result.styleAutocomplete = 'position: fixed;' +
			'border-left: 1px solid #a3bac8;' +
			'border-right: 1px solid #a3bac8;' +
			'border-bottom: 1px solid #a3bac8;' +
			'border-top: 1px solid #a3bac8;' +
			'margin-top: -3px;' +
			'padding-top: 3px;' +
			'z-index: 9999;' +
			'text-align: left;' +
			'background-color: #f6fbfd;';
		
		result.styleAutocompleteUl = 'margin: 5px;padding: 0px;';

		result.styleAutocompleteLi = 'list-style: none;' +
			'display: block;' +
			'position: relative;' +
			'padding: 5px;' +
			'z-index: 1;';

		result.styleAutocompleteA = 
			'font-family: Lucida Grande, Lucida Sans, Arial, sans-serif;' +
			'display: block;' +
			'text-decoration: none;' +
			'position: relative;' +
			'z-index: 0;' +
			'font-size: 12px;' +
			'cursor: pointer;';

		result.autocompleteClass = '';
	}
	result.selectedItemStyle = '';
	result.unSelectedItemStyle = '';
	result.selectedItemClass = selectedItemClass;
	if(!selectedItemClass){
		result.selectedItemClass = '';
		result.selectedItemStyle = 'background-color: #0d93b6;color: #f6fbfd;'
		result.unSelectedItemStyle = 'color: #869aa5;';
	}else{
		result.selectedItemStyle = '';
		result.unSelectedItemStyle = '';
	}
	return result;
}

//create the autocomplete componente instance
//param: the element
//autocompleteClass: the class to be used as background of autocomplete panel
//selectedItemClass: the class to be used on the selected element
function _startAutoComplete(param, autocompleteClass, selectedItemClass) {
	return new function(){
		
		//local function to retrive the element
		function __getInput(){
			return typeof(param) == 'string' ? X._(param) : param;
		}
		
		__getInput().xacId = __getInput().xacId || (__getInput().getAttribute("id") ? 
				("xaci_" + __getInput().getAttribute("id")) : thisX.generateId());
		
		//local function to retrive the autocomplete element panel
		function __getAutoCompleteElementPanel(){
			return document.getElementById('_xautocomplete_' + __getInput().xacId);
		}
		
		xlog.debug('autocomplete', 'Started for input ' + __getInput().xacId);
		var sourceFunction = null;
		var thisObj = this;
		//add to autocomplete instances map
		autocompleteInstances[__getInput().xacId] = thisObj;
		__getInput()._xautocomplete = thisObj;
		
		//function to config the function that gets the description of elements when in panel
		this.setDescriptionFunction = function(fndesc){
			this._fndesc = stripFunction(fndesc);
		};
		
		//function to config the function that gets the description when already selected on input
		this.setFinalDescriptionFunction = function(fn){
			var fnfinaldesc = stripFunction(fn);
			this._fnfinaldesc = fnfinaldesc;
			__getInput()._xautocompletefnfinaldesc = fnfinaldesc;
		};
		
		//function the add a listener to autocomplete triggered when an item is choosen
		//onSetValue can change the value of the autocomplete. Ex: The final value is string, but can be changed for a object
		this.setOnSetValueFunction = function(f){
			this.onSetValue = stripFunction(f);
		};
		
		//function to config the source elements list
		this.setSourceList = function(list){
			sourceFunction = function(typed){
				if(typed != ''){
					var countFound = 0;
					var selList = [];
					for (var i in list) {
						if (thisObj._fnfinaldesc(list[i]).toUpperCase().indexOf(typed.toUpperCase()) >= 0) {
							countFound ++;
							selList.push(list[i]);
						}
						if(countFound > 2){
							break;
						}
					}
					createDropDown(selList);
				}else{
					__getAutoCompleteElementPanel().innerHTML = '';
					xdom.setAtt(__getAutoCompleteElementPanel(), 'data-xsel', null);
					__getAutoCompleteElementPanel().style.display = 'none';
				}
			};
		};
		
		//function to define a function where the elements will be returned from
		this.setSourceFunction = function(fn){
			fn = stripFunction(fn);
			sourceFunction = function(typed){
				fn(typed, createDropDown, thisObj);
			};
		};
		
		//function to externally set the string value of the autocomplete
		this.setInputValue = function(v){
			var obj;
			if(this.onSetValue){
				obj = this.onSetValue(v);
			}else{
				obj = v;
			}
			this.setValue(obj);
		};
		
		//function to externally set the final value of the autocomplete
		this.setValue = function(obj){
			var finalDesc = '';
			if(obj){
				finalDesc = thisObj._fnfinaldesc(obj);
			}
			__getInput()._xautocompletevalue = obj;
			__getInput().value = finalDesc || '';
			xobj.updateObject(__getInput());
			X$._update();
		};
		
		//returns the autocomplete final current value
		this.getValue = function(){
			return __getInput()._xautocompletevalue;
		};
		
		var fixInterval = null;
		var timeout = null;
		var input = __getInput();
		input._xcurrentList = null;
		
		//set the autocomplete value to be the selected item
		var __selectCurrent = function(){
			var index = parseInt(__getAutoCompleteElementPanel().getAttribute('data-xsel'));
			var input = __getInput();
			var currentList = input._xautocompletecurrentList;
			var obj = currentList[index];
			var finalDesc = input._xautocompletefnfinaldesc(obj);
			__getInput().value = finalDesc;
			__getInput()._xautocompletevalue = obj;
			__getAutoCompleteElementPanel().innerHTML = '';
			__getAutoCompleteElementPanel().style.display = 'none';
			xobj.updateObject(__getInput());
			X$._update();
		};
		//style
		var styleVal = _createStyleObj();
		
		//create dropdown function
		var createDropDown = function(list){
			input._xautocompletecurrentList = list;
			var html = '';
			for (var i in list) {
				html += '<li style="' + styleVal.styleAutocompleteLi + '"><span style="' + styleVal.styleAutocompleteA + '" data-xindex="' + i + '">'
						+ thisObj._fndesc(list[i]) + '</span></li>';
			}
			var panel = __getAutoCompleteElementPanel();
			panel.innerHTML = html == '' ? '' : '<ul style="' + styleVal.styleAutocompleteUl + '">' + html + '</ul>';
			if(html != ''){
				setTimeout(function(){
					//workaround
					var fixPos = function(){
						setTimeout(function(){
							var b = __getInput().getBoundingClientRect();
							panel.style.left = b.left + 'px';
							panel.style.top = b.bottom + 'px';
							panel.style.zIndex = xutil.highestZIndex() + 1;
							if(panel.offsetLeft != __getInput().offsetLeft && panel.offsetTop != __getInput().offsetTop + __getInput().offsetHeight){
								fixPos();
							}
						},1);
					};
					fixPos();
					fixInterval = setInterval(function(){
						try{
							var b = __getInput().getBoundingClientRect();
							panel.style.left = b.left + 'px';
							panel.style.top = b.bottom + 'px';		
						}catch(e){
							clearInterval(fixInterval);
						}
					},100);
					setTimeout(function(){
						panel.style.display = 'block';
						panel.style.width = __getInput().offsetWidth + 'px';
					},200);
						
				},10);
				
				_autocomplete_select();
				
				var aList = xdom.getChildNodesByTagNameIgnoreParent(__getAutoCompleteElementPanel(), 'span', true);
				
				function mouseover(e){
					if(!e){
						e = window.event;
					}
					var index = parseInt(e.target.getAttribute('data-xindex'));
					_autocomplete_select(index);
				}
				function click(e){
					__selectCurrent();
				}
				xutil.each(aList, function(item){
					item.onmouseover = mouseover;
					item.onclick = click;
				});
			}else{
				xdom.setAtt(__getAutoCompleteElementPanel(), 'data-xsel', null);
				if(fixInterval){
					clearInterval(fixInterval);
				}
				__getAutoCompleteElementPanel().style.display = 'none';
			}
		};
		
		//create the panel
		var dv = xdom.createElement('div');
		xdom.setAtt(dv, "class", styleVal.autocompleteClass);
		xdom.setAtt(dv, "style", styleVal.styleAutocomplete);
		xdom.setAtt(dv, "id", '_xautocomplete_' + __getInput().xacId);
		document.body.appendChild(dv);
		xdom.setAtt(__getInput(), "autocomplete", "off");
		dv.style.display = 'none'
		
		//select function
		var _autocomplete_select = function(index){
			var elements = xdom.getChildNodesByTagNameIgnoreParent(__getAutoCompleteElementPanel(), 'span', true);
			xutil.each(elements, function(item){
				item.className = "";
				xdom.setAtt(item.parentNode, "style", styleVal.styleAutocompleteA + styleVal.unSelectedItemStyle);
			});
			if(!index || index < 0)
				index = 0;
			if(index >= elements.length){
				index = elements.length - 1;
			}
			xdom.setAtt(__getAutoCompleteElementPanel(), 'data-xsel', index);
			elements[index].className = styleVal.selectedItemClass;
			xdom.setAtt(elements[index].parentNode, "style", styleVal.styleAutocompleteA + styleVal.selectedItemStyle);
			
		};
		if(fixInterval){
			clearInterval(fixInterval);
		}
		var _startBox = function(){
			if (timeout) {
				clearTimeout(timeout);
			}
			timeout = setTimeout(function() {
				if(sourceFunction)
					sourceFunction(__getInput().value);
			}, 200);
		};

		//key up
		__getInput().addEventListener('keyup', function(event) {
			var k = null;
			if (window.event) {
				event = window.event;
				k = event.keyCode;
			} else if (event.which) {
				k = event.which;
			}
			var index = parseInt(__getAutoCompleteElementPanel().getAttribute('data-xsel'));
			if (k == 13) {
				__selectCurrent();
			} else if(k == 38){
				_autocomplete_select(index-1);
			} else if(k == 40){
				_autocomplete_select(index+1);
			} else {
				_startBox();
			}
		});
		//on focus
		__getInput().addEventListener('focus', function(event) {
			if (window.event) {
				event = window.event;
			}
			setTimeout(function() {
				_startBox();
			}, 500);
		});
		//blur
		__getInput().addEventListener('blur', function(event) {
			if (window.event) {
				event = window.event;
			}
			var input = __getInput();
			var currentList = input._xautocompletecurrentList;
			var index = __getAutoCompleteElementPanel().getAttribute('data-xsel');
			if(!index){
				for(var i in currentList){
					if(__getInput().value.toUpperCase() == input._xautocompletefnfinaldesc(currentList[i]).toUpperCase()){
						index = i;
						break;
					}
				}
			}
			
			if(currentList && currentList.length > 0 && index != null){
				thisObj.setValue(currentList[index]);
			}else{
				thisObj.setValue(null);
			}
			setTimeout(function() {
				if(fixInterval){
					clearInterval(fixInterval);
				}
				__getAutoCompleteElementPanel().style.display = 'none';
			}, 50);
		});
		
		//set the autocomplete function if in attributes. This function retrives the list of elements
		var autocompleteFunction = __getInput().getAttribute("data-xac-fn");
		if(autocompleteFunction){
			this.setSourceFunction(createAutoCompleteFunction(__getInput(), autocompleteFunction));
		}
		//set the item description function if in attributes. This function shows values on list
		var itemDescriptionFn = createDescFunction(input, "data-xac-desc-scr");
		if(itemDescriptionFn){
			this.setDescriptionFunction(itemDescriptionFn);
		}
		//set the final description script if in attributes. Shows the value on input
		var descriptionFn = createDescFunction(input, "data-xac-desc-input-scr");
		if(descriptionFn){
			this.setFinalDescriptionFunction(descriptionFn);
		}else{
			//set the final description function if in attributes. Shows the value on input
			descriptionFn = __getInput().getAttribute("data-xac-desc-fn");
			if(descriptionFn){
				this.setFinalDescriptionFunction(createDescriptionFunction(__getInput(), descriptionFn));
			}
		}
	}
};

function createAutoCompleteFunction(input, fnName){
	return function(typed, callback, thisObj){
		var fn = thisX.eval(fnName);
		if(fn){
			fn(typed, callback, thisObj);
		}
	}
}

function createDescriptionFunction(input, fnName){
	return function(item){
		var fn = thisX.eval(fnName);
		if(fn){
			return stripFunction(fn)(item);
		}
		return null;
	}
}

function createDescFunction(input, typeFn){
	if(input.getAttribute(typeFn)){
		var fn = function(item){
			var scriptFn = input.getAttribute(typeFn);
			var varName = input.getAttribute('data-xac-itemvar');
			window._x_temp_autocomplete_item = item;
			var result = thisX.eval('var ' + varName + ' = window._x_temp_autocomplete_item;' + scriptFn + ';');
			delete window._x_temp_autocomplete_item;
			return result;
		}
		return fn;
	}
}

_external(getAutocomplete);