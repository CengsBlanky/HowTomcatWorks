/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/cluster/ClusterReceiver.java,v 1.2
 * 2001/07/22 20:25:06 pier Exp $ $Revision: 1.2 $ $Date: 2001/07/22 20:25:06 $
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

package org.apache.catalina.cluster;

import org.apache.catalina.Logger;

/**
 * This class is responsible for Receiving incoming packets in a Cluster.
 * Different Implementations may use different protocol to
 * communicate within the Cluster.
 *
 * @author Bip Thelin
 * @version $Revision: 1.2 $, $Date: 2001/07/22 20:25:06 $
 */

public interface ClusterReceiver extends Runnable {
    // --------------------------------------------------------- Public Methods

    /**
     * The senderId is a identifier used to identify different
     * packages being received in a Cluster. Each package received through
     * the concrete implementation of this interface will have
     * the senderId set at runtime. Usually the senderId is the
     * name of the component that is using this <code>ClusterReceiver</code>
     *
     * @param senderId The senderId to use
     */
    public void setSenderId(String senderId);

    /**
     * get the senderId used to identify messages being received in a Cluster.
     *
     * @return The senderId for this ClusterReceiver
     */
    public String getSenderId();

    /**
     * Set the debug detail level for this component.
     *
     * @param debug The debug level
     */
    public void setDebug(int debug);

    /**
     * Get the debug level for this component
     *
     * @return The debug level
     */
    public int getDebug();

    /**
     * Set the time in seconds for this component to
     * Sleep before it checks for new received data in the Cluster
     *
     * @param checkInterval The time to sleep
     */
    public void setCheckInterval(int checkInterval);

    /**
     * Get the time in seconds this implementation sleeps
     *
     * @return The time in seconds this implementation sleeps
     */
    public int getCheckInterval();

    /**
     * Set the Logger for this component.
     *
     * @param debug The Logger to use with this component.
     */
    public void setLogger(Logger logger);

    /**
     * Get the Logger for this component
     *
     * @return The Logger associated with this component.
     */
    public Logger getLogger();

    /**
     * The log method to use in the implementation
     *
     * @param message The message to be logged.
     */
    public void log(String message);

    /**
     * Get an array of objects that has been received by this component.
     * Only Objects which was received with the same senderId as the
     * one specified for this <code>ClusterReceiver</code> is being returned.
     *
     * @return a value of type 'Object[]'
     */
    public Object[] getObjects();

    /**
     * Start this component, must be called before it can be used.
     */
    public void start();

    /*
     * The background thread.
     */
    public void run();

    /**
     * The stop method for this component, should be called when closing
     * down the Cluster.
     */
    public void stop();
}
