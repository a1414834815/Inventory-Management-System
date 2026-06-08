/**
 * 出库管理模块
 */
const OutboundModule = {
    currentPage: 1,
    pageSize: 20,

    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-box-arrow-up"></i> 出库管理</h3>
            <button class="btn btn-primary" onclick="OutboundModule.showCreateModal()">
                <i class="bi bi-plus-lg"></i> 新增出库
            </button>
        </div>
        <div class="card">
            <div class="card-body">
                <div class="filter-bar">
                    <input type="date" class="form-control" id="outStartDate" title="开始日期">
                    <input type="date" class="form-control" id="outEndDate" title="结束日期">
                    <select class="form-select" id="outProductFilter"><option value="">全部货品</option></select>
                    <select class="form-select" id="outStoreFilter"><option value="">全部店铺</option></select>
                    <button class="btn btn-outline-primary" onclick="OutboundModule.load()"><i class="bi bi-search"></i> 查询</button>
                    <button class="btn btn-outline-secondary" onclick="OutboundModule.clearFilters()"><i class="bi bi-x-circle"></i> 清除</button>
                </div>
                <div class="table-container">
                    <table class="table table-hover">
                        <thead>
                            <tr><th>出库单号</th><th>货品编码</th><th>货品名称</th><th>规格</th><th>店铺</th><th>数量</th><th>单价</th><th>总价</th><th>日期</th><th>操作员</th><th>操作</th></tr>
                        </thead>
                        <tbody id="outboundTableBody"></tbody>
                    </table>
                </div>
                <div class="pagination-bar" id="outboundPagination"></div>
            </div>
        </div>

        <!-- 新增出库弹窗 -->
        <div class="modal fade" id="outboundModal" tabindex="-1">
            <div class="modal-dialog"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title">新增出库</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <form id="outboundForm">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">货品 <span class="text-danger">*</span></label>
                                <select class="form-select" name="product.id" id="outProductSelect" required
                                        onchange="OutboundModule.onProductChange()">
                                    <option value="">请选择货品</option></select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">出货店铺 <span class="text-danger">*</span></label>
                                <select class="form-select" name="store.id" id="outStoreSelect" required>
                                    <option value="">请选择店铺</option></select>
                            </div>
                        </div>
                        <div id="outInventoryInfo" class="alert alert-info mt-2 mb-0 py-2 d-none"></div>
                        <div class="row g-3 mt-1">
                            <div class="col-md-4">
                                <label class="form-label">出库数量 <span class="text-danger">*</span></label>
                                <input type="number" class="form-control" name="quantity" id="outQuantity"
                                       min="1" required oninput="OutboundModule.calcTotal()">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">出货单价</label>
                                <input type="number" step="0.01" class="form-control" name="unitPrice" id="outUnitPrice"
                                       oninput="OutboundModule.calcTotal()">
                                <small class="text-muted">留空则使用库存均价</small>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">出货总价</label>
                                <input type="text" class="form-control" id="outTotalPrice" readonly>
                            </div>
                        </div>
                        <div class="row g-3 mt-1">
                            <div class="col-md-4">
                                <label class="form-label">出库日期 <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" name="outboundDate" id="outOutboundDate" required>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">操作员</label>
                                <input type="text" class="form-control" name="operator">
                            </div>
                        </div>
                        <div class="mb-3 mt-2">
                            <label class="form-label">备注</label>
                            <input type="text" class="form-control" name="remark">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-primary" onclick="OutboundModule.save()">确认出库</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
        await Promise.all([this.loadFilters(), this.load()]);
        document.getElementById('outOutboundDate').value = new Date().toISOString().split('T')[0];
    },

    async loadFilters() {
        try {
            const [products, stores] = await Promise.all([
                API.get('/api/products/all'),
                API.get('/api/stores')
            ]);
            const prodOpts = products.map(p =>
                `<option value="${p.id}">${p.code} - ${p.name}</option>`).join('');
            const storeOpts = stores.map(s =>
                `<option value="${s.id}">${s.name}</option>`).join('');

            const fProd = document.getElementById('outProductFilter');
            const fStore = document.getElementById('outStoreFilter');
            if (fProd) fProd.innerHTML = '<option value="">全部货品</option>' + prodOpts;
            if (fStore) fStore.innerHTML = '<option value="">全部店铺</option>' + storeOpts;

            const sProd = document.getElementById('outProductSelect');
            const sStore = document.getElementById('outStoreSelect');
            if (sProd) sProd.innerHTML = '<option value="">请选择货品</option>' + prodOpts;
            if (sStore) sStore.innerHTML = '<option value="">请选择店铺</option>' + storeOpts;
        } catch (e) {
            Toast.show('加载筛选选项失败', 'error');
        }
    },

    async load(page = 1) {
        this.currentPage = page;
        const params = new URLSearchParams();
        const start = document.getElementById('outStartDate')?.value;
        const end = document.getElementById('outEndDate')?.value;
        const pid = document.getElementById('outProductFilter')?.value;
        const sid = document.getElementById('outStoreFilter')?.value;
        if (start) params.set('startDate', start);
        if (end) params.set('endDate', end);
        if (pid) params.set('productId', pid);
        if (sid) params.set('storeId', sid);
        params.set('page', page);
        params.set('size', this.pageSize);

        try {
            const data = await API.get(`/api/outbound?${params}`);
            this.renderTable(data.content);
            Pagination.render('outboundPagination', data, (p) => this.load(p));
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    renderTable(orders) {
        const tbody = document.getElementById('outboundTableBody');
        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无出库记录</td></tr>';
            return;
        }
        tbody.innerHTML = orders.map(o => `
            <tr>
                <td><code>${o.orderNo}</code></td>
                <td>${o.product.code}</td>
                <td class="fw-semibold">${o.product.name}</td>
                <td>${o.product.spec || '-'}</td>
                <td>${o.store.name}</td>
                <td>${o.quantity}</td>
                <td>${formatMoney(o.unitPrice)}</td>
                <td class="fw-semibold text-danger">${formatMoney(o.totalPrice)}</td>
                <td>${o.outboundDate}</td>
                <td>${o.operator || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" onclick="OutboundModule.remove(${o.id},'${o.orderNo}')" title="删除（恢复库存）">
                        <i class="bi bi-trash"></i></button>
                </td>
            </tr>`).join('');
    },

    clearFilters() {
        ['outStartDate','outEndDate','outProductFilter','outStoreFilter'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        this.load(1);
    },

    async onProductChange() {
        const pid = document.getElementById('outProductSelect').value;
        const infoEl = document.getElementById('outInventoryInfo');
        if (!pid) {
            infoEl.classList.add('d-none');
            return;
        }
        try {
            const inv = await API.get(`/api/inventory/${pid}`);
            infoEl.classList.remove('d-none');
            infoEl.innerHTML = `<i class="bi bi-info-circle me-1"></i>
                当前库存: <strong>${inv.quantity}</strong> ${inv.productUnit || ''}
                | 库存均价: <strong>${formatMoney(inv.avgPrice)}</strong>
                ${inv.alert ? ' | <span class="text-danger fw-bold">⚠ 库存不足预警</span>' : ''}`;
            // 自动填入均价
            if (inv.avgPrice && inv.avgPrice > 0) {
                document.getElementById('outUnitPrice').placeholder = '均价: ' + inv.avgPrice.toFixed(2);
            }
        } catch (e) {
            infoEl.classList.add('d-none');
        }
    },

    calcTotal() {
        const qty = parseFloat(document.getElementById('outQuantity')?.value) || 0;
        const price = parseFloat(document.getElementById('outUnitPrice')?.value) || 0;
        document.getElementById('outTotalPrice').value = (qty * price).toFixed(2);
    },

    async showCreateModal() {
        document.getElementById('outboundForm').reset();
        document.getElementById('outOutboundDate').value = new Date().toISOString().split('T')[0];
        document.getElementById('outTotalPrice').value = '0.00';
        document.getElementById('outInventoryInfo').classList.add('d-none');
        showModal('outboundModal');
    },

    async save() {
        const pid = document.getElementById('outProductSelect').value;
        const sid = document.getElementById('outStoreSelect').value;
        const qty = parseInt(document.getElementById('outQuantity').value);
        const unitPrice = document.getElementById('outUnitPrice').value;

        if (!pid) { Toast.show('请选择货品', 'warning'); return; }
        if (!sid) { Toast.show('请选择出货店铺', 'warning'); return; }
        if (!qty || qty <= 0) { Toast.show('请输入出库数量', 'warning'); return; }

        const data = {
            product: { id: parseInt(pid) },
            store: { id: parseInt(sid) },
            quantity: qty,
            unitPrice: unitPrice ? parseFloat(unitPrice) : null,
            outboundDate: document.getElementById('outOutboundDate').value,
            operator: document.querySelector('#outboundForm [name="operator"]')?.value || null,
            remark: document.querySelector('#outboundForm [name="remark"]')?.value || null
        };

        try {
            await API.post('/api/outbound', data);
            Toast.show('出库成功！库存已扣减');
            hideModal('outboundModal');
            await this.load(this.currentPage);
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    remove(id, orderNo) {
        confirmDelete(`确定要删除出库单 "${orderNo}" 吗？\n删除后将自动恢复库存。`, async () => {
            try {
                await API.del(`/api/outbound/${id}`);
                Toast.show('出库单已删除，库存已恢复');
                await this.load(this.currentPage);
            } catch (e) {
                Toast.show(e.message, 'error');
            }
        });
    }
};
