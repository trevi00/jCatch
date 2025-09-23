package org.jbd.backend.community.domain;

import jakarta.persistence.*;
import org.jbd.backend.common.entity.BaseEntity;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    protected Category() {}
    
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public int getDisplayOrder() {
        return displayOrder;
    }
}