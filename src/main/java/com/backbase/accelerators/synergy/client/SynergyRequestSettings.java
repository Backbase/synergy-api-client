package com.backbase.accelerators.synergy.client;

import lombok.Data;

import java.math.BigInteger;

@Data
public class SynergyRequestSettings {

    private String baseUrl;
    private String userName;
    private String userPassword;
    private String fileRoom;
    private String searchName;
    private String cabinetApp;
    private String reportType;
    private BigInteger institution;
}
