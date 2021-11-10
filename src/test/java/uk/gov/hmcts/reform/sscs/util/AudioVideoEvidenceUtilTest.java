package uk.gov.hmcts.reform.sscs.util;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@RunWith(JUnitParamsRunner.class)
public class AudioVideoEvidenceUtilTest {

    @Test
    public void shouldSetFlagToYes() {
        SscsCaseData caseData = SscsCaseData.builder().audioVideoEvidence(List.of(AudioVideoEvidence.builder().build())).build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.YES, caseData.getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void shouldSetFlagToNoProvidedNullEvidenceList() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.NO, caseData.getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void shouldSetFlagToNoProvidedEmptyEvidenceList() {
        SscsCaseData caseData = SscsCaseData.builder().audioVideoEvidence(List.of()).build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.NO, caseData.getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    @Parameters({
            "appellantEvidence",
            "jointPartyEvidence",
            "representativeEvidence",
            "dwpEvidence",
            "hmctsEvidence"
    })
    public void shouldBeValidAudioVideoDocumentType(String documentType) {
        assertTrue(AudioVideoEvidenceUtil.isValidAudioVideoDocumentType(documentType));
    }

    @Test
    public void shouldNotBeValidAudioVideoDocumentType() {
        assertFalse(AudioVideoEvidenceUtil.isValidAudioVideoDocumentType("audioDocument"));
    }

    @Test
    @Parameters({
            "jointPartyEvidence, Joint party",
            "representativeEvidence, Representative",
            "dwpEvidence, DWP",
            "hmctsEvidence, HMCTS",
            "default, Appellant"
    })
    public void shouldMatchOriginalSender(String input, String expected) {
        assertEquals(AudioVideoEvidenceUtil.getOriginalSender(input), expected);
    }
}
