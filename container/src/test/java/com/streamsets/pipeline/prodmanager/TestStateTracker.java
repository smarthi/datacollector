/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.prodmanager;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.streamsets.pipeline.cluster.ApplicationState;
import com.streamsets.pipeline.main.RuntimeInfo;
import com.streamsets.pipeline.main.RuntimeModule;
import com.streamsets.pipeline.util.*;
import com.streamsets.pipeline.util.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class TestStateTracker {

  private StateTracker stateTracker;

  @BeforeClass
  public static void beforeClass() throws IOException {
    System.setProperty(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR, "./target/var");
    File f = new File(System.getProperty(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR));
    FileUtils.deleteDirectory(f);
    TestUtil.captureMockStages();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    System.getProperties().remove(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR);
  }

  @Before()
  public void setUp() {
    stateTracker = createStateTracker();
  }

  private static StateTracker createStateTracker() {
    RuntimeInfo info = new RuntimeInfo(RuntimeModule.SDC_PROPERTY_PREFIX, new MetricRegistry(),
      Arrays.asList(TestStateTracker.class.getClassLoader()));
    com.streamsets.pipeline.util.Configuration configuration = new com.streamsets.pipeline.util.Configuration();
    return new StateTracker(info, configuration);
  }

  @After
  public void tearDown() {
    stateTracker.getStateFile().delete();
  }

  @Test
  public void testDirCreation() {
    File dir = new File("target", UUID.randomUUID().toString()).getAbsoluteFile();
    RuntimeInfo info = Mockito.mock(RuntimeInfo.class);
    Mockito.when(info.getDataDir()).thenReturn(dir.getAbsolutePath());
    new StateTracker(info, null);
    Assert.assertTrue(dir.exists());
  }

  @Test
  public void testGetStateBeforeInit() {
    Assert.assertNull(stateTracker.getState());
  }

  @Test
  public void testGetDefaultState() {
    stateTracker.init();
    PipelineState state = stateTracker.getState();
    Assert.assertNull(state);
  }

  @Test
  public void testSetState() throws PipelineManagerException {
    stateTracker.init();
    Map<String, Object> attributes = ImmutableMap.<String, Object>of("two", "2", ClusterPipelineManager.APPLICATION_STATE,
      ImmutableMap.<String, Object>of("one", 1));
    stateTracker.setState("xyz", "2.0", State.RUNNING, "Started pipeline", null, attributes);
    stateTracker = createStateTracker();
    stateTracker.init();
    PipelineState state = stateTracker.getState();
    Assert.assertNotNull(state);
    Assert.assertEquals("2.0", state.getRev());
    Assert.assertEquals("Started pipeline", state.getMessage());
    Assert.assertEquals(State.RUNNING, state.getState());
    Assert.assertEquals(attributes, state.getAttributes());
  }

  @Test
  public void testGetStateFile() throws PipelineManagerException {
    stateTracker.init();
    Assert.assertFalse(stateTracker.getStateFile().exists());
  }

  @Test
  public void testReinit() throws PipelineManagerException {

    stateTracker.init();
    PipelineState state = stateTracker.getState();
    Assert.assertNull(state);

    stateTracker.setState("xyz", "1.0", State.RUNNING, null, null, null);
    state = stateTracker.getState();
    Assert.assertNotNull(state);
    Assert.assertEquals("xyz", state.getName());
    Assert.assertEquals("1.0", state.getRev());
    Assert.assertEquals(null, state.getMessage());
    Assert.assertEquals(State.RUNNING, state.getState());

    stateTracker.init();

    state = stateTracker.getState();

    Assert.assertNotNull(state);
    Assert.assertEquals("xyz", state.getName());
    Assert.assertEquals("1.0", state.getRev());
    Assert.assertEquals(null, state.getMessage());
    Assert.assertEquals(State.RUNNING, state.getState());
  }

  @Test(expected = RuntimeException.class)
  public void testInitInvalidDir() {
    RuntimeInfo info = Mockito.mock(RuntimeInfo.class);
    Mockito.when(info.getDataDir()).thenReturn("\0");
    stateTracker = new StateTracker(info, new Configuration());

    stateTracker.init();

  }

}