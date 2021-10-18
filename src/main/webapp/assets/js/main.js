/**
 * Created by Igor Zakolodyazhnyi
 */
const API_URL = 'http://localhost:8080/image';
const PROCESS_URL = API_URL + '/upload';
const REMOVE_URL = API_URL + '/process';
const DEBUG = true;

const UploadUI = function () {
	const _this = this;
	this.container = '.image__processor__inner-upload';
	this.files = {};
	
	this.controls = {
		uploadButton: '.image__processor__inner-upload-button button',
		processButton: '.image__processor__inner-upload-control button',
		clearButton: '.image__processor__inner-upload-control button:nth-child(2)',
		removeButton: '.image__processor__inner-upload-control button:nth-child(3)',
		fileInput: '[type="file"]',
		dropArea: '.image__processor__inner-upload-drop',
		list: '.image__processor__inner-upload-list'
	};
	
	this.clear = function () {
		_this.files = {};
		_this.controls.list.innerHTML = '';
		_this.controls.processButton.classList.add('hidden');
		_this.controls.clearButton.classList.add('hidden')
	};
	
	this.fileAddHandler = function (name, files) {
		if (name == null) name = 'files';
		if (typeof _this.files != 'object' || _this.files == null) _this.files = {};
		if (typeof _this.files[name] == 'undefined') _this.files[name] = [];
		let l = files.length;	
		
		for (let i=0; i<l; i++) {
			_this.files[name].push(new Blob([files[i]], {type: files[i].type}));
			
			if (files[i].type.indexOf('image') >= 0) {
				const img = document.createElement("img");
				_this.controls.list.appendChild(img);
				const fr = new FileReader();
				fr.image = img;
		        fr.onload = function () { this.image.src = this.result; };
		        fr.readAsDataURL(files[i]);
			}
		}
		
		_this.controls.processButton.classList.remove('hidden');
		_this.controls.clearButton.classList.remove('hidden')
	};
	
	(function() {
		_this.container = document.querySelector(_this.container);
		if (_this.container == null) throw new Error('Error initializing UI, container is null');
		for (let i in _this.controls) _this.controls[i] = _this.container.querySelector(_this.controls[i]);
		
		/*
		_this.controls.dropArea.ondragenter = function (ev) { 
			ev.preventDefault();
			this.classList.add('dragenter')
		}
		
		_this.controls.dropArea.ondragleave = function (ev) { 
			ev.preventDefault();
			this.classList.remove('dragenter') 
		}
		
		_this.controls.dropArea.ondragover = function (ev) { console.log(ev); ev.preventDefault() }
		
		_this.controls.dropArea.ondrop = function (ev) {
			ev.preventDefault();
			this.classList.remove('dragenter');
			_this.fileAddHandler(this.getAttribute('data-name'), ev.dataTransfer.files)
		}
		
		_this.controls.fileInput.addEventListener('change', function (ev) {
			_this.fileAddHandler(this.getAttribute('name'), this.files)
		});
		
		_this.controls.uploadButton.onclick = function (ev) {
			ev.preventDefault();
			ev.stopPropagation();
			this.previousElementSibling.click();
		}
		*/
		
		_this.controls.processButton.onclick = function (ev) {
			const el = this;
			el.text = el.textContent;
			ev.preventDefault();
			ev.stopPropagation();
			
			window.$ajax.sendRequest(PROCESS_URL, 'POST', { 'images': document.querySelector('textarea').value })
				.then((res) => {
					const r = parseObject(res.responseText);
					
					if (r != null) {
						if (r.success) {
							el.textContent = r.message;
						} else el.textContent = 'Error';
					} else el.textContent = 'Error';
					
					setTimeout(() => { el.textContent = el.text }, 5000);
				})
				.catch((err) => {
					el.textContent = 'Error';
					setTimeout(() => { el.textContent = el.text }, 5000);
				});
		}
		
		_this.controls.clearButton.onclick = function (ev) {
			ev.preventDefault();
			ev.stopPropagation();
			_this.clear();
		}
		
		_this.controls.removeButton.onclick = function (ev) {
			ev.preventDefault();
			ev.stopPropagation();
			
			window.$ajax.sendRequest(REMOVE_URL, 'POST')
				.then((res) => {
					
				})
				.catch((err) => {
					
				});
		}
		
		console.log(_this);
	}());
}

const AjaxWrap = function () {
	const _this = this;
	
	this.sendRequest = function (url, method, data, progress) {
		return new Promise((reject, resolve) => {
			const r = _this.prepareRequest(url, method);
			r.onload = (ev) => { resolve(ev.target) };
			r.onerror = (ev) => { reject (ev.target) };
			if (typeof progress == 'function') r.upload.addEventListener("progress", progress, false);
			const fd = data != undefined && data instanceof HTMLElement ? new FormData(data) : new FormData();
			
			if (typeof data == 'object' && data != null) {
				for (let i in data) {
					if (typeof data[i] == 'string' || data[i] instanceof Blob) {
						fd.append(i, data[i])
						continue;
					}
					
					if (data[i] instanceof Array) {
						const l = data[i].length;
						for (let j=0; j<l; j++) fd.append(i, data[i][j]);
						continue;
					}
					
					fd.append(i, typeof data[i] == 'object' && data[i] != null ? JSON.stringify(data[i]) : String.valueOf(data[i]))
				}
			}
			
			r.send(fd);
		});
	};
	
	this.prepareRequest = function (url, method) {
		const r = new XMLHttpRequest();
		r.open(method, url, true);
		return r;
	};
};

(function(){
	window.$UploadUI = new UploadUI();
	window.$ajax = new AjaxWrap();
}())