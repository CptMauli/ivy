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

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Parse a header of a manifest. The manifest header is composed with the following rules:
 * 
 * <pre>
 * header ::= header-element (',' header-element)*
 * header-element ::= values (';' (attribute | directive) )*
 * values ::= value (';' value)*
 * value ::= &lt;any string value that does not have ';' or ','&gt;
 * attribute ::= key '=' value
 * directive ::= key '=' value
 * key ::= token
 * value ::= token | quoted-string | double-quoted-string
 * </pre>
 */
public class ManifestHeaderValue {

    private final List/* <ManifestHeaderElement> */elements = new ArrayList/*
                                                                            * <ManifestHeaderElement>
                                                                            */();

    ManifestHeaderValue() {
        // just for unit testing
    }

    public ManifestHeaderValue(final String header, final boolean isArbitrary)
            throws ParseException {
        if (header != null) {
            new ManifestHeaderParser(header).parse(isArbitrary);
        }
    }

    public ManifestHeaderValue(final String header) throws ParseException {
        if (header != null) {
            new ManifestHeaderParser(header).parse(false);
        }
    }

    public List/* <ManifestHeaderElement> */getElements() {
        return this.elements;
    }

    public String getSingleValue() {
        if (this.elements.isEmpty()) {
            return null;
        }
        final List/* <String> */values = ((ManifestHeaderElement) getElements().iterator().next())
                .getValues();
        if (values.isEmpty()) {
            return null;
        }
        return (String) values.iterator().next();
    }

    public List/* <String> */getValues() {
        if (this.elements.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        final List/* <String> */list = new ArrayList/* <String> */();
        final Iterator itElements = getElements().iterator();
        while (itElements.hasNext()) {
            final ManifestHeaderElement element = (ManifestHeaderElement) itElements.next();
            list.addAll(element.getValues());
        }
        return list;
    }

    void addElement(final ManifestHeaderElement element) {
        this.elements.add(element);
    }

    class ManifestHeaderParser {

        /**
         * header to parse
         */
        private final String header;

        /**
         * the length of the source
         */
        private final int length;

        /**
         * buffer
         */
        private final StringBuffer buffer = new StringBuffer();

        /**
         * position in the source
         */
        private int pos = 0;

        /**
         * last read character
         */
        private char c;

        /**
         * the header element being build
         */
        private ManifestHeaderElement elem = new ManifestHeaderElement();

        /**
         * Once at true (at the first attribute parsed), only parameters are allowed
         */
        private boolean valuesParsed;

        /**
         * the last parsed parameter name
         */
        private String paramName;

        /**
         * true if the last parsed parameter is a directive (assigned via :=)
         */
        private boolean isDirective;

        /**
         * Default constructor
         * 
         * @param header
         *            the header to parse
         */
        ManifestHeaderParser(final String header) {
            this.header = header;
            this.length = header.length();
        }

        /**
         * Do the parsing
         * 
         * @param isArbitrary
         * 
         * @throws ParseException
         */
        void parse(final boolean isArbitrary) throws ParseException {
            if (isArbitrary) {
                this.elem = new ManifestHeaderElement();
                final String trimmedHeader = this.header.trim();
                if (!trimmedHeader.isEmpty()) {
                    this.elem.addValue(trimmedHeader);
                    addElement(this.elem);
                }
            } else {
                do {
                    this.elem = new ManifestHeaderElement();
                    final int posElement = this.pos;
                    parseElement();
                    if (this.elem.getValues().isEmpty()) {
                        error("No defined value", posElement);
                        // try to recover: ignore that element
                        continue;
                    }
                    addElement(this.elem);
                } while (this.pos < this.length);
            }
        }

        private char readNext() {
            if (this.pos == this.length) {
                this.c = '\0';
            } else {
                this.c = this.header.charAt(this.pos++);
            }
            return this.c;
        }

        private void error(final String message) throws ParseException {
            error(message, this.pos - 1);
        }

        private void error(final String message, final int p) throws ParseException {
            throw new ParseException(message, p);
        }

        private void parseElement() throws ParseException {
            this.valuesParsed = false;
            do {
                parseValueOrParameter();
            } while ((this.c == ';') && (this.pos < this.length));
        }

        private void parseValueOrParameter() throws ParseException {
            // true if the value/parameter parsing has started, white spaces skipped
            boolean start = false;
            // true if the value/parameter parsing is ended, then only white spaces are allowed
            boolean end = false;
            do {
                switch (readNext()) {
                    case '\0':
                        break;
                    case ';':
                    case ',':
                        endValue();
                        return;
                    case ':':
                    case '=':
                        endParameterName();
                        parseSeparator();
                        parseParameterValue();
                        return;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        if (start) {
                            end = true;
                        }
                        break;
                    default:
                        if (end) {
                            error("Expecting the end of a value or of an parameter name");
                            // try to recover: restart the parsing of the value or parameter
                            end = false;
                        }
                        start = true;
                        this.buffer.append(this.c);
                }
            } while (this.pos < this.length);
            endValue();
        }

        private void endValue() throws ParseException {
            if (this.valuesParsed) {
                error("Early end of a parameter");
                // try to recover: ignore it
                this.buffer.setLength(0);
                return;
            }
            if (this.buffer.length() == 0) {
                error("Empty value");
                // try to recover: just ignore the error
            }
            this.elem.addValue(this.buffer.toString());
            this.buffer.setLength(0);
        }

        private void endParameterName() throws ParseException {
            if (this.buffer.length() == 0) {
                error("Empty parameter name");
                // try to recover: won't store the value
                this.paramName = null;
            }
            this.paramName = this.buffer.toString();
            this.buffer.setLength(0);
        }

        private void parseSeparator() throws ParseException {
            if (this.c == '=') {
                this.isDirective = false;
                return;
            }
            if (readNext() != '=') {
                error("Expecting '='");
                // try to recover: will ignore this parameter
                this.pos--;
                this.paramName = null;
            }
            this.isDirective = true;
        }

        private void parseParameterValue() throws ParseException {
            // true if the value parsing has started, white spaces skipped
            boolean start = false;
            // true if the value parsing is ended, then only white spaces are allowed
            boolean end = false;
            boolean doubleQuoted = false;
            do {
                switch (readNext()) {
                    case '\0':
                        break;

                    case ',':
                    case ';':
                        endParameterValue();
                        return;
                    case '=':
                    case ':':
                        error("Illegal character '" + this.c + "' in parameter value of "
                                + this.paramName);
                        // try to recover: ignore that parameter
                        this.paramName = null;
                        break;
                    case '\"':
                        doubleQuoted = true;
                    case '\'':
                        if (end && (this.paramName != null)) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            this.paramName = null;
                        }
                        if (start) {
                            // quote in the middle of the value, just add it as a quote
                            this.buffer.append(this.c);
                        } else {
                            start = true;
                            appendQuoted(doubleQuoted);
                            end = true;
                        }
                        break;
                    case '\\':
                        if (end && (this.paramName != null)) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            this.paramName = null;
                        }
                        start = true;
                        appendEscaped();
                        break;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        if (start) {
                            end = true;
                        }
                        break;
                    default:
                        if (end && (this.paramName != null)) {
                            error("Expecting the end of a parameter value");
                            // try to recover: ignore that parameter
                            this.paramName = null;
                        }
                        start = true;
                        this.buffer.append(this.c);
                }
            } while (this.pos < this.length);
            endParameterValue();
        }

