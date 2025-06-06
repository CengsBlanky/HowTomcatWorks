/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/naming/resources/ProxyDirContext.java,v 1.12
 * 2002/06/11 00:31:43 remm Exp $ $Revision: 1.12 $ $Date: 2002/06/11 00:31:43 $
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

package org.apache.naming.resources;

import org.apache.commons.collections.LRUMap;
import org.apache.naming.StringManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

/**
 * Proxy Directory Context implementation.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.12 $ $Date: 2002/06/11 00:31:43 $
 */

public class ProxyDirContext implements DirContext {
    // -------------------------------------------------------------- Constants

    public static final String CONTEXT = "context";
    public static final String HOST = "host";

    // ----------------------------------------------------------- Constructors

    /**
     * Builds a proxy directory context using the given environment.
     */
    public ProxyDirContext(Hashtable env, DirContext dirContext) {
        this.env = env;
        this.dirContext = dirContext;
        if (dirContext instanceof BaseDirContext) {
            // Initialize parameters based on the associated dir context, like
            // the caching policy.
            if (((BaseDirContext) dirContext).isCached()) {
                cache = Collections.synchronizedMap(new LRUMap(cacheSize));
                cacheTTL = ((BaseDirContext) dirContext).getCacheTTL();
                cacheObjectMaxSize = ((BaseDirContext) dirContext).getCacheObjectMaxSize();
            }
        }
        hostName = (String) env.get(HOST);
        contextName = (String) env.get(CONTEXT);
    }

