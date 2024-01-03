package com.SJY.O2O_Automatic_Store_System_Demo.repository.category;

import com.SJY.O2O_Automatic_Store_System_Demo.entity.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByParentIsNull();

}