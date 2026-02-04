package org.wrj.haifa.designpattern.orderpolicyrule.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Cucumber BDD test runner for orderpolicyrule package.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/orderpolicyrule")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "org.wrj.haifa.designpattern.orderpolicyrule.bdd")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
public class RunCucumberTest {
}