        private void endParameterValue() throws ParseException {
            if (this.paramName == null) {
                // recovering from an incorrect parameter: skip the value
                return;
            }
            if (this.buffer.length() == 0) {
                error("Empty parameter value");
                // try to recover: do not store the parameter
                return;
            }
            final String value = this.buffer.toString();
            if (this.isDirective) {
                this.elem.addDirective(this.paramName, value);
            } else {
                this.elem.addAttribute(this.paramName, value);
            }
            this.valuesParsed = true;
            this.buffer.setLength(0);
        }

        private void appendQuoted(final boolean doubleQuoted) {
            do {
                switch (readNext()) {
                    case '\0':
                        break;
                    case '\"':
                        if (doubleQuoted) {
                            return;
                        }
                        this.buffer.append(this.c);
                        break;
                    case '\'':
                        if (!doubleQuoted) {
                            return;
                        }
                        this.buffer.append(this.c);
                        break;
                    case '\\':
                        break;
                    default:
                        this.buffer.append(this.c);
                }
            } while (this.pos < this.length);
        }

        private void appendEscaped() {
            if (this.pos < this.length) {
                this.buffer.append(readNext());
            } else {
                this.buffer.append(this.c);
            }
        }
    }

    public boolean equals(final Object obj) {
        if (!(obj instanceof ManifestHeaderValue)) {
            return false;
        }
        final ManifestHeaderValue other = (ManifestHeaderValue) obj;
        if (other.elements.size() != this.elements.size()) {
            return false;
        }
        final Iterator itElements = this.elements.iterator();
        while (itElements.hasNext()) {
            final ManifestHeaderElement element = (ManifestHeaderElement) itElements.next();
            if (!other.elements.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        String string = "";
        final Iterator/* <ManifestHeaderElement> */it = this.elements.iterator();
        while (it.hasNext()) {
            string = string.concat(it.next().toString());
            if (it.hasNext()) {
                string = string.concat(",");
            }
        }
        return string;
    }

    public static void writeParseException(final PrintStream out, final String source,
            final ParseException e) {
        out.println(e.getMessage());
        out.print("   " + source + "\n   ");
        for (int i = 0; i < e.getErrorOffset(); i++) {
            out.print(' ');
        }
        out.println('^');
    }
}
