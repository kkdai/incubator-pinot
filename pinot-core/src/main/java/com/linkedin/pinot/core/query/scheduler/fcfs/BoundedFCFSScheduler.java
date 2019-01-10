/**
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
package org.apache.pinot.core.query.scheduler.fcfs;

import org.apache.pinot.common.metrics.ServerMetrics;
import org.apache.pinot.core.query.executor.QueryExecutor;
import org.apache.pinot.core.query.scheduler.MultiLevelPriorityQueue;
import org.apache.pinot.core.query.scheduler.PriorityScheduler;
import org.apache.pinot.core.query.scheduler.SchedulerGroup;
import org.apache.pinot.core.query.scheduler.SchedulerGroupFactory;
import org.apache.pinot.core.query.scheduler.SchedulerPriorityQueue;
import org.apache.pinot.core.query.scheduler.TableBasedGroupMapper;
import org.apache.pinot.core.query.scheduler.resources.PolicyBasedResourceManager;
import org.apache.pinot.core.query.scheduler.resources.ResourceManager;
import java.util.concurrent.atomic.LongAccumulator;
import javax.annotation.Nonnull;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Per group FCFS algorithm with bounded resource management per {@link SchedulerGroup}
 * This class is a thin wrapper factory that configures {@link PriorityScheduler} with right
 * concrete classes. All the scheduling logic resides in {@link PriorityScheduler}
 */
public class BoundedFCFSScheduler extends PriorityScheduler {
  private static Logger LOGGER = LoggerFactory.getLogger(BoundedFCFSScheduler.class);

  public static BoundedFCFSScheduler create(@Nonnull Configuration config, @Nonnull QueryExecutor queryExecutor,
      @Nonnull ServerMetrics serverMetrics, @Nonnull LongAccumulator latestQueryTime) {
    final ResourceManager rm = new PolicyBasedResourceManager(config);
    final SchedulerGroupFactory groupFactory = new SchedulerGroupFactory() {
      @Override
      public SchedulerGroup create(Configuration config, String groupName) {
        return new FCFSSchedulerGroup(groupName);
      }
    };
    MultiLevelPriorityQueue queue = new MultiLevelPriorityQueue(config, rm, groupFactory, new TableBasedGroupMapper());
    return new BoundedFCFSScheduler(rm , queryExecutor, queue, serverMetrics, latestQueryTime);
  }

  private BoundedFCFSScheduler(@Nonnull ResourceManager resourceManager, @Nonnull QueryExecutor queryExecutor,
      @Nonnull SchedulerPriorityQueue queue, @Nonnull ServerMetrics metrics, @Nonnull LongAccumulator latestQueryTime) {
    super(resourceManager, queryExecutor, queue, metrics, latestQueryTime);
  }

  @Override
  public String name() {
    return "BoundedFCFSScheduler";
  }
}
