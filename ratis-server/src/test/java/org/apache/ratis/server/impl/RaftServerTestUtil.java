/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.server.impl;

import org.apache.ratis.MiniRaftCluster;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.ClientId;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.statemachine.StateMachine;
import org.apache.ratis.util.JavaUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class RaftServerTestUtil {
  static final Logger LOG = LoggerFactory.getLogger(RaftServerTestUtil.class);

  public static void waitAndCheckNewConf(MiniRaftCluster cluster,
      RaftPeer[] peers, int numOfRemovedPeers, Collection<String> deadPeers)
      throws Exception {
    final long sleepMs = cluster.getMaxTimeout() * (numOfRemovedPeers + 2);
    JavaUtils.attempt(() -> waitAndCheckNewConf(cluster, peers, deadPeers),
        3, sleepMs, "waitAndCheckNewConf", LOG);
  }
  private static void waitAndCheckNewConf(MiniRaftCluster cluster,
      RaftPeer[] peers, Collection<String> deadPeers)
      throws Exception {
    LOG.info(cluster.printServers());
    Assert.assertNotNull(cluster.getLeader());

    int numIncluded = 0;
    int deadIncluded = 0;
    final RaftConfiguration current = RaftConfiguration.newBuilder()
        .setConf(peers).setLogEntryIndex(0).build();
    for (RaftServerImpl server : cluster.iterateServerImpls()) {
      if (deadPeers != null && deadPeers.contains(server.getId().toString())) {
        if (current.containsInConf(server.getId())) {
          deadIncluded++;
        }
        continue;
      }
      if (current.containsInConf(server.getId())) {
        numIncluded++;
        Assert.assertTrue(server.getRaftConf().isStable());
        Assert.assertTrue(server.getRaftConf().hasNoChange(peers));
      } else {
        Assert.assertFalse(server.getId() + " is still running: " + server,
            server.isAlive());
      }
    }
    Assert.assertEquals(peers.length, numIncluded + deadIncluded);
  }

  public static long getRetryCacheSize(RaftServerImpl server) {
    return server.getRetryCache().size();
  }

  public static RetryCache.CacheEntry getRetryEntry(RaftServerImpl server,
      ClientId clientId, long callId) {
    return server.getRetryCache().get(clientId, callId);
  }

  public static boolean isRetryCacheEntryFailed(RetryCache.CacheEntry entry) {
    return entry.isFailed();
  }

  public static RaftServerProxy getRaftServerProxy(RaftPeerId id, StateMachine stateMachine,
      RaftGroup group, RaftProperties properties, Parameters parameters) throws IOException {
    return new RaftServerProxy(id, stateMachine, group, properties, parameters);
  }
}
