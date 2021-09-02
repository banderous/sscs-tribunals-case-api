package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.*;

@Service
@Slf4j
public class ActionPostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private UserDetailsService userDetailsService;

    @Autowired
    public ActionPostponementRequestAboutToSubmitHandler(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        String details = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();
        if (StringUtils.isBlank(details)) {
//            preSubmitCallbackResponse.addError(POSTPONEMENT_DETAILS_IS_MANDATORY);
        } else {
            if (sscsCaseData.getAppealNotePad() == null) {
                sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<>()).build());
            }
            sscsCaseData.getAppealNotePad().getNotesCollection()
                    .add(createPostponementRequestNote(userAuthorisation, details));
        }

        return response;
    }

    private Note createPostponementRequestNote(String userAuthorisation, String details) {
        return Note.builder().value(NoteDetails.builder().noteDetail(details)
                .author(userDetailsService.buildLoggedInUserName(userAuthorisation))
                .noteDate(LocalDate.now().toString()).build()).build();
    }

}
