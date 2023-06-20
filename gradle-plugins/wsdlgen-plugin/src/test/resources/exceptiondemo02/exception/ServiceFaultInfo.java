package exceptiondemo02.exception;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceFaultInfo", propOrder = {
        "category"
})
public class ServiceFaultInfo {

    @jakarta.xml.bind.annotation.XmlElement(required = true)
    protected String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        this.category = value;
    }
}
