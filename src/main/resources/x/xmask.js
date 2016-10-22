var _default_decimal_separator = ',';
var _default_thousand_separator = '.';

%currency_formatter%

function currencyFormatter(decimals, decSep, thousSep){
	return {
		format: function(v){
			var n = v, 
			    c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals, 
			    d = decSep == undefined ? _default_decimal_separator : decSep, 
			    t = thousSep == undefined ? _default_thousand_separator : thousSep, 
			    s = n < 0 ? "-" : "", 
			    i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "", 
			    j = (j = i.length) > 3 ? j % 3 : 0;
			return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
		}
	};
}

var _default_formatter = currencyFormatter();

//removes all the non number chars from a string
function getNumber(s, returnEmpty){
	var def = returnEmpty ? '' : '0';
	var r = '';
	for(var i in s){
		if('0123456789'.indexOf(s[i]) >= 0){
			r += s[i];
		}
	}
	return r == '' ? def : r;
}

//format a input of a event and applies the float mask
function _maskFloat(kc, e){
	var unformated = getNumber(e.target.value);
	if(kc == 8){
		unformated = unformated.substring(0, unformated.length -2);
	}
	if(kc >= 48 && kc <= 57){
		unformated += kc - 48;
	} else if(kc >= 96 && kc <= 105){
		unformated += kc - 96;
	}
	e.target.value = _default_formatter.format(parseInt(unformated)/100);
	return false;
}

//format a input of a event and applies the int mask
function _maskInt(kc, e){
	return (kc >= 48 && kc <= 57) || (kc >= 96 && kc <= 105) || kc == 39 || kc == 37 || kc == 8 || kc == 46;
}

function _checkNumberTyped(kc, val, maxSize){
	if(kc >= 48 && kc <= 57 && val.length < maxSize){
		val += (kc - 48);
	} else if(kc >= 96 && kc <= 105 && val.length < maxSize){
		val += (kc - 96);
	}
	return val;
}

function applyDateMask(unformated, format){
	var formatWithoutOtherChars = format.match(/\b(mm|MM|yyyy|yy|dd|HH|hh|mm|ss)\b/g).join("");
	var index = 0;
	var indexInUnformatted = 0;
	var formatted = "";
	while(indexInUnformatted < unformated.length){
		var sub = format.substring(index, index + 4);
		if(sub.indexOf('mm') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('MM') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('dd') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('yyyy') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 4);
			indexInUnformatted += 4;
			index += 4;
		}else if(sub.indexOf('yy') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('HH') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('hh') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else if(sub.indexOf('ss') == 0){
			formatted += unformated.substring(indexInUnformatted, indexInUnformatted + 2);
			indexInUnformatted += 2;
			index += 2;
		}else{
			formatted += format[index];
			index++;
		}
	}
	return formatted;
}

//format a input of a event and applies the date mask
function _maskDate(kc, e){
	var format = getMask(e.target);
	var unformated = getNumber(e.target.value, true);
	if(kc == 8){
		unformated = unformated.substring(0, unformated.length -1);
	}
	var formatWithoutOtherChars = format.match(/\b(mm|MM|yyyy|yy|dd|HH|hh|mm|ss)\b/g).join("");
	var len = formatWithoutOtherChars.length;
	unformated = _checkNumberTyped(kc, unformated, len);
	
	e.target.value = applyDateMask(unformated, format);
	return false;
}

//format a input of a event and applies the ifloat mask
function _maskIFloat(kc, e){
	var splitted = e.target.value.split(',');
	return kc == 8 || ((splitted.length == 1 || splitted[1].length < 2) && ((kc >= 48 && kc <= 57) || (kc >= 96 && kc <= 105) || kc == 39 || kc == 37 || kc == 46
		|| ((kc == 188 || kc == 108) && e.target.value.indexOf(',') < 0)));
}

//format a input of a event and applies mask depending on the xtype
function mask(e){
	if (!e) e = window.event;
	if(e.type == 'keydown' && !e._xmasked){
		e._xmasked = true;
		var kc = e.keyCode;
		if(kc != 9){
			var xtype = e.target.getAttribute("data-xtype");
			if(xtype == 'ifloat'){
				return _maskIFloat(kc, e);
			}else if(xtype == 'float'){
				return _maskFloat(kc, e);
			}else if(xtype == 'int' || xtype == 'percent'){
				return _maskInt(kc, e);
			}else if(xtype == 'date' || xtype == 'datetime' || xtype == 'time'){
				return _maskDate(kc, e);
			}
		}
	}
}

function getDefaultFormatter(){
	return _default_formatter;
}

function parseDate(strDt, format, type){
	if(!strDt){
		return '';
	}
	var dt = new Date(strDt);
	if(!isNaN(dt.getYear())){
		return dt;
	}
	dt = new Date();
	function parse(mask, method, plus){
		var index = format.indexOf(mask);
		if(mask == 'yy'){
			plus += parseInt(new Date().getUTCFullYear().toString().substring(0, 2)) * 100;
		}
		if(index >= 0){
			method.call(dt, plus + parseInt(strDt.substring(index, index + mask.length)));	
			return true;
		}else{
			method.call(dt, type == 'time' && mask[0] == 'y' ? 1970 : 0);
			return false;
		}
	}
	parse('dd', dt.setDate, 0);
	parse('MM', dt.setMonth, -1);
	if(!parse('yyyy', dt.setYear, 0)){
		parse('yy', dt.setYear, 0);
	}
	parse('HH', dt.setHours, 0);
	parse('mm', dt.setMinutes, 0);
	parse('ss', dt.setSeconds, 0);
	dt.setMilliseconds(0);
	return dt;
}

function padR(str, c, len){
	if(!str){
		str = "";
	}else{
		str = str + "";
	}
	while(str.length < len){
		str += c;
	}
	return str;
}

function padL(str, c, len){
	if(!str){
		str = "";
	}else{
		str = str + "";
	}
	while(str.length < len){
		str = c + str;
	}
	return str;
}

function formatDate(d, format){
	if(!d){
		return "";
	}
	if(typeof(d) == 'string'){
		d = new Date(d);
	}
	format = format.replace(/dd/g, padL(d.getDate(), '0', 2));
	format = format.replace(/MM/g, padL(d.getMonth() + 1, '0', 2));
	format = format.replace(/yyyy/g, padL(d.getFullYear(), '0', 4));
	format = format.replace(/yy/g, padL(d.getFullYear(), '0', 4).substring(2, 4));
	format = format.replace(/HH/g, padL(d.getHours(), '0', 2));
	format = format.replace(/mm/g, padL(d.getMinutes(), '0', 2));
	format = format.replace(/ss/g, padL(d.getSeconds(), '0', 2));
	return format;
}

//returns the mask to be applied. 
//Try to get the mask from an attribute on input or from the default format if no mask attribute is found 
function getMask(input){
	return input.getAttribute('data-xtype') == 'date' ? input.getAttribute("data-xdateformat") || '%defaultdateformat%' :
		input.getAttribute('data-xtype') == 'datetime' ? input.getAttribute("data-xdatetimeformat") || '%defaultdatetimeformat%' : 
			input.getAttribute("data-xtimeformat") || '%defaulttimeformat%';
}

_external(currencyFormatter);
_external(mask);
_expose(getNumber);
_expose(getMask);
_expose(getDefaultFormatter);
_external(formatDate);
_external(parseDate);
_external(applyDateMask);
_external(padL);
_external(padR);