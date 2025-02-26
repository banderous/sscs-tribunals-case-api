package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest.PostponementRequestAboutToStartHandlerTest.getHearing;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.FILENAME;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@RunWith(JUnitParamsRunner.class)
public class PostponementRequestMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private PostponementRequestMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @Mock
    private GenerateFile generateFile;


    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private String templateId = "templateId.docx";


    @Before
    public void setUp() {
        openMocks(this);
        handler = new PostponementRequestMidEventHandler(generateFile, templateId);

        Hearing hearing = getHearing(1);
        List<Hearing> hearings  = List.of(hearing);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .state(State.HEARING)
                .hearings(hearings)
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(null)
                        .postponementRequestHearingDateAndTime(LocalDateTime.now().plusDays(1).toString())
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.POSTPONEMENT_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenANonPostponementRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenAPostponementRequest_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenThereIsNoRequestDetails_thenReturnAnErrorMessage() {
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please enter request details to generate a postponement request document"));
    }

    @Test
    public void givenRequestDetails_thenCreatePdfFromItAndStoreItInThePreviewDocuments() {
        String dmUrl = "http://dm-store/documents/123";
        when(generateFile.assemble(any())).thenReturn(dmUrl);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        DocumentLink documentLink = DocumentLink.builder()
                .documentBinaryUrl(dmUrl + "/binary")
                .documentUrl(dmUrl)
                .documentFilename(FILENAME)
                .build();
        assertThat(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument(), is(documentLink));
    }

}
