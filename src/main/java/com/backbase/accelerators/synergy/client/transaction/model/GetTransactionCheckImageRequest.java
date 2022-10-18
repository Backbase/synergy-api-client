package com.backbase.accelerators.synergy.client.transaction.model;

import lombok.Data;

@Data
public class GetTransactionCheckImageRequest {

    private String accountNumber;
    private String checkNumber;
    private String amount;
    private String processDate;
}
