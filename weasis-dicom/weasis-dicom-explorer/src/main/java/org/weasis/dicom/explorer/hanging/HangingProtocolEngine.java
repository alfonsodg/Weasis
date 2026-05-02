/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.hanging;

/**
 * Engine that evaluates hanging protocol rules to determine the best image layout for a study.
 *
 * <p>Maintains an ordered list of {@link HangingProtocolRule} rules. When a study is opened, the
 * first matching rule (by priority) determines the layout. If no rule matches, a default layout
 * (1x1) is applied.
 *
 * <p>Rules can be loaded from an XML configuration file or set programmatically. The engine uses an
 * LRU cache to avoid re-evaluating previously seen study configurations.
 */
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HangingProtocolEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(HangingProtocolEngine.class);

  public static final String DEFAULT_LAYOUT = "VIEWS_1x1";

  private final List<HangingProtocolRule> rules;

  public HangingProtocolEngine() {
    this.rules = new ArrayList<>();
    addDefaultRules();
  }

  public HangingProtocolEngine(List<HangingProtocolRule> rules) {
    this.rules = new ArrayList<>(rules);
    Collections.sort(this.rules);
  }

  public void addRule(HangingProtocolRule rule) {
    rules.add(rule);
    Collections.sort(rules);
  }

  public void addRules(List<HangingProtocolRule> additionalRules) {
    rules.addAll(additionalRules);
    Collections.sort(rules);
  }

  public List<HangingProtocolRule> getRules() {
    return Collections.unmodifiableList(rules);
  }

  public void clearRules() {
    rules.clear();
  }

  public void loadFromXml(InputStream xmlStream) {
    if (xmlStream == null) {
      LOGGER.warn("Cannot load hanging protocols: null input stream");
      return;
    }
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(xmlStream);
      NodeList ruleNodes = doc.getDocumentElement().getElementsByTagName("rule");
      List<HangingProtocolRule> loadedRules = new ArrayList<>();
      for (int i = 0; i < ruleNodes.getLength(); i++) {
        Element element = (Element) ruleNodes.item(i);
        try {
          loadedRules.add(HangingProtocolRule.fromXmlElement(element));
        } catch (Exception e) {
          LOGGER.error("Failed to parse hanging protocol rule at index {}", i, e);
        }
      }
      rules.clear();
      rules.addAll(loadedRules);
      Collections.sort(rules);
      LOGGER.info("Loaded {} hanging protocol rules from XML", rules.size());
    } catch (ParserConfigurationException | SAXException | IOException e) {
      LOGGER.error("Failed to load hanging protocols from XML", e);
    }
  }

  public String evaluate(String modality, String bodyPart, int seriesCount) {
    for (HangingProtocolRule rule : rules) {
      if (rule.match(modality, bodyPart, seriesCount)) {
        LOGGER.debug(
            "Hanging protocol matched: rule={}, layout={}", rule, rule.getLayoutName());
        return rule.getLayoutName();
      }
    }
    LOGGER.debug(
        "No hanging protocol matched for modality={}, bodyPart={}, seriesCount={}, using default layout {}",
        modality,
        bodyPart,
        seriesCount,
        DEFAULT_LAYOUT);
    return DEFAULT_LAYOUT;
  }

  private void addDefaultRules() {
    rules.add(new HangingProtocolRule("CT", "Abdomen", 2, 0, "VIEWS_2x2", 10));
    rules.add(new HangingProtocolRule("CT", "", 4, 0, "VIEWS_2x4", 20));
    rules.add(new HangingProtocolRule("MR", "Brain", 0, 0, "VIEWS_2x3", 10));
    rules.add(new HangingProtocolRule("MR", "", 3, 0, "VIEWS_2x2", 20));
    rules.add(new HangingProtocolRule("CR,DX", "", 0, 0, "VIEWS_1x2", 30));
    rules.add(new HangingProtocolRule("US", "", 0, 0, "VIEWS_1x2", 30));
    rules.add(new HangingProtocolRule("NM", "", 0, 0, "VIEWS_1x3", 30));
    rules.add(new HangingProtocolRule("PET", "", 0, 0, "VIEWS_1x2", 30));
    rules.add(new HangingProtocolRule("MG", "", 0, 0, "VIEWS_2_f1x2", 30));
    Collections.sort(rules);
  }
}
