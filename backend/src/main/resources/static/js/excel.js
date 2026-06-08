/**
 * Excel 导入导出模块
 */
const ExcelModule = {
    async render() {
        const html = `
        <div class="page-header">
            <h3><i class="bi bi-file-earmark-spreadsheet"></i> Excel 导入导出</h3>
        </div>

        <!-- 导入区域 -->
        <div class="card">
            <div class="card-header"><i class="bi bi-upload me-2"></i>数据导入</div>
            <div class="card-body">
                <div class="row g-4">
                    <div class="col-md-4">
                        <div class="upload-area" onclick="document.getElementById('productFileInput').click()">
                            <i class="bi bi-tags d-block"></i>
                            <strong>货品导入</strong>
                            <p class="text-muted small mb-0">上传 Excel 批量导入货品</p>
                            <input type="file" id="productFileInput" accept=".xlsx" style="display:none"
                                   onchange="ExcelModule.previewImport('product', this)">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="upload-area" onclick="document.getElementById('inboundFileInput').click()">
                            <i class="bi bi-box-arrow-in-down d-block"></i>
                            <strong>入库导入</strong>
                            <p class="text-muted small mb-0">上传 Excel 批量导入入库单</p>
                            <input type="file" id="inboundFileInput" accept=".xlsx" style="display:none"
                                   onchange="ExcelModule.previewImport('inbound', this)">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="upload-area" onclick="document.getElementById('outboundFileInput').click()">
                            <i class="bi bi-box-arrow-up d-block"></i>
                            <strong>出库导入</strong>
                            <p class="text-muted small mb-0">上传 Excel 批量导入出库单</p>
                            <input type="file" id="outboundFileInput" accept=".xlsx" style="display:none"
                                   onchange="ExcelModule.previewImport('outbound', this)">
                        </div>
                    </div>
                </div>
                <div class="mt-4">
                    <strong><i class="bi bi-download me-1"></i>下载导入模板：</strong>
                    <div class="btn-group mt-2">
                        <button class="btn btn-outline-primary btn-sm" onclick="ExcelModule.downloadTemplate('product')">
                            <i class="bi bi-file-earmark-spreadsheet"></i> 货品导入模板</button>
                        <button class="btn btn-outline-primary btn-sm" onclick="ExcelModule.downloadTemplate('inbound')">
                            <i class="bi bi-file-earmark-spreadsheet"></i> 入库导入模板</button>
                        <button class="btn btn-outline-primary btn-sm" onclick="ExcelModule.downloadTemplate('outbound')">
                            <i class="bi bi-file-earmark-spreadsheet"></i> 出库导入模板</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 导出区域 -->
        <div class="card">
            <div class="card-header"><i class="bi bi-download me-2"></i>数据导出</div>
            <div class="card-body">
                <div class="row g-4">
                    <div class="col-md-4">
                        <div class="border rounded p-3">
                            <h6><i class="bi bi-tags me-1"></i>导出货品信息</h6>
                            <input type="text" class="form-control form-control-sm mb-2" id="expProdKeyword" placeholder="搜索关键词（可选）">
                            <button class="btn btn-outline-success btn-sm w-100" onclick="ExcelModule.exportData('product')">
                                <i class="bi bi-download"></i> 导出货品 Excel</button>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="border rounded p-3">
                            <h6><i class="bi bi-box-arrow-in-down me-1"></i>导出入库统计</h6>
                            <input type="date" class="form-control form-control-sm mb-1" id="expInStart" placeholder="开始日期">
                            <input type="date" class="form-control form-control-sm mb-2" id="expInEnd" placeholder="结束日期">
                            <button class="btn btn-outline-success btn-sm w-100" onclick="ExcelModule.exportData('inbound')">
                                <i class="bi bi-download"></i> 导出入库 Excel</button>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="border rounded p-3">
                            <h6><i class="bi bi-box-arrow-up me-1"></i>导出出库统计</h6>
                            <input type="date" class="form-control form-control-sm mb-1" id="expOutStart" placeholder="开始日期">
                            <input type="date" class="form-control form-control-sm mb-2" id="expOutEnd" placeholder="结束日期">
                            <button class="btn btn-outline-success btn-sm w-100" onclick="ExcelModule.exportData('outbound')">
                                <i class="bi bi-download"></i> 导出出库 Excel</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 预览弹窗 -->
        <div class="modal fade" id="previewModal" tabindex="-1">
            <div class="modal-dialog modal-lg"><div class="modal-content">
                <div class="modal-header"><h5 class="modal-title">导入预览</h5>
                    <button class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body" id="previewBody" style="max-height:400px;overflow-y:auto"></div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-primary" id="previewConfirmBtn" onclick="ExcelModule.confirmImport()">
                        <i class="bi bi-check-lg"></i> 确认导入</button>
                </div>
            </div></div>
        </div>`;

        document.getElementById('mainContent').innerHTML = html;
    },

    async downloadTemplate(type) {
        try {
            await API.download(`/api/excel/template?type=${type}`);
            Toast.show('模板下载成功');
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    async exportData(type) {
        const params = new URLSearchParams();
        params.set('type', type);

        if (type === 'product') {
            const kw = document.getElementById('expProdKeyword')?.value?.trim();
            if (kw) params.set('keyword', kw);
        } else if (type === 'inbound') {
            const s = document.getElementById('expInStart')?.value;
            const e = document.getElementById('expInEnd')?.value;
            if (s) params.set('startDate', s);
            if (e) params.set('endDate', e);
        } else if (type === 'outbound') {
            const s = document.getElementById('expOutStart')?.value;
            const e = document.getElementById('expOutEnd')?.value;
            if (s) params.set('startDate', s);
            if (e) params.set('endDate', e);
        }

        const filename = `${type === 'product' ? '货品' : type === 'inbound' ? '入库' : '出库'}_${new Date().toISOString().split('T')[0]}.xlsx`;
        try {
            await API.download(`/api/excel/export?${params}`, filename);
            Toast.show('导出成功');
        } catch (e) {
            Toast.show(e.message, 'error');
        }
    },

    // 保存当前预览状态
    _previewType: null,
    _previewFile: null,

    async previewImport(type, input) {
        if (!input.files[0]) return;
        this._previewType = type;
        this._previewFile = input.files[0];

        const formData = new FormData();
        formData.append('type', type);
        formData.append('file', input.files[0]);

        try {
            const result = await API.upload('/api/excel/import/preview', formData);
            const data = result.data;
            let html = '';

            if (data.errors && data.errors.length > 0) {
                html += `<div class="alert alert-warning"><strong>校验警告：</strong><br>`
                    + data.errors.map(e => `· ${e}`).join('<br>') + '</div>';
            }

            html += `<p><strong>共解析到 <span class="text-primary">${data.total}</span> 条记录</strong></p>`;

            if (data.data && data.data.length > 0) {
                html += '<table class="table table-sm table-bordered"><thead><tr>';
                const first = data.data[0];
                const keys = Object.keys(first).filter(k => k !== 'productId');
                keys.forEach(k => html += `<th>${k}</th>`);
                html += '</tr></thead><tbody>';

                data.data.forEach(row => {
                    html += '<tr>';
                    keys.forEach(k => html += `<td>${row[k] != null ? row[k] : ''}</td>`);
                    html += '</tr>';
                });
                html += '</tbody></table>';
            }

            document.getElementById('previewBody').innerHTML = html;
            document.getElementById('previewConfirmBtn').disabled = data.total === 0;
            showModal('previewModal');

            // 重置 input 以便重新选择同一文件
            input.value = '';
        } catch (e) {
            Toast.show(e.message, 'error');
            input.value = '';
        }
    },

    async confirmImport() {
        if (!this._previewFile || !this._previewType) {
            Toast.show('没有可导入的数据', 'warning');
            return;
        }

        const formData = new FormData();
        formData.append('type', this._previewType);
        formData.append('file', this._previewFile);

        try {
            const result = await API.upload('/api/excel/import/confirm', formData);
            const data = result.data;
            let msg = `成功导入 ${data.saved || data.total} 条记录`;
            if (data.saveErrors && data.saveErrors.length > 0) {
                msg += `，${data.saveErrors.length} 条失败`;
                console.warn('导入错误:', data.saveErrors);
            }
            Toast.show(msg);
            hideModal('previewModal');
        } catch (e) {
            Toast.show('导入失败: ' + e.message, 'error');
        }
    }
};
