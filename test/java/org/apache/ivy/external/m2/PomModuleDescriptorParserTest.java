/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.external.m2;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.ivy.Artifact;
import org.apache.ivy.DependencyDescriptor;
import org.apache.ivy.Ivy;
import org.apache.ivy.ModuleDescriptor;
import org.apache.ivy.ModuleId;
import org.apache.ivy.ModuleRevisionId;
import org.apache.ivy.external.m2.PomModuleDescriptorParser;
import org.apache.ivy.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.repository.url.URLResource;
import org.apache.ivy.xml.XmlModuleDescriptorParserTest;


public class PomModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    // junit test -- DO NOT REMOVE used by ant to know it's a junit test
    

    public void testAccept() throws Exception {
        assertTrue(PomModuleDescriptorParser.getInstance().accept(
                new URLResource(getClass().getResource("test-simple.pom"))));
        assertFalse(PomModuleDescriptorParser.getInstance().accept(
                new URLResource(XmlModuleDescriptorParserTest.class.getResource("test.xml"))));
    }
    
    public void testSimple() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-simple.pom"), false);
        assertNotNull(md);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays.asList(md.getConfigurations()));
        
        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }
    
    public void testParent() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-parent.pom"), false);
        assertNotNull(md);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays.asList(md.getConfigurations()));
        
        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }
    
    public void testParent2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-parent2.pom"), false);
        assertNotNull(md);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());
        
        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays.asList(md.getConfigurations()));
        
        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }
    
    public void testDependencies() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-dependencies.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md.getModuleRevisionId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"), dds[0].getDependencyRevisionId());
    }
    
    public void testWithoutVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-without-version.pom"), false);
        assertNotNull(md);
        
        assertEquals(new ModuleId("org.apache", "test"), md.getModuleRevisionId().getModuleId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"), dds[0].getDependencyRevisionId());
    }
    
    public void testProperties() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-properties.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("drools", "drools-smf", "2.0-beta-18"), md.getModuleRevisionId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("drools", "drools-core", "2.0-beta-18"), dds[0].getDependencyRevisionId());
    }
    
    public void testReal() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("commons-lang-1.0.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("commons-lang", "commons-lang", "1.0"), md.getModuleRevisionId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("junit", "junit", "3.7"), dds[0].getDependencyRevisionId());
    }
    
    public void testOptional() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-optional.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md.getModuleRevisionId());
        assertTrue(Arrays.asList(md.getConfigurationsNames()).contains("optional"));
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"), dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"optional"})), new HashSet(Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)", "master(*)"})), new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("optional"))));        
        
        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));
    }
    
    public void testDependenciesWithScope() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-dependencies-with-scope.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md.getModuleRevisionId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("odmg", "odmg", "3.0"), dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime"})), new HashSet(Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)", "master(*)"})), new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("runtime"))));
        
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"), dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));
        
        
        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("runtime"))));
    }
    
    public void testExclusion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(new Ivy(), getClass().getResource("test-exclusion.pom"), false);
        assertNotNull(md);
        
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md.getModuleRevisionId());
        
        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"), dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[0].getAllDependencyArtifactsExcludes().length);
        
        assertEquals(ModuleRevisionId.newInstance("dom4j", "dom4j", "1.6"), dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("runtime"))));
        assertDependencyModulesExcludes(dds[1], new String[] {"compile"}, new String[] {"jaxme-api", "jaxen"});
        assertDependencyModulesExcludes(dds[1], new String[] {"runtime"}, new String[] {"jaxme-api", "jaxen"});        
        
        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})), new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[2].getAllDependencyArtifactsExcludes().length);
    }
}