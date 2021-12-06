package uk.gov.hmcts.reform.sscs.util;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class PartiesOnCaseUtil {
    private PartiesOnCaseUtil() {
        //
    }

    public static List<DynamicListItem> getPartiesOnCaseWithDwpAndHmcts(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        listOptions.add(new DynamicListItem(HMCTS.getCode(), HMCTS.getLabel()));

        return listOptions;
    }

    public static List<DynamicListItem> getPartiesOnCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        if (sscsCaseData.isThereAJointParty()) {
            listOptions.add(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        }

        if (sscsCaseData.getAppeal().getRep() != null
                && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }

        if (isChildSupportAppeal(sscsCaseData) && isNotEmpty(sscsCaseData.getOtherParties())) {
            addOtherPartiesToListOptions(sscsCaseData, listOptions);
        }

        return listOptions;
    }

    private static void addOtherPartiesToListOptions(SscsCaseData sscsCaseData, List<DynamicListItem> listOptions) {
        for (int i = 0; i < sscsCaseData.getOtherParties().size(); i++) {
            OtherParty otherParty = sscsCaseData.getOtherParties().get(i).getValue();
            addOtherPartyOrOtherPartyAppointeeToListOptions(listOptions, i, otherParty);
            addOtherPartyRepresentativeToListOptions(listOptions, i, otherParty);
        }
    }

    private static void addOtherPartyRepresentativeToListOptions(List<DynamicListItem> listOptions, int i, OtherParty otherParty) {
        if (isYes(ofNullable(otherParty.getRep()).map(Representative::getHasRepresentative).orElse(NO.getValue())) && otherParty.getRep() != null && otherParty.getRep().getName() != null) {
            listOptions.add(new DynamicListItem(OTHER_PARTY_REPRESENTATIVE.getCode(), format("%s %s - Representative - %s", OTHER_PARTY_REPRESENTATIVE.getLabel(), i + 1, otherParty.getRep().getName().getFullNameNoTitle())));
        }
    }

    private static void addOtherPartyOrOtherPartyAppointeeToListOptions(List<DynamicListItem> listOptions, int i, OtherParty otherParty) {
        if (isYes(otherParty.getIsAppointee()) && otherParty.getAppointee() != null && otherParty.getAppointee().getName() != null) {
            listOptions.add(new DynamicListItem(OTHER_PARTY.getCode(), format("%s %s - %s / Appointee - %s", OTHER_PARTY.getLabel(), i + 1, otherParty.getName().getFullNameNoTitle(), otherParty.getAppointee().getName().getFullNameNoTitle())));
        } else {
            listOptions.add(new DynamicListItem(OTHER_PARTY.getCode(), format("%s %s - %s", OTHER_PARTY.getLabel(), i + 1, otherParty.getName().getFullNameNoTitle())));
        }
    }

    private static boolean isChildSupportAppeal(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitType()
                .filter(f -> f == Benefit.CHILD_SUPPORT)
                .isPresent();
    }

    public static List<Pair<String, String>> getAllOtherPartiesOnCase(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(PartiesOnCaseUtil::getOtherPartyIdAndName)
                .collect(Collectors.toList());
    }

    private static Stream<Pair<String, String>> getOtherPartyIdAndName(OtherParty otherParty) {
        List<Pair<String, String>> otherParties = new ArrayList<>();
        if (isYes(otherParty.getIsAppointee()) && null != otherParty.getAppointee()) {
            otherParties.add(Pair.of(otherParty.getAppointee().getId(), otherParty.getAppointee().getName().getFullNameNoTitle() + " - Appointee"));
        } else {
            otherParties.add(Pair.of(otherParty.getId(), otherParty.getName().getFullNameNoTitle()));
        }

        if (null != otherParty.getRep() && isYes(otherParty.getRep().getHasRepresentative())) {
            otherParties.add(Pair.of(otherParty.getRep().getId(), otherParty.getRep().getName().getFullNameNoTitle() + " - Representative"));
        }
        return otherParties.stream();
    }
}
