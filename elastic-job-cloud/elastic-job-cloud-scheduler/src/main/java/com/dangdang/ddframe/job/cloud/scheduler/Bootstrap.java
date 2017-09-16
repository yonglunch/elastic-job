/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.cloud.scheduler;

import com.dangdang.ddframe.job.cloud.scheduler.env.BootstrapEnvironment;
import com.dangdang.ddframe.job.cloud.scheduler.ha.HANode;
import com.dangdang.ddframe.job.cloud.scheduler.ha.SchedulerElectionCandidate;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperElectionService;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.CountDownLatch;

/**
 * Mesos框架启动器.
 *
 * @author caohao
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class Bootstrap {
    
    /**
     * 启动入口.
     * 
     * @param args 命令行参数无需传入
     * @throws InterruptedException 线程中断异常
     */
    // CHECKSTYLE:OFF
    public static void main(final String[] args) throws InterruptedException {
        BootstrapEnvironment.getInstance().getZookeeperConfiguration().setSessionTimeoutMilliseconds(Integer.MAX_VALUE);
        BootstrapEnvironment.getInstance().getZookeeperConfiguration().setConnectionTimeoutMilliseconds(Integer.MAX_VALUE);

        // CHECKSTYLE:ON
        // 初始化 注册中心
        CoordinatorRegistryCenter regCenter = new ZookeeperRegistryCenter(BootstrapEnvironment.getInstance().getZookeeperConfiguration());
        regCenter.init();
        // 初始化 Zookeeper 选举服务
        final ZookeeperElectionService electionService = new ZookeeperElectionService(
                BootstrapEnvironment.getInstance().getFrameworkHostPort(), (CuratorFramework) regCenter.getRawClient(), HANode.ELECTION_NODE, new SchedulerElectionCandidate(regCenter));
        electionService.start();
        // 挂起 主进程
        final CountDownLatch latch = new CountDownLatch(1);
        latch.await();
        // Hook TODO 貌似位置不对？
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
        
            @Override
            public void run() {
                // 停止选举
                electionService.stop();
                latch.countDown();
            }
        });
    }
}
