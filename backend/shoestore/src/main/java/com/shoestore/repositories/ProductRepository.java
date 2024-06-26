package com.shoestore.repositories;

import com.shoestore.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByName(String name);

    // Pagination
    Page<Product> findAll(Pageable pageable);
}
