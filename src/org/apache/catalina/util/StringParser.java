/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/util/StringParser.java,v 1.2 2001/07/22
 * 20:25:14 pier Exp $ $Revision: 1.2 $ $Date: 2001/07/22 20:25:14 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

package org.apache.catalina.util;

/**
 * Utility class for string parsing that is higher performance than
 * StringParser for simple delimited text cases.  Parsing is performed
 * by setting the string, and then using the <code>findXxxx()</code> and
 * <code>skipXxxx()</code> families of methods to remember significant
 * offsets.  To retrieve the parsed substrings, call the <code>extract()</code>
 * method with the appropriate saved offset values.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2001/07/22 20:25:14 $
 */

public final class StringParser {
    // ----------------------------------------------------------- Constructors

    /**
     * Construct a string parser with no preset string to be parsed.
     */
    public StringParser() {
        this(null);
    }

    /**
     * Construct a string parser that is initialized to parse the specified
     * string.
     *
     * @param string The string to be parsed
     */
    public StringParser(String string) {
        super();
        setString(string);
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The characters of the current string, as a character array.  Stored
     * when the string is first specified to speed up access to characters
     * being compared during parsing.
     */
    private char chars[] = null;

    /**
     * The zero-relative index of the current point at which we are
     * positioned within the string being parsed.  <strong>NOTE</strong>:
     * the value of this index can be one larger than the index of the last
     * character of the string (i.e. equal to the string length) if you
     * parse off the end of the string.  This value is useful for extracting
     * substrings that include the end of the string.
     */
    private int index = 0;

    /**
     * The length of the String we are currently parsing.  Stored when the
     * string is first specified to avoid repeated recalculations.
     */
    private int length = 0;

    /**
     * The String we are currently parsing.
     */
    private String string = null;

    // ------------------------------------------------------------- Properties

    /**
     * Return the zero-relative index of our current parsing position
     * within the string being parsed.
     */
    public int getIndex() {
        return (this.index);
    }

    /**
     * Return the length of the string we are parsing.
     */
    public int getLength() {
        return (this.length);
    }

    /**
     * Return the String we are currently parsing.
     */
    public String getString() {
        return (this.string);
    }

    /**
     * Set the String we are currently parsing.  The parser state is also reset
     * to begin at the start of this string.
     *
     * @param string The string to be parsed.
     */
    public void setString(String string) {
        this.string = string;
        if (string != null) {
            this.length = string.length();
            chars = this.string.toCharArray();
        } else {
            this.length = 0;
            chars = new char[0];
        }
        reset();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Advance the current parsing position by one, if we are not already
     * past the end of the string.
     */
    public void advance() {
        if (index < length)
            index++;
    }

    /**
     * Extract and return a substring that starts at the specified position,
     * and extends to the end of the string being parsed.  If this is not
     * possible, a zero-length string is returned.
     *
     * @param start Starting index, zero relative, inclusive
     */
    public String extract(int start) {
        if ((start < 0) || (start >= length))
            return ("");
        else
            return (string.substring(start));
    }

    /**
     * Extract and return a substring that starts at the specified position,
     * and ends at the character before the specified position.  If this is
     * not possible, a zero-length string is returned.
     *
     * @param start Starting index, zero relative, inclusive
     * @param end Ending index, zero relative, exclusive
     */
    public String extract(int start, int end) {
        if ((start < 0) || (start >= end) || (end > length))
            return ("");
        else
            return (string.substring(start, end));
    }

    /**
     * Return the index of the next occurrence of the specified character,
     * or the index of the character after the last position of the string
     * if no more occurrences of this character are found.  The current
     * parsing position is updated to the returned value.
     *
     * @param ch Character to be found
     */
    public int findChar(char ch) {
        while ((index < length) && (ch != chars[index])) index++;
        return (index);
    }

    /**
     * Return the index of the next occurrence of a non-whitespace character,
     * or the index of the character after the last position of the string
     * if no more non-whitespace characters are found.  The current
     * parsing position is updated to the returned value.
     */
    public int findText() {
        while ((index < length) && isWhite(chars[index])) index++;
        return (index);
    }

    /**
     * Return the index of the next occurrence of a whitespace character,
     * or the index of the character after the last position of the string
     * if no more whitespace characters are found.  The current parsing
     * position is updated to the returned value.
     */
    public int findWhite() {
        while ((index < length) && !isWhite(chars[index])) index++;
        return (index);
    }

    /**
     * Reset the current state of the parser to the beginning of the
     * current string being parsed.
     */
    public void reset() {
        index = 0;
    }

    /**
     * Advance the current parsing position while it is pointing at the
     * specified character, or until it moves past the end of the string.
     * Return the final value.
     *
     * @param ch Character to be skipped
     */
    public int skipChar(char ch) {
        while ((index < length) && (ch == chars[index])) index++;
        return (index);
    }

    /**
     * Advance the current parsing position while it is pointing at a
     * non-whitespace character, or until it moves past the end of the string.
     * Return the final value.
     */
    public int skipText() {
        while ((index < length) && !isWhite(chars[index])) index++;
        return (index);
    }

    /**
     * Advance the current parsing position while it is pointing at a
     * whitespace character, or until it moves past the end of the string.
     * Return the final value.
     */
    public int skipWhite() {
        while ((index < length) && isWhite(chars[index])) index++;
        return (index);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Is the specified character considered to be whitespace?
     *
     * @param ch Character to be checked
     */
    protected boolean isWhite(char ch) {
        if ((ch == ' ') || (ch == '\t') || (ch == '\r') || (ch == '\n'))
            return (true);
        else
            return (false);
    }
}
