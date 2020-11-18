package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.uc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class UcIssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

    private UcDecisionNoticeOutcomeService ucDecisionNoticeOutcomeService;

    private DecisionNoticeService decisionNoticeService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    protected static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        ucDecisionNoticeOutcomeService = new UcDecisionNoticeOutcomeService(new UcDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(ucDecisionNoticeOutcomeService), new ArrayList<>());

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, validator);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build())
            .sscsDocument(documentList)
            .writeFinalDecisionTypeOfHearing("")
            .writeFinalDecisionPresentingOfficerAttendedQuestion("")
            .writeFinalDecisionAppellantAttendedQuestion("")
            .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
            .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
            .sscsUcCaseData(SscsUcCaseData.builder()
                .ucWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList())
                .ucWriteFinalDecisionSchedule7ActivitiesApply("")
                .ucWriteFinalDecisionAppropriatenessOfBehaviourQuestion("")
                .ucWriteFinalDecisionAwarenessOfHazardsQuestion("")
                .ucWriteFinalDecisionCommunicationQuestion("")
                .ucWriteFinalDecisionConsciousnessQuestion("")
                .ucWriteFinalDecisionCopingWithChangeQuestion("")
                .ucWriteFinalDecisionGettingAboutQuestion("")
                .ucWriteFinalDecisionLearningTasksQuestion("")
                .ucWriteFinalDecisionLossOfControlQuestion("")
                .ucWriteFinalDecisionMakingSelfUnderstoodQuestion("")
                .ucWriteFinalDecisionManualDexterityQuestion("")
                .ucWriteFinalDecisionMentalAssessmentQuestion(Arrays.asList())
                .ucWriteFinalDecisionMobilisingUnaidedQuestion("")
                .ucWriteFinalDecisionNavigationQuestion("")
                .ucWriteFinalDecisionPersonalActionQuestion("")
                .ucWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList())
                .ucWriteFinalDecisionPickingUpQuestion("")
                .ucWriteFinalDecisionReachingQuestion("")
                .ucWriteFinalDecisionSocialEngagementQuestion("")
                .ucWriteFinalDecisionStandingAndSittingQuestion("")
                .dwpReassessTheAward("")
                .build())
            .showSchedule8Paragraph4Page(YesNo.YES)
            .showSchedule7ActivitiesPage(YesNo.YES)
            .showFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES)
            .doesSchedule9Paragraph4Apply(YesNo.YES)
            .doesSchedule8Paragraph4Apply(YesNo.YES)
            .writeFinalDecisionStartDate("")
            .writeFinalDecisionEndDateType("")
            .writeFinalDecisionEndDate("")
            .writeFinalDecisionDateOfDecision("")
            .writeFinalDecisionReasons(Arrays.asList(new CollectionItem(null, "")))
            .writeFinalDecisionPageSectionReference("")
            .writeFinalDecisionAnythingElse("something else")
            .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEventForGenerateNoticeFlowWhenAllowedOrRefusedIsNull_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWcaAppeal("Yes");
        callback.getCaseDetails().getCaseData().setSupportGroupOnlyAppeal("Yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused(null);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getDwpReassessTheAward());
        assertNotNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getShowSchedule8Paragraph4Page());
        assertNotNull(sscsCaseData.getShowSchedule7ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getDoesSchedule9Paragraph4Apply());
        assertNotNull(sscsCaseData.getDoesSchedule8Paragraph4Apply());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForGenerateNoticeFlowWhenAllowedOrRefusedIsNotNull_ThenDoNotDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWcaAppeal("Yes");
        callback.getCaseDetails().getCaseData().setSupportGroupOnlyAppeal("Yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getDwpReassessTheAward());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getDoesSchedule9Paragraph4Apply());
        assertNull(sscsCaseData.getDoesSchedule8Paragraph4Apply());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }


    @Test
    @Parameters({
        "allowed, decisionInFavourOfAppellant",
        "refused, decisionUpheld"
    })
    public void givenAnIssueFinalDecisionEvent_thenCreateDecisionWithFooterAndClearTransientFields(String allowedOrRefused, String expectedOutcome) {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused(allowedOrRefused);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals(expectedOutcome, response.getData().getOutcome());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getSscsUcCaseData().getDwpReassessTheAward());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getDoesSchedule9Paragraph4Apply());
        assertNull(sscsCaseData.getDoesSchedule8Paragraph4Apply());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
