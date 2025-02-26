package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.checkConfidentiality;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish.IsScottishHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final AirLookupService airLookupService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private IdamService idamService;
    private final boolean workAllocationFeature;

    @Autowired
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper,
                                    AirLookupService airLookupService,
                                    DwpAddressLookupService dwpAddressLookupService,
                                    IdamService idamService,
                                    @Value("${feature.work-allocation.enabled}")  boolean workAllocationFeature) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.airLookupService = airLookupService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.idamService = idamService;
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CASE_UPDATED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();
        final SscsCaseData sscsCaseData = associatedCaseLinkHelper.linkCaseByNino(caseDetails.getCaseData(), caseDetailsBefore);

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        final boolean hasSuperUserRole = userDetails.hasRole(SUPER_USER);

        setCaseCode(preSubmitCallbackResponse, callback, hasSuperUserRole);
        validateBenefitForCase(preSubmitCallbackResponse, callback, hasSuperUserRole);
        if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
            return preSubmitCallbackResponse;
        }

        if (sscsCaseData.getAppeal().getAppellant() != null
                && sscsCaseData.getAppeal().getAppellant().getAddress() != null
                && isNotBlank(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode())) {


            String postCode = resolvePostCode(sscsCaseData);
            RegionalProcessingCenter newRpc = regionalProcessingCenterService.getByPostcode(postCode);

            maybeChangeIsScottish(sscsCaseData.getRegionalProcessingCenter(), newRpc, sscsCaseData);

            sscsCaseData.setRegionalProcessingCenter(newRpc);

            if (newRpc != null) {
                sscsCaseData.setRegion(newRpc.getName());
            }

            updateProcessingVenueIfRequired(caseDetails);

        }

        checkConfidentiality(sscsCaseData);
        updateCaseNameIfNameUpdated(callback, sscsCaseData);
        updateCaseCategoriesIfBenefitTypeUpdated(callback, sscsCaseData, preSubmitCallbackResponse);

        final boolean hasSystemUserRole = userDetails.hasRole(SYSTEM_USER);

        updateHearingTypeForNonSscs1Case(sscsCaseData, preSubmitCallbackResponse, hasSystemUserRole);

        //validate benefit type and dwp issuing office for updateCaseData event triggered by user, which is not by CaseLoader
        if (!hasSystemUserRole) {
            validateAndUpdateDwpHandlingOffice(sscsCaseData, preSubmitCallbackResponse);
            validateHearingOptions(sscsCaseData, preSubmitCallbackResponse);
        }

        return preSubmitCallbackResponse;
    }




    private void validateAndUpdateDwpHandlingOffice(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        MrnDetails mrnDetails = sscsCaseData.getAppeal().getMrnDetails();
        BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();
        boolean validBenefitType = validateBenefitType(benefitType,response);
        boolean validDwpIssuingOffice = validateDwpIssuingOffice(mrnDetails, benefitType, response);

        if (validBenefitType && validDwpIssuingOffice) {
            String regionalCenter = dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(benefitType.getCode(), mrnDetails.getDwpIssuingOffice());
            sscsCaseData.setDwpRegionalCentre(regionalCenter);
        }
    }

    private void validateHearingOptions(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
        if (hearingOptions != null && sscsCaseData.getAppeal().getHearingType() != null
            && HearingType.ORAL.getValue().equals(sscsCaseData.getAppeal().getHearingType())
            && !hearingOptions.isWantsToAttendHearing()) {
            response.addWarning("There is a mismatch between the hearing type and the wants to attend field, "
                + "all hearing options will be cleared please check if this is correct");
        }
    }

    private void updateHearingTypeForNonSscs1Case(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response, boolean hasSystemUserRole) {
        if (sscsCaseData.getAppeal().getHearingType() != null
            && sscsCaseData.getAppeal().getHearingOptions() != null
            && HearingType.PAPER.getValue().equals(sscsCaseData.getAppeal().getHearingType())
            && isBenefitTypeValidForHearingTypeValidation(response.getData().getBenefitType())
            && sscsCaseData.getAppeal().getHearingOptions().isWantsToAttendHearing().equals(Boolean.TRUE)) {

            response.getData().getAppeal().setHearingType(HearingType.ORAL.getValue());
            if (!hasSystemUserRole) {
                response.addWarning("The hearing type will be changed from Paper to Oral as at least one of the"
                    + " parties to the case would like to attend the hearing");
            }
        }
    }

    private boolean isBenefitTypeValidForHearingTypeValidation(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS2.equals(benefit.getSscsType())
            || SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    private boolean validateBenefitType(BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (benefitType == null || StringUtils.isEmpty(benefitType.getCode())) {
            response.addWarning("Benefit type code is empty");
            return false;
        } else if (Benefit.findBenefitByShortName(benefitType.getCode()).isEmpty()) {
            if (!workAllocationFeature) {
                //this can be removed once workallocation is in PROD as the validation and warnings are done
                //in updateCaseCategoriesIfBenefitTypeUpdated below
                String validBenefitTypes = Arrays.stream(Benefit.values()).sequential().map(Benefit::getShortName).collect(Collectors.joining(", "));
                response.addWarning("Benefit type code is invalid, should be one of: " + validBenefitTypes);
            }
            return false;
        }
        return true;
    }

    private boolean validateDwpIssuingOffice(MrnDetails mrnDetails, BenefitType benefitType, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (mrnDetails != null) {
            if (StringUtils.isEmpty(mrnDetails.getDwpIssuingOffice())) {
                response.addWarning("FTA issuing office is empty");
                return false;
            } else if (Benefit.findBenefitByShortName(benefitType.getCode()).isPresent()) {
                if (!dwpAddressLookupService.validateIssuingOffice(benefitType.getCode(), mrnDetails.getDwpIssuingOffice())) {
                    OfficeMapping[] officeMappings = dwpAddressLookupService.getDwpOfficeMappings(benefitType.getCode());
                    String validOffice = Arrays.stream(officeMappings).map(OfficeMapping::getCode).collect(Collectors.joining(", "));
                    response.addWarning("DWP issuing office is invalid, should one of: " + validOffice);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public void maybeChangeIsScottish(RegionalProcessingCenter oldRpc, RegionalProcessingCenter newRpc, SscsCaseData caseData) {
        if (oldRpc != newRpc) {
            String isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private void updateProcessingVenueIfRequired(CaseDetails<SscsCaseData> caseDetails) {

        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        String postCode = resolvePostCode(sscsCaseData);

        String venue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());

        if (venue != null && !venue.equalsIgnoreCase(sscsCaseData.getProcessingVenue())) {
            log.info("Case id: {} - setting venue name to {} from {}", caseDetails.getId(), venue, sscsCaseData.getProcessingVenue());

            sscsCaseData.setProcessingVenue(venue);
        }

    }

    private void updateCaseNameIfNameUpdated(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        if (workAllocationFeature) {
            String caseName = caseData.getAppeal().getAppellant() != null
                    && caseData.getAppeal().getAppellant().getName() != null
                    ? caseData.getAppeal().getAppellant().getName().getFullNameNoTitle()
                    : null;

            CaseDetails<SscsCaseData> oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
            if (oldCaseDetails == null
                    || oldCaseDetails.getCaseData().getWorkAllocationFields().getCaseNameHmctsInternal() == null
                    || !oldCaseDetails.getCaseData().getWorkAllocationFields().getCaseNameHmctsInternal().equals(caseName)) {
                caseData.getWorkAllocationFields().setCaseNames(caseName);
            }
        }
    }

    private void updateCaseCategoriesIfBenefitTypeUpdated(Callback<SscsCaseData> callback, SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (workAllocationFeature) {
            Optional<Benefit> benefit = sscsCaseData.getBenefitType();

            CaseDetails<SscsCaseData> oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
            Optional<Benefit> oldBenefit = getOldBenefitCode(oldCaseDetails);

            if (benefit.isPresent()) {
                sscsCaseData.getWorkAllocationFields().setCategories(benefit.get());
            } else if (benefitCodeHasValue(sscsCaseData)) {
                String validBenefitTypes = Arrays.stream(Benefit.values()).sequential().map(Benefit::getShortName).collect(Collectors.joining(", "));
                preSubmitCallbackResponse.addError("Benefit type code is invalid, should be one of: " + validBenefitTypes);

            } else if (oldBenefit.isPresent()) {
                preSubmitCallbackResponse.addError("Benefit type code is empty");
            }
        }
    }

    private boolean benefitCodeHasValue(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAppeal() != null
                && sscsCaseData.getAppeal().getBenefitType() != null
                && sscsCaseData.getAppeal().getBenefitType().getCode() != null
                && !isEmpty(sscsCaseData.getAppeal().getBenefitType().getCode());
    }

    private Optional<Benefit> getOldBenefitCode(CaseDetails<SscsCaseData> oldCaseDetails) {
        if (oldCaseDetails == null || oldCaseDetails.getCaseData() == null
                || oldCaseDetails.getCaseData().getBenefitType().isEmpty()) {
            return Optional.empty();
        } else {
            return oldCaseDetails.getCaseData().getBenefitType();
        }
    }

    private String resolvePostCode(SscsCaseData sscsCaseData) {
        String postCode;

        if ("yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            postCode = Optional.ofNullable(sscsCaseData.getAppeal().getAppellant().getAppointee())
                .map(Appointee::getAddress)
                .map(Address::getPostcode)
                .filter(appointeePostCode -> !"".equals(appointeePostCode))
                .orElse(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
        } else {
            postCode = sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();
        }
        
        return postCode;
    }

}
