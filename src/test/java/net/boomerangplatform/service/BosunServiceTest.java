package net.boomerangplatform.service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.AbstractBoomerangTest;
import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.entity.PolicyActivityEntity;
import net.boomerangplatform.model.Policy;
import net.boomerangplatform.model.PolicyActivitiesInsights;
import net.boomerangplatform.model.PolicyDefinition;
import net.boomerangplatform.model.PolicyInsights;
import net.boomerangplatform.model.PolicyValidation;
import net.boomerangplatform.model.PolicyViolations;
import net.boomerangplatform.mongo.entity.CiComponentActivityEntity;
import net.boomerangplatform.mongo.service.CiComponentActivityService;
import net.boomerangplatform.repository.model.Artifact;
import net.boomerangplatform.repository.model.ArtifactPackage;
import net.boomerangplatform.repository.model.ArtifactSummary;
import net.boomerangplatform.repository.model.Cfe;
import net.boomerangplatform.repository.model.Component;
import net.boomerangplatform.repository.model.DependencyGraph;
import net.boomerangplatform.repository.model.General;
import net.boomerangplatform.repository.model.Issue;
import net.boomerangplatform.repository.model.Issues;
import net.boomerangplatform.repository.model.License;
import net.boomerangplatform.repository.model.Measures;
import net.boomerangplatform.repository.model.SonarQubeReport;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles(profiles = "test")
@SpringBootTest
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
public class BosunServiceTest extends BosunTests {

  private final static LocalDate LOCAL_DATE = LocalDate.of(2019, 05, 15);
  
  @Autowired
  private CiComponentActivityService activityService;

  @Autowired
  private BosunService bosunService;

  @Autowired
  @Qualifier("internalRestTemplate")
//  @Resource(name = "internalRestTemplate")
  RestTemplate restTemplate;

  private MockRestServiceServer server;

  @MockBean
  private Clock clock;

  private Clock fixedClock;

  @Override
  public void setUp() {
    super.setUp();
    fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault());
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
  }

