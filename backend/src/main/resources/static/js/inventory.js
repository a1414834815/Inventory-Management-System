/**
 * 库存查询模块
 */
const InventoryModule = {
    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-clipboard-data"></i> 库存查询</h3>
        </div>

        <div class="stat-cards" id="inventoryStats"></div>

        <div class="card">
            <div class="card-body">
                <div class="filter-bar">
                    <input type="text" class="form-control" id="invSearch" placeholder="搜索货品编码/名称..."
                           style="max-width:280px" onkeyup="if(event.key==='Enter')InventoryModule.load()">
                    <button class="btn btn-outline-primary" onclick="InventoryModule.load()">
                        <i class="bi bi-search"></i> 查询</button>
                    <button class="btn btn-outline-secondary" onclick="InventoryModule.clearSearch()">
                        <i class="bi bi-x-circle"></i> 清除</button>
                </div>
                <div class="table-container">
                    <table class="table table-hover">
                        <thead>
                            <tr><th>货品编码</th><th>名称</th><th>规格</th><th>单位</th><th>分类</th><th>库存数量</th><th>库存均价</th><th>库存总值</th><th>安全库存</th><th>状态</th><th>操作</th></tr>
                        </thead>
                        <tbody id="inventoryTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 设置安全库存弹窗 -->
        <div class="modal fade" id="safetyStockModal" tabindex="-1">
            <div class="modal-dialog modal-sm"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title">设置安全库存</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <input type="hidden" id="ssProductId">
                    <div class="mb-2"><strong id="ssProductName"></strong></div>
                    <label class="form-label">安全库存阈值</label>
                    <input type="number" class="form-control" id="ssValue" min="0" required>
                    <small class="text-muted">当库存数量 ≤ 此值时系统将发出预警</small>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-primary" onclick="InventoryModule.saveSafetyStock()">保存</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
        await this.load();
    },

    async load() {
        const keyword = document.getElementById('invSearch')?.value?.trim() || '';
        try {
            const data = await API.get(`/api/inventory?keyword=${encodeURIComponent(keyword)}`);
            this.renderStats(data);
            this.renderTable(data);
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    renderStats(data) {
        const totalQty = data.reduce((s, i) => s + i.quantity, 0);
        const totalValue = data.reduce((s, i) => s + (i.totalValue || 0), 0);
        const alertCount = data.filter(i => i.alert).length;
        const itemCount = data.length;

        document.getElementById('inventoryStats').innerHTML = `
            <div class="stat-card">
                <div class="stat-icon blue"><i class="bi bi-box"></i></div>
                <div><div class="stat-value">${itemCount}</div><div class="stat-label">货品种类</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
                <div><div class="stat-value">${totalQty.toLocaleString()}</div><div class="stat-label">库存总量</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon orange"><i class="bi bi-currency-dollar"></i></div>
                <div><div class="stat-value">${formatMoney(totalValue)}</div><div class="stat-label">库存总价值</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon ${alertCount > 0 ? 'red' : 'green'}"><i class="bi bi-exclamation-triangle"></i></div>
                <div><div class="stat-value" style="color:${alertCount > 0 ? 'var(--danger)' : 'var(--success)'}">${alertCount}</div><div class="stat-label">预警货品</div></div>
            </div>`;
    },

    renderTable(data) {
        const tbody = document.getElementById('inventoryTableBody');
        if (!data || data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无库存数据</td></tr>';
            return;
        }
        tbody.innerHTML = data.map(i => `
            <tr class="${i.alert ? 'alert-row' : ''}">
                <td><code>${i.productCode}</code></td>
                <td class="fw-semibold">${i.productName}</td>
                <td>${i.productSpec || '-'}</td>
                <td>${i.productUnit || '-'}</td>
                <td>${i.productCategory || '-'}</td>
                <td class="fw-bold fs-6 ${i.alert ? 'text-danger' : ''}">${i.quantity}</td>
                <td>${formatMoney(i.avgPrice)}</td>
                <td class="fw-semibold">${formatMoney(i.totalValue)}</td>
                <td>${i.safetyStock || 0}</td>
                <td>${i.alert
                    ? '<span class="badge bg-danger"><i class="bi bi-exclamation-triangle me-1"></i>库存不足</span>'
                    : '<span class="badge bg-success">正常</span>'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-warning" onclick="InventoryModule.showSafetyStockModal(${i.productId},'${i.productName}',${i.safetyStock||0})" title="设置预警">
                        <i class="bi bi-bell"></i></button>
                </td>
            </tr>`).join('');
    },

    clearSearch() {
        document.getElementById('invSearch').value = '';
        this.load();
    },

    showSafetyStockModal(productId, name, current) {
        document.getElementById('ssProductId').value = productId;
        document.getElementById('ssProductName').textContent = name;
        document.getElementById('ssValue').value = current;
        showModal('safetyStockModal');
    },

    async saveSafetyStock() {
        const productId = document.getElementById('ssProductId').value;
        const value = parseInt(document.getElementById('ssValue').value) || 0;
        try {
            await API.put(`/api/inventory/${productId}/safety-stock?safetyStock=${value}`);
            Toast.show('安全库存设置成功');
            hideModal('safetyStockModal');
            await this.load();
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    }
};
