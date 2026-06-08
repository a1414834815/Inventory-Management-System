/**
 * 货品管理模块
 */
const ProductModule = {
    currentPage: 1,
    pageSize: 20,
    keyword: '',

    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-tags"></i> 货品管理</h3>
            <button class="btn btn-primary" onclick="ProductModule.showCreateModal()">
                <i class="bi bi-plus-lg"></i> 新增货品
            </button>
        </div>
        <div class="card">
            <div class="card-body">
                <div class="filter-bar">
                    <input type="text" class="form-control" id="productSearch" placeholder="搜索编码/名称..."
                           style="max-width:280px" onkeyup="if(event.key==='Enter')ProductModule.load()">
                    <button class="btn btn-outline-primary" onclick="ProductModule.load()">
                        <i class="bi bi-search"></i> 搜索
                    </button>
                    <button class="btn btn-outline-secondary" onclick="ProductModule.clearSearch()">
                        <i class="bi bi-x-circle"></i> 清除
                    </button>
                </div>
                <div class="table-container">
                    <table class="table table-hover">
                        <thead>
                            <tr><th>货品编码</th><th>名称</th><th>规格</th><th>单位</th><th>分类</th><th>安全库存</th><th>备注</th><th>操作</th></tr>
                        </thead>
                        <tbody id="productTableBody"></tbody>
                    </table>
                </div>
                <div class="pagination-bar" id="productPagination"></div>
            </div>
        </div>

        <!-- 编辑弹窗 -->
        <div class="modal fade" id="productModal" tabindex="-1">
            <div class="modal-dialog"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title" id="productModalTitle">新增货品</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <form id="productForm">
                        <input type="hidden" name="id">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">货品编码 <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="code" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">货品名称 <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="name" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">规格</label>
                                <input type="text" class="form-control" name="spec">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">单位</label>
                                <input type="text" class="form-control" name="unit" placeholder="如：个、箱、kg">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">分类</label>
                                <input type="text" class="form-control" name="category">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">安全库存阈值</label>
                                <input type="number" class="form-control" name="safetyStock" min="0" value="0">
                            </div>
                            <div class="col-12">
                                <label class="form-label">备注</label>
                                <input type="text" class="form-control" name="remark">
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-primary" onclick="ProductModule.save()">保存</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
        await this.load();
    },

    async load(page = 1) {
        this.currentPage = page;
        this.keyword = document.getElementById('productSearch')?.value?.trim() || '';
        try {
            const data = await API.get(`/api/products?keyword=${encodeURIComponent(this.keyword)}&page=${page}&size=${this.pageSize}`);
            this.renderTable(data.content);
            Pagination.render('productPagination', data, (p) => this.load(p));
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    renderTable(products) {
        const tbody = document.getElementById('productTableBody');
        if (!products || products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">暂无数据</td></tr>';
            return;
        }
        tbody.innerHTML = products.map(p => `
            <tr>
                <td><code>${p.code}</code></td>
                <td class="fw-semibold">${p.name}</td>
                <td>${p.spec || '-'}</td>
                <td>${p.unit || '-'}</td>
                <td>${p.category || '-'}</td>
                <td>${p.safetyStock || 0}</td>
                <td class="text-muted small">${p.remark || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="ProductModule.showEditModal(${p.id})" title="编辑">
                        <i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-danger" onclick="ProductModule.remove(${p.id},'${p.code}')" title="删除">
                        <i class="bi bi-trash"></i></button>
                </td>
            </tr>`).join('');
    },

    clearSearch() {
        document.getElementById('productSearch').value = '';
        this.load(1);
    },

    async showCreateModal() {
        document.getElementById('productModalTitle').textContent = '新增货品';
        document.getElementById('productForm').reset();
        document.querySelector('#productForm [name="id"]').value = '';
        showModal('productModal');
    },

    async showEditModal(id) {
        try {
            const p = await API.get(`/api/products/${id}`);
            document.getElementById('productModalTitle').textContent = '编辑货品';
            const form = document.getElementById('productForm');
            form.reset();
            form.querySelector('[name="id"]').value = p.id;
            form.querySelector('[name="code"]').value = p.code;
            form.querySelector('[name="name"]').value = p.name;
            form.querySelector('[name="spec"]').value = p.spec || '';
            form.querySelector('[name="unit"]').value = p.unit || '';
            form.querySelector('[name="category"]').value = p.category || '';
            form.querySelector('[name="safetyStock"]').value = p.safetyStock || 0;
            form.querySelector('[name="remark"]').value = p.remark || '';
            showModal('productModal');
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    async save() {
        const form = document.getElementById('productForm');
        const data = getFormValues(form);
        if (!data.code || !data.name) {
            Toast.show('请填写货品编码和名称', 'warning');
            return;
        }
        try {
            const id = data.id;
            if (id) {
                await API.put(`/api/products/${id}`, data);
                Toast.show('货品更新成功');
            } else {
                await API.post('/api/products', data);
                Toast.show('货品添加成功');
            }
            hideModal('productModal');
            await this.load(this.currentPage);
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    remove(id, code) {
        confirmDelete(`确定要删除货品 "${code}" 吗？\n（存在出入库记录的货品无法删除）`, async () => {
            try {
                await API.del(`/api/products/${id}`);
                Toast.show('货品已删除');
                await this.load(this.currentPage);
            } catch (e) {
                Toast.show(e.message, 'error');
            }
        });
    }
};