//  @Test
//  public void testGetAllDefinitions() {
//    List<PolicyDefinition> definitions = bosunService.getAllDefinitions();
//
//    Assert.assertEquals(3, definitions.size());
//    PolicyDefinition definition = definitions.get(0);
//    Assert.assertEquals(3, definition.getRules().size());
//  }

  @Test
  public void testGetAllOperators() throws JsonProcessingException {
    Map<String, String> operators = bosunService.getAllOperators();

    Assert.assertEquals(5, operators.size());
    Assert.assertEquals("Equals", operators.get("EQUALS"));
  }

  @Test
  public void testGetPoliciesByTeamId() throws JsonProcessingException {
    String teamId = "5cedb53fdd1be20001f3d8c2";

    List<Policy> policies = bosunService.getPoliciesByTeamId(teamId);

    Assert.assertEquals(1, policies.size());

    Policy policy = policies.get(0);

    Assert.assertEquals("Code Medium Validation", policy.getName());
    Assert.assertEquals("5c5b5a0b352b1b614143b7c3", policy.getId());
    Assert.assertEquals(teamId, policy.getTeamId());

    Assert.assertEquals(1, policy.getDefinitions().size());

    PolicyDefinition definition = policy.getDefinitions().get(0);
    Assert.assertEquals("5cd328ae1e9bbbb710590d9d", definition.getPolicyTemplateId());

    Assert.assertEquals(2, definition.getRules().size());
    Map<String, String> rule = definition.getRules().get(0);

    Assert.assertEquals("lines", rule.get("metric"));
    Assert.assertEquals("88", rule.get("value"));
  }


  @Test
  public void testAddPolicy() throws IOException {
    Policy policy = new ObjectMapper().readValue(loadResourceAsString("addCiPolicyEntity.json"),
        Policy.class);
    Policy policyReturn = bosunService.addPolicy(policy);

    System.out.println(parseToJson(policyReturn));

    Assert.assertEquals("Code High Validation", policyReturn.getName());
    Assert.assertEquals(1, policyReturn.getDefinitions().size());

    PolicyDefinition definition = policyReturn.getDefinitions().get(0);


    String definitionId = definition.getPolicyTemplateId();
    Assert.assertEquals("5cd328ae1e9bbbb710590d9d", definitionId);


    List<Policy> policies = bosunService.getPoliciesByTeamId("5cedb53fdd1be20001f3d8c2");
    Assert.assertEquals(1, policies.size());

    Policy policyFound = policies.get(0);
    Assert.assertEquals(definitionId,
        policyFound.getDefinitions().get(0).getPolicyTemplateId());
  }


  @Test
  public void testUpdatePolicy() throws IOException {
    Policy policy = new ObjectMapper()
        .readValue(loadResourceAsString("updateCiPolicyEntity.json"), Policy.class);
    Policy policyReturn = bosunService.updatePolicy(policy);

    System.out.println(parseToJson(policyReturn));

    Assert.assertEquals("Code Low Validation", policyReturn.getName());

    PolicyDefinition ciPolicyConfig = policyReturn.getDefinitions().get(0);
    String definitionId = ciPolicyConfig.getPolicyTemplateId();
    Assert.assertEquals("5cd328ae1e9bbbb710590d9d", definitionId);


    List<Policy> policies = bosunService.getPoliciesByTeamId("9999");
    Assert.assertEquals(1, policies.size());

    Policy policyFound = policies.get(0);
    Assert.assertEquals("Code Low Validation", policyFound.getName());
  }

  @Test
  public void testGetInsights() throws IOException {
    List<PolicyInsights> insights = bosunService.getInsights("9999");

    Assert.assertEquals(1, insights.size());
    PolicyInsights entry = insights.get(0);
    Policy policy = bosunService.getPolicyById(entry.getPolicyId());
    Assert.assertEquals("Code Medium Validation", policy.getName());
    Assert.assertEquals("5c5b5a0b352b1b614143b7c3", policy.getId());
    Integer failCount = 0;
    for (PolicyActivitiesInsights ciPolicyActivitiesInsights : entry.getInsights()) {
      failCount += ciPolicyActivitiesInsights.getViolations();
    }
    Assert.assertEquals(Integer.valueOf(3), failCount);
  }

  @Test
  public void testValidatePolicyWithStaticCodeAnalyse() throws JsonProcessingException {

    this.server = MockRestServiceServer.createServer(restTemplate);

    String sonarQubeURL =
        "http://localhost:8080/repository/sonarqube/report?ciComponentId=5cedbec5dd1be20001f3d942&version=nextgen-2";

    this.server.expect(requestTo(sonarQubeURL))
        .andRespond(withSuccess(parseToJson(getSonarQubeReport()), MediaType.APPLICATION_JSON));

    String opaURL = "http://localhost:8181/v1/data/citadel/static_code_analysis";

    this.server.expect(requestTo(opaURL)).andRespond(
        withSuccess(loadResourceAsString("dataResponse.json"), MediaType.APPLICATION_JSON));

    String componentActivityId = "5cee1d76dd1be20001f3d9c5";
    String ciPolicyId = "5db85e35110fc4000140a5ad";
    String componentId = "";
    String version = "";

    PolicyValidation policyValidation = new PolicyValidation();
    policyValidation.setPolicyId(ciPolicyId);
    
    Map<String, String> label = new HashMap<>();
    label.put("sonarqube-id", componentId);
    label.put("sonarqube-version", version);
    label.put("artifact-version", version);
    
    CiComponentActivityEntity activityEntity = activityService.findById(componentActivityId);
    
    policyValidation.setReferenceId(componentId + activityEntity.getCiStageId());
    
    PolicyActivityEntity savedEntity =
        bosunService.validatePolicy(policyValidation);

    Assert.assertEquals("5c5b5a0b352b1b614143b7c3", savedEntity.getPolicyId());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getValid());
    Assert.assertEquals(1, savedEntity.getResults().size());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getResults().get(0).getValid());
