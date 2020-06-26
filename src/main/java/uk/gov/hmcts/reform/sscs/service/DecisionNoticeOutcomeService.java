package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.ComparedRate;

@Slf4j
@Service
public class DecisionNoticeOutcomeService {

    public Outcome determineOutcome(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getWriteFinalDecisionIsDescriptorFlow() == null) {
            // We need at least this flag to be set in order to determine outcome
            return null;
        } else {
            if (sscsCaseData.isDailyLivingAndOrMobilityDecision()) {
                if ("yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {
                    // If we are generating the notice we use the daily living/mobility descriptors
                    // to determine outcome
                    return determineGenerateNoticeDailyLivingOrMobilityFlowOutcome(sscsCaseData);
                } else {
                    // If we are not generating the notice we use an explicitly set outcome
                    return useExplicitySetOutcome(sscsCaseData);
                }
            } else {
                // If we are in the non-descriptor flow we use an explicitly set outcome.
                return useExplicitySetOutcome(sscsCaseData);
            }
        }
    }

    private Outcome determineGenerateNoticeDailyLivingOrMobilityFlowOutcome(SscsCaseData sscsCaseData) {

        // Daily living and or/mobility

        if (sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion() == null
            || sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion() == null) {
            return null;
        } else {

            try {

                ComparedRate dailyLivingComparedRate = ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
                ComparedRate mobilityComparedRate = ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());

                Set<ComparedRate> comparedRates = new HashSet<>();
                comparedRates.add(dailyLivingComparedRate);
                comparedRates.add(mobilityComparedRate);

                // At least one higher,  and non lower, means the decision is in favour of appellant
                if (comparedRates.contains(ComparedRate.Higher)
                    && !comparedRates.contains(ComparedRate.Lower)) {
                    return DECISION_IN_FAVOUR_OF_APPELLANT;
                } else {
                    // Otherwise, decision upheld
                    return DECISION_UPHELD;
                }

            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
                return null;
            }

        }
    }

    private Outcome useExplicitySetOutcome(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionAllowedOrRefused() == null) {
            return null;
        } else {
            if ("allowed".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionAllowedOrRefused())) {
                return DECISION_IN_FAVOUR_OF_APPELLANT;
            } else {
                return DECISION_UPHELD;
            }
        }
    }
}
