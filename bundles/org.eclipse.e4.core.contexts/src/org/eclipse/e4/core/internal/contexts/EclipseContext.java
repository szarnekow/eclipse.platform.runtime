/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.contexts;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.internal.contexts.osgi.ContextDebugHelper;

/**
 * This implementation assumes that all contexts are of the class EclipseContext. The external
 * methods of it are exposed via IEclipseContext.
 */
public class EclipseContext implements IEclipseContext {

	/**
	 * A context key identifying the parent context.
	 */
	public static final String PARENT = "parentContext"; //$NON-NLS-1$

	/**
	 * A context key (value "debugString") identifying a value to use in debug statements for a
	 * context. A computed value can be used to embed more complex information in debug statements.
	 */
	public static final String DEBUG_STRING = "debugString"; //$NON-NLS-1$

	static class Scheduled {

		public TrackableComputationExt runnable;
		public ContextChangeEvent event;

		public Scheduled(TrackableComputationExt runnable, ContextChangeEvent event) {
			this.runnable = runnable;
			this.event = event;
		}

		public int hashCode() {
			return 31 * (31 + event.hashCode()) + runnable.hashCode();
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Scheduled other = (Scheduled) obj;
			if (!event.equals(other.event))
				return false;
			return runnable.equals(other.runnable);
		}
	}

	private Map<String, HashSet<Computation>> listeners = Collections.synchronizedMap(new HashMap<String, HashSet<Computation>>(10, 0.8f));
	private Map<String, ValueComputation> localValueComputations = Collections.synchronizedMap(new HashMap<String, ValueComputation>());
	private Set<Computation> activeRATs = new HashSet<Computation>();

	final protected Map<String, Object> localValues = Collections.synchronizedMap(new HashMap<String, Object>());

	private ArrayList<String> modifiable;

	private List<Computation> waiting; // list of Computations; null for all non-root entries

	private Set<WeakReference<EclipseContext>> children = new HashSet<WeakReference<EclipseContext>>();

	private Set<IContextDisposalListener> notifyOnDisposal = new HashSet<IContextDisposalListener>();

	static private ThreadLocal<Stack<Computation>> currentComputation = new ThreadLocal<Stack<Computation>>();

	/**
	 * A context key (value "activeChildContext") that identifies another {@link IEclipseContext}
	 * that is a child of the context. The meaning of active is up to the application.
	 */
	public static final String ACTIVE_CHILD = "activeChildContext"; //$NON-NLS-1$

	static private final IEclipseContextDebugger debugAddOn = ContextDebugHelper.getDebugger();

