package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;


@SpringBootTest
@AutoConfigureMockMvc
public class UpdateSscs5OtherPartyDataIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/updateOtherPartySscs5Callback.json");
    }

    @Test
    public void callToAboutToSubmit_willUpdateSscs5CaseWhenRoleIsNotEntered() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(2, result.getData().getOtherParties().size());
        assertEquals("1", result.getData().getOtherParties().get(0).getValue().getId());
        assertEquals("2", result.getData().getOtherParties().get(0).getValue().getAppointee().getId());
        assertEquals("3", result.getData().getOtherParties().get(0).getValue().getRep().getId());
        assertFalse(
            YesNo.isYes(result.getData().getOtherParties().get(0).getValue().getSendNewOtherPartyNotification()));
        assertEquals("4", result.getData().getOtherParties().get(1).getValue().getId());
        assertTrue(
            YesNo.isYes(result.getData().getOtherParties().get(1).getValue().getSendNewOtherPartyNotification()));
        assertNull(result.getData().getOtherParties().get(1).getValue().getAppointee().getId());
        assertNull(result.getData().getOtherParties().get(1).getValue().getRep().getId());
        assertNull(result.getData().getOtherParties().get(0).getValue().getRole());
        assertEquals(YesNo.NO, result.getData().getOtherParties().get(0).getValue().getShowRole());
        assertNull(result.getData().getOtherParties().get(1).getValue().getRole());
        assertEquals(YesNo.NO, result.getData().getOtherParties().get(1).getValue().getShowRole());
    }

}


