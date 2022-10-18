package com.backbase.accelerators.synergy.client.transaction.model;

import lombok.Data;

@Data
public class GetTransactionCheckImageResponse {

    private byte[] front;
    private byte[] back;
}
