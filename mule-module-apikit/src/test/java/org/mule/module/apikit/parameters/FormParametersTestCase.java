/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.parameters;

import static com.jayway.restassured.RestAssured.given;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.jayway.restassured.RestAssured;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FormParametersTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    private final String configFile;

    @Override
    public int getTestTimeoutSecs()
    {
        return 6000;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"org/mule/module/apikit/parameters/form-parameters-endpoint-config.xml"},
                {"org/mule/module/apikit/parameters/form-parameters-config.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        RestAssured.port = serverPort.getNumber();
        super.doSetUp();
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    public FormParametersTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Test
    public void validMultipartFormProvided() throws Exception
    {
        given().multiPart("first", "primero")
                .multiPart("second", "segundo")
                .multiPart("third", "true")
                .multiPart("payload", "3.4")
                .expect().response().statusCode(201)
                .when().post("/api/multipart");
    }

    @Test
    public void requiredMultipartFormParamNotProvided() throws Exception
    {
        given().multiPart("second", "segundo")
                .multiPart("third", "true")
                .multiPart("payload", "3.4")
                .expect().response().statusCode(400)
                .when().post("/api/multipart");
    }

    @Test
    public void validUrlencodedFormProvided() throws Exception
    {
        given().header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("first", "prime")
                .formParam("second", "segundo")
                .formParam("third", "true")
                .expect().response().statusCode(201)
                .when().post("/api/url-encoded");
    }

    @Test
    public void requiredUrlencodedFormParamNotProvided() throws Exception
    {
        given().header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("second", "segundo")
                .formParam("third", "true")
                .expect().response().statusCode(400)
                .when().post("/api/url-encoded");
    }

    @Test
    public void invalidTypeUrlencodedFormProvided() throws Exception
    {
        given().header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("first", "prime")
                .formParam("second", "segundo")
                .formParam("third", "35")
                .expect().response().statusCode(400)
                .when().post("/api/url-encoded");
    }

    @Test
    public void invalidEnumUrlencodedFormProvided() throws Exception
    {
        given().header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("first", "prime")
                .formParam("second", "second")
                .expect().response().statusCode(400)
                .when().post("/api/url-encoded");
    }

}
