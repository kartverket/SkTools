package no.statkart.sktools.interfaceservice.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/* not documented */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleClass")
public class SimpleClass {

    @XmlElement(required = true)
    protected String value;

    public SimpleClass(String value) {
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
             