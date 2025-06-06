/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/mbeans/GroupMBean.java,v 1.2 2002/02/03
 * 00:56:57 craigmcc Exp $ $Revision: 1.2 $ $Date: 2002/02/03 00:56:57 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

package org.apache.catalina.mbeans;

import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;

import java.util.ArrayList;
import java.util.Iterator;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

/**
 * <p>A <strong>ModelMBean</strong> implementation for the
 * <code>org.apache.catalina.Group</code> component.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2002/02/03 00:56:57 $
 */

public class GroupMBean extends BaseModelMBean {
    // ----------------------------------------------------------- Constructors

    /**
     * Construct a <code>ModelMBean</code> with default
     * <code>ModelMBeanInfo</code> information.
     *
     * @exception MBeanException if the initializer of an object
     *  throws an exception
     * @exception RuntimeOperationsException if an IllegalArgumentException
     *  occurs
     */
    public GroupMBean() throws MBeanException, RuntimeOperationsException {
        super();
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The configuration information registry for our managed beans.
     */
    protected Registry registry = MBeanUtils.createRegistry();

    /**
     * The <code>MBeanServer</code> in which we are registered.
     */
    protected MBeanServer mserver = MBeanUtils.createServer();

    /**
     * The <code>ManagedBean</code> information describing this MBean.
     */
    protected ManagedBean managed = registry.findManagedBean("Group");

    // ------------------------------------------------------------- Attributes

    /**
     * Return the MBean Names of all authorized roles for this group.
     */
    public String[] getRoles() {
        Group group = (Group) this.resource;
        ArrayList results = new ArrayList();
        Iterator roles = group.getRoles();
        while (roles.hasNext()) {
            Role role = null;
            try {
                role = (Role) roles.next();
                ObjectName oname = MBeanUtils.createObjectName(managed.getDomain(), role);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException("Cannot create object name for role " + role);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));
    }

    /**
     * Return the MBean Names of all users that are members of this group.
     */
    public String[] getUsers() {
        Group group = (Group) this.resource;
        ArrayList results = new ArrayList();
        Iterator users = group.getUsers();
        while (users.hasNext()) {
            User user = null;
            try {
                user = (User) users.next();
                ObjectName oname = MBeanUtils.createObjectName(managed.getDomain(), user);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException("Cannot create object name for user " + user);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));
    }

    // ------------------------------------------------------------- Operations

    /**
     * Add a new {@link Role} to those this group belongs to.
     *
     * @param rolename Role name of the new role
     */
    public void addRole(String rolename) {
        Group group = (Group) this.resource;
        if (group == null) {
            return;
        }
        Role role = group.getUserDatabase().findRole(rolename);
        if (role == null) {
            throw new IllegalArgumentException("Invalid role name '" + rolename + "'");
        }
        group.addRole(role);
    }

    /**
     * Remove a {@link Role} from those this group belongs to.
     *
     * @param rolename Role name of the old role
     */
    public void removeRole(String rolename) {
        Group group = (Group) this.resource;
        if (group == null) {
            return;
        }
        Role role = group.getUserDatabase().findRole(rolename);
        if (role == null) {
            throw new IllegalArgumentException("Invalid role name '" + rolename + "'");
        }
        group.removeRole(role);
    }
}
