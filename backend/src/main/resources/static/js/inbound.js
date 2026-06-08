/**
 * 入库管理模块
 */
const InboundModule = {
    currentPage: 1,
    pageSize: 20,
    productMap: {},

    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-box-arrow-in-down"></i> 入库管理</h3>
            <button class="btn btn-primary" onclick="InboundModule.showCreateModal()">
                <i class="bi bi-plus-lg"></i> 新增入库
            </button>
        </div>
        <div class="card">
            <div class="card-body">
                <div class="filter-bar">
                    <input type="date" class="form-control" id="inStartDate" title="开始日期">
                    <input type="date" class="form-control" id="inEndDate" title="结束日期">
                    <select class="form-select" id="inProductFilter"><option value="">全部货品</option></select>
                    <input type="text" class="form-control" id="inSourceFilter" placeholder="进货源" style="max-width:160px">
                    <button class="btn btn-outline-primary" onclick="InboundModule.load()"><i class="bi bi-search"></i> 查询</button>
                    <button class="btn btn-outline-secondary" onclick="InboundModule.clearFilters()"><i class="bi bi-x-circle"></i> 清除</button>
                </div>
                <div class="table-container">
                    <table class="table table-hover">
                        <thead>
                            <tr><th>入库单号</th><th>货品编码</th><th>货品名称</th><th>规格</th><th>数量</th><th>单价</th><th>总价</th><th>进货源</th><th>日期</th><th>操作员</th><th>操作</th></tr>
                        </thead>
                        <tbody id="inboundTableBody"></tbody>
                    </table>
                </div>
                <div class="pagination-bar" id="inboundPagination"></div>
            </div>
        </div>

        <!-- 新增入库弹窗 -->
        <div class="modal fade" id="inboundModal" tabindex="-1">
            <div class="modal-dialog"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title">新增入库</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <form id="inboundForm">
                        <div class="mb-3">
                            <label class="form-label">货品 <span class="text-danger">*</span></label>
                            <select class="form-select" name="product.id" id="inProductSelect" required
                                    onchange="InboundModule.onProductChange()">
                                <option value="">请选择货品</option></select>
                        </div>
                        <div class="row g-3">
                            <div class="col-md-4">
                                <label class="form-label">入库数量 <span class="text-danger">*</span></label>
                                <input type="number" class="form-control" name="quantity" id="inQuantity"
                                       min="1" required oninput="InboundModule.calcTotal()">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">进货单价 <span class="text-danger">*</span></label>
                                <input type="number" step="0.01" class="form-control" name="unitPrice" id="inUnitPrice"
                                       required oninput="InboundModule.calcTotal()">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">进货总价</label>
                                <input type="text" class="form-control" id="inTotalPrice" readonly>
                            </div>
                        </div>
                        <div class="row g-3 mt-1">
                            <div class="col-md-4">
                                <label class="form-label">入库日期 <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" name="inboundDate" id="inInboundDate" required>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">进货源</label>
                                <input type="text" class="form-control" name="source" placeholder="供应商/来源">
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
                    <button class="btn btn-primary" onclick="InboundModule.save()">确认入库</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
        await Promise.all([this.loadProducts(), this.load()]);
        document.getElementById('inInboundDate').value = new Date().toISOString().split('T')[0];
    },

    async loadProducts() {
        try {
            const products = await API.get('/api/products/all');
            this.productMap = {};
            products.forEach(p => this.productMap[p.id] = p);

            const options = products.map(p =>
                `<option value="${p.id}">${p.code} - ${p.name} (${p.spec || '-'})</option>`).join('');

            const filterSelect = document.getElementById('inProductFilter');
            if (filterSelect) filterSelect.innerHTML = '<option value="">全部货品</option>' + options;

            const formSelect = document.getElementById('inProductSelect');
            if (formSelect) formSelect.innerHTML = '<option value="">请选择货品</option>' + options;
        } catch (e) {
            Toast.show('加载货品列表失败: ' + e.message, 'error');
        }
    },

    async load(page = 1) {
        this.currentPage = page;
        const params = new URLSearchParams();
        const start = document.getElementById('inStartDate')?.value;
        const end = document.getElementById('inEndDate')?.value;
        const pid = document.getElementById('inProductFilter')?.value;
        const src = document.getElementById('inSourceFilter')?.value;
        if (start) params.set('startDate', start);
        if (end) params.set('endDate', end);
        if (pid) params.set('productId', pid);
        if (src) params.set('source', src);
        params.set('page', page);
        params.set('size', this.pageSize);

        try {
            const data = await API.get(`/api/inbound?${params}`);
            this.renderTable(data.content);
            Pagination.render('inboundPagination', data, (p) => this.load(p));
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    renderTable(orders) {
        const tbody = document.getElementById('inboundTableBody');
        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无入库记录</td></tr>';
            return;
        }
        tbody.innerHTML = orders.map(o => `
            <tr>
                <td><code>${o.orderNo}</code></td>
                <td>${o.product.code}</td>
                <td class="fw-semibold">${o.product.name}</td>
                <td>${o.product.spec || '-'}</td>
                <td>${o.quantity}</td>
                <td>${formatMoney(o.unitPrice)}</td>
                <td class="fw-semibold text-success">${formatMoney(o.totalPrice)}</td>
                <td>${o.source || '-'}</td>
                <td>${o.inboundDate}</td>
                <td>${o.operator || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" onclick="InboundModule.remove(${o.id},'${o.orderNo}')" title="删除（回退库存）">
                        <i class="bi bi-trash"></i></button>
                </td>
            </tr>`).join('');
    },

    clearFilters() {
        document.getElementById('inStartDate').value = '';
        document.getElementById('inEndDate').value = '';
        document.getElementById('inProductFilter').value = '';
        document.getElementById('inSourceFilter').value = '';
        this.load(1);
    },

    onProductChange() {
        const pid = document.getElementById('inProductSelect').value;
        const p = this.productMap[pid];
        // 自动填充规格信息（通过显示项查看）
        this.calcTotal();
    },

    calcTotal() {
        const qty = parseFloat(document.getElementById('inQuantity')?.value) || 0;
        const price = parseFloat(document.getElementById('inUnitPrice')?.value) || 0;
        document.getElementById('inTotalPrice').value = (qty * price).toFixed(2);
    },

    async showCreateModal() {
        document.getElementById('inboundForm').reset();
        document.getElementById('inInboundDate').value = new Date().toISOString().split('T')[0];
        document.getElementById('inTotalPrice').value = '0.00';
        showModal('inboundModal');
    },

    async save() {
        const data = getFormValues(document.getElementById('inboundForm'));
        const pid = document.getElementById('inProductSelect').value;
        if (!pid) { Toast.show('请选择货品', 'warning'); return; }
        if (!data.quantity || data.quantity <= 0) { Toast.show('请输入入库数量', 'warning'); return; }
        if (!data.unitPrice && data.unitPrice !== 0) { Toast.show('请输入进货单价', 'warning'); return; }

        data.product = { id: parseInt(pid) };
        data.unitPrice = parseFloat(data.unitPrice);
        data.quantity = parseInt(data.quantity);

        try {
            await API.post('/api/inbound', data);
            Toast.show('入库成功！库存已更新');
            hideModal('inboundModal');
            await this.load(this.currentPage);
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    remove(id, orderNo) {
        confirmDelete(`确定要删除入库单 "${orderNo}" 吗？\n删除后将自动回退库存。`, async () => {
            try {
                await API.del(`/api/inbound/${id}`);
                Toast.show('入库单已删除，库存已回退');
                await this.load(this.currentPage);
            } catch (e) {
                Toast.show(e.message, 'error');
            }
        });
    }
};
