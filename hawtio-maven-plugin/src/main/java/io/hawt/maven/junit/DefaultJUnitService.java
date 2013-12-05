/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hawt.maven.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class DefaultJUnitService implements JUnitService {

    @Override
    public List<Method> findTestMethods(Class clazz) throws Exception {

        // first find annotations
        Class ann = clazz.getClassLoader().loadClass("org.junit.Test");
        List<Method> annotations = findMethodsWithAnnotation(clazz, ann, false);

        // if no annotations then find by testXXX naming pattern
        List<Method> names = findMethodsWithName(clazz, "test*");

        List<Method> answer = new ArrayList<Method>();
        for (Method method : annotations) {
            if (!answer.contains(method)) {
                answer.add(method);
            }
        }
        for (Method method : names) {
            if (!answer.contains(method)) {
                answer.add(method);
            }
        }

        return answer;
    }

    @Override
    public List<Method> filterTestMethods(List<Method> methods, String filter) {

        boolean wildcard = filter != null && filter.endsWith("*");
        if (wildcard) {
            filter = filter.substring(0, filter.length() - 1);
        }

        List<Method> result = new ArrayList<Method>();
        if (filter != null) {
            for (Method method : methods) {
                if (wildcard && method.getName().startsWith(filter)) {
                    result.add(method);
                } else if (method.getName().equals(filter)) {
                    result.add(method);
                }
            }
        } else {
            result.addAll(methods);
        }

        return result;

    }

    private static List<Method> findMethodsWithName(Class<?> type, String namePattern) {
        List<Method> answer = new ArrayList<Method>();
        do {
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                if (matchPattern(method.getName(), namePattern)) {
                    answer.add(method);
                }
            }
            type = type.getSuperclass();
        } while (type != null);
        return answer;
    }

    private static List<Method> findMethodsWithAnnotation(Class<?> type,
                                                          Class<? extends Annotation> annotationType,
                                                          boolean checkMetaAnnotations) {
        List<Method> answer = new ArrayList<Method>();
        do {
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                if (hasAnnotation(method, annotationType, checkMetaAnnotations)) {
                    answer.add(method);
                }
            }
            type = type.getSuperclass();
        } while (type != null);
        return answer;
    }

    /**
     * Checks if a Class or Method are annotated with the given annotation
     *
     * @param elem                 the Class or Method to reflect on
     * @param annotationType       the annotation type
     * @param checkMetaAnnotations check for meta annotations
     * @return true if annotations is present
     */
    private static boolean hasAnnotation(AnnotatedElement elem, Class<? extends Annotation> annotationType,
                                         boolean checkMetaAnnotations) {
        if (elem.isAnnotationPresent(annotationType)) {
            return true;
        }
        if (checkMetaAnnotations) {
            for (Annotation a : elem.getAnnotations()) {
                for (Annotation meta : a.annotationType().getAnnotations()) {
                    if (meta.annotationType().getName().equals(annotationType.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean matchPattern(String name, String pattern) {
        if (name == null || pattern == null) {
            return false;
        }

        if (name.equals(pattern)) {
            // exact match
            return true;
        }

        if (matchWildcard(name, pattern)) {
            return true;
        }

        if (matchRegex(name, pattern)) {
            return true;
        }

        // no match
        return false;
    }

    private static boolean matchWildcard(String name, String pattern) {
        // we have wildcard support in that hence you can match with: file* to match any file endpoints
        if (pattern.endsWith("*") && name.startsWith(pattern.substring(0, pattern.length() - 1))) {
            return true;
        }
        return false;
    }

    private static boolean matchRegex(String name, String pattern) {
        // match by regular expression
        try {
            if (name.matches(pattern)) {
                return true;
            }
        } catch (PatternSyntaxException e) {
            // ignore
        }
        return false;
    }

}