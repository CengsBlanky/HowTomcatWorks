/*
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
 */
package org.apache.catalina.util;

import java.util.Vector;

/**
 * A simple FIFO queue class which causes the calling thread to wait
 * if the queue is empty and notifies threads that are waiting when it
 * is not empty.
 *
 * @author Anil V (akv@eng.sun.com)
 */
public class Queue {
    private Vector vector = new Vector();

    /**
     * Put the object into the queue.
     *
     * @param   object          the object to be appended to the
     *                          queue.
     */
    public synchronized void put(Object object) {
        vector.addElement(object);
        notify();
    }

    /**
     * Pull the first object out of the queue. Wait if the queue is
     * empty.
     */
    public synchronized Object pull() {
        while (isEmpty()) try {
                wait();
            } catch (InterruptedException ex) {
            }
        return get();
    }

    /**
     * Get the first object out of the queue. Return null if the queue
     * is empty.
     */
    public synchronized Object get() {
        Object object = peek();
        if (object != null)
            vector.removeElementAt(0);
        return object;
    }

    /**
     * Peek to see if something is available.
     */
    public Object peek() {
        if (isEmpty())
            return null;
        return vector.elementAt(0);
    }

    /**
     * Is the queue empty?
     */
    public boolean isEmpty() {
        return vector.isEmpty();
    }

    /**
     * How many elements are there in this queue?
     */
    public int size() {
        return vector.size();
    }
}