//    Assert.assertEquals("[]", savedEntity.getResults().get(0).getViolations());
    Assert.assertEquals("5cd328ae1e9bbbb710590d9d",
        savedEntity.getResults().get(0).getPolicyTemplateId());

    server.verify();

  }

  @Test
  public void testValidatePolicyWithCveSafelist() throws JsonProcessingException {

    this.server = MockRestServiceServer.createServer(restTemplate);

    String sonarQubeURL =
        "http://localhost:8080/repository/xray/artifact/summary?ciComponentId=5cedbec5dd1be20001f3d942&version=nextgen-2";

    this.server.expect(requestTo(sonarQubeURL))
        .andRespond(withSuccess(parseToJson(getArtifactSummary()), MediaType.APPLICATION_JSON));

    String opaURL = "http://localhost:8181/v1/data/citadel/cve_safelist";

    this.server.expect(requestTo(opaURL)).andRespond(
        withSuccess(loadResourceAsString("dataResponse.json"), MediaType.APPLICATION_JSON));

    String componentActivityId = "5cee1d76dd1be20001f3d9c5";
    String ciPolicyId = "5db9a8c7b01c530001b838d1";
    String componentId = "";
    String version = "";

    PolicyValidation policyValidation = new PolicyValidation();
    policyValidation.setPolicyId(ciPolicyId);
    
    Map<String, String> label = new HashMap<>();
    label.put("sonarqube-id", componentId);
    label.put("sonarqube-version", version);
    label.put("artifact-version", version);
    
    CiComponentActivityEntity activityEntity = activityService.findById(componentActivityId);
    
    policyValidation.setReferenceId(componentId + activityEntity.getCiStageId());
    
    PolicyActivityEntity savedEntity =
        bosunService.validatePolicy(policyValidation);
   

    Assert.assertEquals("5cf151691417760001c0a679", savedEntity.getPolicyId());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getValid());
    Assert.assertEquals(1, savedEntity.getResults().size());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getResults().get(0).getValid());
//    Assert.assertEquals("[]", savedEntity.getResults().get(0).getViolations());
    Assert.assertEquals("5cdd8425f6ea74a9bbaf2fe6",
        savedEntity.getResults().get(0).getPolicyTemplateId());

    server.verify();

  }

  @Test
  public void testValidatePolicyWithPackageSafelist() throws JsonProcessingException {

    this.server = MockRestServiceServer.createServer(restTemplate);

    String sonarQubeURL =
        "http://localhost:8080/repository/xray/artifact/dependencygraph?ciComponentId=5cedbec5dd1be20001f3d942&version=nextgen-2";

    this.server.expect(requestTo(sonarQubeURL))
        .andRespond(withSuccess(parseToJson(getDependencyGraph()), MediaType.APPLICATION_JSON));

    String opaURL = "http://localhost:8181/v1/data/citadel/package_safelist";

    this.server.expect(requestTo(opaURL)).andRespond(
        withSuccess(loadResourceAsString("dataResponse.json"), MediaType.APPLICATION_JSON));

    String componentActivityId = "5cee1d76dd1be20001f3d9c5";
    String ciPolicyId = "5dba1ce19e0f890001153730";
    String componentId = "";
    String version = "";

    PolicyValidation policyValidation = new PolicyValidation();
    policyValidation.setPolicyId(ciPolicyId);
    
    Map<String, String> label = new HashMap<>();
    label.put("sonarqube-id", componentId);
    label.put("sonarqube-version", version);
    label.put("artifact-version", version);
    
    CiComponentActivityEntity activityEntity = activityService.findById(componentActivityId);
    
    policyValidation.setReferenceId(componentId + activityEntity.getCiStageId());
    
    PolicyActivityEntity savedEntity =
        bosunService.validatePolicy(policyValidation);

    Assert.assertEquals("5cf151691417760001c0a675", savedEntity.getPolicyId());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getValid());
    Assert.assertEquals(1, savedEntity.getResults().size());
    Assert.assertEquals(Boolean.FALSE, savedEntity.getResults().get(0).getValid());
