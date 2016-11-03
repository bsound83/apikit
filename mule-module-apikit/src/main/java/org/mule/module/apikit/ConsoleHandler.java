/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.apikit;

//import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
//import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
//import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.module.apikit.exception.NotFoundException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.I18nMessage;
import org.mule.runtime.core.message.DefaultMessageBuilder;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.internal.HttpMessageBuilder;
import org.mule.runtime.module.http.internal.component.ResourceNotFoundException;
//import org.mule.transport.http.i18n.HttpMessages; // TODO: it is in the compatibility package. it has messages only, check if we can copy this.
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import com.google.common.net.HttpHeaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleHandler implements Processor
{

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    public static final String MIME_TYPE_JAVASCRIPT = "application/x-javascript";
    public static final String MIME_TYPE_PNG = "image/png";
    public static final String MIME_TYPE_GIF = "image/gif";
    public static final String MIME_TYPE_SVG = "image/svg+xml";
    public static final String MIME_TYPE_CSS = "text/css";
    private static final String RESOURCE_BASE = System.getProperty("apikit.console.old") != null ? "/console" : "/console2";
    private static final String CONSOLE_ELEMENT = "<raml-console-loader";
    private static final String CONSOLE_ELEMENT_OLD = "<raml-console";
    private static final String CONSOLE_ATTRIBUTES = "options=\"{disableRamlClientGenerator: true, disableThemeSwitcher: true}\"";
    private static final String CONSOLE_ATTRIBUTES_OLD = "disable-raml-client-generator=\"\" disable-theme-switcher=\"\"";
    private static final String CONSOLE_ATTRIBUTES_PLACEHOLDER = "console-attributes-placeholder";
    private static final String DEFAULT_API_RESOURCES_PATH = "api/";
    private static final String RAML_QUERY_STRING = "raml";

    public static final int SC_MOVED_PERMANENTLY = 301;
    public static final String HEADER_LOCATION = "Location"; // [Response]
    public static final int SC_OK = org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK.getStatusCode();
    public static final String HEADER_CONTENT_LENGTH = "Content-Length"; // [Entity]
    public static final String HEADER_EXPIRES = "Expires"; // [Entity]


    private String cachedIndexHtml;
    private String embeddedConsolePath;
    private String apiResourcesRelativePath = DEFAULT_API_RESOURCES_PATH;
    private boolean standalone;
    private AbstractConfiguration configuration;

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private String consoleBaseUri;

    public ConsoleHandler(String consoleBaseUri, AbstractConfiguration configuration)
    {
        this(consoleBaseUri, "", configuration);
        standalone = true;
    }

    public ConsoleHandler(String consoleBaseUri, String embeddedConsolePath, AbstractConfiguration configuration)
    {
        this.configuration = configuration;
        this.embeddedConsolePath = sanitize(embeddedConsolePath);
        this.consoleBaseUri = consoleBaseUri;
    }

    public void updateRamlUri()
    {
        String relativeRamlUri = getRelativeRamlUri();
        if (relativeRamlUri != null)
        {
            String consoleElement = CONSOLE_ELEMENT;
            String consoleAttributes = CONSOLE_ATTRIBUTES;
            if (isOldConsole())
            {
                consoleElement = CONSOLE_ELEMENT_OLD;
                consoleAttributes = CONSOLE_ATTRIBUTES_OLD;
            }
            String indexHtml = IOUtils.toString(getClass().getResourceAsStream(RESOURCE_BASE + "/index.html"));
            indexHtml = indexHtml.replaceFirst(consoleElement + " src=\"[^\"]+\"",
                                               consoleElement + " src=\"" + relativeRamlUri + "\"");
            cachedIndexHtml = indexHtml.replaceFirst(CONSOLE_ATTRIBUTES_PLACEHOLDER, consoleAttributes);
        }
        else
        {
            cachedIndexHtml = "RAML Console is DISABLED.";
        }
    }

    private boolean isOldConsole()
    {
        return RESOURCE_BASE.equals("/console");
    }

    private String getRelativeRamlUri()
    {
        if (configuration.isParserV2())
        {
            //check if raml is in /api dir
            String ramlLocation = configuration.getRaml();
            if (ramlLocation.startsWith(DEFAULT_API_RESOURCES_PATH))
            {
                ramlLocation = ramlLocation.substring(DEFAULT_API_RESOURCES_PATH.length());
            }
            File apiResource = new File(configuration.getAppHome(), "/" + DEFAULT_API_RESOURCES_PATH + ramlLocation);
            if (apiResource.isFile())
            {
                return DEFAULT_API_RESOURCES_PATH + "?" + RAML_QUERY_STRING;
            }

            // check if raml is in a classpath subdir
            ramlLocation = configuration.getRaml();
            int idx = ramlLocation.lastIndexOf("/");
            if (idx > 0)
            {
                this.apiResourcesRelativePath = ramlLocation.substring(0, idx + 1);
                return apiResourcesRelativePath + "?" + RAML_QUERY_STRING;
            }

            logger.error("RAML Console is DISABLED. RAML resources cannot be hosted in the classpath root");
            return null;
        }
        return "./?";
    }

    private String sanitize(String consolePath)
    {
        if (consolePath.endsWith("/"))
        {
            consolePath = consolePath.substring(0, consolePath.length() - 1);
        }
        if (!consolePath.isEmpty() && !consolePath.startsWith("/"))
        {
            consolePath = "/" + consolePath;
        }
        return consolePath;
    }

    public Event process(Event event) throws MuleException
    {

        String path = UrlUtils.getRelativePath(event.getMessage());
        String contextPath = UrlUtils.getBasePath(event.getMessage());
        String queryString = UrlUtils.getQueryString(event.getMessage());

        if (logger.isDebugEnabled())
        {
            logger.debug("Console request: " + path);
        }
        Event resultEvent;
        InputStream in = null;
        try
        {
            boolean addContentEncodingHeader = false;
            if (path.equals(embeddedConsolePath) && !(contextPath.endsWith("/") && standalone))
            {
                // client redirect
                Map<String, String> headers = new HashMap<>();
                headers.put(HttpVariableNames.HTTP_STATUS,
                            String.valueOf(SC_MOVED_PERMANENTLY));
                String scheme = UrlUtils.getScheme(event.getMessage());
                String host = event.getMessage().getInboundProperty("Host");
                String requestPath = event.getMessage().getInboundProperty("http.request.path");
                String redirectLocation = scheme + "://" + host + requestPath + "/";
                if (StringUtils.isNotEmpty(queryString))
                {
                    redirectLocation += "?" + queryString;
                }

                headers.put(HEADER_LOCATION, redirectLocation);
                event = EventHelper.addOutboundProperties(event, headers);
                return event;
            }
            if (path.equals(embeddedConsolePath) || path.equals(embeddedConsolePath + "/") || path.equals(embeddedConsolePath + "/index.html"))
            {
                path = RESOURCE_BASE + "/index.html";
                in = new ByteArrayInputStream(cachedIndexHtml.getBytes());
            }
            else
            {
                String apiResourcesFullPath = embeddedConsolePath + "/" + apiResourcesRelativePath;
                if (path.startsWith(apiResourcesFullPath))
                {
                    // check for root raml
                    if (path.equals(apiResourcesFullPath) && queryString.equals(RAML_QUERY_STRING))
                    {
                        path += ".raml"; // to set raml mime type
                        in = new ByteArrayInputStream(configuration.getApikitRamlConsole(event).getBytes());
                    }
                    else
                    {
                        String resourcePath = "/" + apiResourcesRelativePath + path.substring(apiResourcesFullPath.length());
                        File apiResource = new File(configuration.getAppHome(), resourcePath);
                        in = new FileInputStream(apiResource);
                    }
                }
                else if (path.startsWith(embeddedConsolePath + "/scripts"))
                {
                    String acceptEncoding = event.getMessage().getInboundProperty("accept-encoding");
                    if (acceptEncoding != null && acceptEncoding.contains("gzip"))
                    {
                        in = getClass().getResourceAsStream(RESOURCE_BASE + path.substring(embeddedConsolePath.length()) + ".gz");
                        addContentEncodingHeader = true;
                    }
                    else
                    {
                        in = getClass().getResourceAsStream(RESOURCE_BASE + path.substring(embeddedConsolePath.length()));
                    }
                }
                else if (path.startsWith(embeddedConsolePath))
                {
                    in = getClass().getResourceAsStream(RESOURCE_BASE + path.substring(embeddedConsolePath.length()));
                }
            }
            if (in == null)
            {
                throw new NotFoundException(path);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, baos);

            byte[] buffer = baos.toByteArray();

            String mimetype = getMimeType(path);
            if (mimetype == null)
            {
                mimetype = DEFAULT_MIME_TYPE;
            }
            Event.Builder builder = Event.builder(event);
            InternalMessage.Builder messageBuilder = InternalMessage.builder().payload(buffer);
            builder.message(messageBuilder.build());
            resultEvent = builder.build();
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(SC_OK));
            headers.put(HttpHeaders.CONTENT_TYPE, mimetype);
            headers.put(HEADER_CONTENT_LENGTH, Integer.toString(buffer.length));
            headers.put("Access-Control-Allow-Origin", "*");

            if (addContentEncodingHeader)
            {
                headers.put("Content-Encoding", "gzip");
            }
            if (mimetype.equals(MediaType.HTML))
            {
                headers.put(HEADER_EXPIRES, "-1"); //avoid IE ajax response caching
            }
            resultEvent = EventHelper.addOutboundProperties(resultEvent,headers);
        }
        catch (IOException e)
        {
            //TODO FIX EXCEPTION
            throw new ResourceNotFoundException(null, null);// fileNotFound(RESOURCE_BASE + path)
        }

        return resultEvent;
    }

    private String getMimeType(String path)
    {
        String mimeType = DEFAULT_MIME_TYPE;
        if (FilenameUtils.getExtension(path).equals("html"))
        {
            mimeType = "text/html";//MimeTypes.HTML;
        }
        else if (FilenameUtils.getExtension(path).equals("js"))
        {
            mimeType = MIME_TYPE_JAVASCRIPT;
        }
        else if (FilenameUtils.getExtension(path).equals("png"))
        {
            mimeType = MIME_TYPE_PNG;
        }
        else if (FilenameUtils.getExtension(path).equals("gif"))
        {
            mimeType = MIME_TYPE_GIF;
        }
        else if (FilenameUtils.getExtension(path).equals("svg"))
        {
            mimeType = MIME_TYPE_SVG;
        }
        else if (FilenameUtils.getExtension(path).equals("css"))
        {
            mimeType = MIME_TYPE_CSS;
        }
        else if (FilenameUtils.getExtension(path).equals("raml"))
        {
            mimeType = AbstractConfiguration.APPLICATION_RAML;
        }
        return mimeType;
    }

    public String getConsoleUrl()
    {
        String url = consoleBaseUri.endsWith("/") ? consoleBaseUri.substring(0, consoleBaseUri.length() - 1) : consoleBaseUri;
        return url + embeddedConsolePath;
    }

}
