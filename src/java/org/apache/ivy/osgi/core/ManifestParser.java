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
package org.apache.ivy.osgi.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;

/**
 * Provides an OSGi manifest parser.
 * 
 */
public class ManifestParser {

    private static final String EXPORT_PACKAGE = "Export-Package";

    private static final String IMPORT_PACKAGE = "Import-Package";

    private static final String EXPORT_SERVICE = "Export-Service";

    private static final String IMPORT_SERVICE = "Import-Service";

    private static final String REQUIRE_BUNDLE = "Require-Bundle";

    private static final String BUNDLE_VERSION = "Bundle-Version";

    private static final String BUNDLE_NAME = "Bundle-Name";

    private static final String BUNDLE_DESCRIPTION = "Bundle-Description";

    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    private static final String BUNDLE_MANIFEST_VERSION = "Bundle-ManifestVersion";

    private static final String BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT = "Bundle-RequiredExecutionEnvironment";

    private static final String BUNDLE_CLASSPATH = "Bundle-ClassPath";

    private static final String ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle";

    private static final String ATTR_RESOLUTION = "resolution";

    private static final String ATTR_VERSION = "version";

    private static final String ATTR_BUNDLE_VERSION = "bundle-version";

    private static final String ATTR_USE = "use";

    public static BundleInfo parseJarManifest(final InputStream jarStream) throws IOException,
            ParseException {
        final JarInputStream jis = new JarInputStream(jarStream);
        final BundleInfo parseManifest = parseManifest(jis.getManifest());
        jis.close();
        return parseManifest;
    }

    public static BundleInfo parseManifest(final File manifestFile) throws IOException,
            ParseException {
        final FileInputStream fis = new FileInputStream(manifestFile);
        final BundleInfo parseManifest = parseManifest(fis);
        fis.close();
        return parseManifest;
    }

