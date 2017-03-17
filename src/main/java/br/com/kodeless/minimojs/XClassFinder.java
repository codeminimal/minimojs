package br.com.kodeless.minimojs;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author sp00m
 * 
 */
public final class XClassFinder {

	private final static char DOT = '.';
	private final static char SLASH = '/';
	private final static String CLASS_SUFFIX = ".class";
	private final static String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the given '%s' package exists?";

	public final static List<Class<?>> find(final String scannedPackage, Class<? extends Annotation> annot) {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final String scannedPath = scannedPackage.replace(DOT, SLASH);
		final Enumeration<URL> resources;
		try {
			resources = classLoader.getResources(scannedPath);
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format(BAD_PACKAGE_ERROR, scannedPath, scannedPackage), e);
		}
		final List<Class<?>> classes = new LinkedList<Class<?>>();
		while (resources.hasMoreElements()) {
			final File file = new File(resources.nextElement().getFile());
			List<Class<?>> classList = find(file, scannedPackage);

			if (annot != null) {
				for (Class<?> c : classList) {
					if (c.getAnnotation(annot) != null) {
						classes.add(c);
					}
				}
			} else {
				classes.addAll(classList);
			}

		}
		return classes;
	}

	public final static List<Class<?>> find(final String scannedPackage) {
		return find(scannedPackage, null);
	}

	private final static List<Class<?>> find(final File file, final String scannedPackage) {
		final List<Class<?>> classes = new LinkedList<Class<?>>();
		int index = file.getAbsolutePath().lastIndexOf(scannedPackage.replace(".", "/"));
		final String resource = file.getAbsolutePath().substring(index);
		if (file.isDirectory()) {
			for (File nestedFile : file.listFiles()) {
				classes.addAll(find(nestedFile, scannedPackage));
			}
		} else if (resource.endsWith(CLASS_SUFFIX)) {
			final int beginIndex = 0;
			final int endIndex = resource.length() - CLASS_SUFFIX.length();
			final String className = resource.substring(beginIndex, endIndex).replace("/", ".");
			try {
				classes.add(Class.forName(className));
			} catch (ClassNotFoundException ignore) {
			}
		}
		return classes;
	}

}