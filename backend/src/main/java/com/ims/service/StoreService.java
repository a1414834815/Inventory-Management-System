package com.ims.service;

import com.ims.entity.Store;
import com.ims.repository.OutboundOrderRepository;
import com.ims.repository.StoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final OutboundOrderRepository outboundOrderRepository;

    public StoreService(StoreRepository storeRepository,
                        OutboundOrderRepository outboundOrderRepository) {
        this.storeRepository = storeRepository;
        this.outboundOrderRepository = outboundOrderRepository;
    }

    public List<Store> listAll() {
        return storeRepository.findAll();
    }

    public Optional<Store> getById(Long id) {
        return storeRepository.findById(id);
    }

    public Store create(Store store) {
        return storeRepository.save(store);
    }

    public Store update(Long id, Store updated) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
        existing.setName(updated.getName());
        existing.setContact(updated.getContact());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        return storeRepository.save(existing);
    }

    public void delete(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
        if (outboundOrderRepository.existsByStoreId(id)) {
            throw new IllegalArgumentException("该店铺存在出库记录，无法删除");
        }
        storeRepository.delete(store);
    }
}