    public static BundleInfo parseManifest(final String manifest) throws IOException,
            ParseException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(manifest.getBytes("UTF-8"));
        final BundleInfo parseManifest = parseManifest(bais);
        bais.close();
        return parseManifest;
    }

    public static BundleInfo parseManifest(final InputStream manifestStream) throws IOException,
            ParseException {
        final BundleInfo parseManifest = parseManifest(new Manifest(manifestStream));
        manifestStream.close();
        return parseManifest;
    }

    public static BundleInfo parseManifest(final Manifest manifest) throws ParseException {
        final Attributes mainAttributes = manifest.getMainAttributes();

        // Eclipse source bundle doesn't have it. Disable it until proven actually useful
        // String manifestVersion = mainAttributes.getValue(BUNDLE_MANIFEST_VERSION);
        // if (manifestVersion == null) {
        // // non OSGi manifest
        // throw new ParseException("No " + BUNDLE_MANIFEST_VERSION + " in the manifest", 0);
        // }

        final String symbolicName = new ManifestHeaderValue(
                mainAttributes.getValue(BUNDLE_SYMBOLIC_NAME)).getSingleValue();
        if (symbolicName == null) {
            throw new ParseException("No " + BUNDLE_SYMBOLIC_NAME + " in the manifest", 0);
        }

        String description = new ManifestHeaderValue(mainAttributes.getValue(BUNDLE_DESCRIPTION),
                true).getSingleValue();
        if (description == null) {
            description = new ManifestHeaderValue(mainAttributes.getValue(BUNDLE_DESCRIPTION), true)
                    .getSingleValue();
        }

        final String vBundle = new ManifestHeaderValue(mainAttributes.getValue(BUNDLE_VERSION))
                .getSingleValue();
        Version version;
        try {
            version = versionOf(vBundle);
        } catch (final NumberFormatException e) {
            throw new ParseException("The " + BUNDLE_VERSION + " has an incorrect version: "
                    + vBundle + " (" + e.getMessage() + ")", 0);
        }

        final BundleInfo bundleInfo = new BundleInfo(symbolicName, version);

        bundleInfo.setDescription(description);

        final List/* <String> */environments = new ManifestHeaderValue(
                mainAttributes.getValue(BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT)).getValues();
        bundleInfo.setExecutionEnvironments(environments);

        parseRequirement(bundleInfo, mainAttributes, REQUIRE_BUNDLE, BundleInfo.BUNDLE_TYPE,
            ATTR_BUNDLE_VERSION);
        parseRequirement(bundleInfo, mainAttributes, IMPORT_PACKAGE, BundleInfo.PACKAGE_TYPE,
            ATTR_VERSION);
        parseRequirement(bundleInfo, mainAttributes, IMPORT_SERVICE, BundleInfo.SERVICE_TYPE,
            ATTR_VERSION);

        final ManifestHeaderValue exportElements = new ManifestHeaderValue(
                mainAttributes.getValue(EXPORT_PACKAGE));
        final Iterator itExports = exportElements.getElements().iterator();
        while (itExports.hasNext()) {
            final ManifestHeaderElement exportElement = (ManifestHeaderElement) itExports.next();
            final String vExport = (String) exportElement.getAttributes().get(ATTR_VERSION);
            Version v = null;
            try {
                v = versionOf(vExport);
            } catch (final NumberFormatException e) {
                throw new ParseException("The " + EXPORT_PACKAGE + " has an incorrect version: "
                        + vExport + " (" + e.getMessage() + ")", 0);
            }

            final Iterator itNames = exportElement.getValues().iterator();
            while (itNames.hasNext()) {
                final String name = (String) itNames.next();
                final ExportPackage export = new ExportPackage(name, v);
                final String uses = (String) exportElement.getDirectives().get(ATTR_USE);
                if (uses != null) {
                    final String[] split = uses.trim().split(",");
                    for (int i = 0; i < split.length; i++) {
                        export.addUse(split[i].trim());
                    }
                }
                bundleInfo.addCapability(export);
            }
        }

        parseCapability(bundleInfo, mainAttributes, EXPORT_SERVICE, BundleInfo.SERVICE_TYPE);

        // handle Eclipse specific source attachement
        final String eclipseSourceBundle = mainAttributes.getValue(ECLIPSE_SOURCE_BUNDLE);
        if (eclipseSourceBundle != null) {
            bundleInfo.setSource(true);
            final ManifestHeaderValue eclipseSourceBundleValue = new ManifestHeaderValue(
                    eclipseSourceBundle);
            final ManifestHeaderElement element = (ManifestHeaderElement) eclipseSourceBundleValue
                    .getElements().iterator().next();
            final String symbolicNameTarget = (String) element.getValues().iterator().next();
            bundleInfo.setSymbolicNameTarget(symbolicNameTarget);
            final String v = (String) element.getAttributes().get(ATTR_VERSION);
            if (v != null) {
                bundleInfo.setVersionTarget(new Version(v));
            }
        }

        final String bundleClasspath = mainAttributes.getValue(BUNDLE_CLASSPATH);
        if (bundleClasspath != null) {
            final ManifestHeaderValue bundleClasspathValue = new ManifestHeaderValue(
                    bundleClasspath);
            bundleInfo.setClasspath(bundleClasspathValue.getValues());
        }

        return bundleInfo;
    }

    private static void parseRequirement(final BundleInfo bundleInfo,
            final Attributes mainAttributes, final String headerName, final String type,
            final String versionAttr) throws ParseException {
        final ManifestHeaderValue elements = new ManifestHeaderValue(
                mainAttributes.getValue(headerName));
        final Iterator itElement = elements.getElements().iterator();
        while (itElement.hasNext()) {
            final ManifestHeaderElement element = (ManifestHeaderElement) itElement.next();
            final String resolution = (String) element.getDirectives().get(ATTR_RESOLUTION);
            final String attVersion = (String) element.getAttributes().get(versionAttr);
            VersionRange version = null;
            try {
                version = versionRangeOf(attVersion);
            } catch (final ParseException e) {
                throw new ParseException("The " + headerName + " has an incorrect version: "
                        + attVersion + " (" + e.getMessage() + ")", 0);
            }

            final Iterator itNames = element.getValues().iterator();
            while (itNames.hasNext()) {
                final String name = (String) itNames.next();
                bundleInfo.addRequirement(new BundleRequirement(type, name, version, resolution));
            }
        }
    }

    private static void parseCapability(final BundleInfo bundleInfo,
            final Attributes mainAttributes, final String headerName, final String type)
            throws ParseException {
        final ManifestHeaderValue elements = new ManifestHeaderValue(
                mainAttributes.getValue(headerName));
        final Iterator itElement = elements.getElements().iterator();
        while (itElement.hasNext()) {
            final ManifestHeaderElement element = (ManifestHeaderElement) itElement.next();
            final String attVersion = (String) element.getAttributes().get(ATTR_VERSION);
            Version version = null;
            try {
                version = versionOf(attVersion);
            } catch (final NumberFormatException e) {
                throw new ParseException("The " + headerName + " has an incorrect version: "
                        + attVersion + " (" + e.getMessage() + ")", 0);
            }

            final Iterator itNames = element.getValues().iterator();
            while (itNames.hasNext()) {
                final String name = (String) itNames.next();
                final BundleCapability export = new BundleCapability(type, name, version);
                bundleInfo.addCapability(export);
            }
        }

    }

    private static VersionRange versionRangeOf(final String v) throws ParseException {
        if (v == null) {
            return null;
        }
        return new VersionRange(v);
    }

    private static Version versionOf(final String v) throws ParseException {
        if (v == null) {
            return null;
        }
        return new Version(v);
    }

    /**
     * Ensure that the lines are not longer than 72 characters, so it can be parsed by the
     * {@link Manifest} class
     * 
     * @param manifest
     * @return
     */
    public static String formatLines(final String manifest) {
        final StringBuffer buffer = new StringBuffer(manifest.length());
        final String[] lines = manifest.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() <= 72) {
                buffer.append(lines[i]);
                buffer.append('\n');
            } else {
                buffer.append(lines[i].substring(0, 72));
                buffer.append("\n ");
                int n = 72;
                while (n <= (lines[i].length() - 1)) {
                    int end = n + 71;
                    if (end > lines[i].length()) {
                        end = lines[i].length();
                    }
                    buffer.append(lines[i].substring(n, end));
                    buffer.append('\n');
                    if (end != lines[i].length()) {
                        buffer.append(' ');
                    }
                    n = end;
                }
            }
        }
        return buffer.toString();
    }
}
