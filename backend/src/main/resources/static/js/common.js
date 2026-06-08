/**
 * 通用工具库 - API 封装、Toast 提示、分页、弹窗
 */
const API = {
    async get(url) {
        const res = await fetch(url);
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message || '请求失败');
        return json.data;
    },

    async post(url, body) {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message || '操作失败');
        return json;
    },

    async put(url, body) {
        const res = await fetch(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message || '操作失败');
        return json;
    },

    async del(url) {
        const res = await fetch(url, { method: 'DELETE' });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message || '删除失败');
        return json;
    },

    async upload(url, formData) {
        const res = await fetch(url, { method: 'POST', body: formData });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message || '导入失败');
        return json;
    },

    async download(url, filename) {
        const res = await fetch(url);
        if (!res.ok) throw new Error('下载失败');
        const blob = await res.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename || 'export.xlsx';
        a.click();
        URL.revokeObjectURL(a.href);
    }
};

// Toast 通知
const Toast = {
    show(message, type = 'success') {
        const container = document.getElementById('toastContainer');
        const id = 'toast-' + Date.now();
        const bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-warning';
        const icon = type === 'success' ? 'bi-check-circle' : type === 'error' ? 'bi-x-circle' : 'bi-exclamation-triangle';

        container.insertAdjacentHTML('beforeend', `
            <div id="${id}" class="toast align-items-center text-white ${bgClass} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body"><i class="bi ${icon} me-2"></i>${message}</div>
                    <button class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `);

        const toastEl = document.getElementById(id);
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
        toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    }
};

// 分页渲染
const Pagination = {
    render(containerId, pageInfo, onPageChange) {
        const container = document.getElementById(containerId);
        if (!pageInfo || pageInfo.totalPages <= 1) {
            container.innerHTML = '<small class="text-muted">共 ' + (pageInfo?.totalElements || 0) + ' 条记录</small>';
            return '';
        }

        let html = '<ul class="pagination pagination-sm">';
        html += `<li class="page-item ${pageInfo.page <= 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${pageInfo.page - 1}">&laquo;</a></li>`;

        const start = Math.max(1, pageInfo.page - 2);
        const end = Math.min(pageInfo.totalPages, pageInfo.page + 2);

        if (start > 1) {
            html += `<li class="page-item"><a class="page-link" href="#" data-page="1">1</a></li>`;
            if (start > 2) html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        }

        for (let i = start; i <= end; i++) {
            html += `<li class="page-item ${i === pageInfo.page ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}">${i}</a></li>`;
        }

        if (end < pageInfo.totalPages) {
            if (end < pageInfo.totalPages - 1) html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            html += `<li class="page-item"><a class="page-link" href="#" data-page="${pageInfo.totalPages}">${pageInfo.totalPages}</a></li>`;
        }

        html += `<li class="page-item ${pageInfo.page >= pageInfo.totalPages ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${pageInfo.page + 1}">&raquo;</a></li>`;
        html += '</ul>';
        html += `<small class="text-muted ms-2">共 ${pageInfo.totalElements} 条，第 ${pageInfo.page}/${pageInfo.totalPages} 页</small>`;

        container.innerHTML = html;
        container.querySelectorAll('.page-link[data-page]').forEach(link => {
            link.addEventListener('click', e => {
                e.preventDefault();
                const p = parseInt(link.dataset.page);
                if (p && p !== pageInfo.page) onPageChange(p);
            });
        });
    }
};

// 通用确认删除
function confirmDelete(message, callback) {
    if (confirm(message || '确定要删除吗？此操作不可恢复。')) {
        callback();
    }
}

// 格式化日期
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toISOString().split('T')[0];
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toISOString().split('T')[0] + ' ' + d.toTimeString().split(' ')[0].substring(0, 8);
}

// 格式化金额
function formatMoney(val) {
    if (val == null) return '¥0.00';
    return '¥' + Number(val).toFixed(2);
}

// 获取表单值
function getFormValues(form) {
    const data = {};
    form.querySelectorAll('[name]').forEach(el => {
        if (el.type === 'number') {
            data[el.name] = el.value ? Number(el.value) : null;
        } else {
            data[el.name] = el.value || null;
        }
    });
    return data;
}

// 填充表单值
function setFormValues(form, data) {
    Object.keys(data).forEach(key => {
        const el = form.querySelector(`[name="${key}"]`);
        if (el) {
            if (el.type === 'number') {
                el.value = data[key] != null ? data[key] : '';
            } else {
                el.value = data[key] != null ? data[key] : '';
            }
        }
    });
}

// 显示 Bootstrap Modal
function showModal(modalId) {
    const modal = new bootstrap.Modal(document.getElementById(modalId));
    modal.show();
}

function hideModal(modalId) {
    const modal = bootstrap.Modal.getInstance(document.getElementById(modalId));
    if (modal) modal.hide();
}