//    Assert.assertEquals("[]", savedEntity.getResults().get(0).getViolations());
    Assert.assertEquals("5cd498f3f6ea74a9bb6ad0f3",
        savedEntity.getResults().get(0).getPolicyTemplateId());

    server.verify();

  }

  @Test
  public void testGetViolations() throws JsonProcessingException {
    String teamId = "5cedb53fdd1be20001f3d8c2";
    
    List<PolicyViolations> violations = bosunService.getViolations(teamId);
    Assert.assertEquals(1, violations.size());
    PolicyViolations violation = violations.get(0);
    Assert.assertEquals("5cf151691417760001c0a679", violation.getPolicyId());
    Assert.assertEquals("Glens Zero Defns Test", violation.getPolicyName());
    Assert.assertEquals(2, violation.getNbrViolations().intValue());
  }

  private SonarQubeReport getSonarQubeReport() {
    SonarQubeReport report = new SonarQubeReport();

    Issues issues = new Issues();
    issues.setBlocker(1);
    issues.setCritical(2);
    issues.setFilesAnalyzed(99);
    issues.setInfo(1);
    issues.setMajor(0);
    issues.setMinor(15);
    issues.setTotal(19);

    Measures measures = new Measures();
    measures.setComplexity(9);
    measures.setNcloc(1);
    measures.setViolations(3);

    report.setIssues(issues);
    report.setMeasures(measures);

    return report;
  }

  private ArtifactSummary getArtifactSummary() {
    ArtifactSummary artifactSummary = new ArtifactSummary();

    Artifact artifact = new Artifact();
    Issue issue = new Issue();
    issue.setCreated("admin");
    issue.setSeverity("severity");
    issue.setDescription("description");
    issue.setIssueType("BUG");
    issue.setSummary("summary");
    issue.setProvider("provider");
    Cfe cfe = new Cfe();
    cfe.setCve("cve");
    cfe.setCvssV2("cvssV2");
    cfe.setCvssV3("cvssV3");
    cfe.setCwe(Arrays.asList("cwe"));
    issue.setCves(Arrays.asList(cfe));
    issue.setImpactPath(Arrays.asList("impactPaths"));

    General general = new General();
    general.setComponentId("componentId");
    general.setName("name");
    general.setPath("path");
    general.setPkgType("pkgType");
    general.setSha256("sha256");

    artifact.setIssues(Arrays.asList(issue));
    artifact.setGeneral(general);
    License license = new License();
    license.setComponents(Arrays.asList("componentId"));
    license.setFullName("fullName");
    license.setMoreInfoUrl(Arrays.asList("moreInfoUrl"));
    license.setName("name");
    artifact.setLicenses(Arrays.asList(license));
    artifactSummary.setArtifacts(Arrays.asList(artifact));

    return artifactSummary;
  }

  private DependencyGraph getDependencyGraph() {
    DependencyGraph dependencyGraph = new DependencyGraph();

    ArtifactPackage artifact = new ArtifactPackage();
    artifact.setComponentId("componentId");
    artifact.setName("name");
    artifact.setPath("path");
    artifact.setPkgType("pkgType");
    artifact.setSha256("sha256");
    dependencyGraph.setArtifact(artifact);

    Component component = new Component();
    component.setComponentId("componentId");
    component.setComponentName("componentName");
    component.setCreated("admin");
    component.setPackageType("packageType");

    dependencyGraph.setComponents(Arrays.asList(component));

    return dependencyGraph;
  }

}
