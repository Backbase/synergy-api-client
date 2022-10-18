package com.backbase.accelerators.synergy.client.statement;

import com.backbase.accelerators.synergy.client.SynergyRequestSettings;
import com.profitstars.synergy.simnet.v1.GetItem;
import com.profitstars.synergy.simnet.v1.SIMNETWebServiceSoap;
import com.profitstars.synergy.simnet.v1.SynAuthenticationInfo;
import com.profitstars.synergy.simnet.v1.SynItem;
import com.profitstars.synergy.simnet.v1.SynItemRequest;
import com.profitstars.synergy.simnet.v1.SynSourceEnums;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class ElectronicStatementClient {

    private final SIMNETWebServiceSoap simnetWebServiceSoap;
    private final SynergyRequestSettings synergyRequestSettings;

    public ElectronicStatementClient(
        SIMNETWebServiceSoap simnetWebServiceSoap,
        SynergyRequestSettings synergyRequestSettings) {

        Objects.requireNonNull(
            simnetWebServiceSoap,
            "SIMNETWebService cannot be initialized because SIMNETWebService is null.");

        Objects.requireNonNull(
            synergyRequestSettings,
            "AccountClient cannot be initialized because SynergyRequestSettings is null.");

        this.simnetWebServiceSoap = simnetWebServiceSoap;
        this.synergyRequestSettings = synergyRequestSettings;
    }

    public byte[] downloadElectronicStatement(String uid) {
        log.debug("Downloading electronic statement with UID: {}", uid);

        var synItem = new SynItem();
        synItem.setSource(SynSourceEnums.SIM_DOCUMENT_MODULE);
        synItem.setDocID(Long.parseLong(uid));
        synItem.setPageNumber(0);
        synItem.setTotalPages(0);

        var synItemRequest = new SynItemRequest();
        synItemRequest.setStartPage(0);
        synItemRequest.setNumberOfPages(0);

        var synAuthenticationInfo = new SynAuthenticationInfo();
        synAuthenticationInfo.setUserName(synergyRequestSettings.getUserName());
        synAuthenticationInfo.setUserPassword(synergyRequestSettings.getUserPassword());
        synAuthenticationInfo.setFileRoom(synergyRequestSettings.getFileRoom());

        var getItem = new GetItem();
        getItem.setSynItemObjIn(synItem);
        getItem.setSynItemRequestObj(synItemRequest);
        getItem.setSynAuthenticationInfo(synAuthenticationInfo);

        var response = simnetWebServiceSoap.getItem(getItem);
        return response.getBuffer();
    }
}