    /**
     * Builds a clone of this proxy dir context, wrapping the given directory
     * context, and sharing the same cache.
     */
    protected ProxyDirContext(ProxyDirContext proxyDirContext, DirContext dirContext, String vPath) {
        this.env = proxyDirContext.env;
        this.dirContext = dirContext;
        this.vPath = vPath;
        this.cache = proxyDirContext.cache;
        this.cacheSize = proxyDirContext.cacheSize;
        this.cacheTTL = proxyDirContext.cacheTTL;
        this.cacheObjectMaxSize = proxyDirContext.cacheObjectMaxSize;
        this.hostName = proxyDirContext.hostName;
        this.contextName = proxyDirContext.contextName;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * Environment.
     */
    protected Hashtable env;

    /**
     * The string manager for this package.
     */
    protected StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * Associated DirContext.
     */
    protected DirContext dirContext;

    /**
     * Virtual path.
     */
    protected String vPath = null;

    /**
     * Host name.
     */
    protected String hostName;

    /**
     * Context name.
     */
    protected String contextName;

    /**
     * Cache.
     * Path -> Cache entry.
     */
    protected Map cache = null;

    /**
     * Cache size
     */
    protected int cacheSize = 1000;

    /**
     * Cache TTL.
     */
    protected int cacheTTL = 5000; // 5s

    /**
     * Max size of resources which will have their content cached.
     */
    protected int cacheObjectMaxSize = 32768; // 32 KB

    // --------------------------------------------------------- Public Methods

    /**
     * Return the actual directory context we are wrapping.
     */
    public DirContext getDirContext() {
        return this.dirContext;
    }

    /**
     * Return the document root for this component.
     */
    public String getDocBase() {
        if (dirContext instanceof BaseDirContext)
            return ((BaseDirContext) dirContext).getDocBase();
        else
            return "";
    }

    /**
     * Return the host name.
     */
    public String getHostName() {
        return this.hostName;
    }

    /**
     * Return the context name.
     */
    public String getContextName() {
        return this.contextName;
    }

    // -------------------------------------------------------- Context Methods

    /**
     * Retrieves the named object. If name is empty, returns a new instance
     * of this context (which represents the same naming context as this
     * context, but its environment may be modified independently and it may
     * be accessed concurrently).
     *
     * @param name the name of the object to look up
     * @return the object bound to name
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookup(Name name) throws NamingException {
        CacheEntry entry = cacheLookup(name.toString());
        if (entry != null) {
            if (entry.resource != null) {
                // Check content caching.

                return entry.resource;
            } else {
                return entry.context;
            }
        }
        Object object = dirContext.lookup(parseName(name));
        if (object instanceof InputStream)
            return new Resource((InputStream) object);
        else
            return object;
    }

    /**
     * Retrieves the named object.
     *
     * @param name the name of the object to look up
     * @return the object bound to name
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookup(String name) throws NamingException {
        CacheEntry entry = cacheLookup(name);
        if (entry != null) {
            if (entry.resource != null) {
                return entry.resource;
            } else {
                return entry.context;
            }
        }
        Object object = dirContext.lookup(parseName(name));
        if (object instanceof InputStream) {
            return new Resource((InputStream) object);
        } else if (object instanceof DirContext) {
            return object;
        } else if (object instanceof Resource) {
            return object;
        } else {
            return new Resource(new ByteArrayInputStream(object.toString().getBytes()));
        }
    }

    /**
     * Binds a name to an object. All intermediate contexts and the target
     * context (that named by all but terminal atomic component of the name)
     * must already exist.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if object did not supply all
     * mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public void bind(Name name, Object obj) throws NamingException {
        dirContext.bind(parseName(name), obj);
        cacheUnload(name.toString());
    }

    /**
     * Binds a name to an object.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if object did not supply all
     * mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public void bind(String name, Object obj) throws NamingException {
        dirContext.bind(parseName(name), obj);
        cacheUnload(name);
    }

    /**
     * Binds a name to an object, overwriting any existing binding. All
     * intermediate contexts and the target context (that named by all but
     * terminal atomic component of the name) must already exist.
     * <p>
     * If the object is a DirContext, any existing attributes associated with
     * the name are replaced with those of the object. Otherwise, any
     * existing attributes associated with the name remain unchanged.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @exception InvalidAttributesException if object did not supply all
     * mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public void rebind(Name name, Object obj) throws NamingException {
        dirContext.rebind(parseName(name), obj);
        cacheUnload(name.toString());
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @exception InvalidAttributesException if object did not supply all
     * mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj) throws NamingException {
        dirContext.rebind(parseName(name), obj);
        cacheUnload(name);
    }

    /**
     * Unbinds the named object. Removes the terminal atomic name in name
     * from the target context--that named by all but the terminal atomic
     * part of name.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic
     * name is not bound in the target context, but throws
     * NameNotFoundException if any of the intermediate contexts do not exist.
     *
     * @param name the name to bind; may not be empty
     * @exception NameNotFoundException if an intermediate context does not
     * exist
     * @exception NamingException if a naming exception is encountered
     */
    public void unbind(Name name) throws NamingException {
        dirContext.unbind(parseName(name));
        cacheUnload(name.toString());
    }

    /**
     * Unbinds the named object.
     *
     * @param name the name to bind; may not be empty
     * @exception NameNotFoundException if an intermediate context does not
     * exist
     * @exception NamingException if a naming exception is encountered
     */
    public void unbind(String name) throws NamingException {
        dirContext.unbind(parseName(name));
        cacheUnload(name);
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds the
     * old name. Both names are relative to this context. Any attributes
     * associated with the old name become associated with the new name.
     * Intermediate contexts of the old name are not changed.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @exception NameAlreadyBoundException if newName is already bound
     * @exception NamingException if a naming exception is encountered
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        dirContext.rename(parseName(oldName), parseName(newName));
        cacheUnload(oldName.toString());
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds the
     * old name.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @exception NameAlreadyBoundException if newName is already bound
     * @exception NamingException if a naming exception is encountered
     */
    public void rename(String oldName, String newName) throws NamingException {
        dirContext.rename(parseName(oldName), parseName(newName));
        cacheUnload(oldName);
    }

    /**
     * Enumerates the names bound in the named context, along with the class
     * names of objects bound to them. The contents of any subcontexts are
     * not included.
     * <p>
     * If a binding is added to or removed from this context, its effect on
     * an enumeration previously returned is undefined.
     *
     * @param name the name of the context to list
     * @return an enumeration of the names and class names of the bindings in
     * this context. Each element of the enumeration is of type NameClassPair.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(Name name) throws NamingException {
        return dirContext.list(parseName(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the class
     * names of objects bound to them.
     *
     * @param name the name of the context to list
     * @return an enumeration of the names and class names of the bindings in
     * this context. Each element of the enumeration is of type NameClassPair.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(String name) throws NamingException {
        return dirContext.list(parseName(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them. The contents of any subcontexts are not
     * included.
     * <p>
     * If a binding is added to or removed from this context, its effect on
     * an enumeration previously returned is undefined.
     *
     * @param name the name of the context to list
     * @return an enumeration of the bindings in this context.
     * Each element of the enumeration is of type Binding.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return dirContext.listBindings(parseName(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them.
     *
     * @param name the name of the context to list
     * @return an enumeration of the bindings in this context.
     * Each element of the enumeration is of type Binding.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return dirContext.listBindings(parseName(name));
    }

    /**
     * Destroys the named context and removes it from the namespace. Any
     * attributes associated with the name are also removed. Intermediate
     * contexts are not destroyed.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic
     * name is not bound in the target context, but throws
     * NameNotFoundException if any of the intermediate contexts do not exist.
     *
     * In a federated naming system, a context from one naming system may be
     * bound to a name in another. One can subsequently look up and perform
     * operations on the foreign context using a composite name. However, an
     * attempt destroy the context using this composite name will fail with
     * NotContextException, because the foreign context is not a "subcontext"
     * of the context in which it is bound. Instead, use unbind() to remove
     * the binding of the foreign context. Destroying the foreign context
     * requires that the destroySubcontext() be performed on a context from
     * the foreign context's "native" naming system.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @exception NameNotFoundException if an intermediate context does not
     * exist
     * @exception NotContextException if the name is bound but does not name
     * a context, or does not name a context of the appropriate type
     */
    public void destroySubcontext(Name name) throws NamingException {
        dirContext.destroySubcontext(parseName(name));
        cacheUnload(name.toString());
    }

    /**
     * Destroys the named context and removes it from the namespace.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @exception NameNotFoundException if an intermediate context does not
     * exist
     * @exception NotContextException if the name is bound but does not name
     * a context, or does not name a context of the appropriate type
     */
    public void destroySubcontext(String name) throws NamingException {
        dirContext.destroySubcontext(parseName(name));
        cacheUnload(name);
    }

    /**
     * Creates and binds a new context. Creates a new context with the given
     * name and binds it in the target context (that named by all but
     * terminal atomic component of the name). All intermediate contexts and
     * the target context must already exist.
     *
     * @param name the name of the context to create; may not be empty
     * @return the newly created context
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if creation of the subcontext
     * requires specification of mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public Context createSubcontext(Name name) throws NamingException {
        return dirContext.createSubcontext(parseName(name));
    }

    /**
     * Creates and binds a new context.
     *
     * @param name the name of the context to create; may not be empty
     * @return the newly created context
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if creation of the subcontext
     * requires specification of mandatory attributes
     * @exception NamingException if a naming exception is encountered
     */
    public Context createSubcontext(String name) throws NamingException {
        return dirContext.createSubcontext(parseName(name));
    }

    /**
     * Retrieves the named object, following links except for the terminal
     * atomic component of the name. If the object bound to name is not a
     * link, returns the object itself.
     *
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link
     * (if any).
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookupLink(Name name) throws NamingException {
        return dirContext.lookupLink(parseName(name));
    }

    /**
     * Retrieves the named object, following links except for the terminal
     * atomic component of the name.
     *
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link
     * (if any).
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookupLink(String name) throws NamingException {
        return dirContext.lookupLink(parseName(name));
    }

    /**
     * Retrieves the parser associated with the named context. In a
     * federation of namespaces, different naming systems will parse names
     * differently. This method allows an application to get a parser for
     * parsing names into their atomic components using the naming convention
     * of a particular naming system. Within any single naming system,
     * NameParser objects returned by this method must be equal (using the
     * equals() test).
     *
     * @param name the name of the context from which to get the parser
     * @return a name parser that can parse compound names into their atomic
     * components
     * @exception NamingException if a naming exception is encountered
     */
    public NameParser getNameParser(Name name) throws NamingException {
        return dirContext.getNameParser(parseName(name));
    }

    /**
     * Retrieves the parser associated with the named context.
     *
     * @param name the name of the context from which to get the parser
     * @return a name parser that can parse compound names into their atomic
     * components
     * @exception NamingException if a naming exception is encountered
     */
    public NameParser getNameParser(String name) throws NamingException {
        return dirContext.getNameParser(parseName(name));
    }

    /**
     * Composes the name of this context with a name relative to this context.
     * <p>
     * Given a name (name) relative to this context, and the name (prefix)
     * of this context relative to one of its ancestors, this method returns
     * the composition of the two names using the syntax appropriate for the
     * naming system(s) involved. That is, if name names an object relative
     * to this context, the result is the name of the same object, but
     * relative to the ancestor context. None of the names may be null.
     *
     * @param name a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return the composition of prefix and name
     * @exception NamingException if a naming exception is encountered
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        prefix = (Name) name.clone();
        return prefix.addAll(name);
    }

    /**
     * Composes the name of this context with a name relative to this context.
     *
     * @param name a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return the composition of prefix and name
     * @exception NamingException if a naming exception is encountered
     */
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + "/" + name;
    }

    /**
     * Adds a new environment property to the environment of this context. If
     * the property already exists, its value is overwritten.
     *
     * @param propName the name of the environment property to add; may not
     * be null
     * @param propVal the value of the property to add; may not be null
     * @exception NamingException if a naming exception is encountered
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return dirContext.addToEnvironment(propName, propVal);
    }

    /**
     * Removes an environment property from the environment of this context.
     *
     * @param propName the name of the environment property to remove;
     * may not be null
     * @exception NamingException if a naming exception is encountered
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        return dirContext.removeFromEnvironment(propName);
    }

    /**
     * Retrieves the environment in effect for this context. See class
     * description for more details on environment properties.
     * The caller should not make any changes to the object returned: their
     * effect on the context is undefined. The environment of this context
     * may be changed using addToEnvironment() and removeFromEnvironment().
     *
     * @return the environment of this context; never null
     * @exception NamingException if a naming exception is encountered
     */
    public Hashtable getEnvironment() throws NamingException {
        return dirContext.getEnvironment();
    }

    /**
     * Closes this context. This method releases this context's resources
     * immediately, instead of waiting for them to be released automatically
     * by the garbage collector.
     * This method is idempotent: invoking it on a context that has already
     * been closed has no effect. Invoking any other method on a closed
     * context is not allowed, and results in undefined behaviour.
     *
     * @exception NamingException if a naming exception is encountered
     */
    public void close() throws NamingException {
        dirContext.close();
    }

    /**
     * Retrieves the full name of this context within its own namespace.
     * <p>
     * Many naming services have a notion of a "full name" for objects in
     * their respective namespaces. For example, an LDAP entry has a
     * distinguished name, and a DNS record has a fully qualified name. This
     * method allows the client application to retrieve this name. The string
     * returned by this method is not a JNDI composite name and should not be
     * passed directly to context methods. In naming systems for which the
     * notion of full name does not make sense,
     * OperationNotSupportedException is thrown.
     *
     * @return this context's name in its own namespace; never null
     * @exception OperationNotSupportedException if the naming system does
     * not have the notion of a full name
     * @exception NamingException if a naming exception is encountered
     */
    public String getNameInNamespace() throws NamingException {
        return dirContext.getNameInNamespace();
    }

    // ----------------------------------------------------- DirContext Methods

    /**
     * Retrieves all of the attributes associated with a named object.
     *
     * @return the set of attributes associated with name.
     * Returns an empty attribute set if name has no attributes; never null.
     * @param name the name of the object from which to retrieve attributes
     * @exception NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(Name name) throws NamingException {
        CacheEntry entry = cacheLookup(name.toString());
        if (entry != null) {
            return entry.attributes;
        }
        Attributes attributes = dirContext.getAttributes(parseName(name));
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    /**
     * Retrieves all of the attributes associated with a named object.
     *
     * @return the set of attributes associated with name
     * @param name the name of the object from which to retrieve attributes
     * @exception NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name) throws NamingException {
        CacheEntry entry = cacheLookup(name);
        if (entry != null) {
            return entry.attributes;
        }
        Attributes attributes = dirContext.getAttributes(parseName(name));
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    /**
     * Retrieves selected attributes associated with a named object.
     * See the class description regarding attribute models, attribute type
     * names, and operational attributes.
     *
     * @return the requested attributes; never null
     * @param name the name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve. null
     * indicates that all attributes should be retrieved; an empty array
     * indicates that none should be retrieved
     * @exception NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        Attributes attributes = dirContext.getAttributes(parseName(name), attrIds);
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    /**
     * Retrieves selected attributes associated with a named object.
     *
     * @return the requested attributes; never null
     * @param name the name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve. null
     * indicates that all attributes should be retrieved; an empty array
     * indicates that none should be retrieved
     * @exception NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        Attributes attributes = dirContext.getAttributes(parseName(name), attrIds);
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    /**
     * Modifies the attributes associated with a named object. The order of
     * the modifications is not specified. Where possible, the modifications
     * are performed atomically.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mod_op the modification operation, one of: ADD_ATTRIBUTE,
     * REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE
     * @param attrs the attributes to be used for the modification; may not
     * be null
     * @exception AttributeModificationException if the modification cannot be
     * completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        dirContext.modifyAttributes(parseName(name), mod_op, attrs);
    }

    /**
     * Modifies the attributes associated with a named object.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mod_op the modification operation, one of: ADD_ATTRIBUTE,
     * REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE
     * @param attrs the attributes to be used for the modification; may not
     * be null
     * @exception AttributeModificationException if the modification cannot be
     * completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        dirContext.modifyAttributes(parseName(name), mod_op, attrs);
    }

    /**
     * Modifies the attributes associated with a named object using an an
     * ordered list of modifications. The modifications are performed in the
     * order specified. Each modification specifies a modification operation
     * code and an attribute on which to operate. Where possible, the
     * modifications are performed atomically.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mods an ordered sequence of modifications to be performed; may
     * not be null
     * @exception AttributeModificationException if the modification cannot be
     * completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        dirContext.modifyAttributes(parseName(name), mods);
    }

    /**
     * Modifies the attributes associated with a named object using an an
     * ordered list of modifications.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mods an ordered sequence of modifications to be performed; may
     * not be null
     * @exception AttributeModificationException if the modification cannot be
     * completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        dirContext.modifyAttributes(parseName(name), mods);
    }

    /**
     * Binds a name to an object, along with associated attributes. If attrs
     * is null, the resulting binding will have the attributes associated
     * with obj if obj is a DirContext, and no attributes otherwise. If attrs
     * is non-null, the resulting binding will have attrs as its attributes;
     * any attributes associated with obj are ignored.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirContext.bind(parseName(name), obj, attrs);
    }

    /**
     * Binds a name to an object, along with associated attributes.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception NameAlreadyBoundException if name is already bound
     * @exception InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        dirContext.bind(parseName(name), obj, attrs);
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding. If attrs is null and obj is a
     * DirContext, the attributes from obj are used. If attrs is null and obj
     * is not a DirContext, any existing attributes associated with the object
     * already bound in the directory remain unchanged. If attrs is non-null,
     * any existing attributes associated with the object already bound in
     * the directory are removed and attrs is associated with the named
     * object. If obj is a DirContext and attrs is non-null, the attributes
     * of obj are ignored.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirContext.rebind(parseName(name), obj, attrs);
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     *
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        dirContext.rebind(parseName(name), obj, attrs);
    }

    /**
     * Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it in
     * the target context (that named by all but terminal atomic component of
     * the name), and associates the supplied attributes with the newly
     * created object. All intermediate and target contexts must already
     * exist. If attrs is null, this method is equivalent to
     * Context.createSubcontext().
     *
     * @param name the name of the context to create; may not be empty
     * @param attrs the attributes to associate with the newly created context
     * @return the newly created context
     * @exception NameAlreadyBoundException if the name is already bound
     * @exception InvalidAttributesException if attrs does not contain all
     * the mandatory attributes required for creation
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return dirContext.createSubcontext(parseName(name), attrs);
    }

    /**
     * Creates and binds a new context, along with associated attributes.
     *
     * @param name the name of the context to create; may not be empty
     * @param attrs the attributes to associate with the newly created context
     * @return the newly created context
     * @exception NameAlreadyBoundException if the name is already bound
     * @exception InvalidAttributesException if attrs does not contain all
     * the mandatory attributes required for creation
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return dirContext.createSubcontext(parseName(name), attrs);
    }

    /**
     * Retrieves the schema associated with the named object. The schema
     * describes rules regarding the structure of the namespace and the
     * attributes stored within it. The schema specifies what types of
     * objects can be added to the directory and where they can be added;
     * what mandatory and optional attributes an object can have. The range
     * of support for schemas is directory-specific.
     *
     * @param name the name of the object whose schema is to be retrieved
     * @return the schema associated with the context; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchema(Name name) throws NamingException {
        return dirContext.getSchema(parseName(name));
    }

    /**
     * Retrieves the schema associated with the named object.
     *
     * @param name the name of the object whose schema is to be retrieved
     * @return the schema associated with the context; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchema(String name) throws NamingException {
        return dirContext.getSchema(parseName(name));
    }

    /**
     * Retrieves a context containing the schema objects of the named
     * object's class definitions.
     *
     * @param name the name of the object whose object class definition is to
     * be retrieved
     * @return the DirContext containing the named object's class
     * definitions; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return dirContext.getSchemaClassDefinition(parseName(name));
    }

    /**
     * Retrieves a context containing the schema objects of the named
     * object's class definitions.
     *
     * @param name the name of the object whose object class definition is to
     * be retrieved
     * @return the DirContext containing the named object's class
     * definitions; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return dirContext.getSchemaClassDefinition(parseName(name));
    }

    /**
     * Searches in a single context for objects that contain a specified set
     * of attributes, and retrieves selected attributes. The search is
     * performed using the default SearchControls settings.
     *
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or
     * null, all objects in the target context are returned.
     * @param attributesToReturn the attributes to return. null indicates
     * that all attributes are to be returned; an empty array indicates that
     * none are to be returned.
     * @return a non-null enumeration of SearchResult objects. Each
     * SearchResult contains the attributes identified by attributesToReturn
     * and the name of the corresponding object, named relative to the
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
      throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes, attributesToReturn);
    }

    /**
     * Searches in a single context for objects that contain a specified set
     * of attributes, and retrieves selected attributes.
     *
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or
     * null, all objects in the target context are returned.
     * @param attributesToReturn the attributes to return. null indicates
     * that all attributes are to be returned; an empty array indicates that
     * none are to be returned.
     * @return a non-null enumeration of SearchResult objects. Each
     * SearchResult contains the attributes identified by attributesToReturn
     * and the name of the corresponding object, named relative to the
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn)
      throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes, attributesToReturn);
    }

    /**
     * Searches in a single context for objects that contain a specified set
     * of attributes. This method returns all the attributes of such objects.
     * It is equivalent to supplying null as the atributesToReturn parameter
     * to the method search(Name, Attributes, String[]).
     *
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or
     * null, all objects in the target context are returned.
     * @return a non-null enumeration of SearchResult objects. Each
     * SearchResult contains the attributes identified by attributesToReturn
     * and the name of the corresponding object, named relative to the
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes) throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes);
    }

    /**
     * Searches in a single context for objects that contain a specified set
     * of attributes.
     *
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or
     * null, all objects in the target context are returned.
     * @return a non-null enumeration of SearchResult objects. Each
     * SearchResult contains the attributes identified by attributesToReturn
     * and the name of the corresponding object, named relative to the
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes);
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter. Performs the search as specified by the search
     * controls.
     *
     * @param name the name of the context or object to search
     * @param filter the filter expression to use for the search; may not be
     * null
     * @param cons the search controls that control the search. If null,
     * the default search controls are used (equivalent to
     * (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisfy
     * the filter; never null
     * @exception InvalidSearchFilterException if the search filter specified
     * is not supported or understood by the underlying directory
     * @exception InvalidSearchControlsException if the search controls
     * contain invalid settings
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, String filter, SearchControls cons) throws NamingException {
        return dirContext.search(parseName(name), filter, cons);
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter. Performs the search as specified by the search
     * controls.
     *
     * @param name the name of the context or object to search
     * @param filter the filter expression to use for the search; may not be
     * null
     * @param cons the search controls that control the search. If null,
     * the default search controls are used (equivalent to
     * (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisfy
     * the filter; never null
     * @exception InvalidSearchFilterException if the search filter
     * specified is not supported or understood by the underlying directory
     * @exception InvalidSearchControlsException if the search controls
     * contain invalid settings
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        return dirContext.search(parseName(name), filter, cons);
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter. Performs the search as specified by the search
     * controls.
     *
     * @param name the name of the context or object to search
     * @param filterExpr the filter expression to use for the search.
     * The expression may contain variables of the form "{i}" where i is a
     * nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the
     * variables in filterExpr. The value of filterArgs[i] will replace each
     * occurrence of "{i}". If null, equivalent to an empty array.
     * @param cons the search controls that control the search. If null, the
     * default search controls are used (equivalent to (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisy the
     * filter; never null
     * @exception ArrayIndexOutOfBoundsException if filterExpr contains {i}
     * expressions where i is outside the bounds of the array filterArgs
     * @exception InvalidSearchControlsException if cons contains invalid
     * settings
     * @exception InvalidSearchFilterException if filterExpr with filterArgs
     * represents an invalid search filter
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
      throws NamingException {
        return dirContext.search(parseName(name), filterExpr, filterArgs, cons);
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter. Performs the search as specified by the search
     * controls.
     *
     * @param name the name of the context or object to search
     * @param filterExpr the filter expression to use for the search.
     * The expression may contain variables of the form "{i}" where i is a
     * nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the
     * variables in filterExpr. The value of filterArgs[i] will replace each
     * occurrence of "{i}". If null, equivalent to an empty array.
     * @param cons the search controls that control the search. If null, the
     * default search controls are used (equivalent to (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisy the
     * filter; never null
     * @exception ArrayIndexOutOfBoundsException if filterExpr contains {i}
     * expressions where i is outside the bounds of the array filterArgs
     * @exception InvalidSearchControlsException if cons contains invalid
     * settings
     * @exception InvalidSearchFilterException if filterExpr with filterArgs
     * represents an invalid search filter
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
      throws NamingException {
        return dirContext.search(parseName(name), filterExpr, filterArgs, cons);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Parses a name.
     *
     * @return the parsed name
     */
    protected String parseName(String name) throws NamingException {
        return name;
    }

    /**
     * Parses a name.
     *
     * @return the parsed name
     */
    protected Name parseName(Name name) throws NamingException {
        return name;
    }

    /**
     * Lookup in cache.
     */
    protected CacheEntry cacheLookup(String name) {
        if (cache == null)
            return (null);
        CacheEntry cacheEntry = (CacheEntry) cache.get(name);
        if (cacheEntry == null) {
            cacheEntry = new CacheEntry();
            cacheEntry.name = name;
            // Load entry
            if (!cacheLoad(cacheEntry))
                return null;
            return (cacheEntry);
        } else {
            if (!validate(cacheEntry)) {
                if (!revalidate(cacheEntry)) {
                    cacheUnload(cacheEntry.name);
                    return (null);
                } else {
                    cacheEntry.timestamp = System.currentTimeMillis() + cacheTTL;
                }
            }
            return (cacheEntry);
        }
    }

    /**
     * Validate entry.
     */
    protected boolean validate(CacheEntry entry) {
        if ((entry.resource != null) && (entry.resource.getContent() != null)
          && (System.currentTimeMillis() < entry.timestamp)) {
            return true;
        }
        return false;
    }

    /**
     * Revalidate entry.
     */
    protected boolean revalidate(CacheEntry entry) {
        // Get the attributes at the given path, and check the last
        // modification date
        if (entry.attributes == null)
            return false;
        long lastModified = entry.attributes.getLastModified();
        long contentLength = entry.attributes.getContentLength();
        if (lastModified <= 0)
            return false;
        try {
            Attributes tempAttributes = dirContext.getAttributes(entry.name);
            ResourceAttributes attributes = null;
            if (!(tempAttributes instanceof ResourceAttributes)) {
                attributes = new ResourceAttributes(tempAttributes);
            } else {
                attributes = (ResourceAttributes) tempAttributes;
            }
            long lastModified2 = attributes.getLastModified();
            long contentLength2 = attributes.getContentLength();
            return (lastModified == lastModified2) && (contentLength == contentLength2);
        } catch (NamingException e) {
            return false;
        }
    }

    /**
     * Load entry into cache.
     */
    protected boolean cacheLoad(CacheEntry entry) {
        if (cache == null)
            return false;

        String name = entry.name;

        // Retrieve missing info

        // Retrieving attributes
        if (entry.attributes == null) {
            try {
                Attributes attributes = dirContext.getAttributes(entry.name);
                if (!(attributes instanceof ResourceAttributes)) {
                    entry.attributes = new ResourceAttributes(attributes);
                } else {
                    entry.attributes = (ResourceAttributes) attributes;
                }
            } catch (NamingException e) {
                return false;
            }
        }

        // Retriving object
        if ((entry.resource == null) && (entry.context == null)) {
            try {
                Object object = dirContext.lookup(name);
                if (object instanceof InputStream) {
                    entry.resource = new Resource((InputStream) object);
                } else if (object instanceof DirContext) {
                    entry.context = (DirContext) object;
                } else if (object instanceof Resource) {
                    entry.resource = (Resource) object;
                } else {
                    entry.resource = new Resource(new ByteArrayInputStream(object.toString().getBytes()));
                }
            } catch (NamingException e) {
                return false;
            }
        }

        // Load object content
        if ((entry.resource != null) && (entry.resource.getContent() == null)
          && (entry.attributes.getContentLength() >= 0) && (entry.attributes.getContentLength() < cacheObjectMaxSize)) {
            int length = (int) entry.attributes.getContentLength();
            InputStream is = null;
            try {
                is = entry.resource.streamContent();
                int pos = 0;
                byte[] b = new byte[length];
                while (pos < length) {
                    int n = is.read(b, pos, length - pos);
                    if (n < 0)
                        break;
                    pos = pos + n;
                }
                entry.resource.setContent(b);
            } catch (IOException e) {
                ; // Ignore
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    ; // Ignore
                }
            }
        }

        // Set timestamp
        entry.timestamp = System.currentTimeMillis() + cacheTTL;

        // Add new entry to cache
        cache.put(name, entry);

        return true;
    }

    /**
     * Remove entry from cache.
     */
    protected boolean cacheUnload(String name) {
        if (cache == null)
            return false;
        return (cache.remove(name) != null);
    }

    // ------------------------------------------------- CacheEntry Inner Class

    protected class CacheEntry {
        // ------------------------------------------------- Instance Variables

        long timestamp = -1;
        String name = null;
        ResourceAttributes attributes = null;
        Resource resource = null;
        DirContext context = null;

        // ----------------------------------------------------- Public Methods

        public void recycle() {
            timestamp = -1;
            name = null;
            attributes = null;
            resource = null;
            context = null;
        }

        public String toString() {
            return ("Cache entry: " + name + "\n"
              + "Attributes: " + attributes + "\n"
              + "Resource: " + resource + "\n"
              + "Context: " + context);
        }
    }
}
