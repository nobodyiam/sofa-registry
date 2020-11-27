package com.alipay.sofa.registry.server.meta.lease.impl;

import com.alipay.sofa.registry.common.model.metaserver.nodes.MetaNode;
import com.alipay.sofa.registry.exception.DisposeException;
import com.alipay.sofa.registry.exception.InitializeException;
import com.alipay.sofa.registry.exception.StartException;
import com.alipay.sofa.registry.exception.StopException;
import com.alipay.sofa.registry.lifecycle.impl.LifecycleHelper;
import com.alipay.sofa.registry.server.meta.AbstractTest;
import com.alipay.sofa.registry.server.meta.lease.Lease;
import com.alipay.sofa.registry.server.meta.remoting.RaftExchanger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TestAbstractRaftEnabledLeaseManager extends AbstractTest {

    private String serviceId = "TEST-SERVICE-ID";

    private AbstractRaftEnabledLeaseManager<MetaNode> manager = new AbstractRaftEnabledLeaseManager<MetaNode>() {

        @Override
        protected String getServiceId() {
            return serviceId;
        }
    };

    private RaftExchanger raftExchanger;

    @Before
    public void beforeTestAbstractRaftEnabledLeaseManager() throws InitializeException, StartException {
        raftExchanger = mock(RaftExchanger.class);
        manager.setScheduled(scheduled).setRaftExchanger(raftExchanger).setExecutors(executors);
        LifecycleHelper.initializeIfPossible(manager);
        LifecycleHelper.startIfPossible(manager);
        manager.setRaftLeaseManager(manager.new DefaultRaftLeaseManager<>());
    }

    @After
    public void afterTestAbstractRaftEnabledLeaseManager() throws StopException, DisposeException {
        LifecycleHelper.stopIfPossible(manager);
        LifecycleHelper.disposeIfPossible(manager);
    }

    @Test
    public void raftWholeProcess() {

    }

    @Test
    public void testRegister() {
        manager.register(new MetaNode(randomURL(randomIp()), getDc()), 10);
        Assert.assertEquals(1, manager.getLeaseStore().size());
        Lease<MetaNode> lease = manager.getLeaseStore().entrySet().iterator().next().getValue();
        Assert.assertFalse(lease.isExpired());
    }

    @Test
    public void testCancel() {
        MetaNode node = new MetaNode(randomURL(randomIp()), getDc());
        manager.register(node, 10);
        Assert.assertEquals(1, manager.getLeaseStore().size());
        manager.cancel(node);
        Assert.assertEquals(0, manager.getLeaseStore().size());
    }

    @Test
    public void testRenew() throws InterruptedException {
        MetaNode node = new MetaNode(randomURL(randomIp()), getDc());
        manager.renew(node, 1);
        Assert.assertEquals(1, manager.getLeaseStore().size());
        Lease<MetaNode> lease = manager.getLeaseStore().get(node.getIp());
        long prevLastUpdateTime = lease.getLastUpdateTimestamp();
        long prevBeginTime = lease.getBeginTimestamp();
        // let time pass, so last update time could be diff
        Thread.sleep(5);
        manager.renew(node, 10);
        lease = manager.getLeaseStore().get(node.getIp());
        Assert.assertEquals(prevBeginTime, lease.getBeginTimestamp());
        Assert.assertNotEquals(prevLastUpdateTime, lease.getLastUpdateTimestamp());
    }

    @Test
    public void testEvict() {

    }

    @Test
    public void testGetServiceId() {
        Assert.assertEquals(serviceId, manager.getServiceId());
    }
}