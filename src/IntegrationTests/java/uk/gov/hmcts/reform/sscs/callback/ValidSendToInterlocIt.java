package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;

@SpringBootTest
@AutoConfigureMockMvc
public class ValidSendToInterlocIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/validSendToInterloc.json");
    }

    @Test
    public void callToAboutToStart_willPopulateWhoReviewsCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expected = new DynamicList(
                new DynamicListItem("", ""),
                Arrays.asList(new DynamicListItem(SelectWhoReviewsCase.REVIEW_BY_TCW.getId(), SelectWhoReviewsCase.REVIEW_BY_TCW.getLabel()),
                        new DynamicListItem(SelectWhoReviewsCase.REVIEW_BY_JUDGE.getId(), SelectWhoReviewsCase.REVIEW_BY_JUDGE.getLabel()),
                        new DynamicListItem(SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
        );
        assertEquals(expected, result.getData().getSelectWhoReviewsCase());
    }

    @Test
    public void callToAboutToSubmit_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/validSendToInterlocSelectedWhoToReviewCase.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertNull(result.getData().getSelectWhoReviewsCase());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), result.getData().getInterlocReviewState());
    }

    @Test
    public void callToAboutToSubmitPostponementRequest_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/validSendToInterlocSelectedWhoToReviewCasePostponementRequest.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertNull(result.getData().getSelectWhoReviewsCase());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), result.getData().getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId(), result.getData().getInterlocReferralReason());
        assertEquals(DocumentType.POSTPONEMENT_REQUEST.getValue(), result.getData().getSscsDocument().get(3).getValue().getDocumentType());
    }

}


