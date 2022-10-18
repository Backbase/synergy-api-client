package com.backbase.accelerators.synergy.client.statement

import com.backbase.accelerators.synergy.client.SynergyRequestSettings
import com.backbase.accelerators.synergy.client.TestData
import com.profitstars.synergy.simnet.v1.SIMNETWebServiceSoap
import spock.lang.Specification

class ElectronicStatementClientSpec extends Specification {

    private SIMNETWebServiceSoap simnetWebServiceSoap = Mock()
    private SynergyRequestSettings synergyRequestSettings = Mock()
    private ElectronicStatementClient statementClient = new ElectronicStatementClient(simnetWebServiceSoap, synergyRequestSettings)

    def "DownloadStatements"() {
        when:
        byte [] buffer = statementClient.downloadElectronicStatement("62352975")

        then:
        simnetWebServiceSoap.getItem(_) >> TestData.getItemResponse

        expect:
        buffer != null
        buffer instanceof byte[]
    }

}
