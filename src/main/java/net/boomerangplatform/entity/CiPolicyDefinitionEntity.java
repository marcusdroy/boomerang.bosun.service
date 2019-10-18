package net.boomerangplatform.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import net.boomerangplatform.model.CiPolicyDefinitionConfig;

@Document(collection = "ci_policies_definitions")
public class CiPolicyDefinitionEntity {

  @Id
  private String id;

  private String key;

  private String name;

  private String description;

  private Integer order;

  private List<CiPolicyDefinitionConfig> config;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public List<CiPolicyDefinitionConfig> getConfig() {
    return config;
  }

  public void setConfig(List<CiPolicyDefinitionConfig> config) {
    this.config = config;
  }

}
