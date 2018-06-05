package uk.gov.hmcts.sscs.sya;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.controller.SyaController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.idam.Authorize;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.service.idam.AuthTokenSubjectExtractor;
import uk.gov.hmcts.sscs.service.idam.IdamApiClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class SyaEndpointsIt {

    private static final String PDF = "abc";

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private PDFServiceClient pdfServiceClient;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private AuthTokenSubjectExtractor authTokenSubjectExtractor;

    @Captor
    private ArgumentCaptor captor;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper;

    private Session session = Session.getInstance(new Properties());

    @Autowired
    SyaController controller;

    @Value("${appellant.appeal.html.template.path}")
    private String templateName;

    @Value("${appeal.email.from}")
    private String emailFrom;

    @Value("${appeal.email.to}")
    private String emailTo;

    private SyaCaseWrapper caseWrapper;

    private MimeMessage message;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        caseWrapper = getCaseWrapper();

        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);

        given(pdfServiceClient.generateFromHtml(eq(getTemplate()), (Map<String, Object>) captor.capture()))
                .willReturn(PDF.getBytes());

        given(coreCaseDataApi.readForCaseWorker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).willReturn(null);

        Authorize authorize = new Authorize("redirectUrl/", "code", "token");
        given(idamApiClient.authorizeCodeType(anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);
        given(idamApiClient.authorizeToken(anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);

        given(authTokenGenerator.generate()).willReturn("authToken");

        given(authTokenSubjectExtractor.extract(anyString())).willReturn("userId");
    }

    @Test
    public void shouldGeneratePdfAndSend() throws Exception {
        given(coreCaseDataApi.startForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).willReturn(StartEventResponse.builder().build());

        given(coreCaseDataApi.submitForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyBoolean(), any(CaseDataContent.class))).willReturn(CaseDetails.builder().id(123456789876L).build());

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCase()))
                .andExpect(status().isCreated());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));
        assertThat(getPdf(), is(PDF));

        verify(mailSender).send(message);

        assertNotNull(getPdfWrapper().getCcdCaseId());
    }

    private PdfWrapper getPdfWrapper() {
        Map placeHolders = (Map) captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void shouldSendEmailWithPdfWhenCcdIsDown() throws Exception {
        given(coreCaseDataApi.searchForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyMap())).willThrow(new RuntimeException("CCD is down"));

        given(coreCaseDataApi.startForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).willThrow(new RuntimeException("CCD is down"));

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCase()))
                .andExpect(status().isCreated());

        then(mailSender).should(times(1)).send(message);

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));
        assertThat(getPdf(), is(PDF));

        assertNull(getPdfWrapper().getCcdCaseId());
    }

    private String getPdf() throws IOException, MessagingException {
        MimeMultipart content = (MimeMultipart) new MimeMessageHelper(message).getMimeMessage().getContent();
        InputStream input = (InputStream) content.getBodyPart(1).getContent();

        return IOUtils.toString(input, Charset.defaultCharset());
    }

    private byte[] getTemplate() throws IOException {
        URL resource = getClass().getResource(templateName);
        return IOUtils.toByteArray(resource);
    }

    private String getCase() {
        String syaCaseJson = "json/sya.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        try {
            return IOUtils.toString(resource, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private SyaCaseWrapper getCaseWrapper() throws IOException {
        String syaCaseJson = "json/sya.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        return mapper.readValue(resource, SyaCaseWrapper.class);
    }
}
