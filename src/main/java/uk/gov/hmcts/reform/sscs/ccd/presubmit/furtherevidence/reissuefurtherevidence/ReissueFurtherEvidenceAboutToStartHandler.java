package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.userFriendlyName;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        List<? extends AbstractDocument> allSscsDocs = Stream.of(sscsCaseData.getSscsDocument(), sscsCaseData.getSscsWelshDocuments()).flatMap(x -> x == null ? null : x.stream()).filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentType())).collect(Collectors.toList());

        ArrayList<? extends AbstractDocument> availableDocumentsToReIssue =
                Optional.ofNullable(allSscsDocs).map(Collection::stream)
                        .orElse(Stream.empty()).filter(f ->
                                APPELLANT_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                        || REPRESENTATIVE_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                        || DWP_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                        ).collect(Collectors.toCollection(ArrayList::new));

        if (CollectionUtils.isNotEmpty(availableDocumentsToReIssue)) {
            setDocumentDropdown(sscsCaseData, availableDocumentsToReIssue);
            ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
            reissueArtifactUi.setResendToAppellant(null);
            reissueArtifactUi.setResendToRepresentative(null);
            reissueArtifactUi.setResendToDwp(null);
            reissueArtifactUi.setOtherPartyOptions(null);
            sscsCaseData.setOriginalSender(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isEmpty(availableDocumentsToReIssue)) {
            response.addError("There are no evidence documents in the appeal. Cannot reissue further evidence.");
        }

        if (isOtherPartyPresent(sscsCaseData)) {
            setUpOtherPartyOptions(sscsCaseData);
        }

        return response;
    }

    private void setUpOtherPartyOptions(SscsCaseData sscsCaseData) {
        sscsCaseData.getReissueArtifactUi().setShowReissueToOtherPartyUiSection(YesNo.YES);
        sscsCaseData.getReissueArtifactUi().setOtherPartyOptions(getOtherPartyOptions(sscsCaseData));
    }

    private void setDocumentDropdown(SscsCaseData sscsCaseData, List<? extends AbstractDocument> availableDocumentsToReIssue) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        for (AbstractDocument doc : availableDocumentsToReIssue) {
            String label = buildFormattedLabel(doc);
            if (doc.getValue().getDocumentLink() != null) {
                listCostOptions.add(new DynamicListItem(doc.getValue().getDocumentLink().getDocumentUrl(), label));
            }
        }

        sscsCaseData.getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(listCostOptions.get(0), listCostOptions));
    }

    private String buildFormattedLabel(AbstractDocument doc) {
        String filenameLabel = doc.getValue().getDocumentFileName();
        if (doc instanceof SscsWelshDocument) {
            filenameLabel = getBilingualLabel(doc);
        }
        return String.format("%s -  %s", filenameLabel, userFriendlyName(doc.getValue().getDocumentType()));
    }

    @NotNull
    private String getBilingualLabel(AbstractDocument doc) {
        StringBuilder sb = new StringBuilder("Bilingual - ");
        if (doc.getValue().getDocumentLink().getDocumentFilename() != null) {
            sb.append(doc.getValue().getDocumentLink().getDocumentFilename());
        }
        return sb.toString();
    }

    private List<OtherPartyOption> getOtherPartyOptions(SscsCaseData sscsCaseData) {
        List<OtherPartyOption> otherPartyOptions = new ArrayList<>();

        sscsCaseData.getOtherParties().forEach(otherParty -> addOtherPartyOption(otherPartyOptions, otherParty));

        return otherPartyOptions;
    }

    private void addOtherPartyOption(List<OtherPartyOption> otherPartyOptions, CcdValue<OtherParty> otherParty) {
        OtherParty otherPartyDetail = otherParty.getValue();

        if (isOtherPartyWithAppointee(otherPartyDetail)) {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getAppointee().getName().getFullNameNoTitle() + " - Appointee", otherPartyDetail.getAppointee().getId()));
        } else {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getName().getFullNameNoTitle(), otherPartyDetail.getId()));
        }

        if (isOtherPartyWithRepresentative(otherPartyDetail)) {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getRep().getName().getFullNameNoTitle() + " - Representative", otherPartyDetail.getRep().getId()));
        }
    }

    private boolean isOtherPartyWithRepresentative(OtherParty otherPartyDetail) {
        return otherPartyDetail.getRep() != null && "Yes".equals(otherPartyDetail.getRep().getHasRepresentative());
    }

    private boolean isOtherPartyWithAppointee(OtherParty otherPartyDetail) {
        return otherPartyDetail.getAppointee() != null && "Yes".equals(otherPartyDetail.getIsAppointee());
    }

    private OtherPartyOption getOtherPartyElement(String name, String id) {
        return OtherPartyOption.builder()
                .value(OtherPartyOptionDetails.builder()
                        .otherPartyOptionName(name)
                        .otherPartyOptionId(id)
                        .build()).build();
    }
}
