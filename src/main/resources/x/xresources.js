//this source has information about existing pages (and its templates), and scripts. Also has information about authorization
var resourceInfoMap = %resourceInfoMap%;

function _get(path){
    var info = resourceInfoMap[path];
    if(!info){
        if(window.xuser){//checking /index
            info = resourceInfoMap[path + '/index'];
        }
        if(!info){
            info = resourceInfoMap[path + '/_index'];
        }
    }
    return info;
}

//return information about a template of an existing page
function getTplInfo(path){
    var info = _get(path);
    return info ? info.templateName : null;
}

//check if a resource script exists
function isImportable(path){
    var p = path;
    if(p.endsWith(".m.js")){
        p = p.substring(0, path.length - ".m.js".length);
    }
    if(_get(p) != null){
        return true;
    }
    if(p.endsWith(".p.js")){
        p = p.substring(0, path.length - ".p.js".length);
    }
    if(_get(p) != null){
        return true;
    }
    return false;
}

//it is just a helper function to know if a user should access or not a page.
//but it does not block user to access of a page and its data. The data protection must be done in server side
function isAuthorized(path){
    var path = path.split('?')[0].replace(/\.js$/, '');
    var resourceName = path.split('/');
    resourceName = resourceName[resourceName.length - 1]
    if(resourceName.startsWith('_')){
        return true;
    }
    var info = _get(path);
    var user = window.xuser;
    var allowed = user != null;
    if (info != null && info.auth && info.needsAuthentication) {
        allowed = false;
        var roles = authProperties.allowedRoles;
        if (roles && user.role && roles.indexOf(user.role) >= 0) {
            allowed = true;
        }
        if (!allowed) {
            var functions = user.availableFunctions;
            if (functions && authProperties.allowedFunction
                    && functions.indexOf(authProperties.allowedFunction) >= 0) {
                allowed = true;
            }
        }
    }
    return allowed;
}

_expose(getTplInfo);
_expose(isImportable);
_external(isAuthorized);