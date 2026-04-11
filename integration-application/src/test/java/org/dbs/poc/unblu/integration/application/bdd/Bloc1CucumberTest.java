package org.dbs.poc.unblu.integration.application.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/bloc1")
@ConfigurationParameters({
        @ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "org.dbs.poc.unblu.integration.application.bdd.steps"),
        @ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports/bloc1.html"),
        @ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
})
public class Bloc1CucumberTest {}
