/**
 * 应用主控 - 导航与页面切换
 */
(function () {
    'use strict';

    const modules = {
        product: ProductModule,
        store: StoreModule,
        inbound: InboundModule,
        outbound: OutboundModule,
        inventory: InventoryModule,
        excel: ExcelModule
    };

    let currentPage = null;

    // 导航点击
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            const page = this.dataset.page;
            if (page) navigate(page);
        });
    });

    async function navigate(page) {
        // 更新导航高亮
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
        const navItem = document.querySelector(`[data-page="${page}"]`);
        if (navItem) navItem.classList.add('active');

        // 渲染页面
        const module = modules[page];
        if (module && module.render) {
            try {
                await module.render();
                currentPage = page;
            } catch (e) {
                console.error('页面渲染失败:', e);
                document.getElementById('mainContent').innerHTML =
                    `<div class="alert alert-danger m-4">页面加载失败: ${e.message}</div>`;
            }
        }
    }

    // 默认加载货品管理
    navigate('product');
})();
