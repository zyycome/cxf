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
package org.apache.cxf.jaxrs.sse.atmosphere;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventOutput;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseAtmosphereEventOutputImpl implements SseEventOutput {
    private static final Logger LOGGER = LoggerFactory.getLogger(SseAtmosphereEventOutputImpl.class);
    
    private final AtmosphereResource resource;
    private final MessageBodyWriter<OutboundSseEvent> writer;
    private volatile boolean closed = false;
    
    public SseAtmosphereEventOutputImpl(final MessageBodyWriter<OutboundSseEvent> writer, 
            final AtmosphereResource resource) {
        this.writer = writer;
        this.resource = resource;
        
        if (!resource.isSuspended()) {
            resource.suspend();
        }
    }
    
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            if (resource.isSuspended()) {
                resource.resume();
            }

            final Broadcaster broadcaster = resource.getBroadcaster();
            resource.removeFromAllBroadcasters();
            
            try {
                if (!resource.getResponse().isCommitted()) {
                    resource.getResponse().flushBuffer();
                }
            } finally {
                resource.close();
                broadcaster.destroy();
            }
        }
    }

    @Override
    public void write(OutboundSseEvent event) throws IOException {
        if (!closed && writer != null) {
            try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                writer.writeTo(event, event.getClass(), null, new Annotation [] {}, event.getMediaType(), null, os);
                
                // Atmosphere broadcasts asynchronously which is acceptable in most cases.
                // Unfortunately, calling close() may lead to response stream being closed
                // while there are still some SSE delivery scheduled.
                final Future<Object> future = resource
                    .getBroadcaster()
                    .broadcast(os.toString(StandardCharsets.UTF_8.name()));
                
                try {
                    if (!future.isDone()) {
                        // Let us wait at least 200 milliseconds before returning to ensure 
                        // that SSE had the opportunity to be delivered.
                        future.get(200, TimeUnit.MILLISECONDS);
                    }
                } catch (final ExecutionException | InterruptedException ex) {
                    throw new IOException(ex);
                } catch (final TimeoutException ex) {
                    LOGGER.warn("SSE was not delivered within default timeout");
                }
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
