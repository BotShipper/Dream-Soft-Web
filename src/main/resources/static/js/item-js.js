// State
let currentGame = 'htht';
let selectedRewards = {};
let allRewards = {};

// Initialize selected rewards for each game
Object.keys(GAMES).forEach(game => {
    selectedRewards[game] = [];
    allRewards[game] = [];
});

// Render game buttons
function renderGameButtons() {
    const gameGrid = document.getElementById('gameGrid');
    let html = '';

    Object.keys(GAMES).forEach(gameKey => {
        const game = GAMES[gameKey];
        const activeClass = gameKey === currentGame ? 'active' : '';
        html += `
                <div class="game-btn ${gameKey} ${activeClass}" onclick="switchGame('${gameKey}')">
                    <div class="game-icon">${game.icon}</div>
                    <div>${game.name}</div>
                </div>
            `;
    });

    gameGrid.innerHTML = html;
}

// Switch game
function switchGame(gameKey) {
    if (currentGame === gameKey) return;

    currentGame = gameKey;
    const game = GAMES[gameKey];

    // Update UI
    document.body.className = gameKey + '-theme';
    document.getElementById('gameTitle').textContent = `Chọn Quà - ${game.name}`;

    // Update badges
    document.querySelectorAll('.current-game-badge').forEach(badge => {
        badge.textContent = game.name;
        badge.style.backgroundColor = game.color;
    });

    // Update active game button
    document.querySelectorAll('.game-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    const activeBtn = document.querySelector(`.game-btn.${gameKey}`);
    if (activeBtn) {
        activeBtn.classList.add('active');
        // Scroll into view if needed
        activeBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // Load game data if not loaded
    if (allRewards[gameKey].length === 0) {
        loadGameData(gameKey);
    }

    // Clear search
    document.getElementById('rewardSearch').value = '';
    document.getElementById('rewardSearchResults').classList.remove('show');

    // Render current game's selected rewards
    renderSelectedRewards();
    updateJsonOutput();

    showToast(`Đã chuyển sang ${game.name}`, 'info');
}

// Load game data
function loadGameData(gameKey) {
    const game = GAMES[gameKey];

    fetch(game.api)
        .then(res => res.json())
        .then(rewards => {
            allRewards[gameKey] = rewards;
        })
        .catch(err => {
            showToast(`Lỗi load dữ liệu ${game.name}: ${err.message}`, 'danger');
            // Use mock data for demo
            allRewards[gameKey] = generateMockRewards(gameKey);
        });
}

// Generate mock rewards for demo
function generateMockRewards(gameKey) {
    const mockData = {
        pirate: [
            {infoId: 101, name: 'Trái ác quỷ', description: 'Trái ác quỷ hệ Logia'},
            {infoId: 102, name: 'Kiếm đen', description: 'Kiếm huyền thoại'},
            {infoId: 103, name: 'Mũ rơm', description: 'Mũ của Luffy'}
        ],
        dragon: [
            {infoId: 201, name: 'Ngọc rồng 1 sao', description: 'Viên ngọc rồng đầu tiên'},
            {infoId: 202, name: 'Áo Quy Trang', description: 'Trang phục của Goku'},
            {infoId: 203, name: 'Đậu thần', description: 'Hồi phục năng lượng'}
        ],
        ninja: [
            {infoId: 301, name: 'Kunai', description: 'Vũ khí ném'},
            {infoId: 302, name: 'Băng trán Konoha', description: 'Băng trán làng lá'},
            {infoId: 303, name: 'Binh lương hoàn', description: 'Thuốc hồi phục'}
        ],
        pokemon: [
            {infoId: 401, name: 'Poké Ball', description: 'Bắt Pokemon'},
            {infoId: 402, name: 'Thuốc hồi máu', description: 'Hồi phục HP'},
            {infoId: 403, name: 'Rare Candy', description: 'Tăng cấp độ'}
        ]
    };

    return mockData[gameKey] || [];
}

// Load initial game data
loadGameData('htht');

// Reward search
const rewardSearchInput = document.getElementById('rewardSearch');
const rewardSearchResults = document.getElementById('rewardSearchResults');

rewardSearchInput.addEventListener('input', function() {
    const query = this.value.trim().toLowerCase();

    if (query.length === 0) {
        rewardSearchResults.classList.remove('show');
        return;
    }

    const filtered = allRewards[currentGame].filter(reward =>
        reward.name.toLowerCase().includes(query) &&
        !selectedRewards[currentGame].some(r => r.infoId === reward.infoId)
    );

    displayRewardResults(filtered);
});

function displayRewardResults(rewards) {
    if (rewards.length === 0) {
        rewardSearchResults.innerHTML = '<div class="search-item text-muted">Không tìm thấy quà</div>';
        rewardSearchResults.classList.add('show');
        return;
    }

    let html = '';
    rewards.forEach(reward => {
        html += `
                <div class="search-item" onclick="showQuantityPrompt(${reward.infoId}, '${reward.name.replace(/'/g, "\\'")}')">
                    <div class="reward-item">
                        <span class="reward-icon">${reward.infoId}</span>
                        <div>
                            <strong>${reward.name}</strong>
                            <div class="text-muted small">${reward.description || ''}</div>
                        </div>
                    </div>
                </div>
            `;
    });

    rewardSearchResults.innerHTML = html;
    rewardSearchResults.classList.add('show');
}

function showQuantityPrompt(infoId, name) {
    const quantity = prompt(`Nhập số lượng cho "${name}":`, '1');

    if (quantity && parseInt(quantity) > 0) {
        addReward(infoId, name, parseInt(quantity));
    }

    rewardSearchInput.value = '';
    rewardSearchResults.classList.remove('show');
}

function addReward(infoId, name, number) {
    const existingIndex = selectedRewards[currentGame].findIndex(r => r.infoId === infoId);

    if (existingIndex !== -1) {
        showToast(`Quà "${name}" đã có trong danh sách!`, 'warning');
        return;
    }

    selectedRewards[currentGame].push({ infoId, name, number });
    renderSelectedRewards();
    updateJsonOutput();
}

function removeReward(infoId) {
    selectedRewards[currentGame] = selectedRewards[currentGame].filter(r => r.infoId !== infoId);
    renderSelectedRewards();
    updateJsonOutput();
}

function renderSelectedRewards() {
    const container = document.getElementById('selectedRewards');
    const currentRewards = selectedRewards[currentGame];

    if (currentRewards.length === 0) {
        container.innerHTML = '<div class="empty-placeholder">Chưa chọn quà nào</div>';
        return;
    }

    let html = '';
    console.log(currentRewards);
    currentRewards.forEach(reward => {
        const isNull = reward.name.includes('Null');
        console.log(isNull);
        const nameStyle = isNull ? 'style="color: #dc3545;"' : '';
        html += `
                <div class="selected-item">
                    <span ${nameStyle}>${reward.infoId}</span>
                    <span ${nameStyle}><strong>${reward.name}</strong> x${reward.number}</span>
                    <button class="remove-btn" onclick="removeReward(${reward.infoId})" title="Xóa">
                        ×
                    </button>
                </div>
            `;
    });
    container.innerHTML = html;
}

function updateJsonOutput() {
    const currentRewards = selectedRewards[currentGame];

    const jsonArray = currentRewards.map(reward => ({
        infoId: reward.infoId,
        number: reward.number
    }));

    document.getElementById('jsonOutput').textContent = JSON.stringify(jsonArray, null, 2);

    const stringOutput = currentRewards.map(reward =>
        `${reward.infoId}-${reward.number}`
    ).join(';');

    document.getElementById('stringOutput').textContent = stringOutput || '-';

    // Update stats
    const totalQuantity = currentRewards.reduce((sum, r) => sum + r.number, 0);
    document.getElementById('totalItems').textContent = currentRewards.length;
    document.getElementById('totalQuantity').textContent = totalQuantity;
}

// Copy JSON to clipboard
document.getElementById('copyJsonBtn').addEventListener('click', function () {
    const jsonText = document.getElementById('jsonOutput').textContent;

    // Kiểm tra có Clipboard API không
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(jsonText)
            .then(() => {
                showToast('Đã copy JSON vào clipboard!', 'success');
            })
            .catch(err => {
                console.error('Lỗi copy: ' + err.message, 'danger');
                // Fallback nếu clipboard API fail
                fallbackCopy(jsonText);
            });
    } else {
        // Dùng fallback cho môi trường không secure
        fallbackCopy(jsonText);
    }
});

// Copy String to clipboard
document.getElementById('copyStringBtn').addEventListener('click', function() {
    const stringText = document.getElementById('stringOutput').textContent;

    // Kiểm tra có Clipboard API không
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(stringText)
            .then(() => {
                showToast('Đã copy String vào clipboard!', 'success');
            })
            .catch(err => {
                showToast('Lỗi copy: ' + err.message, 'danger');

                // Fallback nếu clipboard API fail
                fallbackCopy(stringText);
            });
    } else {
        // Dùng fallback cho môi trường không secure
        fallbackCopy(stringText);
    }

});

