package com.draagon.timers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Wraps all the facade method calls with a MethodTimer
 * @author Doug
 */
public class MethodTimerHandler implements InvocationHandler {

	private Class<?> interfaceClass = null;
	private Object facade = null;

	public MethodTimerHandler( Object facade, Class<?> interfaceClass ) {
		this.facade = facade;
		this.interfaceClass = interfaceClass;
	}

	public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
		MethodTimer mt = new MethodTimer( interfaceClass, method.getName() );
		try {
			return method.invoke( facade, args );
		} catch( InvocationTargetException e ) {
			throw e.getTargetException();
		} finally {
			mt.done();
		}
	}

	public static Object wrapProxy( Object facade, Class<?> interfaceClass ) {
		
		if ( facade == null ) return null;
		
		// If the requested class is not an interface, then don't wrap it in timers
		if ( !interfaceClass.isInterface() ) {
			return facade;
		}

		// It's an interface, so wrap it up.
		MethodTimerHandler handler = new MethodTimerHandler( facade, interfaceClass );
		Class<?> [] interfacesArray = new Class[] {interfaceClass};
		return Proxy.newProxyInstance( interfaceClass.getClassLoader(), interfacesArray, handler );		
	}
}	

