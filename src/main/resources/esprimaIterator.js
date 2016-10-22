function arrayExpression(v, ident){
	var s = [];
	for(var i in v.elements){
		s.push(expression(v.elements[i], ident));
	}
	return '[' + s.join(", ") + ']';
}
function parametersCall(v, ident){
	var s = [];
	for(var i in v){
		s.push(expression(v[i], ident));
	}
	return s.join(', ');
}
function parametersDeclaration(v, ident){
	var s = [];
	for(var i in v){
		s.push(v[i].name);
	}
	return '(' + s.join(', ') + ')';
}
function property(v, ident, call){
	if(call){
		call.push(v.name);
	}
	return v.type == 'Identifier' ? v.name  : v.type == 'MemberExpression' ? memberExpression(v, ident, call) : v.raw; 
}
function memberExpression(v, ident, call){
	var s = '';
	if(v.object.type == "ThisExpression"){
		s += 'this';
	}else if(v.object.type == "Identifier"){
		if(call){
			call.push(v.object.name);
		}
		s += v.object.name;
	}else if(v.object.type == "MemberExpression"){
		s += memberExpression(v.object, ident, call);
	}else{
		s += callExpression(v.object, ident, call);
	}
	var p = property(v.property, ident, call);
	return s + (v.computed ? '[' + p + ']' : '.' + p);
}
function functionDeclaration(v, ident, makeVarDeclaration){
	var body = iterate(v.body, ident + '  ');
	return (makeVarDeclaration ? 'var ' + v.id.name + ' = ' : '') + 
		'X.$(function' + (v.id ? ' ' + v.id.name: '') + parametersDeclaration(v.params, ident) + '{' + (body ? '\n' + body + ident : '') + '})' +
		(makeVarDeclaration ? ';' : ''); 
}
function objectExpression(v, ident){
	var vIdent = ident + '  ';
	var s = [];
	for(var i in v.properties){
		var p = v.properties[i];
		s.push(vIdent + (p.key.name || p.key.raw) + ':' + expression(p.value, vIdent));
	}
	return '{\n'  + s.join(',\n') + '\n' + ident + '}';
}
function unaryExpression(v, ident){
	return v.operator + ' ' + expression(v.argument);
}
function conditionalExpression(v, ident){
	return '(' + expression(v.test) + ' ? ' + expression(v.consequent) + ' : ' + expression(v.alternate) + ')';
}
function assignmentExpression(v, binary, ident){
	return (binary ? '(' : '') + expression(v.left, ident) +  (binary ? ')' : '') + ' ' + v.operator + ' ' +  (binary ? '(' : '') + expression(v.right, ident) + (binary ? ')' : '');
}
function callExpression(v, ident, otherCall){
	var call = [];
	var s = '';
	if(v.callee.type == "Identifier"){
		call.push(v.callee.name);
		s = v.callee.name;
	}else if(v.callee.type == "MemberExpression"){
		s = memberExpression(v.callee, ident, call);
	}else if(v.callee.type == "FunctionExpression"){
		s = functionDeclaration(v.callee, ident);
	}else if(v.callee.type == "CallExpression"){
		s = callExpression(v.callee, ident, call);
	}
	var appendMeta = false;
	for(var ia in append_xmeta){
		var item = append_xmeta[ia];
		if(item.length == call.length){
			var found = true;
			for(var i = 0; i < call.length; i++){
				if(call[i] != item[i]){
					found = false;
					break;
				}
			}
			if(found){
				appendMeta = true;
				break;
			}			
		}
	}
	if(otherCall){
		for(var i in call){
			var item = call[i];
			otherCall.push(item);
		}
	}
	return s + '(' + parametersCall(v.arguments, ident) + (appendMeta ? (v.arguments.length > 0 ? ', ' : '') : '') + ')';
}
function expression(v, ident){
	if(v.type == "Identifier"){
		return v.name;
	}else if(v.type == "Literal"){
		return v.raw;
	}else if(v.type == "CallExpression"){
		return callExpression(v, ident);
	}else if(v.type == "FunctionExpression"){
		return functionDeclaration(v, ident);
	}else if(v.type == "AssignmentExpression"){
		return assignmentExpression(v, false, ident);
	}else if(v.type == "MemberExpression"){
		return memberExpression(v, ident);
	}else if(v.type == "ArrayExpression"){
		return arrayExpression(v, ident);
	}else if(v.type == "BinaryExpression" || v.type == "LogicalExpression"){
		return assignmentExpression(v, true, ident);
	}else if(v.type == "NewExpression"){
		return 'new ' + callExpression(v, ident);
	}else if(v.type == "UpdateExpression"){
		return v.argument.name + v.operator;
	}else if(v.type == "ObjectExpression"){
		return objectExpression(v, ident);
	}else if(v.type == "UnaryExpression"){
		return unaryExpression(v, ident);
	}else if(v.type == "ConditionalExpression"){
		return conditionalExpression(v, ident);
	}
	
}
function declarations(d, ident){
	var s = [];
	for(var i in d){
		s.push(d[i].id.name + (d[i].init ? " = " +  expression(d[i].init, ident) : "")); 
	}
	return "var " + s.join(", ");
}
function ifStatement(v, ident){
	var x = 'if(' + expression(v.test, ident) + ')' + processItem(v.consequent, ident + '  ');
	if(v.alternate){
		x += 'else';
		if(v.alternate.type == 'IfStatement'){
			x += ' ' + ifStatement(v.alternate, ident)
		}else{
			x += processItem(v.alternate, ident + '  ');
		}
	}
	return x;
}
function tryStatement(v, ident){
	var x = 'try';
	x += processItem(v.block, ident + '  ');
	if(v.handlers && v.handlers.length > 0){
		x += 'catch(' + v.handlers[0].param.name + ')';
		x += processItem(v.handlers[0].body, ident + '  ');	
	}
	if(v.finalizer){
		x += 'finally';
		x += processItem(v.finalizer.body, ident + '  ');	
	}
	return x;
}
function switchStatement(v, ident){
	var vIdent = ident + '  ';
	var x = 'switch(' + expression(v.discriminant, ident) + '){\n';
	for(var i in v.cases){
		var c = v.cases[i];
		x += ident + (c.test ? 'case ' + expression(c.test, ident) : 'default') + ':\n';
		for(var j in c.consequent){
			x += vIdent + processItem(c.consequent[j], ident) + '\n';			
		}
	}
	return x + ident + '}';
}
function whileStatement(v, ident){
	return 'while(' + expression(v.test, ident) + ')' + processItem(v.body, ident + '  ');
}
function doWhileStatement(v, ident){
	return 'do' + processItem(v.body, ident + '  ') + 'while(' + expression(v.test, ident) + ');';
}
function forInStatement(v, ident){
	return 'for(' + (v.left.type == 'VariableDeclaration' ? 'var ' + v.left.declarations[0].id.name : v.left.name) + ' in ' + expression(v.right) + ')' +
		processItem(v.body, ident + '  ')
}
function varOrExpr(v, ident){
	if(v.type == "VariableDeclaration"){
		return declarations(v.declarations, ident);
	}else{
		return expression(v, ident);
	}
}
function forStatement(v, ident){
	return 'for(' + (v.init ? varOrExpr(v.init) : '') + '; ' + (v.test ? expression(v.test) : '') + '; ' + (v.update ? varOrExpr(v.update) : '') + ')' +
		processItem(v.body, ident + '  ')
}
function processItem(item, ident){
	if(item.type == "VariableDeclaration"){
		return ident + declarations(item.declarations, ident) + ';';
	}else if(item.type == "FunctionDeclaration"){
		return ident + functionDeclaration(item, ident, true);
	}else if(item.type == "ExpressionStatement"){
		return ident + expression(item.expression, ident) + ';';
	}else if(item.type == "ReturnStatement"){; 
		return ident + 'return' + (item.argument ? ' ' + expression(item.argument, ident) : '') + ';';
	}else if(item.type == "IfStatement"){ 
		return ident + ifStatement(item, ident);
	}else if(item.type == "WhileStatement"){
		return ident + whileStatement(item, ident);
	}else if(item.type == "ForInStatement"){
		return ident + forInStatement(item, ident);
	}else if(item.type == "ForStatement"){
		return ident + forStatement(item, ident);
	}else if(item.type == "DoWhileStatement"){
		return ident + doWhileStatement(item, ident);
	}else if(item.type == "LabeledStatement"){
		return ident + item.label.name + ': ' + processItem(item.body, ident);
	}else if(item.type == "BlockStatement"){
		return '{\n' + iterate(item, ident + ' ') + ident + '}';
	}else if(item.type == "BreakStatement"){
		return ident + 'break' + (item.label ? ' ' + item.label.name : '') + ';';
	}else if(item.type == "ContinueStatement"){
		return ident + 'continue' + (item.label ? ' ' + item.label.name : '') + ';';
	}else if(item.type == "EmptyStatement"){
		return '';
	}else if(item.type == "SwitchStatement"){
		return ident + switchStatement(item, ident);
	}else if(item.type == "TryStatement"){
		return ident + tryStatement(item, ident);
	}
}
function iterate(parsed, ident){
	ident = ident || '';
	var x = '';
	for(var iFn in parsed.body){
		x += processItem(parsed.body[iFn], ident) + '\n';
	}
	return x;
}
function getFirstLevelFunctions(parsed){
	var x = [];
	for(var iFn in parsed.body){
		var item = parsed.body[iFn];
		if(item.type == "FunctionDeclaration"){
			if(item.id.name.indexOf('_') != 0){
				x.push(item.id.name);
			}
		}else if(item.type == "VariableDeclaration"){
			for(var i in item.declarations){
				var d = item.declarations[i];
				if(d.init && d.init.type == "FunctionExpression"){
					if(item.id.name.indexOf('_') != 0){
						x.push(d.id.name);
					}
				}
			}
		}
	}
	return x.join("|");
}

function getFirstLevelVariables(parsed){
	var x = [];
	for(var iFn in parsed.body){
		var item = parsed.body[iFn];
		if(item.type == "VariableDeclaration"){
			for(var i in item.declarations){
				var d = item.declarations[i];
				if(!d.init || d.init.type != "FunctionExpression"){
					x.push(d.id.name);
				}
			}
		}
	}
	return x.join(" ");
}
