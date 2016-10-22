package br.com.codeminimal.minimojs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XMethod {

	public static enum WEBMethod {
		GET, POST
	}

	String[] rolesAllowed() default {};

	String functionAllowed() default "";

	int cacheExpires() default 0;

	boolean loginRequired() default true;

	boolean transacted() default false;

	boolean upload() default false;

	String url() default "";

	XMethod.WEBMethod forceMethod() default XMethod.WEBMethod.POST;

	boolean useWebObjects() default false;

	boolean responseInOutputStream() default false;
}
