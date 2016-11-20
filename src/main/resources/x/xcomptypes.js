function Attr(idVal){
    this.id = idVal;
    this.defaultValue = function(v){
        return [this, v];
    }
}
var types = {
    'string': new Attr(1),
    'number': new Attr(2),
    bool: new Attr(3),
    boundVariable: new Attr(4),
    innerHTML: new Attr(5),
    bind: new Attr(6),
    'script': new Attr(7),
    any: new Attr(15),
    mandatory: {
      'string': new Attr(8),
      'number': new Attr(9),
      bool: new Attr(10),
      bind: new Attr(13),
      innerHTML: new Attr(12),
      'script': new Attr(14),
      any: new Attr(16)
    }
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
    if (r == null || r == undefined || r.trim() == '') {
      if (!mandatory) r = defaultValue; else
      throw new Error('Property ' + prop + ' of ' + instance._compName + ' is mandatory')
    }
    return _configVal(instance, r, type, prop, evalFn);
  }
}

function _definePropertyForXScr(instance, prop, evalFn, values, type){
    thisX.defineProperty(instance, prop,
        function(){
            var result = [];
            for(var i = 0; i < values.length; i++){
                var item = values[i];
                if(typeof(item) == "string"){
                    result.push(item);
                }else{
                    result.push(evalFn(item.xscr));
                }
            }
            if(result.length > 0){
                result = result.join('');
            }else{
                result = result[0];
            }
            if(type == 'n'){
                result = parseFloat(result);
            }else if(type == 'b'){
                result = result.toUpperCase() == 'TRUE'
            }else if(type == 's'){
                result += '';
            }
            return result;
        },
        function(){}
    );
}

function _configVal(instance, r, type, prop, evalFn){
    if (type == 'scr') {
      instance[prop] = function(){
        return evalFn(r);
      }
      return r;
    }else{
        var match = r.match(/\${(.*?)}/);
        if(match){
            var values = [];
            while(match){
                values.push(r.substring(0, match.index));
                r = r.substring(match.index + match[0].length);
                values.push({xscr: match[1]});
                match = r.match(/\${(.*?)}/);
            }
            values.push(r);
            _definePropertyForXScr(instance, prop, evalFn, values, type);
        }else{
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
            } else if (type == 'a') {
                instance[prop] = r
            }
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

function configAttr(selfcomp, name, attr, defVal){
    if(attr.id == types.string.id){
        _createValProp(false, 's', selfcomp)(name, defVal);
    }else if(attr.id == types.mandatory.string.id){
        _createValProp(true, 's', selfcomp)(name);
    }else if(attr.id == types.number.id){
        _createValProp(false, 'n', selfcomp)(name, defVal);
    }else if(attr.id == types.mandatory.number.id){
        _createValProp(true, 'n', selfcomp)(name);
    }else if(attr.id == types.bool.id){
        _createValProp(false, 'b', selfcomp)(name, defVal);
    }else if(attr.id == types.mandatory.bool.id){
        _createValProp(true, 'b', selfcomp)(name);
    }else if(attr.id == types.mandatory.script.id){
        _createValProp(true, 'scr', selfcomp)(name);
    }else if(attr.id == types.script.id){
        _createValProp(false, 'scr', selfcomp)(name, defVal);
    }else if(attr.id == types.innerHTML.id){
        _createValProp(false, 's', selfcomp)(name, defVal);
    }else if(attr.id == types.mandatory.innerHTML.id){
        _createValProp(true, 's', selfcomp)(name);
    }else if(attr.id == types.boundVariable.id){
        _bindValProp(selfcomp)(name);
    }
}

function confiChildAttr(selfcomp, childName, name, attr, defVal){
    if(attr.id == types.mandatory.string.id){
        _createValProp(true, 's', selfcomp, null, null, true)(childName, name);
    }else if(attr.id == types.string.id){
        _createValProp(false, 's', selfcomp, null, null, true)(childName, name, defVal);
    }else if(attr.id == types.mandatory.number.id){
        _createValProp(true, 'n', selfcomp, null, null, true)(childName, name);
    }else if(attr.id == types.number.id){
        _createValProp(false, 'n', selfcomp, null, null, true)(childName, name, defVal);
    }else if(attr.id == types.mandatory.bool.id){
        _createValProp(true, 'b', selfcomp, null, null, true)(childName, name);
    }else if(attr.id == types.bool.id){
        _createValProp(false, 'b', selfcomp, null, null, true)(childName, name, defVal);
    }else if(attr.id == types.script.id){
        _createValProp(false, 'src', selfcomp, null, null, true)(childName, name, defVal);
    }else if(attr.id == types.mandatory.script.id){
        _createValProp(true, 'src', selfcomp, null, null, true)(childName, name);
    }else if(attr.id == types.innerHTML.id){
        _createValProp(false, 's', selfcomp, null, null, true)(childName, name, defVal);
    }else if(attr.id == types.mandatory.innerHTML.id){
        _createValProp(true, 's', selfcomp, null, null, true)(childName, name);
    }else if(attr.id == types.boundVariable.id){
        _bindValProp(selfcomp)(name);
    }
}

function configAttributes(ctx){
    var defined = ctx.defineAttributes(types);
    _prepareDefinedAttrributes(ctx, defined);
}

function _prepareDefinedAttrributes(ctx, defined){
    for(k in defined){
        if(defined[k] instanceof Attr){
            configAttr(ctx, k, defined[k], null);
        }else if(defined[k] instanceof Array && defined[k][0] instanceof Attr){
            configAttr(ctx, k, defined[k][0], defined[k][1]);
        }else{
            for(ck in defined[k]){
                var childDef = defined[k][ck];
                if(childDef instanceof Attr){
                    confiChildAttr(ctx, k, ck, childDef, null);
                }else if(childDef instanceof Array && childDef[0] instanceof Attr){
                    confiChildAttr(ctx, k, ck, childDef[0], childDef[1]);
                }
            }
        }
    }
}

_expose(configAttributes);