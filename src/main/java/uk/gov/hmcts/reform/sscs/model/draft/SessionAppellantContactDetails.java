package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class SessionAppellantContactDetails {
    private String addressLine1;
    private String addressLine2;
    private String townCity;
    private String county;
    private String postCode;
    private String phoneNumber;
    private String emailAddress;
}