	public EclipseContext(IEclipseContext parent) {
		setParent(parent);
		if (parent == null)
			waiting = Collections.synchronizedList(new ArrayList<Computation>());
		if (debugAddOn != null)
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.CONSTRUCTED, null);
	}

	final static private Set<EclipseContext> noChildren = new HashSet<EclipseContext>(0);

	public Set<EclipseContext> getChildren() {
		Set<EclipseContext> result;
		synchronized (children) {
			if (children.size() == 0)
				return noChildren;
			result = new HashSet<EclipseContext>(children.size());
			for (Iterator<WeakReference<EclipseContext>> i = children.iterator(); i.hasNext();) {
				EclipseContext referredContext = i.next().get();
				if (referredContext == null) {
					i.remove();
					continue;
				}
				result.add(referredContext);
			}
		}
		return result;
	}

	public boolean containsKey(String name) {
		trackAccess(name);
		return containsKey(name, false);
	}

	public boolean containsKey(String name, boolean localOnly) {
		if (isSetLocally(name))
			return true;
		if (localOnly)
			return false;
		EclipseContext parent = getParent();
		if (parent != null && parent.containsKey(name, localOnly))
			return true;
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.e4.core.services.context.IEclipseContext#dispose()
	 */
	public void dispose() {
		// dispose of child contexts first
		for (EclipseContext childContext : getChildren()) {
			childContext.dispose();
		}

		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.DISPOSE, null, null, null);
		Set<Scheduled> scheduled = new LinkedHashSet<Scheduled>();
		Set<Computation> allComputations = getListeners();
		listeners.clear();
		allComputations.addAll(activeRATs);
		activeRATs.clear();
		for (Computation computation : allComputations) {
			computation.handleInvalid(event, scheduled);
		}
		processScheduled(scheduled);

		synchronized (notifyOnDisposal) {
			for (IContextDisposalListener listener : notifyOnDisposal) {
				listener.disposed(this);
			}
		}

		localValueComputations.clear();

		// if this was the parent's active child, deactivate it
		EclipseContext parent = getParent();
		if (parent != null) {
			if (this == parent.getActiveChild())
				parent.set(ACTIVE_CHILD, null);
		}

		localValues.clear();

		if (parent != null)
			parent.removeChild(this);

		if (debugAddOn != null)
			debugAddOn.notify(this, IEclipseContextDebugger.EventType.DISPOSED, null);
	}

	public Object get(String name) {
		trackAccess(name);
		return internalGet(this, name, false);
	}

	public Object getLocal(String name) {
		trackAccess(name);
		return internalGet(this, name, true);
	}

	public Object internalGet(EclipseContext originatingContext, String name, boolean local) {
		if (this == originatingContext) {
			ValueComputation valueComputation = localValueComputations.get(name);
			if (valueComputation != null)
				return valueComputation.get();
		}

		Object result = null;
		// 1. try for local value
		if (localValues.containsKey(name)) {
			result = localValues.get(name);
			if (result == null)
				return null;
		} else
			result = lookup(name, originatingContext);

		// if we found something, compute the concrete value and return
		if (result != null) {
			if (result instanceof IContextFunction) {
				ValueComputation valueComputation = new ValueComputation(name, originatingContext, ((IContextFunction) result));
				// do calculations before adding listeners
				result = valueComputation.get();
				originatingContext.localValueComputations.put(name, valueComputation);
			}
			return result;
		}
		// 3. delegate to parent
		if (!local) {
			IEclipseContext parent = (IEclipseContext) localValues.get(PARENT);
			if (parent != null) {
				return ((EclipseContext) parent).internalGet(originatingContext, name, local);
			}
		}
		return null;
	}

	/**
	 * The given name has been modified or removed in this context. Invalidate all local value
	 * computations and listeners that depend on this name.
	 */
	public void invalidate(String name, int eventType, Object oldValue, Set<Scheduled> scheduled) {
		ContextChangeEvent event = new ContextChangeEvent(this, eventType, null, name, oldValue);
		ValueComputation computation = localValueComputations.get(name);
		if (computation != null) {
			if (computation.shouldRemove(event)) {
				localValueComputations.remove(name);
				Collection<HashSet<Computation>> allListeners = listeners.values();
				for (HashSet<Computation> group : allListeners) {
					group.remove(computation);
				}
			}
			computation.handleInvalid(event, scheduled);
		}
		HashSet<Computation> namedComputations = listeners.get(name);
		if (namedComputations != null) {
			for (Computation listener : namedComputations) {
				listener.handleInvalid(event, scheduled);
			}
		}

		// invalidate this name in child contexts
		for (EclipseContext childContext : getChildren()) {
			childContext.invalidate(name, eventType, oldValue, scheduled);
		}
	}

	private boolean isSetLocally(String name) {
		return localValues.containsKey(name);
	}

	public void remove(String name) {
		if (isSetLocally(name)) {
			Object oldValue = localValues.remove(name);
			Set<Scheduled> scheduled = new LinkedHashSet<Scheduled>();
			invalidate(name, ContextChangeEvent.REMOVED, oldValue, scheduled);
			processScheduled(scheduled);
		}
	}

	public void runAndTrack(final RunAndTrack runnable) {
		TrackableComputationExt computation = new TrackableComputationExt(runnable, this);
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.INITIAL, null, null, null);
		boolean result = computation.update(event);
		if (result)
			activeRATs.add(computation);
	}

	public void removeRAT(Computation computation) {
		activeRATs.remove(computation);
		// also remove from listeners
		Collection<HashSet<Computation>> allListeners = listeners.values();
		for (HashSet<Computation> group : allListeners) {
			group.remove(computation);
		}
	}

	protected void processScheduled(Set<Scheduled> scheduledList) {
		for (Iterator<Scheduled> i = scheduledList.iterator(); i.hasNext();) {
			Scheduled scheduled = i.next();
			scheduled.runnable.update(scheduled.event);
		}
	}

	public void set(String name, Object value) {
		if (PARENT.equals(name)) {
			setParent((IEclipseContext) value);
			return;
		}
		boolean containsKey = localValues.containsKey(name);
		Object oldValue = localValues.put(name, value);
		if (!containsKey || value != oldValue) {
			Set<Scheduled> scheduled = new LinkedHashSet<Scheduled>();
			invalidate(name, ContextChangeEvent.ADDED, oldValue, scheduled);
			processScheduled(scheduled);
		}
	}

	public void modify(String name, Object value) {
		Set<Scheduled> scheduled = new LinkedHashSet<Scheduled>();
		if (!internalModify(name, value, scheduled))
			set(name, value);
		processScheduled(scheduled);
	}

	public boolean internalModify(String name, Object value, Set<Scheduled> scheduled) {
		boolean containsKey = localValues.containsKey(name);
		if (containsKey) {
			if (!checkModifiable(name)) {
				String tmp = "Variable " + name + " is not modifiable in the context " + toString(); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IllegalArgumentException(tmp);
			}
			Object oldValue = localValues.put(name, value);
			if (value != oldValue)
				invalidate(name, ContextChangeEvent.ADDED, oldValue, scheduled);
			return true;
		}

		EclipseContext parent = getParent();
		if (parent != null)
			return parent.internalModify(name, value, scheduled);
		return false;
	}

	public EclipseContext getParent() {
		return (EclipseContext) localValues.get(PARENT);
	}

	public void setParent(IEclipseContext parent) {
		EclipseContext parentContext = (EclipseContext) localValues.get(PARENT);
		if (parent == parentContext)
			return; // no-op
		if (parentContext != null)
			parentContext.removeChild(this);
		Set<Scheduled> scheduled = new LinkedHashSet<Scheduled>();
		handleReparent((EclipseContext) parent, scheduled);
		localValues.put(PARENT, parent);
		if (parent != null)
			((EclipseContext) parent).addChild(this);
		processScheduled(scheduled);
		return;
	}

	/**
	 * Returns a string representation of this context for debugging purposes only.
	 */
	public String toString() {
		Object debugString = localValues.get(DEBUG_STRING);
		return debugString instanceof String ? ((String) debugString) : "Anonymous Context"; //$NON-NLS-1$
	}

	private void trackAccess(String name) {
		Stack<Computation> current = getCalculatedComputations();
		if (current.isEmpty())
			return;
		Computation computation = current.peek(); // only track in the top-most one
		if (computation == null)
			return;
		addDependency(name, computation);
	}

	public void addDependency(String name, Computation computation) {
		HashSet<Computation> nameListeners = listeners.get(name);
		if (nameListeners == null) {
			nameListeners = new HashSet<Computation>(30, 0.75f);
			listeners.put(name, nameListeners);
		}
		nameListeners.add(computation);
	}

	public void declareModifiable(String name) {
		if (name == null)
			return;
		if (modifiable == null)
			modifiable = new ArrayList<String>(3);
		modifiable.add(name);
		if (localValues.containsKey(name))
			return;
		localValues.put(name, null);
	}

	private boolean checkModifiable(String name) {
		if (modifiable == null)
			return false;
		for (Iterator<String> i = modifiable.iterator(); i.hasNext();) {
			String candidate = i.next();
			if (candidate.equals(name))
				return true;
		}
		return false;
	}

	public void removeListenersTo(Object object) {
		if (object == null)
			return;
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.UNINJECTED, new Object[] {object}, null, null);
		Set<Computation> comps = getListeners();
		for (Computation computation : comps) {
			if (computation instanceof TrackableComputationExt)
				((TrackableComputationExt) computation).update(event);
		}
	}

	public Set<Computation> getListeners() {
		Collection<HashSet<Computation>> collection = listeners.values();
		Set<Computation> comps = new HashSet<Computation>();

		for (HashSet<Computation> tmp : collection) {
			comps.addAll(tmp);
		}
		return comps;
	}

	private void handleReparent(EclipseContext newParent, Set<Scheduled> scheduled) {
		// TBD should we lock waiting list while doing reparent?
		// Add "boolean inReparent" on the root context and process right away?
		processWaiting();
		// 1) everybody who depends on me: I need to collect combined list of names injected
		Set<String> usedNames = new HashSet<String>();
		collectDependentNames(usedNames);

		// 2) for each used name:
		for (Iterator<String> i = usedNames.iterator(); i.hasNext();) {
			String name = i.next();
			if (localValues.containsKey(name))
				continue; // it is a local value
			Object oldValue = get(name);
			Object newValue = (newParent != null) ? newParent.get(name) : null;
			if (oldValue != newValue)
				invalidate(name, ContextChangeEvent.ADDED, oldValue, scheduled);
		}

		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.ADDED, null, null, null);
		for (Computation computation : localValueComputations.values()) {
			Collection<HashSet<Computation>> allListeners = listeners.values();
			for (HashSet<Computation> group : allListeners) {
				group.remove(computation);
			}
			computation.handleInvalid(event, scheduled);
		}
		localValueComputations.clear();
	}

	private void collectDependentNames(Set<String> usedNames) {
		Set<String> tmp = listeners.keySet(); // clone internal name list
		usedNames.addAll(tmp);

		for (EclipseContext childContext : getChildren()) {
			childContext.collectDependentNames(usedNames);
		}
	}

	public void processWaiting() {
		// traverse to the root node
		EclipseContext parent = getParent();
		if (parent != null) {
			parent.processWaiting();
			return;
		}
		if (waiting == null)
			return;
		// create update notifications
		Computation[] ls = waiting.toArray(new Computation[waiting.size()]);
		waiting.clear();
		ContextChangeEvent event = new ContextChangeEvent(this, ContextChangeEvent.UPDATE, null, null, null);
		for (int i = 0; i < ls.length; i++) {
			if (ls[i] instanceof TrackableComputationExt)
				((TrackableComputationExt) ls[i]).update(event);
		}
	}

	public void addWaiting(Computation cp) {
		// traverse to the root node
		EclipseContext parent = getParent();
		if (parent != null) {
			parent.addWaiting(cp);
			return;
		}
		if (waiting == null) // could happen on re-parent
			waiting = Collections.synchronizedList(new ArrayList<Computation>());
		waiting.add(cp);
	}

	protected EclipseContext getRoot() {
		EclipseContext current = this;
		EclipseContext root;
		do {
			root = current;
			current = current.getParent();
		} while (current != null);
		return root;
	}

	public void addChild(EclipseContext childContext) {
		synchronized (children) {
			children.add(new WeakReference<EclipseContext>(childContext));
		}
	}

	public void removeChild(EclipseContext childContext) {
		synchronized (children) {
			for (Iterator<WeakReference<EclipseContext>> i = children.iterator(); i.hasNext();) {
				EclipseContext referredContext = i.next().get();
				if (referredContext == null) {
					i.remove();
					continue;
				}
				if (referredContext == childContext) {
					i.remove();
					return;
				}
			}
		}
	}

	public <T> T get(Class<T> clazz) {
		return clazz.cast(get(clazz.getName()));
	}

	public boolean containsKey(Class<?> clazz) {
		return containsKey(clazz.getName());
	}

	public <T> void set(Class<T> clazz, T value) {
		set(clazz.getName(), value);
	}

	public void remove(Class<?> clazz) {
		remove(clazz.getName());
	}

	public <T> T getLocal(Class<T> clazz) {
		return clazz.cast(getLocal(clazz.getName()));
	}

	public <T> void modify(Class<T> clazz, T value) {
		modify(clazz.getName(), value);
	}

	public void declareModifiable(Class<?> clazz) {
		declareModifiable(clazz.getName());
	}

	public IEclipseContext createChild() {
		return new EclipseContext(this); // strategies are not inherited
	}

	public IEclipseContext createChild(String name) {
		IEclipseContext result = createChild();
		result.set(DEBUG_STRING, name);
		return result;
	}

	public void notifyOnDisposal(IContextDisposalListener listener) {
		synchronized (notifyOnDisposal) {
			notifyOnDisposal.add(listener);
		}
	}

	public IEclipseContext getActiveChild() {
		trackAccess(ACTIVE_CHILD);
		return (EclipseContext) internalGet(this, ACTIVE_CHILD, true);
	}

	public IEclipseContext getActiveLeaf() {
		IEclipseContext activeContext = this;
		IEclipseContext child = getActiveChild();
		while (child != null) {
			activeContext = child;
			child = child.getActiveChild();
		}
		return activeContext;
	}

	public void activate() {
		EclipseContext parent = getParent();
		if (parent == null)
			return;
		if (this == parent.getActiveChild())
			return;
		parent.set(ACTIVE_CHILD, this);
	}

	public void activateBranch() {
		for (IEclipseContext i = this; i != null; i = i.getParent()) {
			i.activate();
		}
	}

	public void deactivate() {
		EclipseContext parent = getParent();
		if (parent == null)
			return;
		if (this != parent.getActiveChild())
			return; // this is not an active context; return 
		parent.set(ACTIVE_CHILD, null);
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> localData() {
		Map<String, Object> result = new HashMap<String, Object>(localValues.size());
		for (String string : localValues.keySet()) {
			Object value = localValues.get(string);
			if (value instanceof IContextFunction)
				continue;
			result.put(string, value);
		}
		return result;
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> localContextFunction() {
		Map<String, Object> result = new HashMap<String, Object>(localValues.size());
		for (String string : localValues.keySet()) {
			Object value = localValues.get(string);
			if (value instanceof IContextFunction)
				result.put(string, value);
		}
		return result;
	}

	// This method is for debug only, do not use externally
	public Map<String, Object> cachedCachedContextFunctions() {
		Map<String, Object> result = new HashMap<String, Object>(localValueComputations.size());
		for (String string : localValueComputations.keySet()) {
			ValueComputation vc = localValueComputations.get(string);
			if (vc != null)
				result.put(string, vc.get());
		}
		return result;
	}

	// This method is for debug only, do not use externally
	public Set<String> getRawListenerNames() {
		Set<String> tmp = listeners.keySet(); // clone internal name list
		Set<String> usedNames = new HashSet<String>(tmp.size());
		usedNames.addAll(tmp);
		return usedNames;
	}

	// This method is for debug only, do not use externally
	public Set<Computation> getListeners(String name) {
		HashSet<Computation> tmp = listeners.get(name);
		if (tmp == null)
			return null;
		Set<Computation> result = new HashSet<Computation>(tmp.size());
		result.addAll(tmp);
		return result;
	}

	static public Stack<Computation> getCalculatedComputations() {
		Stack<Computation> current = currentComputation.get();
		if (current == null) {
			current = new Stack<Computation>();
			currentComputation.set(current);
		}
		return current;
	}

	public void pushComputation(Computation comp) {
		Stack<Computation> current = getCalculatedComputations();
		current.push(comp);
	}

	public void popComputation(Computation comp) {
		Stack<Computation> current = getCalculatedComputations();
		Computation ended = current.pop();
		if (ended != comp)
			throw new IllegalArgumentException("Internal error: Invalid nested computation processing"); //$NON-NLS-1$
	}

	/**
	 * This method can be overriden to provide additional source for the requested data.
	 * The override is expected to take care of initiating dynamic updates.
	 */
	protected Object lookup(String name, EclipseContext originatingContext) {
		return null;
	}
}
