/**
 * 店铺管理模块
 */
const StoreModule = {
    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-shop"></i> 店铺管理</h3>
            <button class="btn btn-primary" onclick="StoreModule.showCreateModal()">
                <i class="bi bi-plus-lg"></i> 新增店铺
            </button>
        </div>
        <div class="card">
            <div class="card-body">
                <div class="table-container">
                    <table class="table table-hover">
                        <thead>
                            <tr><th>店铺名称</th><th>联系人</th><th>联系电话</th><th>地址</th><th>创建时间</th><th>操作</th></tr>
                        </thead>
                        <tbody id="storeTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 编辑弹窗 -->
        <div class="modal fade" id="storeModal" tabindex="-1">
            <div class="modal-dialog"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title" id="storeModalTitle">新增店铺</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <form id="storeForm">
                        <input type="hidden" name="id">
                        <div class="mb-3">
                            <label class="form-label">店铺名称 <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="name" required>
                        </div>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">联系人</label>
                                <input type="text" class="form-control" name="contact">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">联系电话</label>
                                <input type="text" class="form-control" name="phone">
                            </div>
                        </div>
                        <div class="mb-3 mt-2">
                            <label class="form-label">地址</label>
                            <input type="text" class="form-control" name="address">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-primary" onclick="StoreModule.save()">保存</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
        await this.load();
    },

    async load() {
        try {
            const stores = await API.get('/api/stores');
            this.renderTable(stores);
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    renderTable(stores) {
        const tbody = document.getElementById('storeTableBody');
        if (!stores || stores.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">暂无店铺数据</td></tr>';
            return;
        }
        tbody.innerHTML = stores.map(s => `
            <tr>
                <td class="fw-semibold">${s.name}</td>
                <td>${s.contact || '-'}</td>
                <td>${s.phone || '-'}</td>
                <td>${s.address || '-'}</td>
                <td class="text-muted small">${formatDateTime(s.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="StoreModule.showEditModal(${s.id})" title="编辑">
                        <i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-danger" onclick="StoreModule.remove(${s.id},'${s.name}')" title="删除">
                        <i class="bi bi-trash"></i></button>
                </td>
            </tr>`).join('');
    },

    async showCreateModal() {
        document.getElementById('storeModalTitle').textContent = '新增店铺';
        document.getElementById('storeForm').reset();
        showModal('storeModal');
    },

    async showEditModal(id) {
        try {
            const s = await API.get(`/api/stores/${id}`);
            document.getElementById('storeModalTitle').textContent = '编辑店铺';
            const form = document.getElementById('storeForm');
            form.reset();
            form.querySelector('[name="id"]').value = s.id;
            form.querySelector('[name="name"]').value = s.name;
            form.querySelector('[name="contact"]').value = s.contact || '';
            form.querySelector('[name="phone"]').value = s.phone || '';
            form.querySelector('[name="address"]').value = s.address || '';
            showModal('storeModal');
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    async save() {
        const data = getFormValues(document.getElementById('storeForm'));
        if (!data.name) {
            Toast.show('请填写店铺名称', 'warning');
            return;
        }
        try {
            if (data.id) {
                await API.put(`/api/stores/${data.id}`, data);
                Toast.show('店铺更新成功');
            } else {
                await API.post('/api/stores', data);
                Toast.show('店铺添加成功');
            }
            hideModal('storeModal');
            await this.load();
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    remove(id, name) {
        confirmDelete(`确定要删除店铺 "${name}" 吗？`, async () => {
            try {
                await API.del(`/api/stores/${id}`);
                Toast.show('店铺已删除');
                await this.load();
            } catch (e) {
                Toast.show(e.message, 'error');
            }
        });
    }
};
