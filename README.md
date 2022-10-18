# synergy-api-client

This project contains a SOAP client library that can be used to call Synergy web services 
for retrieving transaction check images and electronic statements.
WSDLs and XSDs are located in: `src/main/resources`

This project can be utilized in your Backbase integration services as a Maven dependency.
Simply include the following maven coordinates in the `dependency` section of your service's `pom.xml`

```aidl
    <groupId>com.backbase.accelerators</groupId>
    <artifactId>synergy-api-client</artifactId>
    <version>1.0.0</version>
```

## Build this project
From the root directory of this project, run:

```mvn clean install```

This will compile the project and generate Java classes from the WSDLs and XSDs the resources folder.
The generated classes can be found in: `target/generated-sources/cxf`

### Example usage - Defining `application.yml` configuration:

Properties should be defined in your Backbase integration service as follows. Please obtain actual configuration values
from your customer.

```yaml
synergy:
  client:
    baseUrl: https://{SYNGERY_HOSTNAME}/SIMNET/SIMNET.asmx
    userName: {USER_NAME}
    userPassword: {PASSWORD}
    fileRoom: {FILE_ROOM}
    searchName: Checks
    cabinetApp: CHECKS
    reportType: ONUS
    institution: {INSTITUTION}
```

```java

@Data
@Configuration
@ConfigurationProperties("synergy.client")
public class SynergyClientConfigurationProperties {

    private String baseUrl;
    private String userName;
    private String userPassword;
    private String fileRoom;
    private String searchName;
    private String cabinetApp;
    private String reportType;
    private BigInteger institution;

    public SynergyRequestSettings toSynergyRequestSettings() {
        SynergyRequestSettings synergyRequestSettings = new SynergyRequestSettings();
        synergyRequestSettings.setBaseUrl(baseUrl);
        synergyRequestSettings.setUserName(userName);
        synergyRequestSettings.setUserPassword(userPassword);
        synergyRequestSettings.setFileRoom(fileRoom);
        synergyRequestSettings.setSearchName(searchName);
        synergyRequestSettings.setCabinetApp(cabinetApp);
        synergyRequestSettings.setReportType(reportType);
        synergyRequestSettings.setInstitution(institution);

        return synergyRequestSettings;
    }
}
```

### Example usage - Defining a Spring Bean in Your Integration Service:

The below example wires up the `transactionCheckImageClient` and `electronicStatementClient` as a Spring bean.

```java

@Configuration
public class SynergyClientConfiguration {

    @Bean
    @SneakyThrows
    public TransactionCheckImageClient transactionCheckImageClient(SynergyProperties synergyProperties) {
        return new TransactionCheckImageClient(
                getSimNetWebServiceSoap(synergyProperties),
                synergyProperties.toSynergyRequestSettings());
    }

    @Bean
    @SneakyThrows
    public ElectronicStatementClient electronicStatementClient(SynergyProperties synergyProperties) {
        return new ElectronicStatementClient(
                getSimNetWebServiceSoap(synergyProperties),
                synergyProperties.toSynergyRequestSettings());
    }

    @SneakyThrows
    private SIMNETWebServiceSoap getSimNetWebServiceSoap(SynergyProperties synergyProperties) {
        return new SIMNETWebService(new URL(synergyProperties.getBaseUrl())).getSIMNETWebServiceSoap();
    }
}
```