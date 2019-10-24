package exceptiondemo01.exception;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceFaultInfo", propOrder = {
        "category"
})
public class ServiceFaultInfo {

    @javax.xml.bind.annotation.XmlElement(required = true)
    protected String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        this.category = value;
    }
}
            