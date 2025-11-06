function showStatus(message, type = 'info') {
    const status = document.getElementById('status');
    status.textContent = message;
    status.className = `status show ${type}`;
    setTimeout(() => {
        status.classList.remove('show');
    }, 3000);
}

async function doPreview() {
    const proto = document.getElementById('proto').value.trim();
    if (!proto) {
        showStatus('Please enter proto definition', 'error');
        return;
    }

    const lang = document.getElementById('lang').value;
    document.getElementById('output').textContent = '// Generating code...';

    try {
        const fd = new FormData();
        fd.append('prototext', proto);
        fd.append('lang', lang === 'both' ? 'java' : lang);

        const res = await fetch('/api/proto/preview', {method: 'POST', body: fd});

        if (!res.ok) {
            const errorText = await res.text();
            document.getElementById('output').textContent = `// Error:\n${errorText}`;
            showStatus('Generation failed', 'error');
            return;
        }

        const json = await res.json();
        document.getElementById('output').textContent = json.code;
        showStatus('Code generated successfully!', 'success');
    } catch (error) {
        document.getElementById('output').textContent = `// Error: ${error.message}`;
        showStatus('Network error occurred', 'error');
    }
}

document.getElementById('gen').addEventListener('click', doPreview);

document.getElementById('copyBtn').addEventListener('click', async () => {
    const output = document.getElementById('output').textContent;
    const btn = document.getElementById('copyBtn');

    // Prevent spam clicking - check if already copying
    if (btn.disabled) {
        return;
    }

    // Check if there's actual generated code (not just placeholder message)
    if (output.includes('Click "Generate"') || output === '// Generating code...') {
        showStatus('No code to copy yet', 'error');
        return;
    }

    // Disable button to prevent spam
    btn.disabled = true;
    btn.textContent = '⏳ Copying...';

    try {
        // Try modern clipboard API first
        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(output);
        } else {
            // Fallback method for browsers/environments that don't support clipboard API
            const textarea = document.createElement('textarea');
            textarea.value = output;
            textarea.style.position = 'fixed';
            textarea.style.left = '-999999px';
            textarea.style.top = '-999999px';
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();

            const successful = document.execCommand('copy');
            document.body.removeChild(textarea);

            if (!successful) {
                throw new Error('Copy command failed');
            }
        }

        btn.textContent = '✅ Copied!';
        btn.classList.add('copied');
        showStatus('Code copied to clipboard!', 'success');

        // Re-enable after showing success message
        setTimeout(() => {
            btn.textContent = '📋 Copy';
            btn.classList.remove('copied');
            btn.disabled = false;
        }, 1500);
    } catch (error) {
        // If all methods fail, show manual copy instruction
        showStatus('Please select and copy manually (Ctrl+C)', 'error');

        // Optionally, select the text for user to copy manually
        const outputElement = document.getElementById('output');
        const range = document.createRange();
        range.selectNodeContents(outputElement);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);

        // Re-enable button immediately after error
        btn.textContent = '📋 Copy';
        btn.disabled = false;
    }
});

document.getElementById('downloadZip').addEventListener('click', async () => {
    const proto = document.getElementById('proto').value.trim();
    if (!proto) {
        showStatus('Please enter proto definition', 'error');
        return;
    }

    const lang = document.getElementById('lang').value;

    try {
        const fd = new FormData();
        fd.append('prototext', proto);
        fd.append('lang', lang);

        const res = await fetch('/api/proto/generate', {method: 'POST', body: fd});

        if (!res.ok) {
            showStatus('Generate failed', 'error');
            return;
        }

        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'generated.zip';
        a.click();
        URL.revokeObjectURL(url);

        showStatus('Zip file downloaded!', 'success');
    } catch (error) {
        showStatus('Download failed', 'error');
    }
});

// Auto-resize textarea
const textarea = document.getElementById('proto');
textarea.addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = this.scrollHeight + 'px';
});