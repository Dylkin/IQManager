package com.tenderbot.config;

import com.tenderbot.entity.Supplier;
import com.tenderbot.repository.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SupplierRepository supplierRepository;

    public DataSeeder(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void run(String... args) {
        if (supplierRepository.count() == 0) {
            Supplier redhon = new Supplier();
            redhon.setName("RedHon");
            redhon.setSiteUrl("https://redhon.ru");
            redhon.setSearchUrlTemplate("https://redhon.ru/search/?q={query}");
            redhon.setEmail("sales@redhon.ru");
            redhon.setPhone("+7 (495) 123-45-67");
            redhon.setContactPerson("Отдел продаж");
            redhon.setIsActive(true);
            redhon.setSearchSelectorProduct(".product-item, .catalog-item, .item");
            redhon.setSearchSelectorPrice(".price, .cost");
            redhon.setSearchSelectorLink("a[href*=product]");

            Supplier diaM = new Supplier();
            diaM.setName("Диаэм (Dia-M)");
            diaM.setSiteUrl("https://www.dia-m.ru");
            diaM.setSearchUrlTemplate("https://www.dia-m.ru/search/?q={query}");
            diaM.setEmail("info@dia-m.ru");
            diaM.setPhone("8 (800) 234-05-08");
            diaM.setContactPerson("Менеджер по продажам");
            diaM.setIsActive(true);
            diaM.setSearchSelectorProduct(".product-item, .catalog-item, .item, .catalog-element");
            diaM.setSearchSelectorPrice(".price, .cost");
            diaM.setSearchSelectorLink("a[href*=catalog]");

            supplierRepository.save(redhon);
            supplierRepository.save(diaM);
        }
    }
}
