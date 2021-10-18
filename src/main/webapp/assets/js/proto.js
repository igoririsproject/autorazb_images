/**
 * Created By Igor Zakolodyazhyi
 */

function parseObject(data) {
	if (data == null) return data;
	
    if (typeof data === "string" && data.startsWith("{")) {
        try {
            data = JSON.parse(data);
            return data;
        } catch (e) { 
        	console.log(e);
        	return null
        }
    } else if (typeof data === "string" && data.startsWith("[")) {
        try {
            if (data.indexOf("{") >= 0 && data.indexOf("}") >= 0) {
                return JSON.parse(data);
            } else if (data.length > 3 && data.indexOf(",") > 1) {
            	if (data.indexOf('"') === 1 && data.match(/"/g).length >= 2) {
                    return JSON.parse(data);
            	} else return data.replace("[", "").replace("]", "").split(",");
            } else return JSON.parse(data);
        } catch (e) { 
        	console.log(e);
        	return null
        }
    } else return data;
}

(function(){
	if (!String.prototype.startsWith) {
        Object.defineProperty(String.prototype, 'startsWith', {
            enumerable: false,
            configurable: false,
            writable: false,
            value: function (searchString, position) {
                position = position || 0;
                return this.lastIndexOf(searchString, position) === position;
            }
        });
    }  
}())