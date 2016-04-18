/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.sse.servlet;

import org.apache.cxf.jaxrs.sse.atmosphere.SseAtmosphereInterceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.handler.ReflectorServletProcessor;

public class CXFSseServlet extends AtmosphereServlet {
    private static final long serialVersionUID = -874047746532165731L;

    public CXFSseServlet(final CXFNonSpringServlet delegate) {
        // Register and map the dispatcher servlet
        super(true);
        
        framework().addAtmosphereHandler("/*", new ReflectorServletProcessor(delegate));
        framework().interceptor(new SseAtmosphereInterceptor());
        framework().addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework().addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework().addInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, "true");
        framework().addInitParameter(ApplicationConfig.CLOSE_STREAM_ON_CANCEL, "true");
    }
}
