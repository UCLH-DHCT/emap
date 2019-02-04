package uk.ac.ucl.rits.inform.informdb;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Attribute {
    public enum AttributeId {
        FAMILY_NAME
    }
    
    @Id
    private AttributeId attribute_id;
    
    private String description;

    public AttributeId getAttribute_id() {
        return attribute_id;
    }

    public void setAttribute_id(AttributeId aid) {
        this.attribute_id = aid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
