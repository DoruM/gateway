/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.management.filter;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.monitoring.entity.LongMonitoringCounter;
import org.kaazing.gateway.management.monitoring.entity.factory.MonitoringEntityFactory;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Filter for session filter chains. Supports 'strategies' to tailor the amount of processing based on whether management is
 * desired or not at the time. Strategies that actually create a SessionManagementBean store the bean as a session attribute (see
 * IoSession and AttributeKey). All the strategies are stateless and final--they get the sessionManagementBean that they're
 * operating on from the incoming IoSessionEx.
 * <p/>
 * The global strategy is changed from the DefaultManagementContext. As it can be changed at any time, rather than keep it in
 * each ManagementFilter and require it to be updated from DMC, each ManagementFilter will just request it each time it needs to
 * do something.
 * <p/>
 * There is exactly 1 ManagementFilter per service instance (excluding the management services, for which we do not want any
 * management).
 */
public class ManagementFilter extends IoFilterAdapter<IoSessionEx> {
    private static final String CURRENT_NUMBER_OF_SESSIONS = "currentNumberOfSessions";
    protected ServiceManagementBean serviceBean;
    protected ManagementContext managementContext;
    protected ServiceContext serviceContext;
    private LongMonitoringCounter counter;

    public ManagementFilter(ServiceManagementBean serviceBean, MonitoringEntityFactory monitoringEntityFactory) {
        this.serviceBean = serviceBean;
        this.managementContext = serviceBean.getGatewayManagementBean().getManagementContext();
        this.serviceContext = serviceBean.getServiceContext();
        counter = monitoringEntityFactory.makeLongMonitoringCounter(CURRENT_NUMBER_OF_SESSIONS);
    }

    public ServiceManagementBean getServiceBean() {
        return this.serviceBean;  // needed for at least JMS
    }

    public ManagementContext getManagementContext() {
        return this.managementContext;
    }

    @Override
    protected void doSessionClosed(NextFilter nextFilter, IoSessionEx session) throws Exception {
        managementContext.getManagementFilterStrategy()
                .doSessionClosed(managementContext, serviceBean, session.getId(), Utils.getManagementSessionType(session));
        managementContext.decrementOverallSessionCount();
        counter = counter.decrement();

        super.doSessionClosed(nextFilter, session);
    }

    @Override
    protected void doMessageReceived(NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {
        managementContext.getManagementFilterStrategy()
                .doMessageReceived(managementContext, serviceBean, session.getId(), session.getReadBytes(), message);
        super.doMessageReceived(nextFilter, session, message);
    }

    @Override
    protected void doFilterWrite(NextFilter nextFilter, IoSessionEx session, WriteRequest writeRequest) throws Exception {
        managementContext.getManagementFilterStrategy()
                .doFilterWrite(managementContext, serviceBean, session.getId(), session.getWrittenBytes(), writeRequest);
        super.doFilterWrite(nextFilter, session, writeRequest);
    }

    @Override
    protected void doExceptionCaught(NextFilter nextFilter, IoSessionEx session, Throwable cause) throws Exception {
        managementContext.getManagementFilterStrategy()
                .doExceptionCaught(managementContext, serviceBean, session.getId(), cause);
        super.doExceptionCaught(nextFilter, session, cause);
    }

    //
    // Add a new method that informs the management layer there is a new session because of a
    // possible problem with getting writes before doSessionOpened has been called,
    // (per Chris B, it's to fix failures in SOCKS forward connectivity tests because events
    // like filterWrite were sometimes occurring before sessionOpened.  This method delegates
    // to the strategy objects to present the new session if desired.
    // See svn revision 65794, 25-Jul-13.
    //
    public void newManagementSession(IoSessionEx session) throws Exception {
        managementContext.incrementOverallSessionCount();

        // Because strategy may change during execution of this method, refetch it when we need it.
        managementContext.getManagementFilterStrategy()
                .doSessionCreated(managementContext, serviceBean, session, Utils.getManagementSessionType(session));
        counter = counter.increment();
    }
}