// Hàm fallback copy
function fallbackCopy(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        const successful = document.execCommand('copy');
        document.body.removeChild(textArea);
        if (successful) {
            showToast('Đã copy vào clipboard!', 'success');
        } else {
            showToast('Không thể copy. Vui lòng copy thủ công', 'danger');
        }
    } catch (err) {
        document.body.removeChild(textArea);
        showToast('Không thể copy: ' + err.message, 'danger');
    }
}

// Import functionality
document.getElementById('importBtn').addEventListener('click', function() {
    const input = document.getElementById('importInput').value.trim();

    if (!input) {
        showToast('Vui lòng nhập dữ liệu!', 'warning');
        return;
    }

    try {
        let importedData = [];

        if (input.startsWith('[') || input.startsWith('{')) {
            const jsonData = JSON.parse(input);
            importedData = Array.isArray(jsonData) ? jsonData : [jsonData];

            importedData = importedData.map(item => ({
                infoId: parseInt(item.infoId),
                number: parseInt(item.number)
            }));
        } else {
            const pairs = input.split(';');
            importedData = pairs.map(pair => {
                const [infoId, number] = pair.split('-').map(s => parseInt(s.trim()));
                return { infoId, number };
            });
        }

        const validData = importedData.filter(item =>
            !isNaN(item.infoId) && !isNaN(item.number) && item.number > 0
        );

        if (validData.length === 0) {
            showToast('Không có dữ liệu hợp lệ!', 'danger');
            return;
        }

        let addedCount = 0;
        let skippedCount = 0;

        validData.forEach(item => {
            const exists = selectedRewards[currentGame].some(r => r.infoId === item.infoId);

            if (!exists) {
                const reward = allRewards[currentGame].find(r => r.infoId === item.infoId);
                selectedRewards[currentGame].push({
                    infoId: item.infoId,
                    name: reward ? reward.name : `Null #${item.infoId}`,
                    number: item.number
                });
                addedCount++;
            } else {
                skippedCount++;
            }
        });

        renderSelectedRewards();
        updateJsonOutput();
        document.getElementById('importInput').value = '';

        let message = `Import thành công ${addedCount} quà!`;
        if (skippedCount > 0) {
            message += ` (Bỏ qua ${skippedCount} quà đã tồn tại)`;
        }
        showToast(message, 'success');
    } catch (err) {
        showToast('Lỗi format dữ liệu: ' + err.message, 'danger');
    }
});

// Clear all
document.getElementById('clearBtn').addEventListener('click', function() {
    if (selectedRewards[currentGame].length === 0) {
        showToast('Chưa có quà nào để xóa!', 'warning');
        return;
    }

    if (confirm('Xác nhận xóa tất cả quà đã chọn?')) {
        selectedRewards[currentGame] = [];
        renderSelectedRewards();
        updateJsonOutput();
        showToast('Đã xóa tất cả!', 'success');
    }
});

// Close search results when clicking outside
document.addEventListener('click', function(e) {
    if (!e.target.closest('.search-box')) {
        document.querySelectorAll('.search-results').forEach(el => {
            el.classList.remove('show');
        });
    }
});

function showToast(message, type) {
    const toast = document.getElementById('toast');
    const toastBody = toast.querySelector('.toast-body');
    toastBody.textContent = message;
    toast.className = `toast bg-${type} text-white`;
    new bootstrap.Toast(toast).show();
}

// Initialize
renderGameButtons();
updateJsonOutput();