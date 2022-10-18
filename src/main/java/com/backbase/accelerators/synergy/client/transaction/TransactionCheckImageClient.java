package com.backbase.accelerators.synergy.client.transaction;

import com.backbase.accelerators.synergy.client.SynergyRequestSettings;
import com.backbase.accelerators.synergy.client.exception.SynergyImageNotFoundException;
import com.backbase.accelerators.synergy.client.exception.SynergySystemException;
import com.backbase.accelerators.synergy.client.transaction.model.GetTransactionCheckImageRequest;
import com.backbase.accelerators.synergy.client.transaction.model.GetTransactionCheckImageResponse;
import com.profitstars.synergy.simnet.search.CabAppLst;
import com.profitstars.synergy.simnet.search.InstLst;
import com.profitstars.synergy.simnet.search.Search;
import com.profitstars.synergy.simnet.search.SearchItem;
import com.profitstars.synergy.simnet.search.SearchItemLst;
import com.profitstars.synergy.simnet.search.TypeRptLst;
import com.profitstars.synergy.simnet.v1.ArrayOfSynOptionEnums;
import com.profitstars.synergy.simnet.v1.GetItemBySearch;
import com.profitstars.synergy.simnet.v1.GetItemBySearchResponse;
import com.profitstars.synergy.simnet.v1.SIMNETWebServiceSoap;
import com.profitstars.synergy.simnet.v1.SynAuthenticationInfo;
import com.profitstars.synergy.simnet.v1.SynError;
import com.profitstars.synergy.simnet.v1.SynErrorEnums;
import com.profitstars.synergy.simnet.v1.SynErrorLevelEnums;
import com.profitstars.synergy.simnet.v1.SynItemRequest;
import com.profitstars.synergy.simnet.v1.SynOptionEnums;
import com.profitstars.synergy.simnet.v1.SynSearchEnums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TransactionCheckImageClient {

    private static final long FRONT_PAGE = 1;
    private static final long BACK_PAGE = 2;
    private static final String COMPARE_TO = "=";
    private static final String CONNECTOR = "&amp;";
    private static final String ACCOUNT_NUMBER = "ACCOUNT NUMBER";
    private static final String PROCESS_DATE = "PROCESS DATE";
    private static final String AMOUNT = "AMOUNT";
    private static final String CHECK_NUMBER = "CHECK NUMBER";

    private final SIMNETWebServiceSoap simnetWebServiceSoap;
    private final SynergyRequestSettings synergyRequestSettings;

    public TransactionCheckImageClient(
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

    public GetTransactionCheckImageResponse getTransactionCheckImage(GetTransactionCheckImageRequest request) {
        log.debug("Fetching transaction check images with request: {}", request);

        String searchXml = toXmlSearchRequest(
            request.getAccountNumber(),
            request.getCheckNumber(),
            request.getProcessDate(),
            request.getAmount());

        var frontImageSearchResponseCompletableFuture = CompletableFuture
            .supplyAsync(() -> invoke(createGetItemBySearchRequest(searchXml, FRONT_PAGE)));

        var backImageSearchResponseCompletableFuture = CompletableFuture
            .supplyAsync(() -> invoke(createGetItemBySearchRequest(searchXml, BACK_PAGE)));

        var combinedCompletableFuture = frontImageSearchResponseCompletableFuture.thenCombine(
            backImageSearchResponseCompletableFuture,
            (frontImageResponse, backImageResponse) -> {

                checkForErrors(frontImageResponse);
                checkForErrors(backImageResponse);

                var response = new GetTransactionCheckImageResponse();
                if (Objects.nonNull(frontImageResponse.getBuffer())) {
                    response.setFront(frontImageResponse.getBuffer());
                }

                if (Objects.nonNull(backImageResponse.getBuffer())) {
                    response.setBack(backImageResponse.getBuffer());
                }

                log.debug("GetTransactionCheckImageResponse: {}", response);
                return response;
            });

        try {
            return combinedCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error occurred during asynchronous execution: {}", e.getMessage());
            if (Objects.nonNull(e.getCause()) && (e.getCause() instanceof SynergyImageNotFoundException)) {
                throw (SynergyImageNotFoundException) e.getCause();
            }

            throw new RuntimeException(e);
        }
    }

    private GetItemBySearchResponse invoke(GetItemBySearch request) {
        log.debug("Invoking Synergy with request: {}", toXmlString(request));
        return simnetWebServiceSoap.getItemBySearch(request);
    }

    private SynAuthenticationInfo getSynAuthenticationInfo() {
        var synAuthenticationInfo = new SynAuthenticationInfo();
        synAuthenticationInfo.setUserName(synergyRequestSettings.getUserName());
        synAuthenticationInfo.setUserPassword(synergyRequestSettings.getUserPassword());
        synAuthenticationInfo.setFileRoom(synergyRequestSettings.getFileRoom());
        return synAuthenticationInfo;
    }

    private String toXmlSearchRequest(String accountNumber, String checkNumber, String processDate, String amount) {
        var institutionList = new InstLst();
        institutionList.setInst(synergyRequestSettings.getInstitution());

        var cabinetAppList = new CabAppLst();
        cabinetAppList.setCabApp(synergyRequestSettings.getCabinetApp());

        var reportTypeList = new TypeRptLst();
        reportTypeList.setTypeRpt(synergyRequestSettings.getReportType());

        var searchItemLst = new SearchItemLst();
        searchItemLst.getSearchItem().addAll(createSearchItems(accountNumber, checkNumber, processDate, amount));

        var search = new Search();
        search.setInstLst(institutionList);
        search.setCabAppLst(cabinetAppList);
        search.setSearchItemLst(searchItemLst);

        var searchXml = marshall(search);
        log.debug("Created getTransactionCheckImageRequest search XML: {}", searchXml);
        return searchXml;
    }

    private String marshall(Search search) {
        try {
            var stringWriter = new StringWriter();

            var marshaller = JAXBContext.newInstance(Search.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(search, stringWriter);

            return stringWriter.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Exception while marshaling search xml", e);
        }
    }

    private List<SearchItem> createSearchItems(
        String accountNumber,
        String checkNumber,
        String processDate,
        String amount) {

        var searchItemAccountNumber = new SearchItem();
        searchItemAccountNumber.setItemName(ACCOUNT_NUMBER);
        searchItemAccountNumber.setCompare(COMPARE_TO);
        searchItemAccountNumber.setItemVal(accountNumber);
        searchItemAccountNumber.setConnector(CONNECTOR);

        var searchItemProcessDate = new SearchItem();
        searchItemProcessDate.setItemName(PROCESS_DATE);
        searchItemProcessDate.setCompare(COMPARE_TO);
        searchItemProcessDate.setItemVal(processDate);
        searchItemProcessDate.setConnector(CONNECTOR);

        var searchItemAmount = new SearchItem();
        searchItemAmount.setItemName(AMOUNT);
        searchItemAmount.setCompare(COMPARE_TO);
        searchItemAmount.setItemVal(amount);
        searchItemAmount.setConnector(CONNECTOR);

        var searchItemCheckNumber = new SearchItem();
        searchItemCheckNumber.setItemName(CHECK_NUMBER);
        searchItemCheckNumber.setCompare(COMPARE_TO);
        searchItemCheckNumber.setItemVal(checkNumber);

        return Arrays.asList(searchItemAccountNumber, searchItemCheckNumber);
    }

    private GetItemBySearch createGetItemBySearchRequest(String searchXml, long pageNumber) {
        var getItemBySearch = new GetItemBySearch();
        getItemBySearch.setSavedSearchType(SynSearchEnums.SIM_SEARCHTYPE_SYSTEM);
        getItemBySearch.setSearchName(synergyRequestSettings.getSearchName());
        getItemBySearch.setSearchParamXML(searchXml);
        getItemBySearch.setSynAuthenticationInfo(getSynAuthenticationInfo());
        getItemBySearch.setSynItemRequestObj(createSynItemRequest(pageNumber));

        return getItemBySearch;
    }

    private SynItemRequest createSynItemRequest(long startPage) {
        var arrayOfSynOptionEnums = new ArrayOfSynOptionEnums();
        arrayOfSynOptionEnums.getSynOptionEnums().add(SynOptionEnums.SIM_OPTIONS_ANNOTATIONS);

        var synItemRequest = new SynItemRequest();
        synItemRequest.setOptions(arrayOfSynOptionEnums);
        synItemRequest.setStartPage(startPage);
        synItemRequest.setNumberOfPages(1);

        return synItemRequest;
    }

    private void checkForErrors(GetItemBySearchResponse response) {
        var errorLevel = Optional.ofNullable(response)
            .map(GetItemBySearchResponse::getGetItemBySearchResult)
            .map(SynError::getErrorLevel)
            .orElse(null);

        var errorCode = Optional.ofNullable(response)
            .map(GetItemBySearchResponse::getGetItemBySearchResult)
            .map(SynError::getErrorCode)
            .orElse(null);

        var errorMessage = Optional.ofNullable(response)
            .map(GetItemBySearchResponse::getGetItemBySearchResult)
            .map(SynError::getErrorMsg)
            .orElse(null);

        if (isNotFoundError(errorLevel, errorCode)) {
            throw new SynergyImageNotFoundException(errorMessage);
        } else if (isSystemException(errorLevel, errorCode)) {
            throw new SynergySystemException(errorMessage);
        }
    }

    private boolean isNotFoundError(SynErrorLevelEnums errorLevel, SynErrorEnums errorCode) {
        return Objects.nonNull(errorLevel)
            && Objects.nonNull(errorCode)
            && errorLevel.equals(SynErrorLevelEnums.SIM_ERRORLEVEL_WARNING)
            && (errorCode.equals(SynErrorEnums.SIM_WARNING_NO_HITS_FOUND)
            || errorCode.equals(SynErrorEnums.SIM_WARNING_NO_ITEMS_FOUND));
    }

    private boolean isSystemException(SynErrorLevelEnums errorLevel, SynErrorEnums errorCode) {
        return Objects.nonNull(errorLevel)
            && Objects.nonNull(errorCode)
            && errorLevel.equals(SynErrorLevelEnums.SIM_ERRORLEVEL_ERROR)
            && !errorCode.equals(SynErrorEnums.SIM_ERROR_NONE);
    }

    private String toXmlString(Object object) {
        var stringWriter = new StringWriter();
        JAXB.marshal(object, stringWriter);

        return StringEscapeUtils.unescapeXml(stringWriter.toString());
    }
}
