package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class ConfirmPanelCompositionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        processInterloc(sscsCaseData);
        processCaseState(sscsCaseData);

        return response;
    }

    private void processCaseState(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getIsFqpmRequired() != null) {
            if (StringUtils.isBlank(sscsCaseData.getDwpDueDate())) {
                sscsCaseData.setState(State.READY_TO_LIST);
            } else if (!isAllOtherPartyHaveAtLeastOneHearingOption(sscsCaseData)) {
                sscsCaseData.setState(State.NOT_LISTABLE);
            }
        }
    }

    //TODO: Implement after 9813 is merged
    private boolean isAllOtherPartyHaveAtLeastOneHearingOption(SscsCaseData sscsCaseData) {
        return false;
    }

    private void processInterloc(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getIsFqpmRequired() != null
                && sscsCaseData.getInterlocReviewState().equals(InterlocReviewState.REVIEW_BY_JUDGE.getId())) {
            sscsCaseData.setInterlocReferralReason(null);
            sscsCaseData.setInterlocReviewState(null);
        }
    }
}
