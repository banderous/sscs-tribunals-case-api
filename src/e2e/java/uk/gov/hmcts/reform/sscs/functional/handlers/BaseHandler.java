package uk.gov.hmcts.reform.sscs.functional.handlers;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;

import feign.FeignException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
public class BaseHandler {

    protected static final String CREATED_BY_FUNCTIONAL_TEST = "created by functional test";

    @Autowired
    protected CcdService ccdService;

    @Autowired
    private IdamService idamService;

    protected IdamTokens idamTokens;

    @Value("${test-url}")
    private String testUrl;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    protected SscsCaseDetails createCaseInResponseReceivedState(int retry) throws Exception {
        return ccdService.createCase(buildSscsCaseDataForTesting("Mercury", "JK 77 33 22 Z"),
                EventType.CREATE_RESPONSE_RECEIVED_TEST_CASE.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
                CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails createCaseInWithDwpState(int retry) throws Exception {
        return ccdService.createCase(buildSscsCaseDataForTesting("Lennon", "BB 22 55 66 B"),
                EventType.CREATE_WITH_DWP_TEST_CASE.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
                CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected String getMyaResponse(int retry, Long caseId) {
        Response response = null;
        while (retry > 0 && (response == null || response.statusCode() != HttpStatus.OK.value())) {
            response = RestAssured
                    .given()
                    .when()
                    .get("appeals?mya=true&caseId=" + caseId);
            retry--;
        }

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return response.then().extract().body().asString();
    }

    private SscsCaseDetails updateCase(int retry, SscsCaseData caseData, Long caseId, String eventType, String summary, String description, IdamTokens idamTokens) throws Exception {
        while (retry > 0) {
            retry--;
            Thread.sleep(5000);
            try {
                log.info("UpdateCase for caseId {} retry count {}", caseId, retry);
                return ccdService.updateCase(caseData, caseId, eventType, summary, description, idamTokens);
            } catch (FeignException feignException) {
                log.info("UpdateCase failed for caseId {} with error {}", caseId, feignException.getMessage());
                if (feignException.status() < HttpStatus.INTERNAL_SERVER_ERROR.value() || retry <= 0) {
                    throw feignException;
                }
            }
        }
        throw new Exception("UpdateCase failed for caseId:" + caseId);
    }


    public static String getJsonCallbackForTest(String path) throws IOException {
        String pathName = Objects.requireNonNull(BaseHandler.class.getClassLoader().getResource(path)).getFile();
        return FileUtils.readFileToString(new File(pathName), StandardCharsets.UTF_8.name());
    }

    public static String getJsonCallbackForTestAndReplace(String fileLocation, List<String> replaceKeys, List<String> replaceValues) throws IOException {
        String result = getJsonCallbackForTest(fileLocation);
        for (int i = 0; i < replaceKeys.size(); i++) {
            result = result.replace(replaceKeys.get(i), replaceValues.get(i));
        }
        return result;
    }

    protected static SscsCaseData buildSscsCaseDataForTesting(final String surname, final String nino) {
        SscsCaseData testCaseData = buildCaseData(surname, nino);

        addFurtherEvidenceActionData(testCaseData);

        Subscription appellantSubscription = Subscription.builder()
                .tya("app-appeal-number")
                .email("appellant@email.com")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscription appointeeSubscription = Subscription.builder()
                .tya("appointee-appeal-number")
                .email("appointee@hmcts.net")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscription supporterSubscription = Subscription.builder()
                .tya("")
                .email("supporter@email.com")
                .mobile("07700 900555")
                .subscribeEmail("")
                .subscribeSms("")
                .reason("")
                .build();
        Subscription representativeSubscription = Subscription.builder()
                .tya("rep-appeal-number")
                .email("representative@email.com")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .supporterSubscription(supporterSubscription)
                .representativeSubscription(representativeSubscription)
                .appointeeSubscription(appointeeSubscription)
                .build();

        testCaseData.setSubscriptions(subscriptions);
        return testCaseData;
    }

    private static void addFurtherEvidenceActionData(SscsCaseData testCaseData) {
        testCaseData.setInterlocReviewState(null);
        DynamicListItem value = new DynamicListItem("informationReceivedForInterlocJudge", "any");
        DynamicList furtherEvidenceActionList = new DynamicList(value, Collections.singletonList(value));
        testCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
    }

    private static SscsCaseData buildCaseData(final String surname, final String nino) {
        Name name = Name.builder()
                .title("Mr")
                .firstName("User")
                .lastName(surname)
                .build();
        Address address = Address.builder()
                .line1("123 Hairy Lane")
                .line2("Off Hairy Park")
                .town("Hairyfield")
                .county("Kent")
                .postcode("TN32 6PL")
                .build();
        Contact contact = Contact.builder()
                .email("mail@email.com")
                .phone("01234567890")
                .mobile("01234567890")
                .build();
        Identity identity = Identity.builder()
                .dob("1904-03-10")
                .nino(nino)
                .build();

        Appellant appellant = Appellant.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .build();

        BenefitType benefitType = BenefitType.builder()
                .code("PIP")
                .build();

        HearingOptions hearingOptions = HearingOptions.builder()
                .wantsToAttend(YES)
                .languageInterpreter(YES)
                .languages("Spanish")
                .signLanguageType("A sign language")
                .arrangements(Arrays.asList("hearingLoop", "signLanguageInterpreter"))
                .other("Yes, this...")
                .build();

        MrnDetails mrnDetails = MrnDetails.builder()
                .mrnDate("2018-06-29")
                .dwpIssuingOffice("1")
                .mrnLateReason("Lost my paperwork")
                .build();

        final Appeal appeal = Appeal.builder()
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .mrnDetails(mrnDetails)
                .signer("Signer")
                .hearingType("oral")
                .receivedVia("Online")
                .build();


        Subscription appellantSubscription = Subscription.builder()
                .tya("app-appeal-number")
                .email("appellant@email.com")
                .mobile("")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .build();
        return SscsCaseData.builder()
                .caseReference("SC068/17/00013")
                .caseCreated(LocalDate.now().toString())
                .appeal(appeal)
                .subscriptions(subscriptions)
                .region("CARDIFF")
                .createdInGapsFrom(READY_TO_LIST.getId())
                .build();
    }
}
