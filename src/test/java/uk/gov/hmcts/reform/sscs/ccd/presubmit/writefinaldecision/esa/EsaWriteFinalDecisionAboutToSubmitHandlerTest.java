package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class EsaWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase {

    public EsaWriteFinalDecisionAboutToSubmitHandlerTest() throws IOException {
        super(new EsaDecisionNoticeQuestionService());
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithYesAndPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have submitted an unexpected answer for the Regulation 29 question. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsPopulatedWithNoAndPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for regulation 29 to apply
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have submitted an unexpected answer for the Regulation 29 question. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation29FieldIsNotPopulatedAndPointsAreLowAndRequireItToBePopulated_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1w");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have submitted a missing answer for the Regulation 29 question. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation35FieldIsPopulatedWithYesAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have submitted an unexpected answer for the Regulation 35 question. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenRegulation35FieldIsPopulatedWithNoAndRegulation29FieldIsPopulatedWithNoAndPointsAreCorrectForRegulation29ButIncorrectForRegulation35_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points - low, which means regulation 29 must apply.
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have submitted an unexpected answer for the Regulation 35 question. The points awarded don't match. Please review your previous selection.", error);
    }
    
    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(
            Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }
}
