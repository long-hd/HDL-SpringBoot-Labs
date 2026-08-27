package com.hdl.springboot.labs.labx05.nacosconfig.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    private Integer payTimeoutSeconds;
    private Integer createFrequencySeconds;

    public Integer getPayTimeoutSeconds() {
        return payTimeoutSeconds;
    }
    public void setPayTimeoutSeconds(Integer payTimeoutSeconds) {
        this.payTimeoutSeconds = payTimeoutSeconds;
    }
    public Integer getCreateFrequencySeconds() {
        return createFrequencySeconds;
    }
    public void setCreateFrequencySeconds(Integer createFrequencySeconds) {
        this.createFrequencySeconds = createFrequencySeconds;
    }

}
