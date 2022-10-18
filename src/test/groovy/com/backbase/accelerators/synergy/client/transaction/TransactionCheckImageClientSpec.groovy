package com.backbase.accelerators.synergy.client.transaction

import com.backbase.accelerators.synergy.client.SynergyRequestSettings
import com.backbase.accelerators.synergy.client.TestData
import com.backbase.accelerators.synergy.client.transaction.model.GetTransactionCheckImageRequest
import com.backbase.accelerators.synergy.client.transaction.model.GetTransactionCheckImageResponse
import com.github.javafaker.Faker
import com.profitstars.synergy.simnet.v1.SIMNETWebServiceSoap
import spock.lang.Specification

class TransactionCheckImageClientSpec extends Specification {

    private SIMNETWebServiceSoap simnetWebServiceSoap = Mock()
    private SynergyRequestSettings synergyRequestSettings = Mock()

    private TransactionCheckImageClient transactionCheckImageClient = new TransactionCheckImageClient(
        simnetWebServiceSoap,
        synergyRequestSettings)

    private Faker faker = new Faker()

    void 'getTransactionCheckImage invokes synergy and returns the front and rear image of a check'() {
        given:
        GetTransactionCheckImageRequest request = new GetTransactionCheckImageRequest(
            accountNumber: '16000005504960',
            checkNumber: '2234',
            amount: '100.00',
            processDate: '10162021'
        )

        when:
        GetTransactionCheckImageResponse result = transactionCheckImageClient.getTransactionCheckImage(request)

        then:
        2 * synergyRequestSettings.getUserName() >> faker.name().username()
        2 * synergyRequestSettings.getUserPassword() >> faker.crypto().sha256()
        2 * synergyRequestSettings.getFileRoom() >> faker.company().name()
        1 * synergyRequestSettings.getInstitution() >> faker.number().randomDigit()
        1 * synergyRequestSettings.getCabinetApp() >> faker.number().digit()
        1 * synergyRequestSettings.getReportType() >> faker.idNumber().valid()
        2 * synergyRequestSettings.getSearchName() >> faker.name().name()

        and:
        2 * simnetWebServiceSoap.getItemBySearch(_) >> TestData.getItemBySearchResponse_frontImage
    }
}
