package com.fedebacelar.bank.customer.application.mapper;

import com.fedebacelar.bank.customer.application.view.CustomerStatusHistoryDetails;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;

public final class CustomerStatusHistoryMapper {

    private CustomerStatusHistoryMapper() {
    }

    public static CustomerStatusHistoryDetails toDetails(CustomerStatusHistory history) {
        return new CustomerStatusHistoryDetails(
                history.id(),
                history.customerId(),
                history.previousStatus(),
                history.newStatus(),
                history.reason(),
                history.changedBy(),
                history.changedAt()
        );
    }
}
