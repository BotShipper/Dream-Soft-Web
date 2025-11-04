let currentFileName = 'Generated';
let uploadedFile = null;

// File Upload Handlers
const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');

dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('drag-over');
});

dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('drag-over');
});

dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('drag-over');
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        handleFile(files[0]);
    }
});

function handleFileSelect(event) {
    const file = event.target.files[0];
    if (file) {
        handleFile(file);
    }
}

function handleFile(file) {
    if (!file.name.endsWith('.proto') && !file.name.endsWith('.txt')) {
        showAlert('Vui lòng chọn file .proto hoặc .txt', 'error');
        return;
    }

    uploadedFile = file;
    currentFileName = file.name.replace(/\.(proto|txt)$/, '');

    const reader = new FileReader();
    reader.onload = (e) => {
        document.getElementById('protoInput').value = e.target.result;
        showFileInfo(file.name, file.size);
        showAlert('✅ Đã load file: ' + file.name, 'success');
    };
    reader.readAsText(file);
}

function showFileInfo(name, size) {
    const fileInfo = document.getElementById('fileInfo');
    const sizeKB = (size / 1024).toFixed(2);
    fileInfo.innerHTML = `📄 <strong>${name}</strong> (${sizeKB} KB)`;
    fileInfo.style.display = 'block';
}

async function validateProto() {
    const protoContent = document.getElementById('protoInput').value;

    if (!protoContent.trim()) {
        showAlert('Vui lòng nhập nội dung Proto file hoặc upload file', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/validate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({protoContent: protoContent})
        });

        const data = await response.json();

        if (data.success) {
            showAlert('✅ Proto file hợp lệ! Bạn có thể tiến hành compile.', 'success');
        } else {
            showAlert('❌ ' + data.error, 'error');
        }
    } catch (error) {
        showAlert('❌ Lỗi kết nối đến server: ' + error.message, 'error');
    }
}

async function convertAndDownload() {
    const protoContent = document.getElementById('protoInput').value;

    if (!protoContent.trim()) {
        showAlert('Vui lòng nhập nội dung Proto file hoặc upload file', 'error');
        return;
    }

    const loading = document.getElementById('loading');
    loading.classList.add('show');

    try {
        let response;

        if (uploadedFile) {
            // Upload file directly
            const formData = new FormData();
            formData.append('file', uploadedFile);

            response = await fetch(`${API_BASE_URL}/api/proto/upload`, {
                method: 'POST',
                body: formData
            });
        } else {
            // Send content
            response = await fetch(`${API_BASE_URL}/api/proto/convert`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({protoContent: protoContent})
            });
        }

        if (response.ok) {
            // Download ZIP file
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = currentFileName + '_generated.zip';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            showAlert('✅ Compile thành công! File ZIP đã được download.<br>Bên trong có: Proto file, Java code, C# code và README.', 'success');
        } else {
            const data = await response.json();
            showAlert('❌ ' + (data.error || 'Có lỗi xảy ra khi compile'), 'error');
        }
    } catch (error) {
        showAlert('❌ Lỗi: ' + error.message + '<br><br>Đảm bảo protoc compiler đã được cài đặt trên server!', 'error');
    } finally {
        loading.classList.remove('show');
    }
}

function showAlert(message, type) {
    const alertBox = document.getElementById('alertBox');
    alertBox.className = `alert alert-${type}`;
    alertBox.innerHTML = message;
    alertBox.style.display = 'block';

    setTimeout(() => {
        alertBox.style.display = 'none';
    }, 6000);
}

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        convertAndDownload();
    }
});