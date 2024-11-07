package org.meveo.model.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tmp_secured_entity")
public class TmpSecuredEntity {
    
    @Id
    @Column(name = "uuid")
    private String uuid;
    
    @Column(name = "search_id")
    private String searchId;
    
    @Column(name = "code")
    private String code;

    public TmpSecuredEntity() {
    }

    public TmpSecuredEntity(String searchId, String code) {
        this.searchId = searchId;
        this.code = code;
    }

    public String getSearchId() {
        return searchId;
    }

    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    @PrePersist
    public void getUuid() {
        if(this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }
}
